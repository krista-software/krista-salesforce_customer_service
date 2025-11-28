package app.krista.extensions.crm.customer_relations.salesforce_customer_service;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

/**
 * Base class for all Salesforce Customer Service extension tests.
 * Provides common setup and utilities for test classes.
 * 
 * <p>This base class centralizes common test infrastructure to reduce code duplication
 * across test classes. All test classes should extend this base class to ensure
 * consistent test setup and access to common utilities.</p>
 * 
 * <h3>Features:</h3>
 * <ul>
 *   <li>Automatic Mockito initialization for all test classes</li>
 *   <li>Common test data generation utilities</li>
 *   <li>Consistent test setup across all test suites</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * public class MyTest extends SalesforceTestBase {
 *     
 *     {@literal @}Mock
 *     private SomeService mockService;
 *     
 *     {@literal @}Before
 *     public void setUp() {
 *         // baseSetUp() is called automatically by parent class
 *         // Add test-specific setup here
 *     }
 *     
 *     {@literal @}Test
 *     public void testSomething() {
 *         String caseId = createTestCaseId("001");
 *         // ... test implementation
 *     }
 * }
 * </pre>
 * 
 * @author Krista Development Team
 * @version 2.0.17
 * @since 2.0.17
 */
public abstract class SalesforceTestBase {
    
    /**
     * Common setup executed before each test method.
     * Initializes Mockito annotations for all test classes.
     * 
     * <p>This method is automatically called before each test method in subclasses.
     * Subclasses can define their own {@code @Before} methods for test-specific setup,
     * and this base setup will still be executed first.</p>
     */
    @Before
    public void baseSetUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    // ==================== Test Data Generation Utilities ====================
    
    /**
     * Creates a test Salesforce Case ID with the specified suffix.
     * 
     * <p>Salesforce Case IDs follow the format: 500XXXXXXXXXXXXX (15 or 18 characters).
     * This utility generates valid-looking test case IDs for use in unit tests.</p>
     * 
     * @param suffix unique suffix for the case ID (should be 3 characters for 15-char ID)
     * @return formatted Salesforce case ID starting with "500"
     * 
     * @example
     * <pre>
     * String caseId = createTestCaseId("001"); // Returns "500xx000000001"
     * String caseId2 = createTestCaseId("ABC"); // Returns "500xx00000ABC"
     * </pre>
     */
    protected String createTestCaseId(String suffix) {
        return "500xx000000" + suffix;
    }
    
    /**
     * Creates a test Salesforce Account ID with the specified suffix.
     * 
     * <p>Salesforce Account IDs follow the format: 001XXXXXXXXXXXXX (15 or 18 characters).
     * This utility generates valid-looking test account IDs for use in unit tests.</p>
     * 
     * @param suffix unique suffix for the account ID (should be 3 characters for 15-char ID)
     * @return formatted Salesforce account ID starting with "001"
     * 
     * @example
     * <pre>
     * String accountId = createTestAccountId("001"); // Returns "001xx000000001"
     * String accountId2 = createTestAccountId("XYZ"); // Returns "001xx00000XYZ"
     * </pre>
     */
    protected String createTestAccountId(String suffix) {
        return "001xx000000" + suffix;
    }
    
    /**
     * Creates a test Salesforce Contact ID with the specified suffix.
     * 
     * <p>Salesforce Contact IDs follow the format: 003XXXXXXXXXXXXX (15 or 18 characters).
     * This utility generates valid-looking test contact IDs for use in unit tests.</p>
     * 
     * @param suffix unique suffix for the contact ID (should be 3 characters for 15-char ID)
     * @return formatted Salesforce contact ID starting with "003"
     * 
     * @example
     * <pre>
     * String contactId = createTestContactId("001"); // Returns "003xx000000001"
     * </pre>
     */
    protected String createTestContactId(String suffix) {
        return "003xx000000" + suffix;
    }
    
    /**
     * Creates a test Salesforce User ID with the specified suffix.
     * 
     * <p>Salesforce User IDs follow the format: 005XXXXXXXXXXXXX (15 or 18 characters).
     * This utility generates valid-looking test user IDs for use in unit tests.</p>
     * 
     * @param suffix unique suffix for the user ID (should be 3 characters for 15-char ID)
     * @return formatted Salesforce user ID starting with "005"
     * 
     * @example
     * <pre>
     * String userId = createTestUserId("001"); // Returns "005xx000000001"
     * </pre>
     */
    protected String createTestUserId(String suffix) {
        return "005xx000000" + suffix;
    }
    
    /**
     * Creates a test Salesforce instance URL.
     * 
     * <p>Generates a valid Salesforce instance URL for testing purposes.
     * The URL follows the standard Salesforce My Domain format.</p>
     * 
     * @param domain the domain name (e.g., "test", "production", "sandbox")
     * @return formatted Salesforce instance URL
     * 
     * @example
     * <pre>
     * String url = createTestInstanceUrl("test"); // Returns "https://test.my.salesforce.com"
     * String url2 = createTestInstanceUrl("mycompany"); // Returns "https://mycompany.my.salesforce.com"
     * </pre>
     */
    protected String createTestInstanceUrl(String domain) {
        return "https://" + domain + ".my.salesforce.com";
    }
    
    /**
     * Creates a test OAuth client ID.
     * 
     * <p>Generates a mock OAuth client ID for testing authentication flows.
     * The format mimics real Salesforce Connected App client IDs.</p>
     * 
     * @param suffix unique suffix for the client ID
     * @return formatted OAuth client ID
     * 
     * @example
     * <pre>
     * String clientId = createTestClientId("001"); // Returns "3MVG9test_client_id_001"
     * </pre>
     */
    protected String createTestClientId(String suffix) {
        return "3MVG9test_client_id_" + suffix;
    }
    
    /**
     * Creates a test OAuth client secret.
     * 
     * <p>Generates a mock OAuth client secret for testing authentication flows.</p>
     * 
     * @param suffix unique suffix for the client secret
     * @return formatted OAuth client secret
     * 
     * @example
     * <pre>
     * String secret = createTestClientSecret("001"); // Returns "test_client_secret_001"
     * </pre>
     */
    protected String createTestClientSecret(String suffix) {
        return "test_client_secret_" + suffix;
    }
    
    /**
     * Creates a test email address.
     * 
     * <p>Generates a test email address for use in test data.</p>
     * 
     * @param username the username part of the email
     * @return formatted email address
     * 
     * @example
     * <pre>
     * String email = createTestEmail("john.doe"); // Returns "john.doe@test.example.com"
     * </pre>
     */
    protected String createTestEmail(String username) {
        return username + "@test.example.com";
    }
    
    /**
     * Creates a test access token.
     * 
     * <p>Generates a mock OAuth access token for testing authenticated API calls.</p>
     * 
     * @param suffix unique suffix for the token
     * @return formatted access token
     * 
     * @example
     * <pre>
     * String token = createTestAccessToken("001"); // Returns "00D_test_access_token_001"
     * </pre>
     */
    protected String createTestAccessToken(String suffix) {
        return "00D_test_access_token_" + suffix;
    }
    
    /**
     * Creates a test SOQL query string.
     * 
     * <p>Generates a basic SOQL query for testing query building and execution.</p>
     * 
     * @param objectName the Salesforce object name (e.g., "Case", "Account")
     * @param fields comma-separated list of fields to query
     * @return formatted SOQL query string
     * 
     * @example
     * <pre>
     * String query = createTestQuery("Case", "Id, Subject, Status");
     * // Returns "SELECT Id, Subject, Status FROM Case"
     * </pre>
     */
    protected String createTestQuery(String objectName, String fields) {
        return "SELECT " + fields + " FROM " + objectName;
    }
}

