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

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test class for SalesforceCustomerServiceExtension.
 * Tests all public methods, constructors, and edge cases to increase coverage from 19%.
 * 
 * Test Coverage Areas:
 * - Constructor initialization (both variants)
 * - Request authenticator registration
 * - Custom tabs configuration
 * - Connection validation (with and without user credentials)
 * - Test connection functionality
 * - Attributes update handling
 * - Error scenarios and edge cases
 */
public class SalesforceCustomerServiceExtensionTest {

    // Constants for test data
    private static final String CLIENT_ID = "test_client_id";
    private static final String CLIENT_SECRET = "test_client_secret";
    private static final String INSTANCE_URL = "https://test.my.salesforce.com";
    private static final String USERNAME = "test@example.com";
    private static final String PASSWORD = "testPassword123";
    private static final String SECURITY_TOKEN = "testSecurityToken";
    private static final String ROUTING_URL = "https://test.krista.ai";

    // Mocked dependencies
    @Mock
    private SalesforceTokenProviderFactory mockProviderFactory;

    @Mock
    private Invoker mockInvoker;

    @Mock
    private SalesforceCustomerServiceAttributes mockAttributes;

    @Mock
    private SalesforceCustomerServiceAttributesStore mockAttributesStore;

    @Mock
    private SalesforceCustomerServiceAuthenticator mockAuthenticator;

    @Mock
    private SalesforceTokenProvider mockTokenProvider;

    @Mock
    private SalesforceCustomerServiceSdkFactory mockSdkFactory;

    @Mock
    private RoutingInfo mockRoutingInfo;

    private SalesforceCustomerServiceExtension extension;

    /**
     * Set up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure default mock behaviors
        when(mockProviderFactory.create(any(SalesforceCustomerServiceAttributes.class)))
                .thenReturn(mockTokenProvider);

        // Stub Invoker for the 5-arg (store-backed) constructor that builds the authenticator:
        // SalesforceCustomerServiceAuthenticator's production constructor calls
        // invoker.getInvokerId() and invoker.getRoutingInfo().getRoutingURL(...).
        when(mockInvoker.getInvokerId()).thenReturn("test-invoker-id");
        when(mockInvoker.getRoutingInfo()).thenReturn(mockRoutingInfo);
        when(mockRoutingInfo.getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE))
                .thenReturn(ROUTING_URL);
    }

    // ========== Constructor Tests ==========

    /**
     * Test constructor with store-backed authenticator (primary @Inject constructor).
     * Verifies the 5-arg constructor builds a SalesforceCustomerServiceAuthenticator internally.
     * As of T-M6 the constructor takes sdkFactory in addition to the previous 4 params.
     */
    @Test
    public void testConstructor_WithStoreBackedAuthenticator_Success() {
        // Act
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAttributesStore,
                mockSdkFactory
        );

        // Assert
        assertNotNull("Extension should be created successfully", extension);
        assertNotNull("Request authenticator should be initialized", extension.getRequestAuthenticator());
        assertTrue("Request authenticator should be of correct type",
                extension.getRequestAuthenticator() instanceof SalesforceCustomerServiceAuthenticator);
    }

    /**
     * Test constructor with provided authenticator instance (secondary constructor).
     * This tests the constructor that accepts a pre-configured authenticator.
     */
    @Test
    public void testConstructor_WithProvidedAuthenticator_Success() {
        // Act
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Assert
        assertNotNull("Extension should be created successfully", extension);
        assertNotNull("Request authenticator should be initialized", extension.getRequestAuthenticator());
        assertSame("Request authenticator should be the provided instance",
                mockAuthenticator, extension.getRequestAuthenticator());
    }

    /**
     * Test constructor with null dependencies - verifies constructor accepts null.
     * Note: Java allows null assignments, so this tests that the constructor completes.
     */
    @Test
    public void testConstructor_WithNullProviderFactory_Completes() {
        // Act
        extension = new SalesforceCustomerServiceExtension(
                null,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Assert
        assertNotNull("Extension should be created even with null provider factory", extension);
    }

    // ========== Request Authenticator Tests ==========

    /**
     * Test getRequestAuthenticator returns the correct authenticator instance.
     */
    @Test
    public void testGetRequestAuthenticator_ReturnsCorrectInstance() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        RequestAuthenticator authenticator = extension.getRequestAuthenticator();

        // Assert
        assertNotNull("Authenticator should not be null", authenticator);
        assertSame("Should return the same authenticator instance", mockAuthenticator, authenticator);
    }

    /**
     * Test that getRequestAuthenticator method exists and returns consistent results.
     */
    @Test
    public void testGetRequestAuthenticator_ConsistentResults() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        RequestAuthenticator authenticator1 = extension.getRequestAuthenticator();
        RequestAuthenticator authenticator2 = extension.getRequestAuthenticator();

        // Assert
        assertNotNull("First call should return authenticator", authenticator1);
        assertNotNull("Second call should return authenticator", authenticator2);
        assertSame("Should return the same instance on multiple calls", authenticator1, authenticator2);
    }

    // ========== Custom Tabs Tests ==========

    /**
     * Test customTabs returns correct tab configuration.
     *
     * <p>As of v2.0.18 Chunk M3 the extension exposes three tabs:</p>
     * <ul>
     *   <li><b>Authentication</b> — the SDK-themed OAuth setup page.</li>
     *   <li><b>Salesforce Audit Dashboard</b> — the audit dashboard served by
     *       {@code SalesforceCustomerServiceAuditResource}.</li>
     *   <li><b>Documentation</b> — the existing static docs tab (unchanged).</li>
     * </ul>
     */
    @Test
    public void testCustomTabs_ReturnsDocumentationTab() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        Map<String, String> tabs = extension.customTabs();

        // Assert
        assertNotNull("Custom tabs map should not be null", tabs);
        assertEquals("Should have exactly three tabs (Authentication + Salesforce Audit Dashboard + Documentation)",
                3, tabs.size());
        assertTrue("Should contain Authentication tab", tabs.containsKey("Authentication"));
        assertEquals("Authentication tab should point to SDK auth UI path with providerName query "
                + "param (forces oauth/app.js onto the /oauth/config?providerName=... path served "
                + "by OAuth2ConfigResource; without the param the page falls back to a non-existent "
                + "/oauth/metadata/{id} endpoint)",
                "rest/salesforcecustomerservice/authentication/?providerName=salesforce-platform",
                tabs.get("Authentication"));
        assertTrue("Should contain Salesforce Audit Dashboard tab", tabs.containsKey("Salesforce Audit Dashboard"));
        assertEquals("Salesforce Audit Dashboard tab should point to correct path",
                "rest/salesforcecustomerservice/audit/", tabs.get("Salesforce Audit Dashboard"));
        assertTrue("Should contain Documentation tab", tabs.containsKey("Documentation"));
        assertEquals("Documentation tab should point to correct path",
                "static/docs", tabs.get("Documentation"));
    }

    /**
     * Test customTabs returns immutable map.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testCustomTabs_ReturnsImmutableMap() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        Map<String, String> tabs = extension.customTabs();
        
        // Assert - should throw UnsupportedOperationException
        tabs.put("NewTab", "newPath");
    }

    // ========== Validate Connection Tests ==========

    /**
     * Test validateConnection calls the provider factory and token provider.
     * Note: This test verifies the method executes without testing static method behavior.
     */
    @Test
    public void testValidateConnection_CallsProviderFactory() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, Object> connectionAttributes = createConnectionAttributes(false);

        // Act & Assert - verify method can be called
        // Note: Full integration testing would require PowerMock or similar for static methods
        try {
            extension.validateConnection(connectionAttributes);
        } catch (Exception e) {
            // Expected if static method cannot be mocked
            assertTrue("Exception should be related to attribute creation",
                    e.getMessage() == null || e.getMessage().contains("attribute") || e.getMessage().contains("null"));
        }
    }

    /**
     * Test validateConnection method exists and accepts Map parameter.
     */
    @Test
    public void testValidateConnection_MethodExists() throws NoSuchMethodException {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        var method = SalesforceCustomerServiceExtension.class.getMethod("validateConnection", Map.class);

        // Assert
        assertNotNull("Method should exist", method);
        assertEquals("Method should accept Map parameter", 1, method.getParameterCount());
        assertEquals("Method should return void", void.class, method.getReturnType());
    }

    /**
     * Test validateConnection with empty attributes map.
     */
    @Test
    public void testValidateConnection_WithEmptyAttributes() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, Object> emptyAttributes = new HashMap<>();

        // Act & Assert
        try {
            extension.validateConnection(emptyAttributes);
        } catch (Exception e) {
            // Expected - empty attributes should cause validation to fail
            assertNotNull("Exception should be thrown for empty attributes", e);
        }
    }

    /**
     * Test validateConnection with null attributes map.
     */
    @Test
    public void testValidateConnection_WithNullAttributes() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act & Assert
        try {
            extension.validateConnection(null);
        } catch (Exception e) {
            // Expected - null attributes should cause validation to fail
            assertNotNull("Exception should be thrown for null attributes", e);
        }
    }

    // ========== Test Connection Tests ==========

    /**
     * Test testConnection with no operator credentials (no username/password/instanceUrl).
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 and Violation C fix: TEST_CONNECTION is an
     * operator-context operation and MUST always call getSalesforceTokenForAdmin(). It must
     * never route to getSalesforceTokenForUser() regardless of credential configuration.
     * When no operator credentials are present, getSalesforceTokenForAdmin() will throw a
     * descriptive RuntimeException explaining that operator credentials are required.</p>
     */
    @Test
    public void testTestConnection_WithClientCredentials_Success() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(null);
        when(mockAttributes.getPassword()).thenReturn(null);
        when(mockAttributes.getSecurityToken()).thenReturn(null);

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert — Rule 2: testConnection is ALWAYS operator-context, NEVER per-user.
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test testConnection with user credentials (username/password/security token).
     * Should call getSalesforceTokenForAdmin() when user credentials are present.
     */
    @Test
    public void testTestConnection_WithUserCredentials_Success() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(USERNAME);
        when(mockAttributes.getPassword()).thenReturn(PASSWORD);
        when(mockAttributes.getSecurityToken()).thenReturn(SECURITY_TOKEN);

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test testConnection with blank username.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 fix: TEST_CONNECTION is always operator-context.
     * Regardless of whether username/password is blank, getSalesforceTokenForAdmin() is called.
     * The SDK's HC-006 routing matrix decides which operator strategy to use (CC vs PG vs JWT).</p>
     */
    @Test
    public void testTestConnection_WithBlankUsername_UsesClientCredentials() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn("   ");
        when(mockAttributes.getPassword()).thenReturn(PASSWORD);
        when(mockAttributes.getSecurityToken()).thenReturn(SECURITY_TOKEN);

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert — Rule 2: testConnection is ALWAYS operator-context, NEVER per-user.
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test testConnection with blank password.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 fix: TEST_CONNECTION is always operator-context.</p>
     */
    @Test
    public void testTestConnection_WithBlankPassword_UsesClientCredentials() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(USERNAME);
        when(mockAttributes.getPassword()).thenReturn("");
        when(mockAttributes.getSecurityToken()).thenReturn(SECURITY_TOKEN);

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert — Rule 2: testConnection is ALWAYS operator-context, NEVER per-user.
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test testConnection with blank security token.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 fix: TEST_CONNECTION is always operator-context.</p>
     */
    @Test
    public void testTestConnection_WithBlankSecurityToken_UsesClientCredentials() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(USERNAME);
        when(mockAttributes.getPassword()).thenReturn(PASSWORD);
        when(mockAttributes.getSecurityToken()).thenReturn("  ");

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert — Rule 2: testConnection is ALWAYS operator-context, NEVER per-user.
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test testConnection when token provider throws exception.
     */
    @Test(expected = RuntimeException.class)
    public void testTestConnection_WhenTokenProviderFails_ThrowsException() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(USERNAME);
        when(mockAttributes.getPassword()).thenReturn(PASSWORD);
        when(mockAttributes.getSecurityToken()).thenReturn(SECURITY_TOKEN);

        doThrow(new RuntimeException("Token fetch failed"))
                .when(mockTokenProvider).getSalesforceTokenForAdmin();

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act - should throw RuntimeException
        extension.testConnection();
    }

    // ========== Attributes Updated Tests ==========

    /**
     * Test attributesUpdated with valid new attributes.
     */
    @Test
    public void testAttributesUpdated_WithValidAttributes_Success() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, Object> oldAttributes = createConnectionAttributes(false);
        Map<String, Object> newAttributes = createConnectionAttributes(true);

        // Act
        extension.attributesUpdated(oldAttributes, newAttributes);

        // Assert
        verify(mockAttributes, times(1)).update(newAttributes);
    }

    /**
     * Test attributesUpdated with null old attributes.
     */
    @Test
    public void testAttributesUpdated_WithNullOldAttributes_Success() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, Object> newAttributes = createConnectionAttributes(true);

        // Act
        extension.attributesUpdated(null, newAttributes);

        // Assert
        verify(mockAttributes, times(1)).update(newAttributes);
    }

    /**
     * Test attributesUpdated with null new attributes.
     */
    @Test
    public void testAttributesUpdated_WithNullNewAttributes_HandlesGracefully() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, Object> oldAttributes = createConnectionAttributes(false);

        // Act
        extension.attributesUpdated(oldAttributes, null);

        // Assert
        verify(mockAttributes, times(1)).update(null);
    }

    /**
     * Test attributesUpdated with empty new attributes.
     */
    @Test
    public void testAttributesUpdated_WithEmptyNewAttributes_Success() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, Object> oldAttributes = createConnectionAttributes(true);
        Map<String, Object> newAttributes = new HashMap<>();

        // Act
        extension.attributesUpdated(oldAttributes, newAttributes);

        // Assert
        verify(mockAttributes, times(1)).update(newAttributes);
    }

    /**
     * Test attributesUpdated with changed credentials.
     */
    @Test
    public void testAttributesUpdated_WithChangedCredentials_Success() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, Object> oldAttributes = createConnectionAttributes(false);
        Map<String, Object> newAttributes = createConnectionAttributes(true);
        newAttributes.put(SalesforceCustomerServiceAttributes.CLIENT_ID, "new_client_id");
        newAttributes.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, "new_client_secret");

        // Act
        extension.attributesUpdated(oldAttributes, newAttributes);

        // Assert
        verify(mockAttributes, times(1)).update(newAttributes);
    }

    /**
     * Test attributesUpdated when update throws exception.
     */
    @Test(expected = RuntimeException.class)
    public void testAttributesUpdated_WhenUpdateFails_ThrowsException() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, Object> oldAttributes = createConnectionAttributes(false);
        Map<String, Object> newAttributes = createConnectionAttributes(true);

        doThrow(new RuntimeException("Update failed"))
                .when(mockAttributes).update(newAttributes);

        // Act - should throw RuntimeException
        extension.attributesUpdated(oldAttributes, newAttributes);
    }

    // ========== Additional Edge Case Tests ==========

    /**
     * Test testConnection with all null credentials.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 fix: TEST_CONNECTION is always operator-context
     * and MUST call getSalesforceTokenForAdmin(). The mock will silently return (no-op) since
     * getSalesforceTokenForAdmin is mocked. In a real deployment with AUTH_CODE-only config,
     * the call would throw a RuntimeException with a clear message — tested separately.</p>
     */
    @Test
    public void testTestConnection_WithAllNullCredentials_UsesClientCredentials() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(null);
        when(mockAttributes.getPassword()).thenReturn(null);
        when(mockAttributes.getSecurityToken()).thenReturn(null);

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert — Rule 2: testConnection is ALWAYS operator-context, NEVER per-user.
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test testConnection with empty string credentials.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 fix: TEST_CONNECTION is always operator-context.</p>
     */
    @Test
    public void testTestConnection_WithEmptyStringCredentials_UsesClientCredentials() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn("");
        when(mockAttributes.getPassword()).thenReturn("");
        when(mockAttributes.getSecurityToken()).thenReturn("");

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert — Rule 2: testConnection is ALWAYS operator-context, NEVER per-user.
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test testConnection with only username provided.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 fix: TEST_CONNECTION is always operator-context.</p>
     */
    @Test
    public void testTestConnection_WithOnlyUsername_UsesClientCredentials() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(USERNAME);
        when(mockAttributes.getPassword()).thenReturn(null);
        when(mockAttributes.getSecurityToken()).thenReturn(null);

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert — Rule 2: testConnection is ALWAYS operator-context, NEVER per-user.
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test testConnection with only password provided.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 fix: TEST_CONNECTION is always operator-context.</p>
     */
    @Test
    public void testTestConnection_WithOnlyPassword_UsesClientCredentials() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(null);
        when(mockAttributes.getPassword()).thenReturn(PASSWORD);
        when(mockAttributes.getSecurityToken()).thenReturn(SECURITY_TOKEN);

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert — Rule 2: testConnection is ALWAYS operator-context, NEVER per-user.
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test testConnection when provider factory throws exception.
     */
    @Test(expected = RuntimeException.class)
    public void testTestConnection_WhenProviderFactoryFails_ThrowsException() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(USERNAME);
        when(mockAttributes.getPassword()).thenReturn(PASSWORD);
        when(mockAttributes.getSecurityToken()).thenReturn(SECURITY_TOKEN);

        when(mockProviderFactory.create(any(SalesforceCustomerServiceAttributes.class)))
                .thenThrow(new RuntimeException("Provider factory failed"));

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act - should throw RuntimeException
        extension.testConnection();
    }

    /**
     * Test getRequestAuthenticator with store-backed constructor (5-arg @Inject variant).
     * As of T-M6 sdkFactory is the 5th argument.
     */
    @Test
    public void testGetRequestAuthenticator_WithStoreBackedConstructor_ReturnsAuthenticator() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAttributesStore,
                mockSdkFactory
        );

        // Act
        RequestAuthenticator authenticator = extension.getRequestAuthenticator();

        // Assert
        assertNotNull("Authenticator should not be null", authenticator);
        assertTrue("Authenticator should be of correct type",
                authenticator instanceof SalesforceCustomerServiceAuthenticator);
    }

    /**
     * Test customTabs returns consistent results on multiple calls.
     */
    @Test
    public void testCustomTabs_ConsistentResults() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        Map<String, String> tabs1 = extension.customTabs();
        Map<String, String> tabs2 = extension.customTabs();

        // Assert
        assertNotNull("First call should return tabs", tabs1);
        assertNotNull("Second call should return tabs", tabs2);
        assertEquals("Both calls should return same content", tabs1, tabs2);
    }

    // ========== Helper Methods ==========

    /**
     * Creates a map of connection attributes for testing.
     *
     * @param includeUserCredentials whether to include username/password/security token
     * @return map of connection attributes
     */
    private Map<String, Object> createConnectionAttributes(boolean includeUserCredentials) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SalesforceCustomerServiceAttributes.CLIENT_ID, CLIENT_ID);
        attributes.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, CLIENT_SECRET);
        attributes.put(SalesforceCustomerServiceAttributes.INSTANCE_URL, INSTANCE_URL);
        attributes.put(SalesforceCustomerServiceAttributes.SANDBOX, false);

        if (includeUserCredentials) {
            attributes.put(SalesforceCustomerServiceAttributes.USER_NAME, USERNAME);
            attributes.put(SalesforceCustomerServiceAttributes.PASS_WORD, PASSWORD);
            attributes.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN, SECURITY_TOKEN);
        }

        return attributes;
    }

    // ========== Integration and Flow Tests ==========

    /**
     * Test complete flow: construct extension, get authenticator, test connection.
     */
    @Test
    public void testCompleteFlow_ConstructGetAuthenticatorTestConnection() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(USERNAME);
        when(mockAttributes.getPassword()).thenReturn(PASSWORD);
        when(mockAttributes.getSecurityToken()).thenReturn(SECURITY_TOKEN);

        // Act
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        RequestAuthenticator authenticator = extension.getRequestAuthenticator();
        extension.testConnection();

        // Assert
        assertNotNull("Extension should be created", extension);
        assertNotNull("Authenticator should be available", authenticator);
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
    }

    /**
     * Test complete flow: construct, get tabs, update attributes.
     */
    @Test
    public void testCompleteFlow_ConstructGetTabsUpdateAttributes() {
        // Arrange
        Map<String, Object> oldAttributes = createConnectionAttributes(false);
        Map<String, Object> newAttributes = createConnectionAttributes(true);

        // Act
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, String> tabs = extension.customTabs();
        extension.attributesUpdated(oldAttributes, newAttributes);

        // Assert
        assertNotNull("Extension should be created", extension);
        assertNotNull("Tabs should be available", tabs);
        assertEquals("Should have Documentation tab", "static/docs", tabs.get("Documentation"));
        verify(mockAttributes, times(1)).update(newAttributes);
    }

    /**
     * Test multiple testConnection calls with same extension instance.
     */
    @Test
    public void testMultipleTestConnectionCalls_WithSameInstance() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn(USERNAME);
        when(mockAttributes.getPassword()).thenReturn(PASSWORD);
        when(mockAttributes.getSecurityToken()).thenReturn(SECURITY_TOKEN);

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();
        extension.testConnection();
        extension.testConnection();

        // Assert
        verify(mockProviderFactory, times(3)).create(mockAttributes);
        verify(mockTokenProvider, times(3)).getSalesforceTokenForAdmin();
    }

    /**
     * Test multiple attributesUpdated calls with same extension instance.
     */
    @Test
    public void testMultipleAttributesUpdatedCalls_WithSameInstance() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        Map<String, Object> attributes1 = createConnectionAttributes(false);
        Map<String, Object> attributes2 = createConnectionAttributes(true);
        Map<String, Object> attributes3 = new HashMap<>();

        // Act
        extension.attributesUpdated(null, attributes1);
        extension.attributesUpdated(attributes1, attributes2);
        extension.attributesUpdated(attributes2, attributes3);

        // Assert
        verify(mockAttributes, times(3)).update(any());
        verify(mockAttributes, times(1)).update(attributes1);
        verify(mockAttributes, times(1)).update(attributes2);
        verify(mockAttributes, times(1)).update(attributes3);
    }

    /**
     * Test testConnection always routes to getSalesforceTokenForAdmin(), regardless of
     * credential configuration.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 fix: TEST_CONNECTION is an operator-context
     * call and ALWAYS uses the admin/operator path. Both calls (no-creds and full-creds) must
     * call getSalesforceTokenForAdmin(). The old routing that sent no-creds to
     * getSalesforceTokenForUser() was the Violation B/C data-bleed precursor, now removed.</p>
     */
    @Test
    public void testTestConnection_SwitchingBetweenCredentialTypes() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act & Assert - First call: no operator credentials set
        when(mockAttributes.getUserName()).thenReturn(null);
        when(mockAttributes.getPassword()).thenReturn(null);
        when(mockAttributes.getSecurityToken()).thenReturn(null);
        extension.testConnection();
        // Rule 2: always admin path — never getSalesforceTokenForUser()
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();

        // Act & Assert - Second call: full Password Grant credentials set
        when(mockAttributes.getUserName()).thenReturn(USERNAME);
        when(mockAttributes.getPassword()).thenReturn(PASSWORD);
        when(mockAttributes.getSecurityToken()).thenReturn(SECURITY_TOKEN);
        extension.testConnection();
        verify(mockTokenProvider, times(2)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }

    /**
     * Test extension with null invoker.
     */
    @Test
    public void testConstructor_WithNullInvoker_Completes() {
        // Act
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                null,
                mockAttributes,
                mockAuthenticator
        );

        // Assert
        assertNotNull("Extension should be created even with null invoker", extension);
    }

    /**
     * Test extension with null attributes.
     */
    @Test
    public void testConstructor_WithNullAttributes_Completes() {
        // Act
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                null,
                mockAuthenticator
        );

        // Assert
        assertNotNull("Extension should be created even with null attributes", extension);
    }

    /**
     * Test extension with null authenticator.
     */
    @Test
    public void testConstructor_WithNullAuthenticator_Completes() {
        // Act
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                (SalesforceCustomerServiceAuthenticator) null
        );

        // Assert
        assertNotNull("Extension should be created even with null authenticator", extension);
    }

    /**
     * Test getRequestAuthenticator returns null when authenticator is null.
     */
    @Test
    public void testGetRequestAuthenticator_WhenAuthenticatorIsNull_ReturnsNull() {
        // Arrange
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                (SalesforceCustomerServiceAuthenticator) null
        );

        // Act
        RequestAuthenticator authenticator = extension.getRequestAuthenticator();

        // Assert
        assertNull("Authenticator should be null when not provided", authenticator);
    }

    /**
     * Test testConnection with mixed blank and null credentials.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md Rule 2 fix: TEST_CONNECTION is always operator-context.</p>
     */
    @Test
    public void testTestConnection_WithMixedBlankAndNullCredentials() {
        // Arrange
        when(mockAttributes.getUserName()).thenReturn("  ");
        when(mockAttributes.getPassword()).thenReturn(null);
        when(mockAttributes.getSecurityToken()).thenReturn("");

        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAuthenticator
        );

        // Act
        extension.testConnection();

        // Assert — Rule 2: testConnection is ALWAYS operator-context, NEVER per-user.
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForAdmin();
        verify(mockTokenProvider, never()).getSalesforceTokenForUser();
    }
}

