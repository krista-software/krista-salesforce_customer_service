package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for NumberOfPostsValidator.
 * Tests validation logic for number of posts (must be between 1 and 100).
 * 
 * Test Coverage Areas:
 * - Valid number of posts (boundary and middle values)
 * - Invalid number of posts (negative, zero, over 100)
 * - Null and empty inputs
 * - Invalid number formats
 * - Field metadata (name, type, messages)
 */
public class NumberOfPostsValidatorTest {

    private NumberOfPostsValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @Before
    public void setUp() {
        validator = new NumberOfPostsValidator();
        context = new HashMap<>();
    }

    // ========== Valid Input Tests ==========

    /**
     * Test validation with valid number of posts (middle value).
     */
    @Test
    public void testValidate_WithValidMiddleValue_ReturnsTrue() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "50");

        // Act
        Boolean result = validator.validate("50", context);

        // Assert
        assertTrue("Should return true for valid middle value (50)", result);
    }

    /**
     * Test validation with minimum valid value (1).
     */
    @Test
    public void testValidate_WithMinimumValue_ReturnsTrue() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "1");

        // Act
        Boolean result = validator.validate("1", context);

        // Assert
        assertTrue("Should return true for minimum valid value (1)", result);
    }

    /**
     * Test validation with maximum valid value (100).
     */
    @Test
    public void testValidate_WithMaximumValue_ReturnsTrue() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "100");

        // Act
        Boolean result = validator.validate("100", context);

        // Assert
        assertTrue("Should return true for maximum valid value (100)", result);
    }

    /**
     * Test validation with decimal value within range.
     */
    @Test
    public void testValidate_WithDecimalValue_ReturnsTrue() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "50.5");

        // Act
        Boolean result = validator.validate("50.5", context);

        // Assert
        assertTrue("Should return true for decimal value within range", result);
    }

    // ========== Invalid Input Tests ==========

    /**
     * Test validation with zero value.
     */
    @Test
    public void testValidate_WithZeroValue_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "0");

        // Act
        Boolean result = validator.validate("0", context);

        // Assert
        assertFalse("Should return false for zero value", result);
    }

    /**
     * Test validation with negative value.
     */
    @Test
    public void testValidate_WithNegativeValue_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "-5");

        // Act
        Boolean result = validator.validate("-5", context);

        // Assert
        assertFalse("Should return false for negative value", result);
    }

    /**
     * Test validation with value over maximum (101).
     */
    @Test
    public void testValidate_WithValueOverMaximum_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "101");

        // Act
        Boolean result = validator.validate("101", context);

        // Assert
        assertFalse("Should return false for value over maximum (101)", result);
    }

    /**
     * Test validation with very large value.
     */
    @Test
    public void testValidate_WithVeryLargeValue_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "1000");

        // Act
        Boolean result = validator.validate("1000", context);

        // Assert
        assertFalse("Should return false for very large value", result);
    }

    // ========== Null and Empty Tests ==========

    /**
     * Test validation with null value in context.
     */
    @Test
    public void testValidate_WithNullValue_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, null);

        // Act
        Boolean result = validator.validate("null", context);

        // Assert
        assertFalse("Should return false for null value", result);
    }

    /**
     * Test validation with empty string.
     */
    @Test
    public void testValidate_WithEmptyString_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "");

        // Act
        Boolean result = validator.validate("", context);

        // Assert
        assertFalse("Should return false for empty string", result);
    }

    /**
     * Test validation with whitespace only.
     */
    @Test
    public void testValidate_WithWhitespaceOnly_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "   ");

        // Act
        Boolean result = validator.validate("   ", context);

        // Assert
        assertFalse("Should return false for whitespace only", result);
    }

    /**
     * Test validation with missing key in context.
     */
    @Test
    public void testValidate_WithMissingKey_ReturnsFalse() {
        // Arrange - context is empty, no NUMBER_OF_POSTS key

        // Act
        Boolean result = validator.validate("50", context);

        // Assert
        assertFalse("Should return false when key is missing from context", result);
    }

    // ========== Invalid Format Tests ==========

    /**
     * Test validation with non-numeric string.
     */
    @Test
    public void testValidate_WithNonNumericString_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "abc");

        // Act
        Boolean result = validator.validate("abc", context);

        // Assert
        assertFalse("Should return false for non-numeric string", result);
    }

    /**
     * Test validation with alphanumeric string.
     */
    @Test
    public void testValidate_WithAlphanumericString_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "50abc");

        // Act
        Boolean result = validator.validate("50abc", context);

        // Assert
        assertFalse("Should return false for alphanumeric string", result);
    }

    /**
     * Test validation with special characters.
     */
    @Test
    public void testValidate_WithSpecialCharacters_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.NUMBER_OF_POSTS, "50@#");

        // Act
        Boolean result = validator.validate("50@#", context);

        // Assert
        assertFalse("Should return false for special characters", result);
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
                SalesforceErrorHandlingResource.NUMBER_OF_POSTS, fieldName);
    }

    /**
     * Test getFieldType returns number field type.
     */
    @Test
    public void testGetFieldType_ReturnsNumberFieldType() {
        // Act
        String fieldType = validator.getFieldType();

        // Assert
        assertEquals("Should return number field type", FieldTypes.NUMBER_FIELD, fieldType);
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
        assertTrue("Message should mention range 1-100", message.contains("1") && message.contains("100"));
    }

    /**
     * Test getConfirmationStepMessage with invalid input.
     */
    @Test
    public void testGetConfirmationStepMessage_WithInvalidInput_ReturnsFormattedMessage() {
        // Arrange
        String invalidInput = "150";

        // Act
        String message = validator.getConfirmationStepMessage(invalidInput, context);

        // Assert
        assertNotNull("Confirmation message should not be null", message);
        assertTrue("Message should contain the invalid input", message.contains(invalidInput));
        assertTrue("Message should mention range 1-100", message.contains("1") && message.contains("100"));
    }

    /**
     * Test getErrMessage with invalid input.
     */
    @Test
    public void testGetErrMessage_WithInvalidInput_ReturnsFormattedMessage() {
        // Arrange
        String invalidInput = "-5";

        // Act
        String message = validator.getErrMessage(invalidInput);

        // Assert
        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain the invalid input", message.contains(invalidInput));
        assertTrue("Message should mention valid range", message.contains("1") && message.contains("100"));
    }

    /**
     * Test getErrMessage with null input.
     */
    @Test
    public void testGetErrMessage_WithNullInput_ReturnsFormattedMessage() {
        // Act
        String message = validator.getErrMessage(null);

        // Assert
        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain 'null'", message.contains("null"));
    }
}

