package app.krista.extensions.crm.customer_relations.salesforce_customer_service;

import com.github.scribejava.core.oauth.OAuth20Service;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for SalesforceCustomerServiceAttributes class.
 * Tests the configuration attributes including Instance URL functionality.
 */
public class SalesforceCustomerServiceAttributesTest {

    private static final String CLIENT_ID = "test_client_id";
    private static final String CLIENT_SECRET = "test_client_secret";
    private static final String INSTANCE_URL = "https://test.my.salesforce.com";
    private static final String USERNAME = "test@example.com";
    private static final String PASSWORD = "testpassword";
    private static final String SECURITY_TOKEN = "testsecuritytoken";
    private static final String ROUTING_URL = "https://callback.example.com";

    /**
     * Test constructor with all parameters including Instance URL.
     */
    @Test
    public void testConstructor_AllParameters() {
        // Act
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, USERNAME, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);

        // Assert
        assertEquals("Client ID should be set correctly", CLIENT_ID, attributes.getClientId());
        assertEquals("Client Secret should be set correctly", CLIENT_SECRET, attributes.getClientSecret());
        assertEquals("Instance URL should be set correctly", INSTANCE_URL, attributes.getInstanceUrl());
        assertEquals("Username should be set correctly", USERNAME, attributes.getUserName());
        assertEquals("Password should be set correctly", PASSWORD, attributes.getPassword());
        assertEquals("Security Token should be set correctly", SECURITY_TOKEN, attributes.getSecurityToken());
        assertTrue("Sandbox should be set correctly", attributes.isSandbox());
    }

    /**
     * Test constructor with attributes map including Instance URL.
     */
    @Test
    public void testConstructor_AttributesMap() {
        // Arrange
        Map<String, Object> attributesMap = new HashMap<>();
        attributesMap.put(SalesforceCustomerServiceAttributes.CLIENT_ID, CLIENT_ID);
        attributesMap.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, CLIENT_SECRET);
        attributesMap.put(SalesforceCustomerServiceAttributes.INSTANCE_URL, INSTANCE_URL);
        attributesMap.put(SalesforceCustomerServiceAttributes.USER_NAME, USERNAME);
        attributesMap.put(SalesforceCustomerServiceAttributes.PASS_WORD, PASSWORD);
        attributesMap.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN, SECURITY_TOKEN);
        attributesMap.put(SalesforceCustomerServiceAttributes.SANDBOX, true);

        // Act
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(ROUTING_URL, attributesMap);

        // Assert
        assertEquals("Client ID should be set from map", CLIENT_ID, attributes.getClientId());
        assertEquals("Client Secret should be set from map", CLIENT_SECRET, attributes.getClientSecret());
        assertEquals("Instance URL should be set from map", INSTANCE_URL, attributes.getInstanceUrl());
        assertEquals("Username should be set from map", USERNAME, attributes.getUserName());
        assertEquals("Password should be set from map", PASSWORD, attributes.getPassword());
        assertEquals("Security Token should be set from map", SECURITY_TOKEN, attributes.getSecurityToken());
        assertTrue("Sandbox should be set from map", attributes.isSandbox());
    }

    /**
     * Test Instance URL functionality with null value.
     */
    @Test
    public void testInstanceUrl_NullValue() {
        // Act
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Assert
        assertNull("Instance URL should be null", attributes.getInstanceUrl());
    }

    /**
     * Test Instance URL functionality with empty string.
     */
    @Test
    public void testInstanceUrl_EmptyString() {
        // Act
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, "", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Assert
        assertEquals("Instance URL should be empty string", "", attributes.getInstanceUrl());
    }

    /**
     * Test sandbox functionality.
     */
    @Test
    public void testSandbox_Functionality() {
        // Test sandbox true
        SalesforceCustomerServiceAttributes sandboxAttributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, USERNAME, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);
        assertTrue("Sandbox should be true", sandboxAttributes.isSandbox());

        // Test sandbox false
        SalesforceCustomerServiceAttributes prodAttributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertFalse("Sandbox should be false", prodAttributes.isSandbox());

        // Test sandbox null (should default to false)
        SalesforceCustomerServiceAttributes nullSandboxAttributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, USERNAME, PASSWORD, SECURITY_TOKEN, null, ROUTING_URL);
        assertFalse("Null sandbox should default to false", nullSandboxAttributes.isSandbox());
    }

    /**
     * Test callback URL generation.
     */
    @Test
    public void testCallbackUrl_Generation() {
        // Act
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, USERNAME, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);

        // Assert
        String expectedCallbackUrl = ROUTING_URL + "/rest/salesforcecustomerservice/callback";
        assertEquals("Callback URL should be correctly generated", expectedCallbackUrl, attributes.getCallbackUrl());
    }

    /**
     * Test OAuth service creation without custom domain.
     */
    @Test
    public void testGetOauthService_StandardFlow() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);

        // Act
        OAuth20Service service = attributes.getOauthService();

        // Assert
        assertNotNull("OAuth service should not be null", service);
    }

    /**
     * Test OAuth service creation with custom domain (Client Credentials flow).
     */
    @Test
    public void testGetOauthService_CustomDomain() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, true, ROUTING_URL);

        // Act
        OAuth20Service service = attributes.getOauthService(INSTANCE_URL);

        // Assert
        assertNotNull("OAuth service with custom domain should not be null", service);
    }

    /**
     * Test update functionality with new attributes.
     */
    @Test
    public void testUpdate_NewAttributes() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        Map<String, Object> newAttributes = new HashMap<>();
        String newInstanceUrl = "https://updated.my.salesforce.com";
        String newUsername = "updated@example.com";
        // Include all existing values to preserve them
        newAttributes.put(SalesforceCustomerServiceAttributes.CLIENT_ID, CLIENT_ID);
        newAttributes.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, CLIENT_SECRET);
        newAttributes.put(SalesforceCustomerServiceAttributes.INSTANCE_URL, newInstanceUrl);
        newAttributes.put(SalesforceCustomerServiceAttributes.USER_NAME, newUsername);
        newAttributes.put(SalesforceCustomerServiceAttributes.PASS_WORD, PASSWORD);
        newAttributes.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN, SECURITY_TOKEN);
        newAttributes.put(SalesforceCustomerServiceAttributes.SANDBOX, true);

        // Act
        attributes.update(newAttributes);

        // Assert
        assertEquals("Instance URL should be updated", newInstanceUrl, attributes.getInstanceUrl());
        assertEquals("Username should be updated", newUsername, attributes.getUserName());
        assertTrue("Sandbox should be updated", attributes.isSandbox());
        // Original values should remain unchanged since they were included in the update map
        assertEquals("Client ID should remain unchanged", CLIENT_ID, attributes.getClientId());
        assertEquals("Password should remain unchanged", PASSWORD, attributes.getPassword());
    }

    /**
     * Test update functionality with null values.
     */
    @Test
    public void testUpdate_NullValues() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, USERNAME, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);

        Map<String, Object> newAttributes = new HashMap<>();
        newAttributes.put(SalesforceCustomerServiceAttributes.INSTANCE_URL, null);
        newAttributes.put(SalesforceCustomerServiceAttributes.USER_NAME, null);

        // Act
        attributes.update(newAttributes);

        // Assert
        assertNull("Instance URL should be updated to null", attributes.getInstanceUrl());
        assertNull("Username should be updated to null", attributes.getUserName());
    }

    /**
     * Test edge cases with various attribute values.
     */
    @Test
    public void testEdgeCases_AttributeValues() {
        // Test with empty strings
        SalesforceCustomerServiceAttributes attributes1 = new SalesforceCustomerServiceAttributes(
                "", "", "", "", "", "", false, "");
        assertEquals("Empty client ID should be preserved", "", attributes1.getClientId());
        assertEquals("Empty instance URL should be preserved", "", attributes1.getInstanceUrl());

        // Test with whitespace strings
        SalesforceCustomerServiceAttributes attributes2 = new SalesforceCustomerServiceAttributes(
                "  ", "  ", "  ", "  ", "  ", "  ", true, "  ");
        assertEquals("Whitespace client ID should be preserved", "  ", attributes2.getClientId());
        assertEquals("Whitespace instance URL should be preserved", "  ", attributes2.getInstanceUrl());
    }

    /**
     * Test constants are correctly defined.
     */
    @Test
    public void testConstants_CorrectValues() {
        assertEquals("CLIENT_ID constant should be correct", "clientId", SalesforceCustomerServiceAttributes.CLIENT_ID);
        assertEquals("CLIENT_SECRET constant should be correct", "clientSecret", SalesforceCustomerServiceAttributes.CLIENT_SECRET);
        assertEquals("INSTANCE_URL constant should be correct", "Instance URL", SalesforceCustomerServiceAttributes.INSTANCE_URL);
        assertEquals("USER_NAME constant should be correct", "username", SalesforceCustomerServiceAttributes.USER_NAME);
        assertEquals("PASS_WORD constant should be correct", "password", SalesforceCustomerServiceAttributes.PASS_WORD);
        assertEquals("SECURITY_TOKEN constant should be correct", "securityToken", SalesforceCustomerServiceAttributes.SECURITY_TOKEN);
        assertEquals("SANDBOX constant should be correct", "sandbox", SalesforceCustomerServiceAttributes.SANDBOX);
    }

    /**
     * Test Instance URL validation with valid URLs.
     */
    @Test
    public void testInstanceUrlValidation_ValidUrls() {
        // Test valid HTTPS Salesforce URL
        SalesforceCustomerServiceAttributes attributes1 = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, "https://test.my.salesforce.com", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertEquals("Valid HTTPS URL should be accepted", "https://test.my.salesforce.com", attributes1.getInstanceUrl());

        // Test valid HTTP URL (for development)
        SalesforceCustomerServiceAttributes attributes2 = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, "http://localhost:8080", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertEquals("Valid HTTP URL should be accepted", "http://localhost:8080", attributes2.getInstanceUrl());

        // Test URL with trailing slash (should be trimmed)
        SalesforceCustomerServiceAttributes attributes3 = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, "  https://test.my.salesforce.com/  ", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertEquals("URL with whitespace should be trimmed", "https://test.my.salesforce.com/", attributes3.getInstanceUrl());
    }

    /**
     * Test Instance URL validation with invalid URLs.
     */
    @Test
    public void testInstanceUrlValidation_InvalidUrls() {
        // Test invalid protocol
        try {
            new SalesforceCustomerServiceAttributes(
                    CLIENT_ID, CLIENT_SECRET, "ftp://test.salesforce.com", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
            fail("Should throw IllegalArgumentException for invalid protocol");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention protocol", e.getMessage().contains("protocol"));
        }

        // Test malformed URL
        try {
            new SalesforceCustomerServiceAttributes(
                    CLIENT_ID, CLIENT_SECRET, "not-a-url", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
            fail("Should throw IllegalArgumentException for malformed URL");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention invalid format", e.getMessage().contains("Invalid Instance URL format"));
        }

        // Test URL without host
        try {
            new SalesforceCustomerServiceAttributes(
                    CLIENT_ID, CLIENT_SECRET, "https://", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
            fail("Should throw IllegalArgumentException for URL without host");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention host", e.getMessage().contains("host"));
        }
    }

    /**
     * Test Instance URL validation with null and empty values.
     */
    @Test
    public void testInstanceUrlValidation_NullAndEmpty() {
        // Test null Instance URL (should be allowed)
        SalesforceCustomerServiceAttributes attributes1 = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertNull("Null Instance URL should be allowed", attributes1.getInstanceUrl());

        // Test empty Instance URL (should be allowed)
        SalesforceCustomerServiceAttributes attributes2 = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, "", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertEquals("Empty Instance URL should be allowed", "", attributes2.getInstanceUrl());

        // Test whitespace-only Instance URL (should be allowed)
        SalesforceCustomerServiceAttributes attributes3 = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, "   ", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertEquals("Whitespace-only Instance URL should be allowed", "   ", attributes3.getInstanceUrl());
    }

    /**
     * Test Instance URL validation in update method.
     */
    @Test
    public void testInstanceUrlValidation_UpdateMethod() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Test valid URL update
        Map<String, Object> validUpdate = new HashMap<>();
        validUpdate.put(SalesforceCustomerServiceAttributes.CLIENT_ID, CLIENT_ID);
        validUpdate.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, CLIENT_SECRET);
        validUpdate.put(SalesforceCustomerServiceAttributes.INSTANCE_URL, "https://updated.my.salesforce.com");
        validUpdate.put(SalesforceCustomerServiceAttributes.USER_NAME, USERNAME);
        validUpdate.put(SalesforceCustomerServiceAttributes.PASS_WORD, PASSWORD);
        validUpdate.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN, SECURITY_TOKEN);
        validUpdate.put(SalesforceCustomerServiceAttributes.SANDBOX, false);

        attributes.update(validUpdate);
        assertEquals("Valid URL should be updated", "https://updated.my.salesforce.com", attributes.getInstanceUrl());

        // Test invalid URL update
        Map<String, Object> invalidUpdate = new HashMap<>();
        invalidUpdate.put(SalesforceCustomerServiceAttributes.CLIENT_ID, CLIENT_ID);
        invalidUpdate.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, CLIENT_SECRET);
        invalidUpdate.put(SalesforceCustomerServiceAttributes.INSTANCE_URL, "invalid-url");
        invalidUpdate.put(SalesforceCustomerServiceAttributes.USER_NAME, USERNAME);
        invalidUpdate.put(SalesforceCustomerServiceAttributes.PASS_WORD, PASSWORD);
        invalidUpdate.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN, SECURITY_TOKEN);
        invalidUpdate.put(SalesforceCustomerServiceAttributes.SANDBOX, false);

        try {
            attributes.update(invalidUpdate);
            fail("Should throw IllegalArgumentException for invalid URL in update");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention invalid format", e.getMessage().contains("Invalid Instance URL format"));
        }
    }

    /**
     * Test OAuth service creation error handling with missing Client ID.
     */
    @Test
    public void testOAuthServiceCreation_MissingClientId() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                null, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Act & Assert
        try {
            attributes.getOauthService();
            fail("Should throw IllegalStateException for missing Client ID");
        } catch (IllegalStateException e) {
            assertTrue("Error message should mention Client ID", e.getMessage().contains("Client ID"));
        }
    }

    /**
     * Test OAuth service creation error handling with missing Client Secret.
     */
    @Test
    public void testOAuthServiceCreation_MissingClientSecret() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, null, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Act & Assert
        try {
            attributes.getOauthService();
            fail("Should throw IllegalStateException for missing Client Secret");
        } catch (IllegalStateException e) {
            assertTrue("Error message should mention Client Secret", e.getMessage().contains("Client Secret"));
        }
    }

    /**
     * Test OAuth service creation error handling with missing routing URL.
     */
    @Test
    public void testOAuthServiceCreation_MissingRoutingUrl() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, null);

        // Act & Assert
        try {
            attributes.getOauthService();
            fail("Should throw IllegalStateException for missing routing URL");
        } catch (IllegalStateException e) {
            assertTrue("Error message should mention routing URL",
                    e.getCause() != null && e.getCause().getMessage().contains("Routing URL"));
        }
    }

    /**
     * Test OAuth service creation error handling with invalid routing URL.
     */
    @Test
    public void testOAuthServiceCreation_InvalidRoutingUrl() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, "not-a-url");

        // Act & Assert
        try {
            attributes.getOauthService();
            fail("Should throw IllegalStateException for invalid routing URL");
        } catch (IllegalStateException e) {
            assertTrue("Error message should mention callback URL format",
                    e.getCause() != null && e.getCause().getMessage().contains("valid HTTP or HTTPS URL"));
        }
    }

    /**
     * Test OAuth service creation with custom domain error handling.
     */
    @Test
    public void testOAuthServiceCreation_CustomDomain_NullDomain() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Act & Assert
        try {
            attributes.getOauthService(null);
            fail("Should throw IllegalArgumentException for null domain");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention domain URL", e.getMessage().contains("Domain URL"));
        }
    }

    /**
     * Test OAuth service creation with custom domain error handling for empty domain.
     */
    @Test
    public void testOAuthServiceCreation_CustomDomain_EmptyDomain() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Act & Assert
        try {
            attributes.getOauthService("");
            fail("Should throw IllegalArgumentException for empty domain");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention domain URL", e.getMessage().contains("Domain URL"));
        }
    }

    /**
     * Test OAuth service creation with custom domain error handling for invalid domain.
     */
    @Test
    public void testOAuthServiceCreation_CustomDomain_InvalidDomain() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Act & Assert
        try {
            attributes.getOauthService("invalid-domain");
            fail("Should throw IllegalArgumentException for invalid domain");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention domain format",
                    e.getMessage().contains("http://") || e.getMessage().contains("https://"));
        }
    }

    /**
     * Test successful OAuth service creation.
     */
    @Test
    public void testOAuthServiceCreation_Success() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Act
        OAuth20Service service = attributes.getOauthService();

        // Assert
        assertNotNull("OAuth service should be created successfully", service);
    }

    /**
     * Test successful OAuth service creation with custom domain.
     */
    @Test
    public void testOAuthServiceCreation_CustomDomain_Success() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        // Act
        OAuth20Service service = attributes.getOauthService("https://test.my.salesforce.com");

        // Assert
        assertNotNull("OAuth service should be created successfully with custom domain", service);
    }

    // ========== Configuration Validation Tests ==========

    @Test
    public void testValidateConfiguration_ClientCredentials_Valid() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.CLIENT_CREDENTIALS);

        assertTrue("Configuration should be valid", result.isValid());
        assertFalse("Should not have errors", result.hasErrors());
        assertEquals("Should have no errors", 0, result.getErrors().size());
    }

    @Test
    public void testValidateConfiguration_ClientCredentials_MissingClientId() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                null, CLIENT_SECRET, INSTANCE_URL, null, null, null, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.CLIENT_CREDENTIALS);

        assertFalse("Configuration should be invalid", result.isValid());
        assertTrue("Should have errors", result.hasErrors());
        assertTrue("Should contain client ID error",
                result.getErrors().stream().anyMatch(error -> error.contains("Client ID is required")));
    }

    @Test
    public void testValidateConfiguration_ClientCredentials_MissingInstanceUrl() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, null, null, null, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.CLIENT_CREDENTIALS);

        assertFalse("Configuration should be invalid", result.isValid());
        assertTrue("Should have errors", result.hasErrors());
        assertTrue("Should contain instance URL error",
                result.getErrors().stream().anyMatch(error -> error.contains("Instance URL is required for Client Credentials flow")));
    }

    @Test
    public void testValidateConfiguration_ClientCredentials_WithUsernamePassword() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.CLIENT_CREDENTIALS);

        assertTrue("Configuration should be valid", result.isValid());
        assertTrue("Should have warnings", result.hasWarnings());
        assertTrue("Should warn about unused credentials",
                result.getWarnings().stream().anyMatch(warning -> warning.contains("Username/password credentials are not needed")));
    }

    @Test
    public void testValidateConfiguration_AuthorizationCode_Valid() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, null, null, null, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.AUTHORIZATION_CODE);

        assertTrue("Configuration should be valid", result.isValid());
        assertFalse("Should not have errors", result.hasErrors());
    }

    @Test
    public void testValidateConfiguration_AuthorizationCode_WithInstanceUrl() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.AUTHORIZATION_CODE);

        assertTrue("Configuration should be valid", result.isValid());
        assertTrue("Should have warnings", result.hasWarnings());
        assertTrue("Should warn about unused instance URL",
                result.getWarnings().stream().anyMatch(warning -> warning.contains("Instance URL is not needed")));
    }

    @Test
    public void testValidateConfiguration_PasswordGrant_Valid() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.PASSWORD_GRANT);

        assertTrue("Configuration should be valid", result.isValid());
        assertFalse("Should not have errors", result.hasErrors());
    }

    @Test
    public void testValidateConfiguration_PasswordGrant_MissingCredentials() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, null, null, null, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.PASSWORD_GRANT);

        assertFalse("Configuration should be invalid", result.isValid());
        assertTrue("Should have errors", result.hasErrors());
        assertTrue("Should contain username error",
                result.getErrors().stream().anyMatch(error -> error.contains("Username is required")));
        assertTrue("Should contain password error",
                result.getErrors().stream().anyMatch(error -> error.contains("Password is required")));
        assertTrue("Should contain security token error",
                result.getErrors().stream().anyMatch(error -> error.contains("Security Token is required")));
    }

    @Test
    public void testValidateConfiguration_AutoDetect_ClientCredentialsAvailable() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.AUTO_DETECT);

        assertTrue("Configuration should be valid", result.isValid());
        assertTrue("Should have info messages", result.hasInfo());
        assertTrue("Should indicate Client Credentials flow will be selected",
                result.getInfo().stream().anyMatch(info -> info.contains("Auto-detection will select: Client Credentials flow")));
    }

    @Test
    public void testValidateConfiguration_AutoDetect_AuthorizationCodeFallback() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, null, null, null, false, ROUTING_URL);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.AUTO_DETECT);

        assertTrue("Configuration should be valid", result.isValid());
        assertTrue("Should have info messages", result.hasInfo());
        assertTrue("Should indicate Authorization Code flow will be selected",
                result.getInfo().stream().anyMatch(info -> info.contains("Auto-detection will select: Authorization Code flow")));
    }

    @Test
    public void testValidateConfiguration_AutoDetect_NoFlowAvailable() {
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
                null, null, null, null, null, null, false, null);

        SalesforceCustomerServiceAttributes.ValidationResult result =
                attributes.validateConfiguration(SalesforceCustomerServiceAttributes.AuthenticationFlowType.AUTO_DETECT);

        assertFalse("Configuration should be invalid", result.isValid());
        assertTrue("Should have errors", result.hasErrors());
        assertTrue("Should indicate no flow is available",
                result.getErrors().stream().anyMatch(error -> error.contains("No authentication flow is available")));
    }

    @Test
    public void testValidationResult_Methods() {
        SalesforceCustomerServiceAttributes.ValidationResult result =
                new SalesforceCustomerServiceAttributes.ValidationResult();

        // Test empty result
        assertTrue("Empty result should be valid", result.isValid());
        assertFalse("Empty result should not have errors", result.hasErrors());
        assertFalse("Empty result should not have warnings", result.hasWarnings());
        assertFalse("Empty result should not have info", result.hasInfo());

        // Add error
        result.addError("Test error");
        assertFalse("Result with error should be invalid", result.isValid());
        assertTrue("Result should have errors", result.hasErrors());
        assertEquals("Should have one error", 1, result.getErrors().size());
        assertEquals("Error message should match", "Test error", result.getErrors().get(0));

        // Add warning
        result.addWarning("Test warning");
        assertTrue("Result should have warnings", result.hasWarnings());
        assertEquals("Should have one warning", 1, result.getWarnings().size());

        // Add info
        result.addInfo("Test info");
        assertTrue("Result should have info", result.hasInfo());
        assertEquals("Should have one info", 1, result.getInfo().size());

        // Test toString
        String resultString = result.toString();
        assertTrue("toString should contain valid=false", resultString.contains("valid=false"));
        assertTrue("toString should contain errors", resultString.contains("errors="));
        assertTrue("toString should contain warnings", resultString.contains("warnings="));
        assertTrue("toString should contain info", resultString.contains("info="));
    }
}
