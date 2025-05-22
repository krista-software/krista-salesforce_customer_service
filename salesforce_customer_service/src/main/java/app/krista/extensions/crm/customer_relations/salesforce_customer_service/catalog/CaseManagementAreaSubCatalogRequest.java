package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.SubCatalogRequest;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.*;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.KristaMediaClient;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.ValidationOrchestrator;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.*;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.CaseManagementArea.getEscapeCharacterMessage;

@SuppressWarnings("DefaultAnnotationParam")
@Service
public class CaseManagementAreaSubCatalogRequest {

    public static final String REENTER = "Do you want to continue?";
    public static final String REENTER_WAS_TRUE_HENCE_CONTINUING = "Reenter was true. Hence continuing.";
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseManagementAreaSubCatalogRequest.class);
    public static final String HANDLE_REENTER_GET_CASE_FEED_ITEMS_BY_CASE_ID = "handleReenterGetCaseFeedItemsByCaseId";
    public static final String HANDLE_REENTER_SHARE_UPDATE_TO_CASE = "handleReenterShareUpdateToCase";
    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;
    private final CaseDaoImpl caseDao;
    private final KristaMediaClient kristaMediaClient;
    private final CaseManagementArea caseManagementArea;

    @Inject
    public CaseManagementAreaSubCatalogRequest(ExtensionResponseGenerator responseGenerator, ErrorHandlingStateManager internalStateManager, CaseDaoImpl caseDao, KristaMediaClient kristaMediaClient, CaseManagementArea caseManagementArea) {
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;
        this.caseDao = caseDao;
        this.kristaMediaClient = kristaMediaClient;
        this.caseManagementArea = caseManagementArea;
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
     * Gets a list of File objects from the given state map.
     * It looks inside the state for files stored as a list, converts each one to a File object,
     * and returns all of them as a list. If there are no files, it returns an empty list.
     *
     * @param state a map that may contain files stored under a specific key
     * @return a list of File objects found in the state, or an empty list if none are found
     */

    @NotNull
    static List<File> getFileList(Map<String, Object> state) {
        List<File> fileList = new ArrayList<>();
        Object fileObj = state.get(SalesforceErrorHandlingResource.FILE);
        if (fileObj instanceof List<?> results) {
            for (Object item : results) {
                fileList.add(Constants.GSON.fromJson(Constants.GSON.toJson(item), File.class));
            }
        }
        return fileList;
    }

    /**
     * Asks the user if they want to enter the Case Id again when getting case posts.
     * - If user says "No," it returns an error message.
     * - If user says "Yes," it moves forward to get the case posts.
     *
     * @param map A set of inputs from the user including:
     *            - Whether to continue (true/false)
     *            - Current state ID
     *            - Case Id
     *            - Number of posts to get
     * @return A response telling what to do next based on the user’s answer.
     */

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_REENTER_GET_CASE_FEED_ITEMS_BY_CASE_ID,
            description = "Checks if user wants to re-enter Case Id and if yes, sends prompt to do so",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterGetCaseFeedItemsByCaseId(
            @Field.Desc(name = "inputMap", type = "{ Do you want to continue?: Boolean, stateId: Text, Case Id: Text , Number Of Posts: Number}", required = true) Map<String, Object> map) {

        LOGGER.info("SubCatalogRequest confirmReenterGetCaseFeedItemsByCaseId start: {}", map);
        Boolean reenter = (Boolean) map.get(REENTER);
        String stateId = (String) map.get(SalesforceErrorHandlingResource.STATE_ID);
        Map<String, Object> state = internalStateManager.get(stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(state);
        if (Boolean.FALSE.equals(reenter)) {
            return responseGenerator.generateFetchDenyResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults, null,
                    Map.of());
        }

        LOGGER.info(REENTER_WAS_TRUE_HENCE_CONTINUING);
        return responseGenerator.generateFetchResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, HANDLE_REENTER_GET_CASE_FEED_ITEMS_BY_CASE_ID,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    /**
     * Gets the list of feed posts for a given Case Id after the user enters the case details again.
     * It checks if the Case Id and number of posts are valid, then finds the case and fetches the feed posts.
     * If there are no posts or if there is an error, it returns a clear message.
     *
     * @param map A map with keys: "Case Id" (the case to look up) and "Number Of Posts" (how many posts to get).
     * @return The feed posts for the case or an error message if something is wrong.
     */

    @SubCatalogRequest(
            name = HANDLE_REENTER_GET_CASE_FEED_ITEMS_BY_CASE_ID,
            description = "Fetches feed posts for a specified case ID after the user has re-entered or confirmed the case details.",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Feed Items", type = "[ { Actor Name: Text, Post Message: Text, File: File } ]", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleReenterGetCaseFeedItemsByCaseId(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Case Id : Text, Number Of Posts : Number}") Map<String, Object> map) {

        LOGGER.info("SubCatalogRequest handleReenterGetCaseFeedItemsByCaseId start: {}", map);
        String caseId = (String) map.get(SalesforceErrorHandlingResource.CASE_ID);
        Double numberOfPosts = (Double) map.get(SalesforceErrorHandlingResource.NUMBER_OF_POSTS);

        if (caseId == null) {
            return ExtensionResponseFactory.create(Constants.INVALID_CASE_ID + ": null", ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    List.of(RemediationActionFactory.createInformActionALLParticipants(Constants.INVALID_CASE_ID + null, List.of())),
                    null, Map.of());
        }
        if (numberOfPosts == null) {
            return ExtensionResponseFactory.create(Constants.INVALID_NUMBER_OF_POSTS + ": null", ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    List.of(RemediationActionFactory.createInformActionALLParticipants(Constants.INVALID_NUMBER_OF_POSTS + null, List.of())),
                    null, Map.of());
        }

        Case caseObj;
        try {
            caseObj = caseManagementArea.validateAndFetchCase(caseId);
        } catch (IllegalArgumentException cause) {
            return ExtensionResponseFactory.create(cause.getMessage(),
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    List.of(RemediationActionFactory.createInformActionALLParticipants(cause.getMessage(), List.of())),
                    null, Map.of());
        }
        Double posts = CaseManagementArea.validateMaxNumberOfPosts(numberOfPosts);
        String caseNumber = caseObj.getId();

        try {
            List<Map<String, Object>> feeds = caseDao.getFeedItemsForCaseId(caseNumber, posts);
            if (feeds.isEmpty()) {
                return ExtensionResponseFactory.create(Map.of("The Feed Items is empty because the entries do not exist", List.of()));
            } else {
                return ExtensionResponseFactory.create(Map.of("Feed Items", feeds));
            }
        } catch (MustAuthorizeException cause) {
            LOGGER.error(cause.getMessage());
            throw cause;
        } catch (Exception cause) {
            return ExtensionResponseFactory.create(cause, "Error occurred while fetching post items from case.", ExtensionResponse.Error.ExceptionType.LOGIC_ERROR);
        }
    }

    /**
     * Checks if the user wants to try sharing an update to a case again.
     * If the user says no, it sends back an error message.
     * If the user says yes, it allows the update to continue.
     *
     * @param map A set of inputs from the user, like whether to continue, case ID, message, and file.
     * @return A response telling what to do next based on the user's answer.
     */
    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_REENTER_SHARE_UPDATE_TO_CASE,
            description = "Checks if user wants to retry sharing update to case and if yes, sends prompt to do so",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    @SuppressWarnings("unchecked")
    public ExtensionResponse confirmReenterShareUpdateToCase(
            @Field.Desc(name = "inputMap", type = "{ Do you want to continue?: Boolean, stateId: Text, Case Id: Text, Update Message: Paragraph, File: File }", required = true) Map<String, Object> map) {
        LOGGER.info("SubCatalogRequest confirmReenterShareUpdateToCase start: {}", map);
        Boolean reenter = (Boolean) map.get(REENTER);
        String stateId = (String) map.get(SalesforceErrorHandlingResource.STATE_ID);
        Map<String, Object> state = internalStateManager.get(stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(state);
        if (Boolean.FALSE.equals(reenter)) {
            return responseGenerator.generateFetchDenyResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults, null, Map.of());
        }
        LOGGER.info(REENTER_WAS_TRUE_HENCE_CONTINUING);
        return responseGenerator.generateFetchResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, HANDLE_REENTER_SHARE_UPDATE_TO_CASE,
                addMetaDataToLinkedHashMap(map, stateId)
        );
    }

    /**
     * Adds an update message (and optional file) to a case’s feed after the user confirms or re-enters the case details.
     * It checks if a file is attached:
     * - If yes, it uploads the file along with the update message.
     * - If no, it just adds the update message.
     *
     * @param map A set of inputs including:
     *            - stateId: Current process state ID
     *            - Case Id: The ID of the case to update
     *            - Update Message: The message to add to the case feed
     *            - File: (Optional) A file to attach with the update
     * @return A response indicating whether the update was successful ("Is Updated": true/false).
     */

    @SubCatalogRequest(
            name = HANDLE_REENTER_SHARE_UPDATE_TO_CASE,
            description = "Handles adding an update to a case’s feed after the user has re-entered or confirmed the case details.",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Is Updated", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleReenterShareUpdateToCase(
            @Field.Desc(name = "inputMap", type = "{ stateId: Text, Case Id: Text, Update Message: Paragraph, File: File }", required = true) Map<String, Object> map) {

        LOGGER.info("SubCatalogRequest handleReenterShareUpdateToCase started: {}", map);
        try {
            List<File> file = getFileList(map);
            String caseId = (String) map.get(SalesforceErrorHandlingResource.CASE_ID);
            String updateMessage = (String) map.get(SalesforceErrorHandlingResource.UPDATE_MESSAGE);
            if (!file.isEmpty()) {
                java.io.File javaFile = kristaMediaClient.toJavaFile(file.getFirst());
                String base64EncodedString = caseDao.getBase64EncodedString(javaFile);
                Boolean resultValue = caseDao.addFeedUpdateToCaseById(
                        caseId, getEscapeCharacterMessage(updateMessage), base64EncodedString, javaFile.getName());
                return new ExtensionResponseBuilder().success(Map.of("Is Updated", resultValue)).build();
            } else {
                Boolean isUpdated = caseDao.addFeedUpdateToCaseById(
                        caseId, getEscapeCharacterMessage(updateMessage), null, null);
                return new ExtensionResponseBuilder().success(Map.of("Is Updated", isUpdated)).build();
            }
        } catch (Exception cause) {
            return ExtensionResponseFactory.create(cause, "Failed to share update to case",
                    ExtensionResponse.Error.ExceptionType.LOGIC_ERROR);
        }
    }
}







