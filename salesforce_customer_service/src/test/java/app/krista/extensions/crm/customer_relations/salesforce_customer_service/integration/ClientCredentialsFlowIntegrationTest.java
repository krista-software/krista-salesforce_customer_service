package app.krista.extensions.crm.customer_relations.salesforce_customer_service.integration;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.api.SalesforceCustomApi;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration tests for OAuth 2.0 Client Credentials flow.
 * 
 * These tests verify the end-to-end functionality of the Client Credentials flow
 * including configuration validation, OAuth service creation, and authentication
 * method selection logic.
 * 
 * Note: These are integration tests that test the interaction between multiple
 * components but do not make actual network calls to Salesforce.
 */
public class ClientCredentialsFlowIntegrationTest {

    private static final String CLIENT_ID = "REDACTED_SECRET_0";
    private static final String CLIENT_SECRET = "REDACTED_SECRET_3";
    private static final String INSTANCE_URL = "https://test.my.salesforce.com";
    private static final String ROUTING_URL = "https://callback.example.com";

    /**
     * Test complete Client Credentials flow configuration and setup.
     */
    @Test
    public void testClientCredentialsFlow_CompleteSetup() {
        // Arrange - Create attributes for Client Credentials flow
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, false, ROUTING_URL);

        // Act & Assert - Verify configuration
        assertEquals("Client ID should be set", CLIENT_ID, attributes.getClientId());
        assertEquals("Client Secret should be set", CLIENT_SECRET, attributes.getClientSecret());
        assertEquals("Instance URL should be set", INSTANCE_URL, attributes.getInstanceUrl());
        assertNull("Username should be null for Client Credentials", attributes.getUserName());
        assertNull("Password should be null for Client Credentials", attributes.getPassword());
        assertNull("Security Token should be null for Client Credentials", attributes.getSecurityToken());
        assertFalse("Should not be sandbox", attributes.isSandbox());

        // Verify OAuth service creation with custom domain
        OAuth20Service oauthService = attributes.getOauthService(INSTANCE_URL);
        assertNotNull("OAuth service should be created", oauthService);

        // Verify authentication method selection
        assertTrue("Should use OAuth for Client Credentials flow",
                SalesforceTokenProvider.validateUsingOAuth(false, attributes));
    }

    /**
     * Test Client Credentials flow with sandbox configuration.
     */
    @Test
    public void testClientCredentialsFlow_SandboxConfiguration() {
        // Arrange - Create sandbox attributes
        String sandboxInstanceUrl = "https://test--sandbox.my.salesforce.com";
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, sandboxInstanceUrl, null, null, null, true, ROUTING_URL);

        // Act & Assert
        assertEquals("Instance URL should be sandbox", sandboxInstanceUrl, attributes.getInstanceUrl());
        assertTrue("Should be sandbox", attributes.isSandbox());

        // Verify OAuth service creation
        OAuth20Service oauthService = attributes.getOauthService(sandboxInstanceUrl);
        assertNotNull("OAuth service should be created for sandbox", oauthService);
    }

    /**
     * Test SalesforceCustomApi integration with Client Credentials flow.
     */
    @Test
    public void testSalesforceCustomApi_Integration() {
        // Arrange
        SalesforceCustomApi customApi = new SalesforceCustomApi(INSTANCE_URL);

        // Act & Assert - Verify token endpoint construction
        String expectedTokenEndpoint = INSTANCE_URL + "/services/oauth2/token";

        assertEquals("Token endpoint should be correctly constructed",
                expectedTokenEndpoint, customApi.getAccessTokenEndpoint());

        // Note: getAuthorizationBaseUrl() is protected, so we can't test it directly
        // but it's tested indirectly through the OAuth service creation
    }

    /**
     * Test Client Credentials flow priority over other authentication methods.
     */
    @Test
    public void testClientCredentialsFlow_Priority() {
        // Scenario 1: Instance URL provided with no other credentials
        SalesforceCustomerServiceAttributes clientCredentialsOnly = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, false, ROUTING_URL);
        
        assertTrue("Client Credentials only should use OAuth",
                SalesforceTokenProvider.validateUsingOAuth(false, clientCredentialsOnly));

        // Scenario 2: Instance URL provided with full credentials (Instance URL takes precedence)
        SalesforceCustomerServiceAttributes mixedCredentials = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, "user@example.com", "password", "token", false, ROUTING_URL);
        
        // Note: validateUsingOAuth checks credentials, not Instance URL, so this returns false
        // But the actual flow logic in getSalesforceToken will use Client Credentials because Instance URL is present
        assertFalse("Mixed credentials should return false from validateUsingOAuth",
                SalesforceTokenProvider.validateUsingOAuth(false, mixedCredentials));
        
        // However, Instance URL is present, so Client Credentials flow would be used
        assertNotNull("Instance URL should be present", mixedCredentials.getInstanceUrl());
        assertFalse("Instance URL should not be empty", mixedCredentials.getInstanceUrl().isEmpty());
    }

    /**
     * Test Client Credentials flow configuration validation.
     */
    @Test
    public void testClientCredentialsFlow_ConfigurationValidation() {
        // Test valid configuration
        SalesforceCustomerServiceAttributes validConfig = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, false, ROUTING_URL);
        
        assertNotNull("Valid configuration should be created", validConfig);
        assertEquals("Instance URL should be validated and set", INSTANCE_URL, validConfig.getInstanceUrl());

        // Test invalid Instance URL
        try {
            new SalesforceCustomerServiceAttributes(
                    CLIENT_ID, CLIENT_SECRET, "invalid-url", null, null, null, false, ROUTING_URL);
            fail("Should throw exception for invalid Instance URL");
        } catch (IllegalArgumentException e) {
            assertTrue("Error should mention invalid URL format. Actual message: " + e.getMessage(),
                    e.getMessage().contains("Instance URL validation failed due to invalid format"));
        }
    }

    /**
     * Test Client Credentials flow with various Instance URL formats.
     */
    @Test
    public void testClientCredentialsFlow_VariousInstanceUrlFormats() {
        String[] validUrls = {
                "https://test.my.salesforce.com",
                "https://test--sandbox.my.salesforce.com",
                "https://custom.lightning.force.com",
                "https://myorg.my.salesforce.com/",
                "http://localhost:8080"
        };

        for (String url : validUrls) {
            SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                    CLIENT_ID, CLIENT_SECRET, url, null, null, null, false, ROUTING_URL);
            
            assertNotNull("Should accept valid URL: " + url, attributes);
            assertNotNull("Should create OAuth service for: " + url, 
                    attributes.getOauthService(url));
        }
    }

    /**
     * Test Client Credentials flow update functionality.
     */
    @Test
    public void testClientCredentialsFlow_UpdateFunctionality() {
        // Arrange - Start with no Instance URL
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, "user@example.com", "password", "token", false, ROUTING_URL);
        
        // Initially should not use OAuth (has full credentials, no Instance URL)
        assertFalse("Should not use OAuth initially",
                SalesforceTokenProvider.validateUsingOAuth(false, attributes));

        // Act - Update to add Instance URL (switch to Client Credentials flow)
        java.util.Map<String, Object> updateAttributes = new java.util.HashMap<>();
        updateAttributes.put(SalesforceCustomerServiceAttributes.CLIENT_ID, CLIENT_ID);
        updateAttributes.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, CLIENT_SECRET);
        updateAttributes.put(SalesforceCustomerServiceAttributes.INSTANCE_URL, INSTANCE_URL);
        updateAttributes.put(SalesforceCustomerServiceAttributes.USER_NAME, null);
        updateAttributes.put(SalesforceCustomerServiceAttributes.PASS_WORD, null);
        updateAttributes.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN, null);
        updateAttributes.put(SalesforceCustomerServiceAttributes.SANDBOX, false);

        attributes.update(updateAttributes);

        // Assert - Now should use OAuth (no credentials, has Instance URL)
        assertEquals("Instance URL should be updated", INSTANCE_URL, attributes.getInstanceUrl());
        assertNull("Username should be null after update", attributes.getUserName());
        assertTrue("Should use OAuth after adding Instance URL",
                SalesforceTokenProvider.validateUsingOAuth(false, attributes));
    }

    /**
     * Test error handling in Client Credentials flow setup.
     */
    @Test
    public void testClientCredentialsFlow_ErrorHandling() {
        // Test SalesforceCustomApi with invalid domains
        String[] invalidDomains = {
                null,
                "",
                "   ",
                "not-a-url",
                "ftp://invalid.com",
                "https://"
        };

        for (String invalidDomain : invalidDomains) {
            try {
                new SalesforceCustomApi(invalidDomain);
                fail("Should throw exception for invalid domain: " + invalidDomain);
            } catch (IllegalArgumentException e) {
                // Expected
                assertNotNull("Exception message should not be null", e.getMessage());
            }
        }

        // Test SalesforceCustomerServiceAttributes with invalid Instance URLs
        try {
            new SalesforceCustomerServiceAttributes(
                    CLIENT_ID, CLIENT_SECRET, "not-a-valid-url", null, null, null, false, ROUTING_URL);
            fail("Should throw exception for invalid Instance URL");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention invalid URL format. Actual message: " + e.getMessage(),
                    e.getMessage().contains("Instance URL validation failed due to invalid format"));
        }
    }

    /**
     * Test callback URL generation for Client Credentials flow.
     */
    @Test
    public void testClientCredentialsFlow_CallbackUrlGeneration() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, false, ROUTING_URL);

        // Act
        String callbackUrl = attributes.getCallbackUrl();

        // Assert
        String expectedCallbackUrl = ROUTING_URL + "/rest/salesforcecustomerservice/callback";
        assertEquals("Callback URL should be correctly generated", expectedCallbackUrl, callbackUrl);
    }
}
