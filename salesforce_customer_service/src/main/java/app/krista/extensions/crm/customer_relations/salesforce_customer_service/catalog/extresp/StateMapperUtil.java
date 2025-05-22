package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp;

import app.krista.model.base.File;

import javax.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StateMapperUtil {

    private StateMapperUtil() {
    }

    /**
     * Builds a metadata map for the "Get Case Feed Items By Case Id" catalog request.
     *
     * @param caseId        The case identifier (required).
     * @param numberOfPosts The number of posts to fetch (optional, may be null).
     * @param stateId       The state identifier for tracking (optional, may be null).
     * @return A LinkedHashMap containing the metadata for the request.
     */
    @NotNull
    public static Map<String, Object> addGetCaseFeedItemsByCaseIdMetaToMap(String caseId, Double numberOfPosts, String stateId) {
        Map<String, Object> metaData = new LinkedHashMap<>();
        metaData.put(SalesforceErrorHandlingResource.STATE_ID, stateId);
        metaData.put(SalesforceErrorHandlingResource.CASE_ID, caseId);
        metaData.put(SalesforceErrorHandlingResource.NUMBER_OF_POSTS, numberOfPosts);
        return metaData;
    }

    /**
     * Builds a metadata map for the "Share Update to Case" catalog request.
     *
     * @param caseId        The case identifier (required).
     * @param updateMessage The update message to be shared (required).
     * @param file          The file to attach to the update (optional, may be null).
     * @param stateId       The state identifier for tracking (optional, may be null).
     * @return A LinkedHashMap containing the metadata for the request.
     */
    @NotNull
    public static Map<String, Object> addShareUpdateToCaseMetaToMap(String caseId, String updateMessage, File file, String stateId) {
        Map<String, Object> metaData = new LinkedHashMap<>();
        metaData.put(SalesforceErrorHandlingResource.STATE_ID, stateId);
        metaData.put(SalesforceErrorHandlingResource.CASE_ID, caseId);
        metaData.put(SalesforceErrorHandlingResource.UPDATE_MESSAGE, updateMessage);
        if (file != null) {
            metaData.put(SalesforceErrorHandlingResource.FILE, List.of(file));
        }
        return metaData;
    }

    /**
     * Creates a metadata map with object and custom field information.
     *
     * @param objectId     The ID of the Salesforce object.
     * @param objectType   The type of the Salesforce object (e.g., "Case", "Account").
     * @param customFields The list of custom field names.
     * @param stateId      The state ID for tracking (can be null).
     * @return A map containing the state ID, object ID, object type, and custom fields.
     */
    @NotNull
    public static Map<String, Object> addCustomFieldsMetaToMap(String objectId, String objectType, List<String> customFields, String stateId) {
        Map<String, Object> metaData = new LinkedHashMap<>();
        metaData.put(SalesforceErrorHandlingResource.STATE_ID, stateId);
        metaData.put(SalesforceErrorHandlingResource.OBJECT_ID, objectId);
        metaData.put(SalesforceErrorHandlingResource.OBJECT_TYPE, objectType);
        metaData.put(SalesforceErrorHandlingResource.CUSTOM_FIELDS, customFields);
        return metaData;
    }




}
