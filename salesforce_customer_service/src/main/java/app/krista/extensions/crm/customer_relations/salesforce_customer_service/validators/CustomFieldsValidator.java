package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class CustomFieldsValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFieldsValidator.class);

    private final SalesforceService salesforceService;

    public CustomFieldsValidator(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    @Override
    public Boolean validate(String customFieldsInput, Map<ValidationResource, String> customFieldsContext) {
        if (customFieldsInput == null || customFieldsInput.isBlank()) {
            return false;
        }

        List<String> customFields = Arrays.stream(customFieldsInput.split(", "))
                .map(String::trim)
                .toList();

        for (String field : customFields) {
            if (!field.endsWith("__c")) {
                LOGGER.warn("{} is not a custom field", field);
                return false;
            }
        }

        String objectType = customFieldsContext.get(ValidationResource.OBJECT_TYPE);
        if (objectType == null || objectType.isBlank()) {
            LOGGER.error("Object type missing in context, cannot validate fields");
            return false;
        }

        for (String field : customFields) {
            boolean exists = salesforceService.checkFieldExistsInSalesforce(objectType, field);
            if (!exists) {
                LOGGER.warn("Field {} does not exist on Salesforce object {}", field, objectType);
                return false;
            }
        }
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return SalesforceErrorHandlingResource.CUSTOM_FIELDS;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.LIST_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a list of Salesforce custom field names (each must end with '__c').";
    }

    @Override
    public String getConfirmationStepMessage(String customFieldsInput, Map<ValidationResource, String> customFieldContext) {
        return String.format("The provided custom field(s): %s do not exist.", customFieldsInput);
    }

    @Override
    public String getErrMessage(String customFieldsInput) {
        return String.format("One or more custom fields are invalid: [%s]. Please ensure each field name ends with '__c'.", customFieldsInput);
    }

}
