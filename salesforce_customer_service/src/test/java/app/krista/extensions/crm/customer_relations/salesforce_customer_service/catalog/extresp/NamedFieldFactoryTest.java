package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp;

import app.krista.model.field.NamedField;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NamedFieldFactory}.
 */
public class NamedFieldFactoryTest {

    @Test
    public void testCreateSwitchField_WithValidName_ReturnsNamedField() {
        String fieldName = "testSwitch";

        NamedField result = NamedFieldFactory.createSwitchField(fieldName);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }

    @Test
    public void testCreateSwitchField_WithEmptyName_ReturnsNamedField() {
        String fieldName = "";

        NamedField result = NamedFieldFactory.createSwitchField(fieldName);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should be empty", "", result.getName());
    }

    @Test
    public void testCreateField_WithTextFieldType_ReturnsNamedField() {
        String fieldName = "testField";
        String fieldType = FieldTypes.TEXT_FIELD;

        NamedField result = NamedFieldFactory.createField(fieldName, fieldType);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }

    @Test
    public void testCreateField_WithNumberFieldType_ReturnsNamedField() {
        String fieldName = "numberField";
        String fieldType = FieldTypes.NUMBER_FIELD;

        NamedField result = NamedFieldFactory.createField(fieldName, fieldType);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }

    @Test
    public void testCreateField_WithFileFieldType_ReturnsNamedField() {
        String fieldName = "fileField";
        String fieldType = FieldTypes.FILE_FIELD;

        NamedField result = NamedFieldFactory.createField(fieldName, fieldType);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }

    @Test
    public void testCreateField_WithListFieldType_ReturnsNamedField() {
        String fieldName = "listField";
        String fieldType = FieldTypes.LIST_FIELD;

        NamedField result = NamedFieldFactory.createField(fieldName, fieldType);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }
}

