package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test class for CaseIdValidator.
 * Tests validation logic for Salesforce Case IDs.
 * 
 * Test Coverage Areas:
 * - Valid case ID (case exists in Salesforce)
 * - Invalid case ID (case does not exist)
 * - Authorization exceptions
 * - General exceptions
 * - Field metadata (name, type, messages)
 */
public class CaseIdValidatorTest {

    private static final String VALID_CASE_ID = "500xx000000YYYY";
    private static final String INVALID_CASE_ID = "500xx000000ZZZZ";

    @Mock
    private CaseDaoImpl mockCaseDao;

    @Mock
    private Case mockCase;

    private CaseIdValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new CaseIdValidator(mockCaseDao);
        context = new HashMap<>();
    }

    // ========== Valid Input Tests ==========

    /**
     * Test validation with valid case ID that exists in Salesforce.
     */
    @Test
    public void testValidate_WithValidCaseId_ReturnsTrue() throws Exception {
        // Arrange
        when(mockCaseDao.getCaseById(VALID_CASE_ID)).thenReturn(mockCase);

        // Act
        Boolean result = validator.validate(VALID_CASE_ID, context);

        // Assert
        assertTrue("Should return true for valid case ID", result);
        verify(mockCaseDao, times(1)).getCaseById(VALID_CASE_ID);
    }

    /**
     * Test validation with valid case ID called multiple times.
     */
    @Test
    public void testValidate_WithValidCaseId_CalledMultipleTimes_ReturnsTrue() throws Exception {
        // Arrange
        when(mockCaseDao.getCaseById(VALID_CASE_ID)).thenReturn(mockCase);

        // Act
        Boolean result1 = validator.validate(VALID_CASE_ID, context);
        Boolean result2 = validator.validate(VALID_CASE_ID, context);

        // Assert
        assertTrue("First call should return true", result1);
        assertTrue("Second call should return true", result2);
        verify(mockCaseDao, times(2)).getCaseById(VALID_CASE_ID);
    }

    // ========== Invalid Input Tests ==========

    /**
     * Test validation with invalid case ID that does not exist.
     */
    @Test
    public void testValidate_WithInvalidCaseId_ReturnsFalse() throws Exception {
        // Arrange
        when(mockCaseDao.getCaseById(INVALID_CASE_ID))
                .thenThrow(new RuntimeException("Case not found"));

        // Act
        Boolean result = validator.validate(INVALID_CASE_ID, context);

        // Assert
        assertFalse("Should return false for invalid case ID", result);
        verify(mockCaseDao, times(1)).getCaseById(INVALID_CASE_ID);
    }

    /**
     * Test validation with null case ID.
     */
    @Test
    public void testValidate_WithNullCaseId_ReturnsFalse() throws Exception {
        // Arrange
        when(mockCaseDao.getCaseById(null))
                .thenThrow(new IllegalArgumentException("Case ID cannot be null"));

        // Act
        Boolean result = validator.validate(null, context);

        // Assert
        assertFalse("Should return false for null case ID", result);
    }

    /**
     * Test validation with empty case ID.
     */
    @Test
    public void testValidate_WithEmptyCaseId_ReturnsFalse() throws Exception {
        // Arrange
        when(mockCaseDao.getCaseById(""))
                .thenThrow(new IllegalArgumentException("Case ID cannot be empty"));

        // Act
        Boolean result = validator.validate("", context);

        // Assert
        assertFalse("Should return false for empty case ID", result);
    }

    // ========== Authorization Exception Tests ==========

    /**
     * Test validation with MustAuthorizeException (should propagate).
     */
    @Test(expected = MustAuthorizeException.class)
    public void testValidate_WithAuthorizationException_ThrowsException() throws Exception {
        // Arrange
        MustAuthorizeException authException = mock(MustAuthorizeException.class);
        when(authException.getMessage()).thenReturn("Authorization required");
        when(mockCaseDao.getCaseById(VALID_CASE_ID)).thenThrow(authException);

        // Act
        validator.validate(VALID_CASE_ID, context);

        // Assert - exception should be thrown
    }

    /**
     * Test validation with MustAuthorizeException verifies it's logged and rethrown.
     */
    @Test
    public void testValidate_WithAuthorizationException_LogsAndRethrows() throws Exception {
        // Arrange
        MustAuthorizeException authException = mock(MustAuthorizeException.class);
        when(authException.getMessage()).thenReturn("Authorization required");
        when(mockCaseDao.getCaseById(VALID_CASE_ID)).thenThrow(authException);

        // Act & Assert
        try {
            validator.validate(VALID_CASE_ID, context);
            fail("Should have thrown MustAuthorizeException");
        } catch (MustAuthorizeException e) {
            assertEquals("Should be the same exception", authException, e);
            verify(mockCaseDao, times(1)).getCaseById(VALID_CASE_ID);
        }
    }

    // ========== General Exception Tests ==========

    /**
     * Test validation with general exception.
     */
    @Test
    public void testValidate_WithGeneralException_ReturnsFalse() throws Exception {
        // Arrange
        when(mockCaseDao.getCaseById(INVALID_CASE_ID))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        Boolean result = validator.validate(INVALID_CASE_ID, context);

        // Assert
        assertFalse("Should return false for general exception", result);
    }

    /**
     * Test validation with NullPointerException.
     */
    @Test
    public void testValidate_WithNullPointerException_ReturnsFalse() throws Exception {
        // Arrange
        when(mockCaseDao.getCaseById(anyString()))
                .thenThrow(new NullPointerException("Null pointer"));

        // Act
        Boolean result = validator.validate("test", context);

        // Assert
        assertFalse("Should return false for NullPointerException", result);
    }

    /**
     * Test validation with IllegalArgumentException.
     */
    @Test
    public void testValidate_WithIllegalArgumentException_ReturnsFalse() throws Exception {
        // Arrange
        when(mockCaseDao.getCaseById(anyString()))
                .thenThrow(new IllegalArgumentException("Invalid argument"));

        // Act
        Boolean result = validator.validate("test", context);

        // Assert
        assertFalse("Should return false for IllegalArgumentException", result);
    }

    // ========== Field Metadata Tests ==========

    /**
     * Test getFetchFieldName returns correct field name.
     */
    @Test
    public void testGetFetchFieldName_ReturnsCorrectName() {
        // Act
        String fieldName = validator.getFetchFieldName();

        // Assert
        assertEquals("Should return correct field name", 
                SalesforceErrorHandlingResource.CASE_ID, fieldName);
    }

    /**
     * Test getFieldType returns text field type.
     */
    @Test
    public void testGetFieldType_ReturnsTextFieldType() {
        // Act
        String fieldType = validator.getFieldType();

        // Assert
        assertEquals("Should return text field type", FieldTypes.TEXT_FIELD, fieldType);
    }

    /**
     * Test getFetchStepMessage returns appropriate message.
     */
    @Test
    public void testGetFetchStepMessage_ReturnsAppropriateMessage() {
        // Act
        String message = validator.getFetchStepMessage();

        // Assert
        assertNotNull("Fetch step message should not be null", message);
        assertTrue("Message should mention Case ID", message.toLowerCase().contains("case id"));
    }

    /**
     * Test getConfirmationStepMessage with invalid case ID.
     */
    @Test
    public void testGetConfirmationStepMessage_WithInvalidCaseId_ReturnsFormattedMessage() {
        // Arrange
        String invalidCaseId = "INVALID123";

        // Act
        String message = validator.getConfirmationStepMessage(invalidCaseId, context);

        // Assert
        assertNotNull("Confirmation message should not be null", message);
        assertTrue("Message should contain the invalid case ID", message.contains(invalidCaseId));
        assertTrue("Message should mention validation failure", 
                message.toLowerCase().contains("validation failed") || 
                message.toLowerCase().contains("does not exist"));
    }

    /**
     * Test getErrMessage with invalid case ID.
     */
    @Test
    public void testGetErrMessage_WithInvalidCaseId_ReturnsFormattedMessage() {
        // Arrange
        String invalidCaseId = "INVALID456";

        // Act
        String message = validator.getErrMessage(invalidCaseId);

        // Assert
        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain the invalid case ID", message.contains(invalidCaseId));
        assertTrue("Message should mention operation failure", 
                message.toLowerCase().contains("failed") || 
                message.toLowerCase().contains("invalid"));
    }

    /**
     * Test getErrMessage with null case ID.
     */
    @Test
    public void testGetErrMessage_WithNullCaseId_ReturnsFormattedMessage() {
        // Act
        String message = validator.getErrMessage(null);

        // Assert
        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain 'null'", message.contains("null"));
    }

    /**
     * Test constructor with null CaseDao.
     */
    @Test
    public void testConstructor_WithNullCaseDao_Success() {
        // Act
        CaseIdValidator validatorWithNull = new CaseIdValidator(null);

        // Assert
        assertNotNull("Validator should be created even with null CaseDao", validatorWithNull);
    }
}

