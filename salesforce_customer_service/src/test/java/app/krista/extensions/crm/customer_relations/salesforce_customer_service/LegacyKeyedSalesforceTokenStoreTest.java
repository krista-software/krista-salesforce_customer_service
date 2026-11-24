/*
 * Salesforce Customer Service Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.crm.customer_relations.salesforce_customer_service;

import app.krista.extensions.util.KeyValueStore;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LegacyKeyedSalesforceTokenStore}.
 *
 * <p>This adapter is the boundary that preserves multi-tenant token isolation across
 * the microsoft-auth-sdk 1.1.0 migration: it translates the SDK's canonical
 * {@code sfv1/{accountId}/{hash}} key format into the legacy
 * {@code {accountId}&{clientId}+{clientSecret}} format that existing customers'
 * KeyValueStore entries are keyed under. If this contract regresses, every existing
 * customer is forced to re-authorize on upgrade.</p>
 *
 * <h2>Coverage</h2>
 * <ul>
 *   <li>{@link #translateKey_sdkKeyWithHash_extractsAccountIdAndAppendsCreds()} — happy path</li>
 *   <li>{@link #translateKey_sdkKeyWithoutTrailingHash_extractsAccountIdRest()} — edge case</li>
 *   <li>{@link #translateKey_nonSdkKey_usesVerbatimAsAccountId()} — fallback for non-canonical keys</li>
 *   <li>{@link #translateKey_differentCredentials_produceDistinctTranslatedKeys()} — multi-tenant isolation</li>
 *   <li>{@link #readRaw_kvReturnsNull_returnsNull()} — empty store</li>
 *   <li>{@link #readRaw_kvReturnsString_returnsStringVerbatim()} — pre-SDK legacy entries</li>
 *   <li>{@link #readRaw_kvReturnsMap_reSerializesToJson()} — CRIT-003 fix: Krista's KV
 *       auto-deserializes JSON values to {@code Map}; we must re-serialize so the SDK's
 *       envelope parser sees the original JSON shape, not Java's Map.toString() output.</li>
 *   <li>{@link #writeRaw_delegatesToKeyValueStorePut()}</li>
 *   <li>{@link #deleteRaw_delegatesToKeyValueStoreRemove()}</li>
 * </ul>
 */
public class LegacyKeyedSalesforceTokenStoreTest {

    private static final String CLIENT_ID = "3MVG9testClientId";
    private static final String CLIENT_SECRET = "testSecret123";
    private static final String OTHER_CLIENT_ID = "3MVG9otherClientId";
    private static final String OTHER_CLIENT_SECRET = "REDACTED_SECRET";

    @Mock
    private KeyValueStore kv;

    private LegacyKeyedSalesforceTokenStore store;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        store = new LegacyKeyedSalesforceTokenStore(kv, CLIENT_ID, CLIENT_SECRET);
    }

    // ── translateKey ────────────────────────────────────────────────────────────────

    @Test
    public void translateKey_sdkKeyWithHash_extractsAccountIdAndAppendsCreds() {
        // CRIT-031: pre-2.0.18 KV writes concatenated clientId+clientSecret with NO
        // separator. The original T-M2 adapter inserted a '+' which silently broke the
        // upgrade contract. The corrected adapter matches the pre-2.0.18 wire format.
        String translated = store.translateKey("sfv1/wsContact_abc/def123hash");
        assertEquals("wsContact_abc&" + CLIENT_ID + CLIENT_SECRET, translated);
    }

    @Test
    public void translateKey_sdkKeyWithoutTrailingHash_extractsAccountIdRest() {
        // "sfv1/" prefix but no second slash — accountId is everything after the prefix.
        String translated = store.translateKey("sfv1/user_only");
        assertEquals("user_only&" + CLIENT_ID + CLIENT_SECRET, translated);
    }

    @Test
    public void translateKey_nonSdkKey_usesVerbatimAsAccountId() {
        // Defensive fallback for future SDK key-format changes.
        String translated = store.translateKey("legacyAccountId");
        assertEquals("legacyAccountId&" + CLIENT_ID + CLIENT_SECRET, translated);
    }

    @Test
    public void translateKey_userIdPrefixedAccountId_preservesUnderscores() {
        String translated = store.translateKey("sfv1/user_123/somehash");
        assertEquals("user_123&" + CLIENT_ID + CLIENT_SECRET, translated);
    }

    /**
     * Multi-tenant isolation: two adapters bound to different (clientId, clientSecret)
     * pairs MUST produce different translated keys for the same input SDK key. If this
     * regresses, two customers with different Connected Apps would alias to the same
     * KV entry — security bug.
     */
    @Test
    public void translateKey_differentCredentials_produceDistinctTranslatedKeys() {
        LegacyKeyedSalesforceTokenStore other =
                new LegacyKeyedSalesforceTokenStore(kv, OTHER_CLIENT_ID, OTHER_CLIENT_SECRET);

        String sameSdkKey = "sfv1/wsContact_shared/somehash";
        String translatedA = store.translateKey(sameSdkKey);
        String translatedB = other.translateKey(sameSdkKey);

        assertNotEquals("Different credentials must produce different translated keys",
                translatedA, translatedB);
        assertTrue("First adapter's key must include CLIENT_ID",
                translatedA.contains(CLIENT_ID));
        assertTrue("Second adapter's key must include OTHER_CLIENT_ID",
                translatedB.contains(OTHER_CLIENT_ID));
    }

    // ── readRaw ─────────────────────────────────────────────────────────────────────

    @Test
    public void readRaw_kvReturnsNull_returnsNull() {
        when(kv.get("translated", Object.class)).thenReturn(null);
        assertNull(store.readRaw("translated"));
    }

    @Test
    public void readRaw_kvReturnsString_returnsStringVerbatim() {
        // Pre-SDK legacy entries are plain refresh-token strings, not envelopes.
        when(kv.get("translated", Object.class)).thenReturn("legacyRefreshTokenString");
        assertEquals("legacyRefreshTokenString", store.readRaw("translated"));
    }

    /**
     * CRIT-003: Krista's RestKeyValueStore auto-deserializes JSON values to {@code Map}
     * when read via {@code get(key, Object.class)}. The adapter must re-serialize the Map
     * back to canonical JSON so the SDK's envelope parser (in the base class) sees the
     * original JSON shape — NOT {@code Map.toString()} output like
     * {@code "{token=X, metadata={...}}"} (which uses '=' instead of ':' and isn't valid JSON).
     */
    @Test
    public void readRaw_kvReturnsMap_reSerializesToJson() {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("token", "myRefreshToken");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("issuedAt", "2026-06-08T00:00:00Z");
        envelope.put("metadata", metadata);

        when(kv.get("translated", Object.class)).thenReturn(envelope);

        String roundTripped = store.readRaw("translated");
        assertNotNull(roundTripped);

        // Verify it parses back as JSON, not as Java Map.toString()
        Map<?, ?> parsed = new Gson().fromJson(roundTripped, Map.class);
        assertEquals("myRefreshToken", parsed.get("token"));
        assertNotNull("metadata should round-trip through JSON", parsed.get("metadata"));

        // Negative assertion: must NOT contain '=' (Java Map.toString separator)
        // — a regression to Map.toString() would put '=' between key and value.
        assertFalse("Re-serialized value must be JSON, not Map.toString()",
                roundTripped.contains("token=myRefreshToken"));
    }

    // ── writeRaw ────────────────────────────────────────────────────────────────────

    @Test
    public void writeRaw_delegatesToKeyValueStorePut() {
        store.writeRaw("translatedKey", "envelopeJson");
        verify(kv, times(1)).put("translatedKey", "envelopeJson");
        verifyNoMoreInteractions(kv);
    }

    @Test
    public void writeRaw_nullValue_stillDelegates() {
        // The base class' save() should not pass null, but the override must remain a pure
        // delegate — don't add filtering that the base class doesn't expect.
        store.writeRaw("translatedKey", null);
        verify(kv, times(1)).put("translatedKey", (Object) null);
    }

    // ── deleteRaw ───────────────────────────────────────────────────────────────────

    @Test
    public void deleteRaw_delegatesToKeyValueStoreRemove() {
        store.deleteRaw("translatedKey");
        verify(kv, times(1)).remove("translatedKey");
        verifyNoMoreInteractions(kv);
    }

    // ── Constructor ─────────────────────────────────────────────────────────────────

    @Test
    public void constructor_nullClientIdAndSecret_constructsButTranslatesWithNullSegments() {
        // The class permits null creds (defensive — caller may not have resolved attrs yet).
        // The translated key will literally contain the string "null" — caller's job to
        // avoid this state in production.
        LegacyKeyedSalesforceTokenStore nullCreds =
                new LegacyKeyedSalesforceTokenStore(kv, null, null);
        assertEquals("acct&nullnull", nullCreds.translateKey("sfv1/acct/hash"));
    }
}
