package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import java.util.Map;

public interface Validator {

    enum ValidationResource {
        CASE_ID,
        NUMBER_OF_POSTS,
        UPDATE_MESSAGE,
        FILE,
        OBJECT_ID,
        OBJECT_TYPE,
        CUSTOM_FIELDS
    }

    Boolean validate(String resourceId, Map<ValidationResource, String> context);

    String getFetchFieldName();

    String getFieldType();

    String getFetchStepMessage();

    String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context);

    String getErrMessage(String resourceId);
}
