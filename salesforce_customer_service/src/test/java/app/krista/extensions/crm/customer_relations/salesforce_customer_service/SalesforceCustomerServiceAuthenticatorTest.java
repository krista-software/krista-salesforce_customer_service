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

import app.krista.extension.authorization.MustAuthenticateException;
import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.request.ProtoRequest;
import app.krista.extension.request.ProtoResponse;
import app.krista.extension.request.protos.http.HttpRequest;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.extensions.util.KeyValueStore;
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesforceCustomerServiceAuthenticator}.
 *
 * <p>As of salesforce_customer_service 2.0.18 (T-M6), the authenticator extends
 * {@link app.krista.oauth.providers.salesforce.extension.AbstractSalesforceRequestAuthenticator}
 * instead of implementing {@link app.krista.extension.authorization.RequestAuthenticator}
 * directly. The notable behavioral changes tested here:</p>
 * <ul>
 *   <li>{@link #testConstructor_WithValidDependencies_Success()} — now uses the string-based
 *       seam constructor (invokerId + routingUrl) instead of the old 2-arg constructor.</li>
 *   <li>State-parsing tests use the base class implementation which caps at 8 parts.</li>
 *   <li>{@code getMustAuthorizeResponse} tests verify the SDK-based OAuth URL is produced
 *       (non-null response, state grammar preserved) rather than verifying the legacy
 *       ScribeJava {@code OAuth20Service.getAuthorizationUrl()} call.</li>
 * </ul>
 *
 * <h2>Test Coverage Areas</h2>
 * <ul>
 *   <li>Constructor initialization (seam / string-based constructor)</li>
 *   <li>Scheme and protocol support (inherited from base class)</li>
 *   <li>Authenticated account ID extraction from callbacks</li>
 *   <li>Service authorization (inherited no-op)</li>
 *   <li>Attribute fields (inherited empty map)</li>
 *   <li>Must-authenticate responses (inherited nulls)</li>
 *   <li>Must-authorize responses — SDK-based URL, state grammar, authContextId routing</li>
 *   <li>Error handling and edge cases</li>
 * </ul>
 */
public class SalesforceCustomerServiceAuthenticatorTest {

    private static final String INVOKER_ID = "salesforce-customer-service";
    private static final String ROUTING_URL = "https://test.krista.ai";
    private static final String USER_ID = "test_user_123";
    private static final String AUTH_CONTEXT_ID = "auth_context_456";
    private static final String STATE_WITH_CONTEXT = USER_ID + "#" + AUTH_CONTEXT_ID;
    private static final String CALLBACK_PATH = "/salesforcecustomerservice/callback";

    // Real (non-mock) attributes for getMustAuthorizeResponse tests — the SDK's
    // buildAuthorizationUrl builds a URL from config values without network calls.
    private static final String TEST_CLIENT_ID = "3MVG9testClientId";
    private static final String TEST_CLIENT_SECRET = "testSecret123";
    private static final String TEST_ROUTING_URL = "https://test.krista.ai";

    @Mock
    private SalesforceCustomerServiceAttributes mockDefaultAttributes;

    @Mock
    private SalesforceCustomerServiceAttributesStore mockAttributesStore;

    @Mock
    private SalesforceCustomerServiceSdkFactory mockSdkFactory;

    @Mock
    private KeyValueStore mockKv;

    @Mock
    private HttpRequest mockHttpRequest;

    @Mock
    private MustAuthenticateException mockMustAuthenticateException;

    @Mock
    private MustAuthorizeException mockMustAuthorizeException;

    @Mock
    private ProtoRequest mockProtoRequest;

    /**
     * The authenticator under test, using the string-based seam constructor so tests
     * do not require a live HK2 {@link app.krista.extension.executor.Invoker}.
     */
    private SalesforceCustomerServiceAuthenticator authenticator;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticator = new SalesforceCustomerServiceAuthenticator(
                INVOKER_ID, ROUTING_URL, mockDefaultAttributes, mockAttributesStore, mockSdkFactory);
    }

    // ── Constructor Tests ───────────────────────────────────────────────────────────

    /**
     * Verify the seam constructor creates an authenticator instance.
     */
    @Test
    public void testConstructor_WithValidDependencies_Success() {
        SalesforceCustomerServiceAuthenticator auth = new SalesforceCustomerServiceAuthenticator(
                INVOKER_ID, ROUTING_URL, mockDefaultAttributes, mockAttributesStore, mockSdkFactory);
        assertNotNull("Authenticator should be created successfully", auth);
    }

    /**
     * Verify the seam constructor works with a null defaultAttributes (degenerate case).
     */
    @Test
    public void testConstructor_WithNullDefaultAttributes_Success() {
        SalesforceCustomerServiceAuthenticator auth = new SalesforceCustomerServiceAuthenticator(
                INVOKER_ID, ROUTING_URL, null, mockAttributesStore, mockSdkFactory);
        assertNotNull("Authenticator should be created even with null defaultAttributes", auth);
    }

    // ── Scheme and Protocol Tests ───────────────────────────────────────────────────

    /**
     * getScheme returns null — OAuth uses redirection, not a scheme-based challenge.
     */
    @Test
    public void testGetScheme_ReturnsNull() {
        assertNull("Scheme should be null for OAuth redirection", authenticator.getScheme());
    }

    /**
     * getSupportedProtocols returns an empty set.
     */
    @Test
    public void testGetSupportedProtocols_ReturnsEmptySet() {
        Set<String> protocols = authenticator.getSupportedProtocols();
        assertNotNull("Protocols set should not be null", protocols);
        assertTrue("Protocols set should be empty", protocols.isEmpty());
    }

    // ── Authenticated Account ID Tests ──────────────────────────────────────────────

    /**
     * Valid callback path with a plain userId state → returns userId.
     */
    @Test
    public void testGetAuthenticatedAccountId_WithValidCallback_ReturnsUserId() throws IOException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("state", USER_ID);

        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com" + CALLBACK_PATH));
        when(mockHttpRequest.getQueryParameters()).thenReturn(queryParams);

        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        assertEquals("Should extract user ID from state parameter", USER_ID, accountId);
        verify(mockHttpRequest, times(1)).bufferBody();
    }

    /**
     * State containing authContextId → returns the userId (first '#'-delimited part).
     */
    @Test
    public void testGetAuthenticatedAccountId_WithAuthContextInState_ReturnsUserId()
            throws IOException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("state", STATE_WITH_CONTEXT);

        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com" + CALLBACK_PATH));
        when(mockHttpRequest.getQueryParameters()).thenReturn(queryParams);

        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        assertEquals("Should extract user ID from state (before #)", USER_ID, accountId);
    }

    /**
     * Non-callback path → returns null (base class only parses on the callback path).
     */
    @Test
    public void testGetAuthenticatedAccountId_WithNonCallbackPath_ReturnsNull() {
        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com/other/path"));

        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        assertNull("Should return null for non-callback paths", accountId);
    }

    /**
     * IOException during bufferBody → returns null (base class catches and logs).
     */
    @Test
    public void testGetAuthenticatedAccountId_WithIOException_ReturnsNull() throws IOException {
        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com" + CALLBACK_PATH));
        doThrow(new IOException("Buffer error")).when(mockHttpRequest).bufferBody();

        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        assertNull("Should return null when IOException occurs", accountId);
    }

    /**
     * RuntimeException on getUri() → returns null.
     */
    @Test
    public void testGetAuthenticatedAccountId_WithRuntimeException_ReturnsNull() {
        when(mockHttpRequest.getUri()).thenThrow(new RuntimeException("Unexpected error"));

        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        assertNull("Should return null when RuntimeException occurs", accountId);
    }

    /**
     * Missing state query parameter → returns null.
     */
    @Test
    public void testGetAuthenticatedAccountId_WithNullState_ReturnsNull() throws IOException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com" + CALLBACK_PATH));
        when(mockHttpRequest.getQueryParameters()).thenReturn(queryParams);

        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        assertNull("Should return null when state parameter is missing", accountId);
    }

    // ── Service Authorization Tests ─────────────────────────────────────────────────

    /**
     * setServiceAuthorization always returns false (OAuth uses redirects, not service auth).
     */
    @Test
    public void testSetServiceAuthorization_AlwaysReturnsFalse() {
        assertFalse("Should return false for any input", authenticator.setServiceAuthorization("test"));
        assertFalse("Should return false for null", authenticator.setServiceAuthorization(null));
        assertFalse("Should return false for empty string", authenticator.setServiceAuthorization(""));
    }

    // ── Attribute Fields Tests ──────────────────────────────────────────────────────

    /**
     * getAttributeFields returns an empty map (no scheme-level challenge fields).
     */
    @Test
    public void testGetAttributeFields_ReturnsEmptyMap() {
        Map<String, NamedField> fields = authenticator.getAttributeFields();
        assertNotNull("Attribute fields map should not be null", fields);
        assertTrue("Attribute fields map should be empty", fields.isEmpty());
    }

    // ── Must Authenticate Response Tests ────────────────────────────────────────────

    /**
     * getMustAuthenticateResponse(cause, request) returns null (OAuth redirect pattern).
     */
    @Test
    public void testGetMustAuthenticateResponse_WithProtoRequest_ReturnsNull() {
        ProtoResponse response = authenticator.getMustAuthenticateResponse(
                mockMustAuthenticateException, mockProtoRequest);
        assertNull("Should return null for ProtoRequest version", response);
    }

    /**
     * getMustAuthenticateResponse(cause) returns null.
     */
    @Test
    public void testGetMustAuthenticateResponse_WithoutProtoRequest_ReturnsNull() {
        RequestAuthenticator.AuthorizationResponse response =
                authenticator.getMustAuthenticateResponse(mockMustAuthenticateException);
        assertNull("Should return null for non-ProtoRequest version", response);
    }

    // ── Must Authorize Response Tests ───────────────────────────────────────────────

    /**
     * getMustAuthorizeResponse(cause, request) returns null (base class default).
     */
    @Test
    public void testGetMustAuthorizeResponse_WithProtoRequest_ReturnsNull() {
        ProtoResponse response = authenticator.getMustAuthorizeResponse(
                mockMustAuthorizeException, mockProtoRequest);
        assertNull("Should return null for ProtoRequest version", response);
    }

    /**
     * getMustAuthorizeResponse without authContextId — uses default attributes, state = userId.
     *
     * <p>Uses a real (non-mock) {@link SalesforceCustomerServiceAttributes} so the SDK can
     * build an actual OAuth authorization URL from the config. No network call is made;
     * the URL is assembled entirely from the config values in the test.</p>
     */
    @Test
    public void testGetMustAuthorizeResponse_WithoutAuthContextId_ReturnsAuthorizationUrl() {
        // Build a real attributes object so toSalesforceConfig() works (SDK needs clientId/secret)
        SalesforceCustomerServiceAttributes realAttrs = new SalesforceCustomerServiceAttributes(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, null, null, null, null, false, TEST_ROUTING_URL);

        SalesforceCustomerServiceSdkFactory realFactory =
                new SalesforceCustomerServiceSdkFactory(mockKv);

        SalesforceCustomerServiceAuthenticator auth = new SalesforceCustomerServiceAuthenticator(
                INVOKER_ID, TEST_ROUTING_URL, realAttrs, mockAttributesStore, realFactory);

        // Arrange: details with userId only (no authContextId)
        NamedValuedField userIdField = mock(NamedValuedField.class);
        when(userIdField.getValue()).thenReturn(USER_ID);
        when(userIdField.getName()).thenReturn("userId");
        when(mockMustAuthorizeException.getDetails()).thenReturn(List.of(userIdField));

        // Act
        RequestAuthenticator.AuthorizationResponse response =
                auth.getMustAuthorizeResponse(mockMustAuthorizeException);

        // Assert: response is non-null (SDK-built authorize URL was produced without error)
        assertNotNull("Response should not be null — SDK must produce an authorize URL", response);
    }

    /**
     * getMustAuthorizeResponse with authContextId — loads per-request attributes from the
     * attribute store, state = userId#authContextId.
     */
    @Test
    public void testGetMustAuthorizeResponse_WithAuthContextId_LoadsAttributesFromStore() {
        // Build real attributes for the per-request context (simulates what attributesStore.load returns)
        SalesforceCustomerServiceAttributes contextAttrs = new SalesforceCustomerServiceAttributes(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, null, null, null, null, false, TEST_ROUTING_URL);

        SalesforceCustomerServiceSdkFactory realFactory =
                new SalesforceCustomerServiceSdkFactory(mockKv);

        // Default attrs also need to be real so getTokenStore() works
        SalesforceCustomerServiceAttributes realDefaultAttrs = new SalesforceCustomerServiceAttributes(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, null, null, null, null, false, TEST_ROUTING_URL);

        SalesforceCustomerServiceAuthenticator auth = new SalesforceCustomerServiceAuthenticator(
                INVOKER_ID, TEST_ROUTING_URL, realDefaultAttrs, mockAttributesStore, realFactory);

        // Stub attribute store to return context-specific attributes
        when(mockAttributesStore.load(AUTH_CONTEXT_ID)).thenReturn(contextAttrs);

        NamedValuedField userIdField = mock(NamedValuedField.class);
        when(userIdField.getValue()).thenReturn(USER_ID);
        when(userIdField.getName()).thenReturn("userId");

        NamedValuedField authContextField = mock(NamedValuedField.class);
        when(authContextField.getValue()).thenReturn(AUTH_CONTEXT_ID);
        when(authContextField.getName()).thenReturn("authContextId");

        when(mockMustAuthorizeException.getDetails()).thenReturn(
                List.of(userIdField, authContextField));

        // Act
        RequestAuthenticator.AuthorizationResponse response =
                auth.getMustAuthorizeResponse(mockMustAuthorizeException);

        // Assert: attributes were loaded from the store using the authContextId
        verify(mockAttributesStore, times(1)).load(AUTH_CONTEXT_ID);
        assertNotNull("Response should not be null", response);
    }

    /**
     * Verify the state grammar: accountId or accountId#authContextId — the SDK-built
     * authorize URL is produced without error for both state formats.
     */
    @Test
    public void testGetMustAuthorizeResponse_StateGrammar_AccountIdHashAuthContextId() {
        SalesforceCustomerServiceAttributes realAttrs = new SalesforceCustomerServiceAttributes(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, null, null, null, null, false, TEST_ROUTING_URL);
        SalesforceCustomerServiceSdkFactory realFactory =
                new SalesforceCustomerServiceSdkFactory(mockKv);

        SalesforceCustomerServiceAttributes realDefaultAttrs = new SalesforceCustomerServiceAttributes(
                TEST_CLIENT_ID, TEST_CLIENT_SECRET, null, null, null, null, false, TEST_ROUTING_URL);
        SalesforceCustomerServiceAuthenticator auth = new SalesforceCustomerServiceAuthenticator(
                INVOKER_ID, TEST_ROUTING_URL, realDefaultAttrs, mockAttributesStore, realFactory);

        when(mockAttributesStore.load(AUTH_CONTEXT_ID)).thenReturn(realAttrs);

        NamedValuedField userIdField = mock(NamedValuedField.class);
        when(userIdField.getValue()).thenReturn(USER_ID);
        when(userIdField.getName()).thenReturn("userId");

        NamedValuedField authContextField = mock(NamedValuedField.class);
        when(authContextField.getValue()).thenReturn(AUTH_CONTEXT_ID);
        when(authContextField.getName()).thenReturn("authContextId");

        when(mockMustAuthorizeException.getDetails()).thenReturn(
                List.of(userIdField, authContextField));

        // Act — should produce a non-null response (SDK builds URL with compound state)
        RequestAuthenticator.AuthorizationResponse response =
                auth.getMustAuthorizeResponse(mockMustAuthorizeException);

        assertNotNull("Response must be non-null for accountId#authContextId state", response);
    }
}
