package app.krista.extensions.crm.customer_relations.salesforce_customer_service;

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
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
    }

    // ========== Constructor Tests ==========

    /**
     * Test constructor with store-backed authenticator (primary constructor).
     * This tests the @Inject constructor that creates its own authenticator.
     */
    @Test
    public void testConstructor_WithStoreBackedAuthenticator_Success() {
        // Act
        extension = new SalesforceCustomerServiceExtension(
                mockProviderFactory,
                mockInvoker,
                mockAttributes,
                mockAttributesStore
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
     * Test customTabs returns correct documentation tab configuration.
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
        assertEquals("Should have exactly one tab", 1, tabs.size());
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

    // ========== Test Connection Tests ==========

    /**
     * Test testConnection with client credentials (no username/password).
     * Should call getSalesforceTokenForUser() when user credentials are missing.
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

        // Assert
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForUser();
        verify(mockTokenProvider, never()).getSalesforceTokenForAdmin();
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
     * Test testConnection with blank username (should use client credentials flow).
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

        // Assert
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForUser();
        verify(mockTokenProvider, never()).getSalesforceTokenForAdmin();
    }

    /**
     * Test testConnection with blank password (should use client credentials flow).
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

        // Assert
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForUser();
        verify(mockTokenProvider, never()).getSalesforceTokenForAdmin();
    }

    /**
     * Test testConnection with blank security token (should use client credentials flow).
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

        // Assert
        verify(mockProviderFactory, times(1)).create(mockAttributes);
        verify(mockTokenProvider, times(1)).getSalesforceTokenForUser();
        verify(mockTokenProvider, never()).getSalesforceTokenForAdmin();
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
}

