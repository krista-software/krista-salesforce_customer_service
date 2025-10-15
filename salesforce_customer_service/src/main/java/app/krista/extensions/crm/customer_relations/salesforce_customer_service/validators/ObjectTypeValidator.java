package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import java.util.Map;

public class ObjectTypeValidator implements Validator {

    @Override
    public Boolean validate(String objectTypeInput, Map<ValidationResource, String> objectTypeContext) {
        return objectTypeInput != null && !objectTypeInput.isBlank();
    }

    @Override
    public String getFetchFieldName() {
        return SalesforceErrorHandlingResource.OBJECT_TYPE;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.PICK_ONE;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please select a valid Salesforce object type from the available options (Account, Case, Contact, Customer, Individual).";
    }

    @Override
    public String getConfirmationStepMessage(String objectTypeInput, Map<ValidationResource, String> objectTypeContext) {
        return String.format("Object type validation failed because '%s' is not a supported object type. " +
                "Please select from the available options: Account, Case, Contact, Customer, or Individual.", objectTypeInput);
    }

    @Override
    public String getErrMessage(String objectTypeInput) {
        return String.format("Object type validation failed because '%s' is not a supported object type. " +
                "Only the following object types are supported: Account, Case, Contact, Customer, Individual. " +
                "Please select a valid object type from the available options.", objectTypeInput);
    }
}
