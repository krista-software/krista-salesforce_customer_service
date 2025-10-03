package app.krista.extensions.crm.customer_relations.salesforce_customer_service.api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for SalesforceCustomApi class.
 * Tests the custom API implementation for OAuth 2.0 Client Credentials flow.
 */
public class SalesforceCustomApiTest {

    private static final String VALID_DOMAIN = "https://test.my.salesforce.com";
    private static final String VALID_SANDBOX_DOMAIN = "https://test--sandbox.my.salesforce.com";
    private static final String DOMAIN_WITHOUT_PROTOCOL = "test.my.salesforce.com";
    private static final String DOMAIN_WITH_TRAILING_SLASH = "https://test.my.salesforce.com/";

    /**
     * Test successful creation of SalesforceCustomApi with valid domain.
     */
    @Test
    public void testConstructor_ValidDomain() {
        // Act
        SalesforceCustomApi api = new SalesforceCustomApi(VALID_DOMAIN);

        // Assert
        assertNotNull("API instance should not be null", api);
        assertEquals("Access token endpoint should be correctly constructed",
                VALID_DOMAIN + "/services/oauth2/token",
                api.getAccessTokenEndpoint());
    }

    /**
     * Test access token endpoint construction with various domain formats.
     */
    @Test
    public void testGetAccessTokenEndpoint_VariousDomains() {
        // Test standard domain
        SalesforceCustomApi api1 = new SalesforceCustomApi(VALID_DOMAIN);
        assertEquals("Standard domain should work correctly",
                VALID_DOMAIN + "/services/oauth2/token",
                api1.getAccessTokenEndpoint());

        // Test sandbox domain
        SalesforceCustomApi api2 = new SalesforceCustomApi(VALID_SANDBOX_DOMAIN);
        assertEquals("Sandbox domain should work correctly",
                VALID_SANDBOX_DOMAIN + "/services/oauth2/token",
                api2.getAccessTokenEndpoint());

        // Test domain with trailing slash (results in double slash)
        SalesforceCustomApi api3 = new SalesforceCustomApi(DOMAIN_WITH_TRAILING_SLASH);
        assertEquals("Domain with trailing slash should result in double slash",
                DOMAIN_WITH_TRAILING_SLASH + "/services/oauth2/token",
                api3.getAccessTokenEndpoint());
    }

    /**
     * Test authorization base URL construction with various domain formats.
     */
    @Test
    public void testGetAuthorizationBaseUrl_VariousDomains() {
        // Test standard domain
        SalesforceCustomApi api1 = new SalesforceCustomApi(VALID_DOMAIN);
        assertEquals("Standard domain authorization URL should be correct",
                VALID_DOMAIN + "/services/oauth2/authorize",
                api1.getAuthorizationBaseUrl());

        // Test sandbox domain
        SalesforceCustomApi api2 = new SalesforceCustomApi(VALID_SANDBOX_DOMAIN);
        assertEquals("Sandbox domain authorization URL should be correct",
                VALID_SANDBOX_DOMAIN + "/services/oauth2/authorize",
                api2.getAuthorizationBaseUrl());

        // Test domain with trailing slash (results in double slash)
        SalesforceCustomApi api3 = new SalesforceCustomApi(DOMAIN_WITH_TRAILING_SLASH);
        assertEquals("Domain with trailing slash authorization URL should result in double slash",
                DOMAIN_WITH_TRAILING_SLASH + "/services/oauth2/authorize",
                api3.getAuthorizationBaseUrl());
    }

    /**
     * Test constructor with null domain.
     * The constructor should now throw IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullDomain() {
        // Act & Assert - Should throw IllegalArgumentException
        new SalesforceCustomApi(null);
    }

    /**
     * Test constructor with empty domain.
     * The constructor should now throw IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_EmptyDomain() {
        // Act & Assert - Should throw IllegalArgumentException
        new SalesforceCustomApi("");
    }

    /**
     * Test constructor with whitespace-only domain.
     * The constructor should now throw IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_WhitespaceDomain() {
        // Act & Assert - Should throw IllegalArgumentException
        new SalesforceCustomApi("   ");
    }

    /**
     * Test that the API correctly rejects domains without protocol.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDomainWithoutProtocol() {
        // Act & Assert - Should throw IllegalArgumentException
        new SalesforceCustomApi(DOMAIN_WITHOUT_PROTOCOL);
    }

    /**
     * Test endpoint paths are correctly formatted.
     */
    @Test
    public void testEndpointPaths() {
        SalesforceCustomApi api = new SalesforceCustomApi(VALID_DOMAIN);

        // Assert endpoint paths
        assertTrue("Access token endpoint should end with correct path",
                api.getAccessTokenEndpoint().endsWith("/services/oauth2/token"));
        assertTrue("Authorization base URL should end with correct path",
                api.getAuthorizationBaseUrl().endsWith("/services/oauth2/authorize"));
    }

    /**
     * Test that different instances with same domain produce same results.
     */
    @Test
    public void testConsistency_SameDomain() {
        // Arrange
        SalesforceCustomApi api1 = new SalesforceCustomApi(VALID_DOMAIN);
        SalesforceCustomApi api2 = new SalesforceCustomApi(VALID_DOMAIN);

        // Assert
        assertEquals("Same domain should produce same access token endpoint",
                api1.getAccessTokenEndpoint(), api2.getAccessTokenEndpoint());
        assertEquals("Same domain should produce same authorization base URL",
                api1.getAuthorizationBaseUrl(), api2.getAuthorizationBaseUrl());
    }

    /**
     * Test that different domains produce different results.
     */
    @Test
    public void testDifferentDomains_ProduceDifferentResults() {
        // Arrange
        SalesforceCustomApi api1 = new SalesforceCustomApi("https://domain1.salesforce.com");
        SalesforceCustomApi api2 = new SalesforceCustomApi("https://domain2.salesforce.com");

        // Assert
        assertNotEquals("Different domains should produce different access token endpoints",
                api1.getAccessTokenEndpoint(), api2.getAccessTokenEndpoint());
        assertNotEquals("Different domains should produce different authorization base URLs",
                api1.getAuthorizationBaseUrl(), api2.getAuthorizationBaseUrl());
    }

    /**
     * Test domain validation with invalid protocols.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDomainValidation_InvalidProtocol() {
        // Act & Assert - Should throw IllegalArgumentException
        new SalesforceCustomApi("ftp://test.salesforce.com");
    }

    /**
     * Test domain validation with missing protocol.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDomainValidation_MissingProtocol() {
        // Act & Assert - Should throw IllegalArgumentException
        new SalesforceCustomApi("test.salesforce.com");
    }

    /**
     * Test domain validation with too short domain.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDomainValidation_TooShort() {
        // Act & Assert - Should throw IllegalArgumentException
        new SalesforceCustomApi("http://");
    }

    /**
     * Test domain validation with valid domains.
     */
    @Test
    public void testDomainValidation_ValidDomains() {
        // Test various valid domain formats
        SalesforceCustomApi api1 = new SalesforceCustomApi("https://test.my.salesforce.com");
        assertNotNull("Valid HTTPS domain should be accepted", api1);

        SalesforceCustomApi api2 = new SalesforceCustomApi("http://localhost:8080");
        assertNotNull("Valid HTTP domain should be accepted", api2);

        SalesforceCustomApi api3 = new SalesforceCustomApi("https://custom-domain.example.com");
        assertNotNull("Valid custom domain should be accepted", api3);
    }

    /**
     * Test domain trimming functionality.
     */
    @Test
    public void testDomainTrimming() {
        // Arrange & Act
        SalesforceCustomApi api = new SalesforceCustomApi("  https://test.my.salesforce.com  ");

        // Assert - Domain should be trimmed
        assertEquals("Domain should be trimmed",
                "https://test.my.salesforce.com/services/oauth2/token",
                api.getAccessTokenEndpoint());
    }

    /**
     * Test comprehensive domain validation scenarios.
     */
    @Test
    public void testDomainValidation_ComprehensiveScenarios() {
        // Valid scenarios should work
        assertNotNull("Standard Salesforce URL should work",
                new SalesforceCustomApi("https://myorg.my.salesforce.com"));
        assertNotNull("Sandbox URL should work",
                new SalesforceCustomApi("https://myorg--sandbox.my.salesforce.com"));
        assertNotNull("Lightning URL should work",
                new SalesforceCustomApi("https://myorg.lightning.force.com"));
        assertNotNull("Custom domain should work",
                new SalesforceCustomApi("https://custom.example.com"));

        // Invalid scenarios should throw exceptions
        String[] invalidDomains = {
            null,
            "",
            "   ",
            "not-a-url",
            "ftp://test.com",
            "https://",
            "http://"
        };

        for (String invalidDomain : invalidDomains) {
            try {
                new SalesforceCustomApi(invalidDomain);
                fail("Should throw IllegalArgumentException for invalid domain: " + invalidDomain);
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }
    }
}
