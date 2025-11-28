package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link FieldTypes} constants class.
 */
public class FieldTypesTest {

    @Test
    public void testTextFieldConstant() {
        assertEquals("com.krista.fields.Text", FieldTypes.TEXT_FIELD);
    }

    @Test
    public void testNumberFieldConstant() {
        assertEquals("com.krista.fields.Number", FieldTypes.NUMBER_FIELD);
    }

    @Test
    public void testSwitchFieldConstant() {
        assertEquals("com.krista.fields.Switch", FieldTypes.SWITCH_FIELD);
    }

    @Test
    public void testFileFieldConstant() {
        assertEquals("com.krista.fields.File", FieldTypes.FILE_FIELD);
    }

    @Test
    public void testPickOneConstant() {
        assertEquals("com.krista.fields.PickOne", FieldTypes.PICK_ONE);
    }

    @Test
    public void testListFieldConstant() {
        assertEquals("com.krista.fields.List", FieldTypes.LIST_FIELD);
    }
}

