package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UpdateMessageValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateMessageValidator.class);

    @Override
    public Boolean validate(String messageInput, Map<ValidationResource, String> messageContext) {
        String updateMessage = messageContext.get(ValidationResource.UPDATE_MESSAGE);
        if (updateMessage == null || updateMessage.isBlank()) {
            LOGGER.error("Update message is null or empty");
            return false;
        }
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return SalesforceErrorHandlingResource.UPDATE_MESSAGE;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please provide a valid update message.";
    }

    @Override
    public String getConfirmationStepMessage(String messageInput, Map<ValidationResource, String> messageContext) {
        return String.format("The Provided Update message from caseId : %s", messageInput);
    }

    @Override
    public String getErrMessage(String messageInput) {
        return String.format("Invalid update message: %s", messageInput);
    }
}
