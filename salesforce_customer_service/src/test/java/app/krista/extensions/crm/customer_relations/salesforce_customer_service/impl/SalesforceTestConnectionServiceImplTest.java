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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceSdkFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.oauth.core.OAuth2AccessContext;
import app.krista.oauth.providers.salesforce.SalesforceOAuth2Sdk;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesforceTestConnectionServiceImpl}.
 *
 * <p>The class routes {@code testConnection(attrs, flow)} to per-flow strategies.
 * Everything reachable WITHOUT a live Salesforce HTTP call is covered here:</p>
 * <ul>
 *   <li>flow validation (null / blank / unknown)</li>
 *   <li>CLIENT_CREDENTIALS / PASSWORD_GRANT operator-token probe (success, failure,
 *       MustAuthorizeException rethrow) — the token acquisition is on the mocked
 *       {@link SalesforceTokenProvider}, so no network occurs.</li>
 *   <li>AUTH_CODE — input validation, secret resolution from KV, credential-rotation
 *       eviction, existing-RT success, popup-URL fallback, persist failure, URL-build
 *       failure, and MAE rethrow. The SDK is mocked so no network occurs.</li>
 *   <li>AUTH_CODE_QUICK — missing-email rejection, credential-load failure, popup-URL
 *       fallback, and the UNVERIFIABLE soft-pass (no access token in the returned
 *       context, so the identity probe never issues an HTTP call).</li>
 * </ul>
 *
 * <h3>Paths deliberately skipped (require a real HTTP call that cannot be mocked)</h3>
 * <p>The AUTH_CODE_QUICK identity MATCH and MISMATCH outcomes call
 * {@code verifyEnteredIdentity -> fetchIdentity -> httpGetJson}, which performs a live
 * {@code java.net.http.HttpClient} GET against the Salesforce userinfo/Chatter endpoints.
 * Those two outcomes (and the private static {@code httpGetJson}/{@code fetchIdentity}
 * helpers they drive) are not unit-testable without a real network and are intentionally
 * not exercised. The UNVERIFIABLE branch IS covered because it short-circuits before any
 * HTTP call when the access token is absent.</p>
 */
public class SalesforceTestConnectionServiceImplTest {

    private static final String INVOKER_ID = "INV_test";
    private static final String ROUTING_URL = "https://test.krista.ai/extension/api/tok";
    private static final String SF_ROUTING = "https://sf.example";

    @Mock
    private SalesforceTokenProviderFactory providerFactory;
    @Mock
    private SalesforceCustomerServiceAttributesStore attributesStore;
    @Mock
    private SalesforceCustomerServiceSdkFactory sdkFactory;
    @Mock
    private AuthorizationContext authorizationContext;
    @Mock
    private Invoker invoker;
    @Mock
    private RoutingInfo routingInfo;
    @Mock
    private SalesforceTokenProvider tokenProvider;
    @Mock
    private SalesforceOAuth2Sdk sdk;
    @Mock
    private OAuth2AccessContext accessContext;

    private SalesforceTestConnectionServiceImpl service;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(invoker.getInvokerId()).thenReturn(INVOKER_ID);
        service = new SalesforceTestConnectionServiceImpl(
                providerFactory, attributesStore, sdkFactory, authorizationContext, invoker);
    }

    private static JsonObject parse(String result) {
        return JsonParser.parseString(result).getAsJsonObject();
    }

    /** Advanced (private-app) attrs with the given fields; routingUrl fixed. */
    private static SalesforceCustomerServiceAttributes attrs(String clientId, String secret) {
        return new SalesforceCustomerServiceAttributes(
                clientId, secret, null, null, null, null, false, SF_ROUTING);
    }

    /** Advanced attrs carrying an email (9-arg overload). */
    private static SalesforceCustomerServiceAttributes attrsWithEmail(String clientId, String secret, String email) {
        return new SalesforceCustomerServiceAttributes(
                clientId, secret, null, null, null, null, false, SF_ROUTING, email);
    }

    // ── flow validation ────────────────────────────────────────────────────────────────

    @Test
    public void testConnection_nullFlow_returnsBlankFlowFailure() {
        String result = service.testConnection(attrs("cid", "csec"), null);
        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Flow identifier must not be blank.", r.get("errorMessage").getAsString());
    }

    @Test
    public void testConnection_blankFlow_returnsBlankFlowFailure() {
        String result = service.testConnection(attrs("cid", "csec"), "   ");
        assertFalse(parse(result).get("isSuccess").getAsBoolean());
    }

    @Test
    public void testConnection_unknownFlow_returnsUnsupportedFailure() {
        String result = service.testConnection(attrs("cid", "csec"), "JWT_BEARER");
        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Flow 'JWT_BEARER' is not supported by this extension.",
                r.get("errorMessage").getAsString());
    }

    // ── CLIENT_CREDENTIALS / PASSWORD_GRANT (admin token probe) ──────────────────────────

    @Test
    public void testConnection_clientCredentials_success() {
        when(providerFactory.create(any())).thenReturn(tokenProvider);
        // getSalesforceTokenForAdmin() is void; default mock does nothing => success.

        String result = service.testConnection(attrs("cid", "csec"), "CLIENT_CREDENTIALS");

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
        verify(tokenProvider).getSalesforceTokenForAdmin();
    }

    @Test
    public void testConnection_passwordGrant_success() {
        when(providerFactory.create(any())).thenReturn(tokenProvider);

        String result = service.testConnection(attrs("cid", "csec"), "PASSWORD_GRANT");

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
    }

    @Test
    public void testConnection_clientCredentials_tokenFailure_mapsExceptionMessage() {
        when(providerFactory.create(any())).thenReturn(tokenProvider);
        doThrow(new RuntimeException("invalid_client")).when(tokenProvider).getSalesforceTokenForAdmin();

        String result = service.testConnection(attrs("cid", "csec"), "CLIENT_CREDENTIALS");

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("invalid_client", r.get("errorMessage").getAsString());
    }

    @Test(expected = MustAuthorizeException.class)
    public void testConnection_clientCredentials_mustAuthorize_isRethrown() {
        MustAuthorizeException mae = mock(MustAuthorizeException.class);
        when(providerFactory.create(any())).thenReturn(tokenProvider);
        doThrow(mae).when(tokenProvider).getSalesforceTokenForAdmin();

        service.testConnection(attrs("cid", "csec"), "CLIENT_CREDENTIALS");
    }

    // ── AUTH_CODE validation ─────────────────────────────────────────────────────────────

    @Test
    public void testConnection_authCode_missingClientId_returnsConsumerKeyRequired() {
        // clientId null but secret non-null -> no public-config fallback, clientId stays null.
        String result = service.testConnection(attrs(null, "csec"), "AUTH_CODE");
        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Consumer Key is required for Authorization Code flow.",
                r.get("errorMessage").getAsString());
    }

    @Test
    public void testConnection_authCode_missingEmail_returnsConsumerEmailRequired() {
        String result = service.testConnection(attrs("cid", "csec"), "AUTH_CODE");
        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Please enter the Consumer Email before testing User Sign-In.",
                r.get("errorMessage").getAsString());
    }

    @Test
    public void testConnection_authCode_missingSecretAndNoStored_returnsConsumerSecretRequired() {
        // secret null, email present -> resolution path loads KV; store returns null.
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(null);

        String result = service.testConnection(attrsWithEmail("cid", null, "admin@example.com"), "AUTH_CODE");

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Consumer Secret is required for Authorization Code flow.",
                r.get("errorMessage").getAsString());
        verify(attributesStore).loadByInvokerId(INVOKER_ID);
    }

    // ── AUTH_CODE flow branches ──────────────────────────────────────────────────────────

    @Test
    public void testConnection_authCode_existingRefreshTokenValid_returnsSuccess() {
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(null); // first setup, no eviction
        when(sdkFactory.newSdk(any())).thenReturn(sdk);
        when(sdk.acquireForUser(anyString())).thenReturn(accessContext); // no throw => RT valid

        String result = service.testConnection(attrsWithEmail("cid", "csec", "admin@example.com"), "AUTH_CODE");

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
        verify(attributesStore).save(any(SalesforceCustomerServiceAttributes.class), eq(INVOKER_ID), eq("AUTH_CODE"));
        verify(sdk).acquireForUser("admin@example.com");
    }

    @Test
    public void testConnection_authCode_noRefreshToken_returnsPopupUrlWithPromptLogin() {
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(null);
        when(sdkFactory.newSdk(any())).thenReturn(sdk);
        when(sdk.acquireForUser(anyString())).thenThrow(new RuntimeException("no refresh token"));
        when(sdk.buildAuthorizationUrl(anyString(), any()))
                .thenReturn("https://login.salesforce.com/services/oauth2/authorize?client_id=cid");

        String result = service.testConnection(attrsWithEmail("cid", "csec", "admin@example.com"), "AUTH_CODE");

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertTrue(r.has("url"));
        assertTrue(r.get("url").getAsString().contains("prompt=login"));
    }

    @Test
    public void testConnection_authCode_secretResolvedFromStored_returnsSuccess() {
        // Incoming secret null -> resolved from KV; then RT valid.
        SalesforceCustomerServiceAttributes stored = attrs("cid", "realSecret");
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(stored);
        when(sdkFactory.newSdk(any())).thenReturn(sdk);
        when(sdk.acquireForUser(anyString())).thenReturn(accessContext);

        String result = service.testConnection(attrsWithEmail("cid", null, "admin@example.com"), "AUTH_CODE");

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
    }

    @Test
    public void testConnection_authCode_credentialsRotated_stillReturnsSuccess() {
        // Stored attrs with a different clientId -> exercises the rotation-detection branch.
        SalesforceCustomerServiceAttributes stored = attrs("oldClientId", "csec");
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(stored);
        when(sdkFactory.newSdk(any())).thenReturn(sdk);
        when(sdk.acquireForUser(anyString())).thenReturn(accessContext);

        String result = service.testConnection(attrsWithEmail("newClientId", "csec", "admin@example.com"), "AUTH_CODE");

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
    }

    @Test
    public void testConnection_authCode_persistFailure_returnsPrepareFailure() {
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(null);
        doThrow(new RuntimeException("kv down"))
                .when(attributesStore).save(any(SalesforceCustomerServiceAttributes.class), anyString(), anyString());

        String result = service.testConnection(attrsWithEmail("cid", "csec", "admin@example.com"), "AUTH_CODE");

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertTrue(r.get("errorMessage").getAsString()
                .startsWith("Failed to prepare credentials for authorization:"));
    }

    @Test
    public void testConnection_authCode_authorizeUrlBuildFailure_returnsFailure() {
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(null);
        when(sdkFactory.newSdk(any())).thenReturn(sdk);
        when(sdk.acquireForUser(anyString())).thenThrow(new RuntimeException("no rt"));
        when(sdk.buildAuthorizationUrl(anyString(), any())).thenThrow(new RuntimeException("bad url"));

        String result = service.testConnection(attrsWithEmail("cid", "csec", "admin@example.com"), "AUTH_CODE");

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertTrue(r.get("errorMessage").getAsString().startsWith("Failed to build authorization URL:"));
    }

    @Test(expected = MustAuthorizeException.class)
    public void testConnection_authCode_mustAuthorizeOnAcquire_isRethrown() {
        MustAuthorizeException mae = mock(MustAuthorizeException.class);
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(null);
        when(sdkFactory.newSdk(any())).thenReturn(sdk);
        when(sdk.acquireForUser(anyString())).thenThrow(mae);

        service.testConnection(attrsWithEmail("cid", "csec", "admin@example.com"), "AUTH_CODE");
    }

    // ── AUTH_CODE_QUICK ──────────────────────────────────────────────────────────────────

    @Test
    public void testConnection_quick_missingEmail_returnsUserEmailRequired() {
        // Advanced attrs with no email -> Quick Setup requires an admin email.
        String result = service.testConnection(attrs("cid", "csec"), "AUTH_CODE_QUICK");

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Please enter the Salesforce User Email before testing the Quick Setup connection.",
                r.get("errorMessage").getAsString());
        verify(attributesStore, never()).save(any(SalesforceCustomerServiceAttributes.class), anyString(), anyString());
    }

    @Test
    public void testConnection_quick_publicCredentialLoadFails_returnsLoadFailure() {
        // Email present but invoker.getRoutingInfo() is unstubbed (null) -> createPublic throws
        // inside the service and is mapped to a friendly failure.
        String result = service.testConnection(
                attrsWithEmail("cid", "csec", "admin@example.com"), "AUTH_CODE_QUICK");

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertTrue(r.get("errorMessage").getAsString().startsWith("Quick Setup credentials could not be loaded:"));
    }

    @Test
    public void testConnection_quick_noRefreshToken_returnsPopupUrl() {
        when(invoker.getRoutingInfo()).thenReturn(routingInfo);
        when(routingInfo.getRoutingURL(any(), any())).thenReturn(ROUTING_URL);
        when(sdkFactory.newSdk(any())).thenReturn(sdk);
        when(sdk.acquireForUser(anyString())).thenThrow(new RuntimeException("no rt"));
        when(sdk.buildAuthorizationUrl(anyString(), any()))
                .thenReturn("https://login.salesforce.com/services/oauth2/authorize?client_id=public");

        String result = service.testConnection(
                attrsWithEmail("cid", "csec", "admin@example.com"), "AUTH_CODE_QUICK");

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertTrue(r.has("url"));
        assertTrue(r.get("url").getAsString().contains("prompt=login"));
    }

    @Test
    public void testConnection_quick_refreshOkButIdentityUnverifiable_softPassSuccess() {
        when(invoker.getRoutingInfo()).thenReturn(routingInfo);
        when(routingInfo.getRoutingURL(any(), any())).thenReturn(ROUTING_URL);
        when(sdkFactory.newSdk(any())).thenReturn(sdk);
        when(sdk.acquireForUser(anyString())).thenReturn(accessContext);
        // No access token -> verifyEnteredIdentity short-circuits to UNVERIFIABLE (no HTTP call).
        when(accessContext.accessToken()).thenReturn(null);

        String result = service.testConnection(
                attrsWithEmail("cid", "csec", "admin@example.com"), "AUTH_CODE_QUICK");

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
    }
}
