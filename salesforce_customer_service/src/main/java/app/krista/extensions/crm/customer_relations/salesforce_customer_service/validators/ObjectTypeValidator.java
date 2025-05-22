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
        return "Please select the Salesforce object type to validate (e.g., Account, Case, Contact, Customer, Individual).";
    }

    @Override
    public String getConfirmationStepMessage(String objectTypeInput, Map<ValidationResource, String> objectTypeContext) {
        return String.format("You have selected Object Type: %s.", objectTypeInput);
    }

    @Override
    public String getErrMessage(String objectTypeInput) {
        return "The object type \"" + objectTypeInput + "\" \"" + Constants.INVALID + "\". Please choose from: Account, Case, Contact, Customer, or Individual.";
    }
}
