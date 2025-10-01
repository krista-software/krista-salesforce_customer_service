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

        System.out.println("✅ Client Credentials Flow - Method Selection Test Passed");
        System.out.println("Instance URL: " + INSTANCE_URL);
        System.out.println("Authentication Method: OAuth 2.0 Client Credentials Flow");
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

        System.out.println("✅ Authorization Code Flow - Method Selection Test Passed");
        System.out.println("Instance URL: [BLANK]");
        System.out.println("Authentication Method: OAuth 2.0 Authorization Code Flow");
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

        System.out.println("✅ Password Grant Flow - Method Selection Test Passed");
        System.out.println("Username: " + USERNAME);
        System.out.println("Authentication Method: OAuth 2.0 Password Grant Flow");
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

        System.out.println("✅ Admin Context - Always OAuth Test Passed");
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

        System.out.println("✅ Missing Credentials - OAuth Trigger Test Passed");
    }

    /**
     * Test comprehensive authentication method selection scenarios.
     * This test covers all three authentication flows in one comprehensive test.
     */
    @Test
    public void testComprehensiveAuthenticationMethodSelection() {
        System.out.println("=== Comprehensive Authentication Method Selection Test ===");

        // Scenario 1: Client Credentials Flow (Instance URL provided)
        SalesforceCustomerServiceAttributes clientCredentialsAttrs = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, INSTANCE_URL, null, null, null, true, ROUTING_URL);

        assertTrue("Client Credentials Flow should use OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, clientCredentialsAttrs));
        System.out.println("✅ Client Credentials Flow: OAuth Selected");

        // Scenario 2: Authorization Code Flow (Instance URL blank, missing credentials)
        SalesforceCustomerServiceAttributes authCodeAttrs = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, "", null, null, null, true, ROUTING_URL);

        assertTrue("Authorization Code Flow should use OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, authCodeAttrs));
        System.out.println("✅ Authorization Code Flow: OAuth Selected");

        // Scenario 3: Password Grant Flow (All credentials provided, no Instance URL)
        SalesforceCustomerServiceAttributes passwordGrantAttrs = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, USERNAME, PASSWORD, SECURITY_TOKEN, true, ROUTING_URL);

        assertFalse("Password Grant Flow should NOT use OAuth",
                   SalesforceTokenProvider.validateUsingOAuth(false, passwordGrantAttrs));
        System.out.println("✅ Password Grant Flow: Direct Authentication Selected");

        System.out.println("=== All Authentication Methods Tested Successfully ===");
    }

    /**
     * Test edge cases and boundary conditions for authentication method selection.
     */
    @Test
    public void testAuthenticationMethodEdgeCases() {
        System.out.println("=== Authentication Method Edge Cases Test ===");

        // Edge Case 1: Empty strings vs null
        SalesforceCustomerServiceAttributes attributes1 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, "", "", "", true, ROUTING_URL);
        assertTrue("Empty strings should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes1));
        System.out.println("✅ Empty strings trigger OAuth");

        // Edge Case 2: Whitespace-only strings
        SalesforceCustomerServiceAttributes attributes2 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, "   ", "   ", "   ", true, ROUTING_URL);
        assertTrue("Whitespace-only strings should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes2));
        System.out.println("✅ Whitespace-only strings trigger OAuth");

        // Edge Case 3: Mixed valid and invalid credentials
        SalesforceCustomerServiceAttributes attributes3 = new SalesforceCustomerServiceAttributes(
            CLIENT_ID, CLIENT_SECRET, null, USERNAME, "", SECURITY_TOKEN, true, ROUTING_URL);
        assertTrue("Mixed credentials should trigger OAuth",
                  SalesforceTokenProvider.validateUsingOAuth(false, attributes3));
        System.out.println("✅ Mixed credentials trigger OAuth");

        System.out.println("=== Edge Cases Tested Successfully ===");
    }
}
