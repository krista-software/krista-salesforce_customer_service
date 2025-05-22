package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResourceUtil {

    private ValidationResourceUtil() {
    }

    /**
     * Generates a validation map for a case using the provided case ID and number of posts.
     * Adds entries to the map only if the corresponding values are not null:
     *
     * @param caseId        the ID of the case to validate
     * @param numberOfPosts the number of posts associated with the case
     * @return a map containing the non-null validation resources and their string values
     */
    public static Map<Validator.ValidationResource, String> generateCaseValidationMap(String caseId, Double numberOfPosts) {
        Map<Validator.ValidationResource, String> map = new HashMap<>();
        if (isNotNull(caseId)) {
            map.put(Validator.ValidationResource.CASE_ID, caseId);
        }
        if (isNotNull(String.valueOf(numberOfPosts))) {
            map.put(Validator.ValidationResource.NUMBER_OF_POSTS, String.valueOf(numberOfPosts));
        }
        return map;
    }

    private static boolean isNotNull(String input) {
        return input != null;
    }

    /**
     * Generates a validation map for content fields using the provided object ID, object type, and custom fields.
     * Adds entries to the map only if the corresponding values are not null or empty:
     *
     * @param objectId     the ID of the object to validate
     * @param objectType   the type of the object to validate
     * @param customFields a list of custom field names to validate
     * @return a map containing the non-null validation resources and their string values
     */
    public static Map<Validator.ValidationResource, String> generateContentFieldValidationMap(String objectId, String objectType, List<String> customFields) {
        Map<Validator.ValidationResource, String> map = new HashMap<>();
        if (isNotNull(objectId)) {
            map.put(Validator.ValidationResource.OBJECT_ID, objectId);
        }
        if (isNotNull(objectType)) {
            map.put(Validator.ValidationResource.OBJECT_TYPE, objectType);
        }
        if (customFields != null && !customFields.isEmpty()) {
            map.put(Validator.ValidationResource.CUSTOM_FIELDS, String.join(", ", customFields));
        }
        return map;
    }
}
