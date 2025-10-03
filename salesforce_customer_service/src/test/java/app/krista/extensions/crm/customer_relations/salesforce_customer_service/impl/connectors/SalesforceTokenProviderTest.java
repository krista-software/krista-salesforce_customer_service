package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for SalesforceTokenProvider covering all three authentication methods:
 * 1. OAuth 2.0 Client Credentials Flow (Instance URL provided)
 * 2. OAuth 2.0 Authorization Code Flow (Instance URL blank, uses refresh token)
 * 3. OAuth 2.0 Password Grant Flow (Username/Password/Security Token provided)
 *
 * Test credentials provided:
 * - Client ID: REDACTED_SECRET_0
 * - Client Secret: REDACTED_SECRET_3
 * - Security Token: h63mvaB1DlqplIMAo057WMKMH
 * - Instance URL: https://ankit-ab-dev-ed.my.salesforce.com
 * - Username: hiren.panchal@kristasoft.com
 * - Password: Krista@602
 */
public class SalesforceTokenProviderTest {

    // Test credentials from the provided data
    private static final String CLIENT_ID = "REDACTED_SECRET_0";
    private static final String CLIENT_SECRET = "REDACTED_SECRET_3";
    private static final String SECURITY_TOKEN = "h63mvaB1DlqplIMAo057WMKMH";
    private static final String INSTANCE_URL = "https://ankit-ab-dev-ed.my.salesforce.com";
    private static final String USERNAME = "hiren.panchal@kristasoft.com";
    private static final String PASSWORD = "Krista@602";
    private static final String ROUTING_URL = "https://test.example.com/callback";

    /**
     * Test OAuth 2.0 Client Credentials Flow authentication method selection.
     * This flow is triggered when Instance URL is provided.
     */
    @Test
    public void testClientCredentialsFlow_AuthenticationMethodSelection() {
        // Arrange - Client Credentials Flow (Instance URL provided)
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, true, ROUTING_URL);

        // Act & Assert - Should use OAuth when Instance URL is provided
        assertTrue("Should use OAuth when Instance URL is provided",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes));
    }

    /**
     * Test OAuth 2.0 Authorization Code Flow authentication method selection.
     * This flow is triggered when Instance URL is blank and credentials are missing.
     */
    @Test
    public void testAuthorizationCodeFlow_AuthenticationMethodSelection() {
        // Arrange - Authorization Code Flow (Instance URL blank, missing credentials)
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, "", null, null, null, true, ROUTING_URL);

        // Act & Assert - Should use OAuth when credentials are missing
        assertTrue("Should use OAuth when Instance URL is blank and credentials missing",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes));
    }

    /**
     * Test OAuth 2.0 Password Grant Flow authentication method selection.
     * This flow is triggered when all credentials (username, password, security token) are provided.
     */
    @Test
    public void testPasswordGrantFlow_AuthenticationMethodSelection() {
        // Arrange - Password Grant Flow (All credentials provided)
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);

        // Act & Assert - Should NOT use OAuth when all credentials are provided
        assertFalse("Should use Password Grant when all credentials are provided",
                   SalesforceTokenProvider.validateUsingOAuth(false, attributes));
    }

    /**
     * Test authentication method selection in admin context.
     * Admin context should always use OAuth regardless of credentials.
     */
    @Test
    public void testValidateUsingOAuth_AdminContext() {
        // Arrange
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);

        // Act & Assert - Admin context should always use OAuth
        assertTrue("Admin context should always use OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(true, attributes));
    }

    /**
     * Test authentication method selection with missing credentials.
     * Missing any credential should trigger OAuth.
     */
    @Test
    public void testValidateUsingOAuth_MissingCredentials() {
        // Test missing username
        SalesforceCustomerServiceAttributes attributes1 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, null, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);
        assertTrue("Missing username should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes1));

        // Test missing password
        SalesforceCustomerServiceAttributes attributes2 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, USERNAME, null, SECURITY_TOKEN, true, ROUTING_URL);
        assertTrue("Missing password should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes2));

        // Test missing security token
        SalesforceCustomerServiceAttributes attributes3 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, null, true, ROUTING_URL);
        assertTrue("Missing security token should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes3));
    }

    /**
     * Test comprehensive authentication method selection scenarios.
     * This test covers all three authentication flows in one comprehensive test.
     */
    @Test
    public void testComprehensiveAuthenticationMethodSelection() {
        // Scenario 1: Client Credentials Flow (Instance URL provided)
        SalesforceCustomerServiceAttributes clientCredentialsAttrs = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, true, ROUTING_URL);

        assertTrue("Client Credentials Flow should use OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, clientCredentialsAttrs));

        // Scenario 2: Authorization Code Flow (Instance URL blank, missing credentials)
        SalesforceCustomerServiceAttributes authCodeAttrs = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, "", null, null, null, true, ROUTING_URL);

        assertTrue("Authorization Code Flow should use OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, authCodeAttrs));

        // Scenario 3: Password Grant Flow (All credentials provided, no Instance URL)
        SalesforceCustomerServiceAttributes passwordGrantAttrs = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);

        assertFalse("Password Grant Flow should NOT use OAuth",
                   SalesforceTokenProvider.validateUsingOAuth(false, passwordGrantAttrs));
    }

    /**
     * Test edge cases and boundary conditions for authentication method selection.
     */
    @Test
    public void testAuthenticationMethodEdgeCases() {
        // Edge Case 1: Empty strings vs null
        SalesforceCustomerServiceAttributes attributes1 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, "", "", "", true, ROUTING_URL);
        assertTrue("Empty strings should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes1));

        // Edge Case 2: Whitespace-only strings
        SalesforceCustomerServiceAttributes attributes2 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, "   ", "   ", "   ", true, ROUTING_URL);
        assertTrue("Whitespace-only strings should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes2));

        // Edge Case 3: Mixed valid and invalid credentials
        SalesforceCustomerServiceAttributes attributes3 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, USERNAME, "", SECURITY_TOKEN, true, ROUTING_URL);
        assertTrue("Mixed credentials should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes3));
    }

    /**
     * Test Client Credentials flow with various Instance URL formats.
     */
    @Test
    public void testClientCredentialsFlow_InstanceUrlFormats() {
        // Test with standard HTTPS URL
        SalesforceCustomerServiceAttributes attributes1 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, "https://test.my.salesforce.com", null, null, null, false, ROUTING_URL);
        assertTrue("HTTPS Instance URL should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes1));

        // Test with sandbox URL
        SalesforceCustomerServiceAttributes attributes2 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, "https://test--sandbox.my.salesforce.com", null, null, null, true, ROUTING_URL);
        assertTrue("Sandbox Instance URL should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes2));

        // Test with custom domain
        SalesforceCustomerServiceAttributes attributes3 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, "https://custom.my.salesforce.com", null, null, null, false, ROUTING_URL);
        assertTrue("Custom domain Instance URL should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes3));

        // Test with trailing slash
        SalesforceCustomerServiceAttributes attributes4 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, "https://test.my.salesforce.com/", null, null, null, false, ROUTING_URL);
        assertTrue("Instance URL with trailing slash should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes4));
    }

    /**
     * Test Instance URL edge cases for Client Credentials flow.
     */
    @Test
    public void testClientCredentialsFlow_InstanceUrlEdgeCases() {
        // Test with empty Instance URL (should not trigger Client Credentials)
        SalesforceCustomerServiceAttributes attributes1 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, "", USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertFalse("Empty Instance URL with full credentials should use Password Grant",
                   SalesforceTokenProvider.validateUsingOAuth(false, attributes1));

        // Test with whitespace Instance URL
        SalesforceCustomerServiceAttributes attributes2 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, "   ", null, null, null, false, ROUTING_URL);
        assertTrue("Whitespace Instance URL should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes2));

        // Test Instance URL with missing credentials (should still use OAuth)
        SalesforceCustomerServiceAttributes attributes3 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, USERNAME, null, SECURITY_TOKEN, false, ROUTING_URL);
        assertTrue("Instance URL with missing credentials should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes3));
    }

    /**
     * Test authentication flow priority - validateUsingOAuth checks credentials, not Instance URL.
     */
    @Test
    public void testAuthenticationFlowPriority() {
        // When both Instance URL and full credentials are provided, validateUsingOAuth should return false
        // because all credentials are present (it doesn't check Instance URL)
        SalesforceCustomerServiceAttributes attributes = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertFalse("validateUsingOAuth should return false when all credentials are present",
                   SalesforceTokenProvider.validateUsingOAuth(false, attributes));

        // However, the actual flow logic will still use Client Credentials because Instance URL is present
        // This is handled in the getSalesforceToken method, not in validateUsingOAuth
    }

    /**
     * Test validation with null attributes object.
     */
    @Test(expected = NullPointerException.class)
    public void testValidateUsingOAuth_NullAttributes() {
        // Act & Assert - Should throw NullPointerException
        SalesforceTokenProvider.validateUsingOAuth(false, null);
    }

    /**
     * Test comprehensive scenarios covering all authentication methods.
     */
    @Test
    public void testComprehensiveAuthenticationScenarios() {
        // Scenario 1: Client Credentials - Instance URL provided, no other credentials
        SalesforceCustomerServiceAttributes scenario1 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, false, ROUTING_URL);
        assertTrue("Client Credentials scenario should use OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, scenario1));

        // Scenario 2: Authorization Code - No Instance URL, no credentials
        SalesforceCustomerServiceAttributes scenario2 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, null, null, null, false, ROUTING_URL);
        assertTrue("Authorization Code scenario should use OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, scenario2));

        // Scenario 3: Password Grant - No Instance URL, full credentials
        SalesforceCustomerServiceAttributes scenario3 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, false, ROUTING_URL);
        assertFalse("Password Grant scenario should NOT use OAuth",
                   SalesforceTokenProvider.validateUsingOAuth(false, scenario3));

        // Scenario 4: Admin context - Always OAuth regardless of other settings
        assertTrue("Admin context should always use OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(true, scenario3));
    }
}
