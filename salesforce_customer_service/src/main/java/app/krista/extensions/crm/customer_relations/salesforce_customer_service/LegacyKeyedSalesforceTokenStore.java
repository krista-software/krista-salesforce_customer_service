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
import app.krista.oauth.core.token.KeyTranslatingTokenStore;

/**
 * ITokenStore adapter that translates the SDK's canonical token-store key format
 * ({@code sfv1/{accountId}/{hex hash}}) to the salesforce_customer_service legacy
 * key format ({@code {accountId}&{clientId}+{clientSecret}}).
 *
 * <p><b>Why this exists:</b> Before v1.1.0 of microsoft-auth-sdk, this extension
 * stored refresh tokens directly in {@link KeyValueStore} under the legacy key format.
 * After migrating to consume the SDK, the SDK's strategies compute canonical keys in
 * a different format. Without translation at the boundary, existing customers would
 * have to re-authorize after the upgrade because their tokens are stored at legacy
 * keys but the SDK looks them up at canonical keys.</p>
 *
 * <p>This adapter is per-request bound: each instance is constructed with the
 * (clientId, clientSecret) pair for one specific request. For multi-tenant cases
 * (different OAuth apps per request via {@code authContextId}), a fresh adapter is
 * built per request via {@code SalesforceCustomerServiceSdkFactory} (added in
 * a subsequent migration chunk).</p>
 *
 * <p>The {@link KeyTranslatingTokenStore} base class handles the JSON envelope vs
 * plain-string value-format fallback automatically: new writes go to a JSON envelope
 * (preserving the SDK's atomic structured storage invariant); reads accept both
 * JSON envelopes (post-SDK entries) and plain strings (legacy pre-SDK entries).</p>
 *
 * <p><b>Thread-safety:</b> Instances are immutable after construction (all fields final,
 * delegated methods rely on the thread-safety of the underlying {@link KeyValueStore}).
 * Safe to share across threads for the same (clientId, clientSecret) pair.</p>
 *
 * @since salesforce_customer_service 2.0.18 (microsoft-auth-sdk 1.1.0 migration)
 */
public class LegacyKeyedSalesforceTokenStore extends KeyTranslatingTokenStore {

    private final KeyValueStore kv;
    private final String clientId;
    private final String clientSecret;

    /**
     * Construct an adapter bound to one specific (clientId, clientSecret) credential pair.
     * For multi-tenant flows, construct a fresh adapter per request using that request's
     * resolved attributes.
     *
     * @param kv            the underlying Krista KeyValueStore (typically request-scoped)
     * @param clientId      the Salesforce Connected App client ID for THIS request
     * @param clientSecret  the Salesforce Connected App client secret for THIS request
     */
    public LegacyKeyedSalesforceTokenStore(KeyValueStore kv, String clientId, String clientSecret) {
        this.kv = kv;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Translates SDK canonical key {@code sfv1/{accountId}/{hash}} to the pre-2.0.18
     * extension-native format {@code {accountId}&{clientId}{clientSecret}}.
     *
     * <p><b>CRIT-031 fix (no '+' separator).</b> The original T-M2 implementation
     * inserted a {@code "+"} between {@code clientId} and {@code clientSecret}, which
     * did NOT match the actual pre-2.0.18 storage format used by
     * {@code AuthenticationResource.callback()} and
     * {@code SalesforceTokenProvider.getRefreshTokenKey()} — both of which concatenated
     * {@code clientId + clientSecret} with no separator. Customers upgrading from
     * pre-2.0.18 saw their refresh tokens become invisible because the new code looked
     * at a key that the old code never wrote. The {@code "+"} is removed here; legacy
     * RTs become readable in place. See {@link #readRaw(String)} for the orphaned-buggy-key
     * fallback that covers any RTs written by the broken adapter between v2.0.18-GA and
     * this fix.</p>
     *
     * <p>If the input doesn't start with {@code "sfv1/"}, it's used verbatim as the
     * accountId segment (defensive fallback — should not occur with the current SDK
     * strategies but protects against future SDK key-format changes).</p>
     */
    @Override
    protected String translateKey(String sdkKey) {
        String translated = extractAccountId(sdkKey) + "&" + clientId + clientSecret;
        System.out.println("[SF-DIAG] LegacyStore.translateKey: sdkKey=" + sdkKey +
                " → translated=" + translated +
                " (clientId=" + clientId +
                ", clientSecretLen=" + (clientSecret != null ? clientSecret.length() : -1) + ")");
        return translated;
    }

    @Override
    protected String readRaw(String translatedKey) {
        // CRIT-003 fix: Krista's KeyValueStore.get(key, Object.class) auto-deserializes
        // JSON strings into Java Maps. Calling .toString() on the Map produced
        // {token=X, metadata={...}} (Java Map format with `=`, NOT valid JSON `{"token":"X"}`).
        // KeyTranslatingTokenStore.tryParseEnvelope then failed to parse it, fell back to
        // "treat as legacy plain string", and passed the gibberish to Salesforce as the
        // refresh_token — SF responded `invalid_grant: expired access/refresh token`.
        //
        // Resolution: read as Object (Krista's preferred path, won't log ERROR), then
        // re-serialize Maps back to JSON via Gson so KeyTranslatingTokenStore can parse
        // the envelope. Strings (pre-SDK legacy entries) pass through unchanged.
        String value = materializeAsString(kv.get(translatedKey, Object.class));
        if (value != null) {
            System.out.println("[SF-DIAG] LegacyStore.readRaw: key=" + translatedKey +
                    " found=true valuePrefix=" + previewValue(value));
            return value;
        }

        // CRIT-031 transition fallback. Any RTs that were written by the broken T-M2
        // adapter between v2.0.18-GA and this fix live at the buggy slot
        // {accountId}&{clientId}+{clientSecret}. Read once and migrate to the corrected
        // key. One-shot per entry — after the move, normal reads at the corrected key
        // hit on the fast path. Cheap insurance for any QA / internal users who
        // authenticated against a pre-fix 2.0.18 build.
        String buggyKey = extractAccountId(translatedKey) + "&" + clientId + "+" + clientSecret;
        String stranded = materializeAsString(kv.get(buggyKey, Object.class));
        if (stranded != null) {
            kv.put(translatedKey, stranded);
            kv.remove(buggyKey);
            System.out.println("[SF-DIAG] CRIT-031 LegacyStore.readRaw: one-shot RT migration buggyKey="
                    + buggyKey + " → correctKey=" + translatedKey);
            return stranded;
        }

        System.out.println("[SF-DIAG] LegacyStore.readRaw: key=" + translatedKey + " found=false");
        return null;
    }

    /**
     * Coerces a raw KV value (String / Map / null) into a JSON string suitable for the
     * {@code KeyTranslatingTokenStore} envelope parser. Returns {@code null} if input is null.
     *
     * <p>Why we avoid {@code kv.get(key, String.class)}: when the stored value is a JSON
     * object, Krista's {@code RestKeyValueStore} throws and emits an ERROR-level log per
     * call. The exception was harmless (we caught it and fell back) but the log noise was
     * confusing during diagnostics. This Object-first path is silent in normal operation.</p>
     */
    private static String materializeAsString(Object raw) {
        if (raw == null) return null;
        if (raw instanceof String) return (String) raw;
        // KV auto-deserialized a JSON value into Map/List/etc. — re-serialize as JSON
        // so the SDK's envelope parser sees the original JSON form.
        return new com.google.gson.Gson().toJson(raw);
    }

    private static String previewValue(String value) {
        if (value == null) return null;
        return value.length() > 20 ? value.substring(0, 20) + "..." : value;
    }

    @Override
    protected void writeRaw(String translatedKey, String value) {
        System.out.println("[SF-DIAG] LegacyStore.writeRaw: key=" + translatedKey +
                " valueLen=" + (value != null ? value.length() : -1));
        kv.put(translatedKey, value);
    }

    @Override
    protected void deleteRaw(String translatedKey) {
        kv.remove(translatedKey);
    }

    /**
     * Extracts the accountId from an SDK canonical key {@code sfv1/{accountId}/{hash}}.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "sfv1/wsContact_abc/def123"} &rarr; {@code "wsContact_abc"}</li>
     *   <li>{@code "sfv1/user_123/abcd"} &rarr; {@code "user_123"}</li>
     *   <li>{@code "non-sdk-key"} &rarr; {@code "non-sdk-key"} (fallback)</li>
     * </ul>
     */
    private static String extractAccountId(String sdkKey) {
        if (sdkKey != null && sdkKey.startsWith("sfv1/")) {
            int slash = sdkKey.indexOf('/', 5);
            return slash > 0 ? sdkKey.substring(5, slash) : sdkKey.substring(5);
        }
        return sdkKey;
    }
}
