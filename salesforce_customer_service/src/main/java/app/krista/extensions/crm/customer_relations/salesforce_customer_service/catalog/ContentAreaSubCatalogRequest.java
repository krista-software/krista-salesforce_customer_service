package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.SubCatalogRequest;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.ExtensionResponseFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.RemediationActionFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SubCatalogConstants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.*;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.KristaMediaClient;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.ValidationOrchestrator;
import app.krista.model.base.FreeForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("DefaultAnnotationParam")
public class ContentAreaSubCatalogRequest {
    public static final String REENTER = "Do you want to continue?";
    public static final String HANDLE_REENTER_RETRIEVE_CUSTOM_FIELDS_FOR_GIVEN_OBJECT = "handleReenterRetrieveCustomFieldsForGivenObject";
    public static final String REENTER_WAS_TRUE_HENCE_CONTINUING = "Reenter was true. Hence continuing.";
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentAreaSubCatalogRequest.class);
    public final SalesforceService salesforceService;
    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;
    private final CaseDaoImpl caseDao;
    private final AccountDaoImpl accountDao;
    private final ContactDaoImpl contactDao;
    private final CustomerDaoImpl customerDao;
    private final IndividualDaoImpl individualDao;
    private final ContentArea contentArea;

    @Inject
    public ContentAreaSubCatalogRequest(SalesforceService salesforceService, ExtensionResponseGenerator responseGenerator, ErrorHandlingStateManager internalStateManager, CaseDaoImpl caseDao, AccountDaoImpl accountDao, ContactDaoImpl contactDao, CustomerDaoImpl customerDao, IndividualDaoImpl individualDao, KristaMediaClient kristaMediaClient, ContentArea contentArea) {
        this.salesforceService = salesforceService;
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;
        this.caseDao = caseDao;
        this.accountDao = accountDao;
        this.contactDao = contactDao;
        this.customerDao = customerDao;
        this.individualDao = individualDao;
        this.contentArea = contentArea;
    }

    @NotNull
    private static List<ValidationOrchestrator.ValidationResult> getValidationResults(Map<String, Object> state) {
        List<ValidationOrchestrator.ValidationResult> validationResults = new ArrayList<>();
        List<?> results = (List<?>) state.get(SubCatalogConstants.VALIDATION_RESULTS);
        for (Object item : results) {
            validationResults.add(Constants.GSON.fromJson(Constants.GSON.toJson(item), ValidationOrchestrator.ValidationResult.class));
        }
        return validationResults;
    }

    @NotNull
    private static Map<String, Object> addMetaDataToLinkedHashMap(Map<String, Object> map, String stateId) {
        Map<String, Object> metaData = new LinkedHashMap<>();
        metaData.put(SalesforceErrorHandlingResource.STATE_ID, stateId);
        metaData.putAll(map);
        return metaData;
    }

    /**
     * Asks the user if they want to confirm or re-enter the details before retrieving custom fields for a Salesforce object.
     * - If the user says "No," it stops and returns the errors found.
     * - If the user says "Yes," it continues and prepares to get the custom fields.
     *
     * @param map A set of inputs including:
     *            - Whether to continue or re-enter (Boolean)
     *            - The current state ID (String)
     *            - The Object ID (String)
     *            - The Object Type (String)
     *            - The list of Custom Fields (List of Strings)
     * @return A response that either stops or proceeds based on the user’s choice.
     */

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_RETRIEVE_CUSTOM_FIELDS_FOR_GIVEN_OBJECT,
            description = "Checks if user wants to confirm or re-enter input details when retrieving custom field values for a specified Salesforce object and if yes, sends prompt to do so.",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmRetrieveCustomFieldsForGivenObject(
            @Field.Desc(name = "inputMap", type = "{ Do you want to continue?: Boolean, stateId: Text, Object Id: Text, Object Type: Text, Custom Fields: [ Text ] }", required = true)
            Map<String, Object> map) {
        LOGGER.info("SubCatalogRequest confirmRetrieveCustomFieldsForGivenObject start: {}", map);
        Boolean reenter = (Boolean) map.get(REENTER);
        String stateId = (String) map.get(SalesforceErrorHandlingResource.STATE_ID);

        Map<String, Object> state = internalStateManager.get(stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(state);

        if (Boolean.FALSE.equals(reenter)) {
            return responseGenerator.generateFetchDenyResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, validationResults,
                    null, Map.of());
        }

        LOGGER.info(REENTER_WAS_TRUE_HENCE_CONTINUING);
        return responseGenerator.generateFetchResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, HANDLE_REENTER_RETRIEVE_CUSTOM_FIELDS_FOR_GIVEN_OBJECT,
                addMetaDataToLinkedHashMap(map, stateId));

    }

    /**
     * Retrieves custom fields for a Salesforce object after the user re-enters the object details.
     * It checks the inputs for validity, including Object I'd, Object Type, and Custom Fields.
     * - Ensures custom fields end with "__c"
     * - Verifies the fields exist in Salesforce
     * - Returns errors if inputs are missing, invalid, or Salesforce service is unavailable
     * If all inputs are valid, it fetches the custom field data from Salesforce and returns it.
     *
     * @param map A map containing:
     *            - stateId: Current state identifier
     *            - Object I'd: Salesforce object identifier
     *            - Object Type: Type of Salesforce object (e.g., Account, Case)
     *            - Custom Fields: List of custom field names to retrieve
     * @return An ExtensionResponse with the custom field details or error messages if validation fails.
     */

    @SubCatalogRequest(
            name = HANDLE_REENTER_RETRIEVE_CUSTOM_FIELDS_FOR_GIVEN_OBJECT,
            description = "Handles the retrieval of custom fields for a Salesforce object after the user has re-entered the object details.",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field(name = "Field Details", type = "FreeForm", required = false, attributes = {}, options = {})
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleReenterRetrieveCustomFieldsForGivenObject(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Object Id: Text, Object Type: Text, Custom Fields: [ Text ] }", required = true) Map<String, Object> map) {

        LOGGER.info("SubCatalogRequest handleReenterRetrieveCustomFieldsForGivenObject start: {}", map);
        String objectId = (String) map.get(SalesforceErrorHandlingResource.OBJECT_ID);
        String objectType = (String) map.get(SalesforceErrorHandlingResource.OBJECT_TYPE);
        List<String> customFields = (List<String>) map.get(SalesforceErrorHandlingResource.CUSTOM_FIELDS);

        if (objectId == null || objectType == null || customFields == null || customFields.isEmpty()) {
            return ExtensionResponseFactory.create(
                    "Please ensure that Object Id, Object Type, and Custom Fields are correctly provided.",
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    List.of(RemediationActionFactory.createInformActionALLParticipants("Missing or invalid input(s).", List.of())),
                    null, Map.of());
        }

        if (!contentValidator(objectId, objectType)) {
            return ExtensionResponseFactory.create(Constants.INVALID + " ObjectId or Custom Field.", ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    List.of(RemediationActionFactory.createInformActionALLParticipants("Please check the Object Id and Custom Field try again.",
                            Collections.emptyList()
                    )), null, Map.of());
        }

        boolean hasInvalidFields = customFields.stream()
                .anyMatch(field -> field == null || !field.trim().endsWith("__c"));

        if (hasInvalidFields) {
            return ExtensionResponseFactory.create("Each custom field must end with '__c'.", ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    List.of(RemediationActionFactory.createInformActionALLParticipants("Please ensure all custom field names end with '__c'.", List.of())),
                    null,
                    Map.of()
            );
        }

        List<String> invalidFields = new ArrayList<>();

        // Null check before using salesforceService
        if (salesforceService == null) {
            return ExtensionResponseFactory.create("Salesforce service is not available. Please contact your administrator.",
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    List.of(RemediationActionFactory.createInformActionALLParticipants(
                            "System error: Salesforce connection could not be established. Please try again later or contact support.", List.of())),
                    null,
                    Map.of());
        }

        for (String field : customFields) {
            if (field == null || field.trim().isEmpty() || !field.endsWith("__c") || !salesforceService.checkFieldExistsInSalesforce(objectType, field)) {
                invalidFields.add(field);
            }
        }

        if (!invalidFields.isEmpty()) {
            return ExtensionResponseFactory.create("Some custom fields are invalid or do not exist in Salesforce.",
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    List.of(RemediationActionFactory.createInformActionALLParticipants(
                            "Incorrect custom fields: " + String.join(", ", invalidFields) + ". Please ensure the field names are correct and try again.",
                            List.of())),
                    null,
                    Map.of());
        }

        try {
            String customFieldClause = ContentArea.buildCustomFieldClause(customFields);
            FreeForm result = contentArea.fetchRecordFromSalesforce(objectId, objectType, customFieldClause, customFields);
            return new ExtensionResponseBuilder().success(Map.of("Field Details", result)).build();

        } catch (MustAuthorizeException cause) {
            LOGGER.error(cause.getMessage());
            throw cause;
        } catch (Exception cause) {
            return ExtensionResponseFactory.create(cause, "Failed to retrieval of custom fields for a Salesforce.", ExtensionResponse.Error.ExceptionType.LOGIC_ERROR);
        }
    }

    /**
     * Checks if a Salesforce object with the given ID and type exists.
     * Returns true if the object exists, or false if it does not or if there is an error.
     *
     * @param objectId   The ID of the object to check.
     * @param objectType The type of the object (e.g., "Account", "Case", "Contact", "Customer", "Individual").
     * @return true if the object exists, false otherwise.
     */
    private Boolean contentValidator(String objectId, String objectType) {
        if (objectId == null || objectId.isBlank() || objectType == null || objectType.isBlank()) {
            return false;
        }
        try {
            return switch (objectType.toLowerCase()) {
                case "account" -> {
                    accountDao.getAccountById(objectId);
                    yield true;
                }
                case "case" -> caseDao.getCaseById(objectId) != null;
                case "contact" -> contactDao.getContactById(objectId) != null;
                case "customer" -> customerDao.getCustomerById(objectId) != null;
                case "individual" -> individualDao.getIndividualById(objectId) != null;
                default -> false;
            };
        } catch (IOException | ExecutionException | InterruptedException | com.google.gson.JsonSyntaxException cause) {
            LOGGER.error("Error validating content for {}: {}", objectType, cause.getMessage(), cause);
            return false;
        } catch (RuntimeException cause) {
            LOGGER.error("Unexpected error validating content for {}: {}", objectType, cause.getMessage(), cause);
            return false;
        }
    }
}









