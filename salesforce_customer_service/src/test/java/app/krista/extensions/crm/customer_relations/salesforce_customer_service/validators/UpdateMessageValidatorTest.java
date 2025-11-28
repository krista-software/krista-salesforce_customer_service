package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for UpdateMessageValidator.
 * Tests validation logic for update messages (must not be null or blank).
 * 
 * Test Coverage Areas:
 * - Valid update messages
 * - Invalid update messages (null, empty, blank)
 * - Field metadata (name, type, messages)
 */
public class UpdateMessageValidatorTest {

    private UpdateMessageValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @Before
    public void setUp() {
        validator = new UpdateMessageValidator();
        context = new HashMap<>();
    }

    // ========== Valid Input Tests ==========

    /**
     * Test validation with valid update message.
     */
    @Test
    public void testValidate_WithValidMessage_ReturnsTrue() {
        // Arrange
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, "This is a valid update message");

        // Act
        Boolean result = validator.validate("This is a valid update message", context);

        // Assert
        assertTrue("Should return true for valid message", result);
    }

    /**
     * Test validation with short valid message.
     */
    @Test
    public void testValidate_WithShortMessage_ReturnsTrue() {
        // Arrange
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, "OK");

        // Act
        Boolean result = validator.validate("OK", context);

        // Assert
        assertTrue("Should return true for short valid message", result);
    }

    /**
     * Test validation with long valid message.
     */
    @Test
    public void testValidate_WithLongMessage_ReturnsTrue() {
        // Arrange
        String longMessage = "This is a very long update message that contains a lot of information " +
                "about the case update and provides detailed context for the team members.";
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, longMessage);

        // Act
        Boolean result = validator.validate(longMessage, context);

        // Assert
        assertTrue("Should return true for long valid message", result);
    }

    /**
     * Test validation with message containing special characters.
     */
    @Test
    public void testValidate_WithSpecialCharacters_ReturnsTrue() {
        // Arrange
        String messageWithSpecialChars = "Update: Case #12345 - Status changed! @user";
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, messageWithSpecialChars);

        // Act
        Boolean result = validator.validate(messageWithSpecialChars, context);

        // Assert
        assertTrue("Should return true for message with special characters", result);
    }

    /**
     * Test validation with message containing numbers.
     */
    @Test
    public void testValidate_WithNumbers_ReturnsTrue() {
        // Arrange
        String messageWithNumbers = "Case 12345 updated on 2024-01-15";
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, messageWithNumbers);

        // Act
        Boolean result = validator.validate(messageWithNumbers, context);

        // Assert
        assertTrue("Should return true for message with numbers", result);
    }

    // ========== Invalid Input Tests ==========

    /**
     * Test validation with null message in context.
     */
    @Test
    public void testValidate_WithNullMessage_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, null);

        // Act
        Boolean result = validator.validate("null", context);

        // Assert
        assertFalse("Should return false for null message", result);
    }

    /**
     * Test validation with empty string.
     */
    @Test
    public void testValidate_WithEmptyString_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, "");

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
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, "   ");

        // Act
        Boolean result = validator.validate("   ", context);

        // Assert
        assertFalse("Should return false for whitespace only", result);
    }

    /**
     * Test validation with tab characters only.
     */
    @Test
    public void testValidate_WithTabsOnly_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, "\t\t\t");

        // Act
        Boolean result = validator.validate("\t\t\t", context);

        // Assert
        assertFalse("Should return false for tabs only", result);
    }

    /**
     * Test validation with newline characters only.
     */
    @Test
    public void testValidate_WithNewlinesOnly_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, "\n\n\n");

        // Act
        Boolean result = validator.validate("\n\n\n", context);

        // Assert
        assertFalse("Should return false for newlines only", result);
    }

    /**
     * Test validation with mixed whitespace characters.
     */
    @Test
    public void testValidate_WithMixedWhitespace_ReturnsFalse() {
        // Arrange
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, " \t\n ");

        // Act
        Boolean result = validator.validate(" \t\n ", context);

        // Assert
        assertFalse("Should return false for mixed whitespace", result);
    }

    /**
     * Test validation with missing key in context.
     */
    @Test
    public void testValidate_WithMissingKey_ReturnsFalse() {
        // Arrange - context is empty, no UPDATE_MESSAGE key

        // Act
        Boolean result = validator.validate("test message", context);

        // Assert
        assertFalse("Should return false when key is missing from context", result);
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
                SalesforceErrorHandlingResource.UPDATE_MESSAGE, fieldName);
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
        assertTrue("Message should mention update message", 
                message.toLowerCase().contains("update message") || message.toLowerCase().contains("message"));
    }

    /**
     * Test getConfirmationStepMessage returns formatted message.
     */
    @Test
    public void testGetConfirmationStepMessage_ReturnsFormattedMessage() {
        // Arrange
        String invalidInput = "";

        // Act
        String message = validator.getConfirmationStepMessage(invalidInput, context);

        // Assert
        assertNotNull("Confirmation message should not be null", message);
        assertTrue("Message should mention validation failure", 
                message.toLowerCase().contains("validation failed") || 
                message.toLowerCase().contains("empty") ||
                message.toLowerCase().contains("invalid"));
    }

    /**
     * Test getErrMessage with empty input.
     */
    @Test
    public void testGetErrMessage_WithEmptyInput_ReturnsFormattedMessage() {
        // Arrange
        String emptyInput = "";

        // Act
        String message = validator.getErrMessage(emptyInput);

        // Assert
        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain the empty input", message.contains(emptyInput));
        assertTrue("Message should mention failure", 
                message.toLowerCase().contains("failed") || 
                message.toLowerCase().contains("invalid"));
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

    /**
     * Test getErrMessage with whitespace input.
     */
    @Test
    public void testGetErrMessage_WithWhitespaceInput_ReturnsFormattedMessage() {
        // Arrange
        String whitespaceInput = "   ";

        // Act
        String message = validator.getErrMessage(whitespaceInput);

        // Assert
        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain the whitespace input", message.contains(whitespaceInput));
    }

    /**
     * Test validation with message containing only a single character.
     */
    @Test
    public void testValidate_WithSingleCharacter_ReturnsTrue() {
        // Arrange
        context.put(Validator.ValidationResource.UPDATE_MESSAGE, "A");

        // Act
        Boolean result = validator.validate("A", context);

        // Assert
        assertTrue("Should return true for single character message", result);
    }
}

