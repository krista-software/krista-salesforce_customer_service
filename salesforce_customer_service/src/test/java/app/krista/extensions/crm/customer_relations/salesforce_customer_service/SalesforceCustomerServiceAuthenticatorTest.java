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
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;
import com.github.scribejava.core.oauth.OAuth20Service;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test class for SalesforceCustomerServiceAuthenticator.
 * Tests OAuth2 authentication flow, callback handling, and authorization responses.
 * 
 * Test Coverage Areas:
 * - Constructor initialization
 * - Scheme and protocol support
 * - Authenticated account ID extraction from callbacks
 * - Service authorization
 * - Attribute fields
 * - Must authenticate responses
 * - Must authorize responses (with and without authContextId)
 * - Error handling and edge cases
 */
public class SalesforceCustomerServiceAuthenticatorTest {

    private static final String USER_ID = "test_user_123";
    private static final String AUTH_CONTEXT_ID = "auth_context_456";
    private static final String STATE_WITH_CONTEXT = USER_ID + "#" + AUTH_CONTEXT_ID;
    private static final String AUTHORIZATION_URL = "https://login.salesforce.com/services/oauth2/authorize?client_id=test";
    private static final String CALLBACK_PATH = "/salesforcecustomerservice/callback";

    @Mock
    private SalesforceCustomerServiceAttributes mockAttributes;

    @Mock
    private SalesforceCustomerServiceAttributesStore mockAttributesStore;

    @Mock
    private OAuth20Service mockOAuthService;

    @Mock
    private HttpRequest mockHttpRequest;

    @Mock
    private MustAuthenticateException mockMustAuthenticateException;

    @Mock
    private MustAuthorizeException mockMustAuthorizeException;

    @Mock
    private ProtoRequest mockProtoRequest;

    private SalesforceCustomerServiceAuthenticator authenticator;

    /**
     * Set up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Configure default mock behaviors
        when(mockAttributes.getOauthService()).thenReturn(mockOAuthService);
        when(mockOAuthService.getAuthorizationUrl(anyString())).thenReturn(AUTHORIZATION_URL);
        
        authenticator = new SalesforceCustomerServiceAuthenticator(mockAttributes, mockAttributesStore);
    }

    // ========== Constructor Tests ==========

    /**
     * Test constructor with valid dependencies.
     */
    @Test
    public void testConstructor_WithValidDependencies_Success() {
        // Act
        SalesforceCustomerServiceAuthenticator auth = new SalesforceCustomerServiceAuthenticator(
                mockAttributes, mockAttributesStore);

        // Assert
        assertNotNull("Authenticator should be created successfully", auth);
    }

    /**
     * Test constructor with null attributes.
     */
    @Test
    public void testConstructor_WithNullAttributes_Success() {
        // Act
        SalesforceCustomerServiceAuthenticator auth = new SalesforceCustomerServiceAuthenticator(
                null, mockAttributesStore);

        // Assert
        assertNotNull("Authenticator should be created even with null attributes", auth);
    }

    // ========== Scheme and Protocol Tests ==========

    /**
     * Test getScheme returns null (OAuth uses redirection, not scheme-based auth).
     */
    @Test
    public void testGetScheme_ReturnsNull() {
        // Act
        String scheme = authenticator.getScheme();

        // Assert
        assertNull("Scheme should be null for OAuth redirection", scheme);
    }

    /**
     * Test getSupportedProtocols returns empty set.
     */
    @Test
    public void testGetSupportedProtocols_ReturnsEmptySet() {
        // Act
        Set<String> protocols = authenticator.getSupportedProtocols();

        // Assert
        assertNotNull("Protocols set should not be null", protocols);
        assertTrue("Protocols set should be empty", protocols.isEmpty());
    }

    // ========== Authenticated Account ID Tests ==========

    /**
     * Test getAuthenticatedAccountId with valid callback request.
     */
    @Test
    public void testGetAuthenticatedAccountId_WithValidCallback_ReturnsUserId() throws IOException {
        // Arrange
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("state", USER_ID);
        
        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com" + CALLBACK_PATH));
        when(mockHttpRequest.getQueryParameters()).thenReturn(queryParams);

        // Act
        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        // Assert
        assertEquals("Should extract user ID from state parameter", USER_ID, accountId);
        verify(mockHttpRequest, times(1)).bufferBody();
    }

    /**
     * Test getAuthenticatedAccountId with state containing authContextId.
     */
    @Test
    public void testGetAuthenticatedAccountId_WithAuthContextInState_ReturnsUserId() throws IOException {
        // Arrange
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("state", STATE_WITH_CONTEXT);
        
        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com" + CALLBACK_PATH));
        when(mockHttpRequest.getQueryParameters()).thenReturn(queryParams);

        // Act
        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        // Assert
        assertEquals("Should extract user ID from state (before #)", USER_ID, accountId);
    }

    /**
     * Test getAuthenticatedAccountId with non-callback path.
     */
    @Test
    public void testGetAuthenticatedAccountId_WithNonCallbackPath_ReturnsNull() {
        // Arrange
        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com/other/path"));

        // Act
        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        // Assert
        assertNull("Should return null for non-callback paths", accountId);
    }

    /**
     * Test getAuthenticatedAccountId with IOException during buffer.
     */
    @Test
    public void testGetAuthenticatedAccountId_WithIOException_ReturnsNull() throws IOException {
        // Arrange
        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com" + CALLBACK_PATH));
        doThrow(new IOException("Buffer error")).when(mockHttpRequest).bufferBody();

        // Act
        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        // Assert
        assertNull("Should return null when IOException occurs", accountId);
    }

    /**
     * Test getAuthenticatedAccountId with RuntimeException.
     */
    @Test
    public void testGetAuthenticatedAccountId_WithRuntimeException_ReturnsNull() {
        // Arrange
        when(mockHttpRequest.getUri()).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        // Assert
        assertNull("Should return null when RuntimeException occurs", accountId);
    }

    /**
     * Test getAuthenticatedAccountId with null state parameter.
     */
    @Test
    public void testGetAuthenticatedAccountId_WithNullState_ReturnsNull() throws IOException {
        // Arrange
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        
        when(mockHttpRequest.getUri()).thenReturn(URI.create("http://test.com" + CALLBACK_PATH));
        when(mockHttpRequest.getQueryParameters()).thenReturn(queryParams);

        // Act
        String accountId = authenticator.getAuthenticatedAccountId(mockHttpRequest);

        // Assert
        assertNull("Should return null when state parameter is missing", accountId);
    }

    // ========== Service Authorization Tests ==========

    /**
     * Test setServiceAuthorization always returns false.
     */
    @Test
    public void testSetServiceAuthorization_AlwaysReturnsFalse() {
        // Act
        boolean result1 = authenticator.setServiceAuthorization("test");
        boolean result2 = authenticator.setServiceAuthorization(null);
        boolean result3 = authenticator.setServiceAuthorization("");

        // Assert
        assertFalse("Should return false for any input", result1);
        assertFalse("Should return false for null", result2);
        assertFalse("Should return false for empty string", result3);
    }

    // ========== Attribute Fields Tests ==========

    /**
     * Test getAttributeFields returns empty map.
     */
    @Test
    public void testGetAttributeFields_ReturnsEmptyMap() {
        // Act
        Map<String, NamedField> fields = authenticator.getAttributeFields();

        // Assert
        assertNotNull("Attribute fields map should not be null", fields);
        assertTrue("Attribute fields map should be empty", fields.isEmpty());
    }

    // ========== Must Authenticate Response Tests ==========

    /**
     * Test getMustAuthenticateResponse with ProtoRequest returns null.
     */
    @Test
    public void testGetMustAuthenticateResponse_WithProtoRequest_ReturnsNull() {
        // Act
        ProtoResponse response = authenticator.getMustAuthenticateResponse(
                mockMustAuthenticateException, mockProtoRequest);

        // Assert
        assertNull("Should return null for ProtoRequest version", response);
    }

    /**
     * Test getMustAuthenticateResponse without ProtoRequest returns null.
     */
    @Test
    public void testGetMustAuthenticateResponse_WithoutProtoRequest_ReturnsNull() {
        // Act
        RequestAuthenticator.AuthorizationResponse response = 
                authenticator.getMustAuthenticateResponse(mockMustAuthenticateException);

        // Assert
        assertNull("Should return null for non-ProtoRequest version", response);
    }

    // ========== Must Authorize Response Tests ==========

    /**
     * Test getMustAuthorizeResponse with ProtoRequest returns null.
     */
    @Test
    public void testGetMustAuthorizeResponse_WithProtoRequest_ReturnsNull() {
        // Act
        ProtoResponse response = authenticator.getMustAuthorizeResponse(
                mockMustAuthorizeException, mockProtoRequest);

        // Assert
        assertNull("Should return null for ProtoRequest version", response);
    }

    /**
     * Test getMustAuthorizeResponse without authContextId.
     */
    @Test
    public void testGetMustAuthorizeResponse_WithoutAuthContextId_ReturnsAuthorizationUrl() {
        // Arrange
        NamedValuedField userIdField = mock(NamedValuedField.class);
        when(userIdField.getValue()).thenReturn(USER_ID);
        when(userIdField.getName()).thenReturn("userId");
        when(mockMustAuthorizeException.getDetails()).thenReturn(List.of(userIdField));

        // Act
        RequestAuthenticator.AuthorizationResponse response =
                authenticator.getMustAuthorizeResponse(mockMustAuthorizeException);

        // Assert
        assertNotNull("Response should not be null", response);
        verify(mockOAuthService, times(1)).getAuthorizationUrl(USER_ID);
        verify(mockAttributes, times(1)).getOauthService();
    }

    /**
     * Test getMustAuthorizeResponse with authContextId.
     */
    @Test
    public void testGetMustAuthorizeResponse_WithAuthContextId_LoadsAttributesFromStore() {
        // Arrange
        NamedValuedField userIdField = mock(NamedValuedField.class);
        when(userIdField.getValue()).thenReturn(USER_ID);
        when(userIdField.getName()).thenReturn("userId");

        NamedValuedField authContextField = mock(NamedValuedField.class);
        when(authContextField.getValue()).thenReturn(AUTH_CONTEXT_ID);
        when(authContextField.getName()).thenReturn("authContextId");

        SalesforceCustomerServiceAttributes contextAttributes = mock(SalesforceCustomerServiceAttributes.class);
        OAuth20Service contextOAuthService = mock(OAuth20Service.class);

        when(mockMustAuthorizeException.getDetails()).thenReturn(List.of(userIdField, authContextField));
        when(mockAttributesStore.load(AUTH_CONTEXT_ID)).thenReturn(contextAttributes);
        when(contextAttributes.getOauthService()).thenReturn(contextOAuthService);
        when(contextOAuthService.getAuthorizationUrl(STATE_WITH_CONTEXT)).thenReturn(AUTHORIZATION_URL);

        // Act
        RequestAuthenticator.AuthorizationResponse response =
                authenticator.getMustAuthorizeResponse(mockMustAuthorizeException);

        // Assert
        assertNotNull("Response should not be null", response);
        verify(mockAttributesStore, times(1)).load(AUTH_CONTEXT_ID);
        verify(contextOAuthService, times(1)).getAuthorizationUrl(STATE_WITH_CONTEXT);
        verify(contextAttributes, times(1)).getOauthService();
    }
}

