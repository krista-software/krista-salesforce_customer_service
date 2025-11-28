package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.RemediationActions;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.*;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceService;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.ContentDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.ValidationOrchestrator;
import app.krista.model.base.FreeForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("DefaultAnnotationParam")
@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "b7a2dadc-2b0c-4f59-a4f9-befbae5897e9")
public class ContentArea {
    private final SalesforceService salesforceService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentArea.class);
    private final ContentDaoImpl contentDao;
    private final ValidationOrchestrator validationOrchestrator;
    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;


    @Inject
    public ContentArea(SalesforceService salesforceService, ContentDaoImpl contentDao, ValidationOrchestrator validationOrchestrator, ExtensionResponseGenerator responseGenerator, ErrorHandlingStateManager internalStateManager, CaseDaoImpl caseDao) {
        this.salesforceService = salesforceService;
        this.contentDao = contentDao;
        this.validationOrchestrator = validationOrchestrator;
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;
    }

    /**
     * Gets custom fields for a given Salesforce object.
     * It checks if the inputs are correct:
     * - If yes, it fetches and returns the custom fields.
     * - If no, it asks for confirmation or fixes.
     * Custom fields must end with "__c".
     *
     * @param objectId              The ID of the object (like Account or Case).
     * @param objectType            The type of the object (Account, Case, Contact, Customer, or Individual).
     * @param customFields          A list of custom field names to get.
     * @param allowRetry            If true, triggers error handling flow when validation fails; if false, returns error directly.
     * @return A response with the custom fields or a message asking to fix inputs.
     */
    @CatalogRequest(
            id = "localDomainRequest_519557fc-c610-4769-8a3c-cfe3c984fb94",
            name = "Retrieve Custom Fields For Given Object",
            description = "Retrieve Custom Fields For Given Object",
            area = "Content",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = "Field Details", type = "FreeForm", required = false, attributes = {}, options = {})

    public ExtensionResponse RetrieveCustomFieldsForGivenObject(
            @Field.Text(name = "Object Id", required = true) String objectId,
            @Field.PickOne(name = "Object Type", values = {"Account", "Case", "Contact", "Customer", "Individual"}, required = true) String objectType,
            @Field.Desc(name = "Custom Fields", type = "[ Text ]", required = true) List<String> customFields,
            @Field.Boolean(name = "Allow Retry", required = true) Boolean allowRetry) {

        try {

            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(ValidationResourceUtil.generateContentFieldValidationMap(objectId, objectType, customFields));
            if (validationResults.isEmpty()) {

                String customFieldClause = buildCustomFieldClause(customFields);
                if (customFieldClause.isEmpty()) {
                    return ExtensionResponseFactory.create(
                            "One or more fields are not valid custom fields. All custom fields must end with '__c'.",
                            ExtensionResponse.Error.ExceptionType.INPUT_ERROR
                    );
                }
                FreeForm customField = fetchRecordFromSalesforce(objectId, objectType, customFieldClause, customFields);
                return new ExtensionResponseBuilder().success(Map.of("Field Details", customField)).build();


            } else {
                // Check if retry is allowed
                if (Boolean.TRUE.equals(allowRetry)) {
                    // Go to error handling flow
                    String stateId = UUID.randomUUID().toString();
                    internalStateManager.put(stateId,
                            Constants.GSON.toJson(Map.of(SalesforceErrorHandlingResource.OBJECT_TYPE, objectType, SubCatalogConstants.VALIDATION_RESULTS, validationResults)));
                    LOGGER.info("Validation Result: {}", validationResults);
                    return responseGenerator.generateConfirmationResponse(
                            ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                            validationResults,
                            SubCatalogConstants.CONFIRM_RETRIEVE_CUSTOM_FIELDS_FOR_GIVEN_OBJECT,
                            StateMapperUtil.addCustomFieldsMetaToMap(objectId, objectType, customFields, stateId)
                    );
                } else {
                    // Return error directly without going to error handling flow
                    LOGGER.info("Retry disabled. Returning validation errors directly: {}", validationResults);
                    StringBuilder errorMessage = new StringBuilder("Validation failed:\n");
                    for (ValidationOrchestrator.ValidationResult result : validationResults) {
                        errorMessage.append(result.errMessage()).append("\n");
                    }
                    return ExtensionResponseFactory.create(
                            errorMessage.toString(),
                            ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                            List.of(RemediationActionFactory.createInformActionALLParticipants(
                                    "Please correct the validation errors and try again.", List.of())),
                            null,
                            Map.of()
                    );
                }
            }
        } catch (MustAuthorizeException cause) {
            LOGGER.error("Authorization failed: {}", cause.getMessage());
            throw cause;
        } catch (Exception cause) {
            LOGGER.error("Unexpected error while retrieving custom fields: {}", cause.getMessage(), cause);

            // Check if retry is allowed for exceptions as well
            if (Boolean.TRUE.equals(allowRetry)) {
                RemediationActions remediationActions = new RemediationActions(List.of(RemediationActionFactory.createInformActionALLParticipants(
                        "An unexpected error occurred while retrieving custom fields.", List.of())), null);

                ExtensionResponse.Error error = new ExtensionResponse.Error("An unexpected error occurred. Please try again later.",
                        System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, "");

                return new ExtensionResponseBuilder().failure(error, remediationActions, null).build();
            } else {
                // Return error directly without remediation actions
                return ExtensionResponseFactory.create(
                        "An unexpected error occurred: " + cause.getMessage(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
                );
            }
        }

    }

    /**
     * Builds a comma-separated string of custom field names.
     * Checks that each field name ends with "__c". If any field is invalid, returns an empty string.
     *
     * @param customFields The list of custom field names.
     * @return A comma-separated string of valid custom field names, or an empty string if any field is invalid.
     */
    public static String buildCustomFieldClause(List<String> customFields) {
        StringBuilder customFieldBuilder = new StringBuilder();
        for (String str : customFields) {
            if (!str.endsWith("__c")) {
                LOGGER.warn("Field '{}' is not a valid custom field. Custom fields must end with '__c'.", str);
                return "";
            }
            customFieldBuilder.append(str).append(",");
        }
        if (!customFields.isEmpty()) {
            customFieldBuilder.deleteCharAt(customFieldBuilder.length() - 1);
        }
        return customFieldBuilder.toString();
    }

    /**
     * Retrieves custom field values for a Salesforce object by running a query.
     * Builds a query using the given object ID, object type, and field names.
     * Returns the custom field values as a FreeForm object.
     *
     * @param objectId     The ID of the Salesforce object.
     * @param objectType   The type of the Salesforce object (e.g., "Account", "Case").
     * @param fields       The comma-separated field names to retrieve.
     * @param customFields The list of custom field names to include.
     * @return A FreeForm object containing the custom field values.
     * @throws IOException If there is an error retrieving or parsing the record.
     */
    public FreeForm fetchRecordFromSalesforce(String objectId, String objectType, String fields, List<String> customFields)
            throws IOException {
        try {
            Set<String> allFieldsSet = new LinkedHashSet<>();
            if (fields != null && !fields.trim().isEmpty()) {
                allFieldsSet.addAll(Arrays.asList(fields.split(",")));
            }
            if (customFields != null && !customFields.isEmpty()) {
                allFieldsSet.addAll(customFields);
            }
            String allFields = String.join(",", allFieldsSet);

            String query = "SELECT " + allFields + " FROM " + objectType + " WHERE Id = '" + objectId + "'";
            LOGGER.debug("Executing query: {}", query);

            String recordByQuery = salesforceService.getRecordByQuery(query);
            LOGGER.debug("Raw record response: {}", recordByQuery);

            if (recordByQuery != null && !recordByQuery.isEmpty()) {
                LOGGER.debug("Validating objectId={}, objectType={}, customFields={}", objectId, objectType, customFields);
                return contentDao.parseJSONToMap(recordByQuery, customFields);
            } else {
                LOGGER.warn("No records found for query: {}", query);
                return new FreeForm();
            }
        } catch (Exception cause) {
            LOGGER.error("Error retrieving or parsing record for objectType={}, objectId={}", objectType, objectId, cause);
            throw new IOException("Failed to retrieve custom fields for object " + objectType, cause);
        }
    }
}



