package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class NumberOfPostsValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NumberOfPostsValidator.class);

    @Override
    public Boolean validate(String numberOfPostsInput, Map<ValidationResource, String> context) {
        try {
            String val = context.get(ValidationResource.NUMBER_OF_POSTS);
            if (val == null || val.trim().isEmpty()) {
                LOGGER.warn("Number of posts is null or empty");
                return false;
            }
            double numberOfPosts = Double.parseDouble(val);
            return numberOfPosts > 0 && numberOfPosts <= 100;
        } catch (NumberFormatException cause) {
            LOGGER.warn("Invalid number format for number of posts: {}", numberOfPostsInput, cause);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return SalesforceErrorHandlingResource.NUMBER_OF_POSTS;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.NUMBER_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter the number of posts (1 to 100).";
    }

    @Override
    public String getConfirmationStepMessage(String numberOfPostsInput, Map<ValidationResource, String> context) {
        return String.format("The provided number of posts: %s is invalid.", numberOfPostsInput);
    }

    @Override
    public String getErrMessage(String numberOfPostsInput) {
        return String.format(Constants.INVALID + " number of posts: %s. Please enter an integer between 1 and 100.", numberOfPostsInput);
    }
}

