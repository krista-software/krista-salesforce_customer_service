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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.api;

import app.krista.extension.executor.Invoker;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceSdkFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceSaveConfigurationImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceTestConnectionServiceImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.extensions.util.EventHandler;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthenticationResource}, focused on the post-microsoft-auth-sdk-1.1.0-migration
 * fix commits the user iterated on in this branch:
 *
 * <ul>
 *   <li><b>CRIT-005</b> — {@code resolveMaskedSecrets} (static helper): converts the masked
 *       placeholder back to the real KV-stored secret BEFORE the credentials are persisted.
 *       Without this, saveCredentials persists {@code "••••••••"} into KV and every
 *       subsequent OAuth call fails with {@code invalid_client}.</li>
 *   <li><b>CRIT-009</b> (commit {@code 3ba4e04fdd}) — {@code /getCredentials} emits camelCase
 *       wire keys ({@code instanceUrl}, {@code userName}) matching the SDK-bundled theme JSON.
 *       Legacy snake_case / space-separated keys ({@code "Instance URL"}, {@code "username"})
 *       made the JS {@code populateFields} miss the values, so the field rendered empty even
 *       though KV had them.</li>
 *   <li>{@code /getCredentials} Quick Setup branch: clientId and clientSecret MUST NOT leave
 *       the server — only the sandbox toggle + admin email are surfaced (CRIT-022).</li>
 *   <li>{@code /getCredentials} guards: no saved flow / flow mismatch / no attrs → {@code "{}"}.</li>
 * </ul>
 *
 * <p>The full {@code /callback} and {@code /v3/oauth/callback} flows require deep SDK mocking
 * and are exercised by the {@code SalesforceCustomerServiceAuthenticatorTest} +
 * integration paths instead. This test focuses on the static helper plus the deterministic
 * branches of {@code /getCredentials}.</p>
 */
public class AuthenticationResourceTest {

    private static final String INVOKER_ID = "salesforce-customer-service";
    private static final String STORED_CLIENT_SECRET = "realStoredClientSecret";
    private static final String STORED_PASSWORD = "realStoredPassword";
    private static final String STORED_TOKEN = "realStoredSecurityToken";
    private static final String MASK = AuthenticationResource.STORED_SECRET_MASK;

    private static final Gson GSON = new Gson();

    @Mock
    private SalesforceCustomerServiceAttributesStore attributesStore;
    @Mock
    private SalesforceTokenProviderFactory providerFactory;
    @Mock
    private AuthorizationContext context;
    @Mock
    private AuthorizationListener authorizationListener;
    @Mock
    private EventHandler eventHandler;
    @Mock
    private SalesforceCustomerServiceSdkFactory sdkFactory;
    @Mock
    private SalesforceSaveConfigurationImpl saveConfigurationImpl;
    @Mock
    private SalesforceTestConnectionServiceImpl testConnectionService;
    @Mock
    private Invoker invoker;
    @Mock
    private SalesforceCustomerServiceAttributes storedAttrs;

    private AuthenticationResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(invoker.getInvokerId()).thenReturn(INVOKER_ID);
        resource = new AuthenticationResource(
                attributesStore, providerFactory, context, authorizationListener, eventHandler,
                sdkFactory, saveConfigurationImpl, testConnectionService, invoker);
    }

    // ── STORED_SECRET_MASK constant ─────────────────────────────────────────────────

    @Test
    public void storedSecretMask_isExactlyEightBulletCharacters() {
        // The UI matches on this exact string; changing it breaks the mask handshake.
        assertEquals("••••••••", AuthenticationResource.STORED_SECRET_MASK);
        assertEquals(8, AuthenticationResource.STORED_SECRET_MASK.length());
    }

    // ── resolveMaskedSecrets — CRIT-005 ─────────────────────────────────────────────

    @Test
    public void resolveMaskedSecrets_nullAttrMap_returnsNull() {
        Map<String, Object> result = AuthenticationResource.resolveMaskedSecrets(
                null, attributesStore, INVOKER_ID);
        assertNull(result);
        verifyNoInteractions(attributesStore);
    }

    @Test
    public void resolveMaskedSecrets_nullStore_returnsAttrMapUnchanged() {
        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, MASK);

        Map<String, Object> result = AuthenticationResource.resolveMaskedSecrets(
                attrMap, null, INVOKER_ID);

        assertSame(attrMap, result);
        // Mask was NOT resolved (store was null) — caller's job to handle.
        assertEquals(MASK, result.get(SalesforceCustomerServiceAttributes.CLIENT_SECRET));
    }

    @Test
    public void resolveMaskedSecrets_nullInvokerId_returnsAttrMapUnchanged() {
        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, MASK);

        Map<String, Object> result = AuthenticationResource.resolveMaskedSecrets(
                attrMap, attributesStore, null);

        assertSame(attrMap, result);
        verifyNoInteractions(attributesStore);
    }

    @Test
    public void resolveMaskedSecrets_maskedClientSecret_replacedWithStoredValue() {
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(storedAttrs);
        when(storedAttrs.getClientSecret()).thenReturn(STORED_CLIENT_SECRET);

        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, MASK);
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_ID, "realClientId");

        AuthenticationResource.resolveMaskedSecrets(attrMap, attributesStore, INVOKER_ID);

        assertEquals("Mask must be replaced with the real stored secret — otherwise " +
                        "saveCredentials persists '••••••••' into KV (CRIT-005)",
                STORED_CLIENT_SECRET, attrMap.get(SalesforceCustomerServiceAttributes.CLIENT_SECRET));
        assertEquals("Non-secret fields must pass through unchanged",
                "realClientId", attrMap.get(SalesforceCustomerServiceAttributes.CLIENT_ID));
    }

    @Test
    public void resolveMaskedSecrets_maskedPasswordAndToken_bothReplacedFromOneStoreCall() {
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(storedAttrs);
        when(storedAttrs.getPassword()).thenReturn(STORED_PASSWORD);
        when(storedAttrs.getSecurityToken()).thenReturn(STORED_TOKEN);

        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put(SalesforceCustomerServiceAttributes.PASS_WORD, MASK);
        attrMap.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN, MASK);

        AuthenticationResource.resolveMaskedSecrets(attrMap, attributesStore, INVOKER_ID);

        assertEquals(STORED_PASSWORD,
                attrMap.get(SalesforceCustomerServiceAttributes.PASS_WORD));
        assertEquals(STORED_TOKEN,
                attrMap.get(SalesforceCustomerServiceAttributes.SECURITY_TOKEN));

        // Single KV read for both — store is loaded lazily once, not per-field.
        verify(attributesStore, times(1)).loadByInvokerId(INVOKER_ID);
    }

    @Test
    public void resolveMaskedSecrets_noStoredAttrs_leavesMaskInPlace() {
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(null);

        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, MASK);

        AuthenticationResource.resolveMaskedSecrets(attrMap, attributesStore, INVOKER_ID);

        // Caller's downstream code will fail with a clean "secret required" error rather than
        // silently persisting the mask.
        assertEquals(MASK, attrMap.get(SalesforceCustomerServiceAttributes.CLIENT_SECRET));
    }

    @Test
    public void resolveMaskedSecrets_corruptKvStoredSecretIsTheMask_wipesAttribute() {
        // Defensive: if an EARLIER version of saveCredentials wrote the mask back into KV,
        // we want to detect it and wipe the corrupt value rather than re-persist it.
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(storedAttrs);
        when(storedAttrs.getClientSecret()).thenReturn(MASK); // CORRUPT KV

        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, MASK);

        AuthenticationResource.resolveMaskedSecrets(attrMap, attributesStore, INVOKER_ID);

        assertEquals("Corrupt KV must be wiped to empty string so the downstream error is " +
                        "clean (\"secret required\") rather than invalid_client",
                "", attrMap.get(SalesforceCustomerServiceAttributes.CLIENT_SECRET));
    }

    @Test
    public void resolveMaskedSecrets_nonMaskValue_passesThroughUnchanged() {
        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, "freshSecretFromForm");

        AuthenticationResource.resolveMaskedSecrets(attrMap, attributesStore, INVOKER_ID);

        // Store must NOT be hit when no mask is present (avoids unnecessary KV read).
        verifyNoInteractions(attributesStore);
        assertEquals("freshSecretFromForm",
                attrMap.get(SalesforceCustomerServiceAttributes.CLIENT_SECRET));
    }

    // ── /getCredentials — CRIT-009 camelCase wire keys ─────────────────────────────

    @Test
    public void getCredentials_noSavedFlow_returnsEmptyJsonObject() {
        when(attributesStore.getSavedFlow(INVOKER_ID)).thenReturn(null);

        Response resp = resource.getCredentials("oauth", "AUTH_CODE");

        assertEquals(200, resp.getStatus());
        assertEquals("{}", resp.getEntity());
    }

    @Test
    public void getCredentials_flowMismatch_returnsEmptyJsonObject() {
        // Saved=CLIENT_CREDENTIALS but UI is asking for AUTH_CODE.
        when(attributesStore.getSavedFlow(INVOKER_ID)).thenReturn("CLIENT_CREDENTIALS");

        Response resp = resource.getCredentials("oauth", "AUTH_CODE");

        assertEquals(200, resp.getStatus());
        assertEquals("{}", resp.getEntity());
    }

    @Test
    public void getCredentials_savedFlowMatchesButNoAttrs_returnsEmptyJsonObject() {
        when(attributesStore.getSavedFlow(INVOKER_ID)).thenReturn("AUTH_CODE");
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(null);

        Response resp = resource.getCredentials("oauth", "AUTH_CODE");

        assertEquals(200, resp.getStatus());
        assertEquals("{}", resp.getEntity());
    }

    @Test
    public void getCredentials_authCode_emitsCamelCaseWireKeysAndMasksSecrets() {
        // CRIT-009: the UI's JS populateFields expects camelCase keys; legacy 'Instance URL'
        // and 'username' keys (the Java constant values) won't match and the form stays empty.
        when(attributesStore.getSavedFlow(INVOKER_ID)).thenReturn("AUTH_CODE");
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(storedAttrs);
        when(storedAttrs.getClientId()).thenReturn("3MVG9realClientId");
        when(storedAttrs.getClientSecret()).thenReturn(STORED_CLIENT_SECRET);
        when(storedAttrs.getInstanceUrl()).thenReturn("https://my.salesforce.com");
        when(storedAttrs.getUserName()).thenReturn("admin@example.com");
        when(storedAttrs.getPassword()).thenReturn(STORED_PASSWORD);
        when(storedAttrs.getSecurityToken()).thenReturn(STORED_TOKEN);
        when(storedAttrs.isSandbox()).thenReturn(true);

        Response resp = resource.getCredentials("oauth", "AUTH_CODE");
        assertEquals(200, resp.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = GSON.fromJson((String) resp.getEntity(), Map.class);

        // CamelCase wire keys (the fix).
        assertTrue("Wire payload must include camelCase 'instanceUrl' key — " +
                        "CRIT-009 / commit 3ba4e04fdd",
                body.containsKey("instanceUrl"));
        assertTrue("Wire payload must include camelCase 'userName' key — " +
                        "CRIT-009 / commit 3ba4e04fdd",
                body.containsKey("userName"));

        // Regression guard: legacy keys must NOT appear.
        assertFalse("Legacy 'Instance URL' (space) wire key must not appear",
                body.containsKey("Instance URL"));
        assertFalse("Legacy 'username' (lowercase) wire key must not appear",
                body.containsKey("username"));

        // Values: non-secrets pass through, secrets are masked.
        assertEquals("https://my.salesforce.com", body.get("instanceUrl"));
        assertEquals("admin@example.com", body.get("userName"));
        assertEquals("3MVG9realClientId", body.get("clientId"));
        assertEquals("Stored client secret must be masked, never returned in clear",
                MASK, body.get("clientSecret"));
        assertEquals("Stored password must be masked",
                MASK, body.get("password"));
        assertEquals("Stored security token must be masked",
                MASK, body.get("securityToken"));
        assertEquals(true, body.get("sandbox"));
        assertEquals("AUTH_CODE", body.get("savedFlow"));
    }

    @Test
    public void getCredentials_blankStoredSecret_omitsMaskAndReturnsEmptyString() {
        when(attributesStore.getSavedFlow(INVOKER_ID)).thenReturn("AUTH_CODE");
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(storedAttrs);
        when(storedAttrs.getClientId()).thenReturn("3MVG9");
        when(storedAttrs.getClientSecret()).thenReturn(""); // not stored
        when(storedAttrs.getPassword()).thenReturn(null);
        when(storedAttrs.getSecurityToken()).thenReturn(null);

        Response resp = resource.getCredentials("oauth", "AUTH_CODE");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = GSON.fromJson((String) resp.getEntity(), Map.class);

        // When nothing is stored, return empty string — UI then shows a blank field rather
        // than a misleading "stored secret" mask affordance.
        assertEquals("", body.get("clientSecret"));
        assertEquals("", body.get("password"));
        assertEquals("", body.get("securityToken"));
    }

    @Test
    public void getCredentials_quickSetup_returnsOnlyEmailSandboxAndFlowNoClientSecrets() {
        // CRIT-022: Quick Setup credentials are Krista-owned. clientId/clientSecret MUST
        // never leave the server.
        when(attributesStore.getSavedFlow(INVOKER_ID)).thenReturn("AUTH_CODE_QUICK");
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(storedAttrs);
        when(storedAttrs.getEmail()).thenReturn("admin@example.com");
        when(storedAttrs.isSandbox()).thenReturn(false);

        Response resp = resource.getCredentials("oauth", "AUTH_CODE_QUICK");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = GSON.fromJson((String) resp.getEntity(), Map.class);

        assertEquals("admin@example.com", body.get("email"));
        assertEquals(false, body.get("sandbox"));
        assertEquals("AUTH_CODE_QUICK", body.get("savedFlow"));

        // Critical negative assertions: NO clientId, NO clientSecret in the response.
        assertFalse("Quick Setup must never leak Krista-owned clientId to the browser",
                body.containsKey("clientId"));
        assertFalse("Quick Setup must never leak Krista-owned clientSecret to the browser",
                body.containsKey("clientSecret"));
        assertFalse("Quick Setup must not include userName field",
                body.containsKey("userName"));
        assertFalse("Quick Setup must not include password field",
                body.containsKey("password"));
        assertFalse("Quick Setup must not include securityToken field",
                body.containsKey("securityToken"));
    }

    @Test
    public void getCredentials_setsNoStoreCacheControlHeader() {
        when(attributesStore.getSavedFlow(INVOKER_ID)).thenReturn(null);

        Response resp = resource.getCredentials("oauth", "AUTH_CODE");
        // Cache-Control must be no-store — secrets/identity bound responses must not be cached.
        assertEquals("no-store", resp.getHeaderString("Cache-Control"));
    }
}
