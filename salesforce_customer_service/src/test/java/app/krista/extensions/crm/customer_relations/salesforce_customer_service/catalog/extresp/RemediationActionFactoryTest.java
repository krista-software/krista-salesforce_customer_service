package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp;

import app.krista.extension.executor.RemediationAction;
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RemediationActionFactory}.
 */
public class RemediationActionFactoryTest {

    @Test
    public void testCreateAskAction_WithValidParameters_ReturnsRemediationAction() {
        String message = "Please provide the following information";
        List<NamedField> fields = new ArrayList<>();
        fields.add(new NamedField("field1", FieldTypes.TEXT_FIELD, null, null));

        RemediationAction result = RemediationActionFactory.createAskAction(message, fields);

        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testCreateAskAction_WithEmptyFields_ReturnsRemediationAction() {
        String message = "Test message";
        List<NamedField> fields = new ArrayList<>();

        RemediationAction result = RemediationActionFactory.createAskAction(message, fields);

        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testCreateInformAction_WithValidParameters_ReturnsRemediationAction() {
        String message = "Here is the information";
        List<NamedValuedField> fields = new ArrayList<>();
        fields.add(new NamedValuedField("field1", FieldTypes.TEXT_FIELD, "value1", null, null));

        RemediationAction result = RemediationActionFactory.createInformAction(message, fields);

        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testCreateInformAction_WithEmptyFields_ReturnsRemediationAction() {
        String message = "Test message";
        List<NamedValuedField> fields = new ArrayList<>();

        RemediationAction result = RemediationActionFactory.createInformAction(message, fields);

        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testCreateInformAction_WithMultipleFields_ReturnsRemediationAction() {
        String message = "Multiple fields";
        List<NamedValuedField> fields = new ArrayList<>();
        fields.add(new NamedValuedField("field1", FieldTypes.TEXT_FIELD, "value1", null, null));
        fields.add(new NamedValuedField("field2", FieldTypes.NUMBER_FIELD, 123, null, null));

        RemediationAction result = RemediationActionFactory.createInformAction(message, fields);

        assertNotNull("Result should not be null", result);
    }
}

