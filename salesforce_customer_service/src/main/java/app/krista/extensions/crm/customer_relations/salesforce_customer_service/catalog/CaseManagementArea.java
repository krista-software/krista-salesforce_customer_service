/*
 * Salesforce Customer Service Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.Invoker;
import app.krista.extension.executor.RemediationActions;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAuditService;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.*;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.KristaMediaClient;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.ValidationOrchestrator;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.Validator;
import app.krista.model.base.File;
import app.krista.model.base.FreeForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.SFObjectHelper.upsertCaseMap;

@SuppressWarnings("ALL")
@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "abc90187-31a8-460e-b19b-99ae146e6ee8")
public class CaseManagementArea {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseManagementArea.class);
    private final CaseDaoImpl caseDao;
    private final KristaMediaClient kristaMediaClient;
    private final ValidationOrchestrator validationOrchestrator;
    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;
    private final SalesforceCustomerServiceAuditService auditService;
    private final Invoker invoker;

    @Inject
    private CaseManagementArea(CaseDaoImpl caseDao, KristaMediaClient kristaMediaClient,
                               ValidationOrchestrator validationOrchestrator,
                               ExtensionResponseGenerator responseGenerator,
                               ErrorHandlingStateManager internalStateManager,
                               SalesforceCustomerServiceAuditService auditService,
                               Invoker invoker) {
        this.caseDao = caseDao;
        this.kristaMediaClient = kristaMediaClient;
        this.validationOrchestrator = validationOrchestrator;
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;
        this.auditService = auditService;
        this.invoker = invoker;
    }

    /**
     * Wraps a catalog request body with audit recording, mirroring the outlook extension's
     * {@code audited()} pattern: {@code startRequest()} for duration + execution-log capture,
     * then a terminal {@code CATALOG_REQUEST_SUCCESS}/{@code FAILURE} record chosen by the
     * request's OUTCOME. A handler that catches its own error and returns a FAILURE
     * {@link ExtensionResponse} (instead of throwing) is still recorded as a failure — this is
     * why we no longer rely on {@code executeCatalog}'s throw-only detection.
     *
     * <p>{@link MustAuthorizeException} is recorded as a failure and re-thrown so the platform
     * can redirect the user to the consent URL.</p>
     */
    private ExtensionResponse audited(String catalogName,
                                      java.util.function.Supplier<ExtensionResponse> body) {
        String invokerId = invoker.getInvokerId();
        auditService.startRequest();
        // Logged AFTER startRequest() so these lines are captured into the record's execution
        // logs — guarantees the dashboard's "Logs" view is never empty, even for quiet requests.
        LOGGER.info("Catalog request '{}' started", catalogName);
        try {
            ExtensionResponse response = body.get();
            if (response != null && response.getResult() == ExtensionResponse.Result.FAILURE) {
                String reason = failureReason(response);
                LOGGER.info("Catalog request '{}' completed with FAILURE response: {}", catalogName, reason);
                auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", catalogName, reason, null, null);
            } else {
                LOGGER.info("Catalog request '{}' completed successfully", catalogName);
                auditService.record(invokerId, "CATALOG_REQUEST_SUCCESS", "success", catalogName, null, null, null);
            }
            return response;
        } catch (MustAuthorizeException e) {
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", catalogName,
                    e.getMessage(), e.getClass().getSimpleName(), null);
            throw e;
        } catch (RuntimeException e) {
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", catalogName,
                    e.getMessage(), e.getClass().getSimpleName(), null);
            throw e;
        } catch (Exception e) {
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", catalogName,
                    e.getMessage(), e.getClass().getSimpleName(), null);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Best-effort extraction of the error message from a FAILURE {@link ExtensionResponse}.
     * The {@code ExtensionResponse.Error} message accessor differs across krista-apis versions,
     * so this resolves it reflectively (method named like get/getMessage/getErrorMessage, else a
     * String field containing "message"). Returns {@code null} when nothing can be extracted.
     */
    static String failureReason(ExtensionResponse response) {
        try {
            ExtensionResponse.Error err = response.getError();
            if (err == null) {
                return null;
            }
            for (String name : new String[]{"getMessage", "getErrorMessage", "getReason", "message"}) {
                try {
                    Object v = err.getClass().getMethod(name).invoke(err);
                    if (v instanceof String s && !s.isBlank()) {
                        return s;
                    }
                } catch (NoSuchMethodException ignored) {
                    // try the next candidate accessor
                }
            }
            for (java.lang.reflect.Field f : err.getClass().getDeclaredFields()) {
                if (f.getType() == String.class && f.getName().toLowerCase().contains("message")) {
                    f.setAccessible(true);
                    Object v = f.get(err);
                    if (v instanceof String s && !s.isBlank()) {
                        return s;
                    }
                }
            }
        } catch (Exception ignored) {
            // reason extraction is best-effort; never break the request for it
        }
        return null;
    }

    /**
     * The {@code CaseManagementArea} class handles Salesforce Case management tasks,
     * like getting case feed items, sharing case updates, and managing alerts.
     * It works within the "Customer Relations" part of the CRM system.
     * This class uses {@link CaseDaoImpl} to access case data,
     * {@link KristaMediaClient} to handle files,
     * and uses {@link ValidationOrchestrator}, {@link ExtensionResponseGenerator},
     * and {@link ErrorHandlingStateManager} for validation and error handling.
     *
     * @param caseId                The ID of the Salesforce Case.
     * @param numberOfPosts         The number of posts to retrieve (optional).
     * @param allowRetry            If true, triggers error handling flow when validation fails; if false, returns error directly.
     * @return An ExtensionResponse with feed items or error messages.
     */
    @CatalogRequest(
            id = "localDomainRequest_4adceae0-923e-4163-bcc0-1c0fd9cf8b19",
            name = "Get Case Feed Items By Case Id",
            description = "This request will fetch the list of post items for the given case Id",
            area = "Case management",
            type = CatalogRequest.Type.QUERY_SYSTEM , tool = true)
    @Field.Desc(name = "Feed Items", type = "[ { Actor Name: Text, Post Message: Text, File: File } ]", required = false)
    public ExtensionResponse getCaseFeedItemsByCaseId(
            @Field.Text(name = "Case Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String caseId,
            @Field(name = "Number Of Posts", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Double numberOfPosts,
            @Field.Boolean(name = "Allow Retry", required = true) Boolean allowRetry) {
        return audited("Get Case Feed Items By Case Id",
                () -> getCaseFeedItemsByCaseIdImpl(caseId, numberOfPosts, allowRetry));
    }

    private ExtensionResponse getCaseFeedItemsByCaseIdImpl(String caseId, Double numberOfPosts, Boolean allowRetry) {

        try {
            // Validate both caseId and numberOfPosts together
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(ValidationResourceUtil.generateCaseValidationMap(caseId, numberOfPosts));

            if (validationResults.isEmpty()) {
                Case caseObj = validateAndFetchCase(caseId);
                String case_Id = caseObj.getId();
                Double posts = CaseManagementArea.validateMaxNumberOfPosts(numberOfPosts);
                List<Map<String, Object>> feeds = caseDao.getFeedItemsForCaseId(case_Id, posts);
                if (feeds.isEmpty()) {
                    return new ExtensionResponseBuilder().success(Map.of("The Feed Items is empty because the entries do not exist", List.of())).build();
                } else {
                    return new ExtensionResponseBuilder().success(Map.of("Feed Items", feeds)).build();
                }

            } else {
                // Check if retry is allowed
                if (Boolean.TRUE.equals(allowRetry)) {
                    // Go to error handling flow
                    String stateId = UUID.randomUUID().toString();
                    internalStateManager.put(stateId, Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));

                    return responseGenerator.generateConfirmationResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, validationResults, SubCatalogConstants.CONFIRM_REENTER_GET_CASE_FEED_ITEMS_BY_CASE_ID,
                            StateMapperUtil.addGetCaseFeedItemsByCaseIdMetaToMap(caseId, numberOfPosts, stateId)
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
            LOGGER.error(cause.getMessage());
            throw cause;
        } catch (Exception cause) {
            LOGGER.error("Error Occurred while fetching the list of post items for the given case {}", cause.getMessage(), cause);

            // Check if retry is allowed for exceptions as well
            if (Boolean.TRUE.equals(allowRetry)) {
                RemediationActions remediationActions = new RemediationActions(List.of(RemediationActionFactory
                        .createInformActionALLParticipants("Error Occurred while fetching the list of post items for the given case", List.of())),
                        null);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Error occurred while fetching post items from case.", System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, "");
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
     * Fetches a single Salesforce Case by its id and returns it as the typed {@code Case} entity.
     *
     * <p>Issues {@code GET /services/data/v53.0/sobjects/Case/{caseID}} via {@link CaseDaoImpl},
     * then converts the service-layer record to the catalog-layer {@code Case} entity declared
     * by the {@code @Field.Desc(type = "Entity(Case)")} return type.</p>
     *
     * @param caseID the Salesforce Case Id (e.g. {@code 500xx000000XXXXX})
     * @return {@link ExtensionResponse} with the fetched case under the {@code "Case"} output key
     */
    @CatalogRequest(
            id = "localDomainRequest_894682d8-3410-458b-a6d2-1008815b182e",
            name = "Get Case By Id",
            description = "Get Case By Id",
            area = "Case management",
            type = CatalogRequest.Type.CHANGE_SYSTEM , tool = true)
    @Field.Desc(name = "Case", type = "Entity(Case)", required = false)
    public ExtensionResponse getCaseById(
            @Field.Text(name = "Case ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String caseID) {
        return audited("Get Case By Id", () -> getCaseByIdImpl(caseID));
    }

    private ExtensionResponse getCaseByIdImpl(String caseID) {
        try {
            if (caseID == null || caseID.isBlank()) {
                return ExtensionResponseFactory.create(
                        "Case ID is required.",
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR
                );
            }

            Case serviceCase = caseDao.getCaseById(caseID);
            app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Case entityCase =
                    CatalogTypes.fromSFCase(serviceCase);

            return new ExtensionResponseBuilder()
                    .success(Map.of("Case", entityCase))
                    .build();

        } catch (MustAuthorizeException cause) {
            // Rule 3: rethrow BEFORE broader catch — MAE must propagate to the runtime
            // so the platform can redirect the user to the consent URL.
            LOGGER.error("Authorization required while fetching case {}: {}", caseID, cause.getMessage());
            throw cause;
        } catch (Exception cause) {
            LOGGER.error("Error occurred while fetching case {}: {}", caseID, cause.getMessage(), cause);
            return ExtensionResponseFactory.create(
                    "An unexpected error occurred while fetching the case: " + cause.getMessage(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
            );
        }
    }
    /**
     * Validates the number of posts requested and returns the allowed maximum.
     *
     * @param numberOfPosts The requested number of posts (nullable Double).
     * @return The validated maximum number of posts (double).
     * @throws IllegalArgumentException if the input is invalid.
     */
    public static Double validateMaxNumberOfPosts(Double numberOfPosts) {
        if (numberOfPosts != null && Double.isFinite(numberOfPosts)) {
            if (numberOfPosts % 1 != 0) {
                throw new IllegalArgumentException("Bad Request. Number of posts must be an integer.");
            }
            int requestedPosts = numberOfPosts.intValue();
            if (requestedPosts <= 0) {
                throw new IllegalArgumentException("Bad Request. Number of posts must be greater than 0.");
            }
            if (requestedPosts > 100) {
                throw new IllegalArgumentException("Bad Request. Number of posts must be less than or equal to 100.");
            }
        }
        return numberOfPosts;
    }

    /**
     * Validates the provided case ID and retrieves the corresponding {@link Case} object.
     * Throws a {@link RuntimeException} if the case ID is null, blank, or if retrieval fails.
     *
     * @param caseId the unique identifier of the case to retrieve
     * @return the {@link Case} object corresponding to the provided case ID
     * @throws RuntimeException if the case ID is null, blank, or invalid, or if retrieval fails
     */
    public Case validateAndFetchCase(String caseId) {
        if (caseId == null || caseId.isBlank()) {
            throw new RuntimeException("Case ID is required.");
        }

        try {
            return caseDao.getCaseById(caseId);
        } catch (MustAuthorizeException cause) {
            // Rule 3: rethrow BEFORE broader catch — MAE must propagate to the runtime.
            throw cause;
        } catch (Exception cause) {
            throw new RuntimeException("Invalid Case ID: " + caseId);
        }
    }

    /**
     * Adds an update message (and optionally a file) to the feed of a Salesforce Case.
     * This method validates the case ID and optional file, then posts the update to the case feed.
     * If validation fails, it returns a response requesting the user to re-enter the data.
     * If an error occurs, it returns a system error response.</p>
     *
     * @param caseId                the ID of the Salesforce Case to update (required)
     * @param updateMessage         the update message text to add to the case feed (required)
     * @param file                  an optional file to attach to the update (can be null)
     * @param allowRetry            If true, triggers error handling flow when validation fails; if false, returns error directly.
     * @return an {@link ExtensionResponse} indicating success or failure of the operation,
     * including validation errors if any
     */
    @CatalogRequest(
            id = "localDomainRequest_b3f52b0d-f4af-462b-a600-62d8afd83836",
            name = "Share Update to Case",
            description = "This request will add update to the Feed of the Case.",
            area = "Case management",
            type = CatalogRequest.Type.CHANGE_SYSTEM , tool = true)
    @Field.Boolean(name = "Is Updated", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse shareUpdateToCase(
            @Field.Text(name = "Case Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String caseId,
            @Field(name = "Update Message", type = "Paragraph", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String updateMessage,
            @Field.File(name = "File", multipleFileUpload = false, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) File file,
            @Field.Boolean(name = "Allow Retry", required = true) Boolean allowRetry) {
        return audited("Share Update to Case",
                () -> shareUpdateToCaseImpl(caseId, updateMessage, file, allowRetry));
    }

    private ExtensionResponse shareUpdateToCaseImpl(String caseId, String updateMessage, File file, Boolean allowRetry) {
        try {
            Map<Validator.ValidationResource, String> validationParams = new HashMap<>();
            validationParams.put(Validator.ValidationResource.CASE_ID, caseId);

            if (file != null) {
                validationParams.put(Validator.ValidationResource.FILE, getFileName(file));
            }
            List<ValidationOrchestrator.ValidationResult> validationResults =
                    validationOrchestrator.validate(validationParams);
            LOGGER.info("Validation results: {}", validationResults);
            if (validationResults.isEmpty()) {
                if (file != null) {
                    LOGGER.info("file is not null ...");
                    java.io.File javaFile = kristaMediaClient.toJavaFile(file);
                    String base64EncodedString = caseDao.getBase64EncodedString(javaFile);
                    Boolean resultValue = caseDao.addFeedUpdateToCaseById(
                            caseId, getEscapeCharacterMessage(updateMessage), base64EncodedString, javaFile.getName());
                    return new ExtensionResponseBuilder().success(Map.of("Is Updated", resultValue)).build();
                } else {
                    LOGGER.info("file is null ...");
                    Boolean isUpdated = caseDao.addFeedUpdateToCaseById(
                            caseId, getEscapeCharacterMessage(updateMessage), null, null);
                    return new ExtensionResponseBuilder().success(Map.of("Is Updated", isUpdated)).build();
                }
            } else {
                // Check if retry is allowed
                if (Boolean.TRUE.equals(allowRetry)) {
                    // Go to error handling flow
                    String stateId = UUID.randomUUID().toString();
                    internalStateManager.put(stateId, Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));

                    return responseGenerator.generateConfirmationResponse(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, validationResults,
                            SubCatalogConstants.CONFIRM_REENTER_SHARE_UPDATE_TO_CASE,
                            StateMapperUtil.addShareUpdateToCaseMetaToMap(caseId, updateMessage, file, stateId));
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
            // Rule 3: rethrow BEFORE broader catch — MAE must propagate to the runtime
            // so the platform can redirect the user to the consent URL.
            LOGGER.error("Authorization required while sharing update to case: {}", cause.getMessage());
            throw cause;
        } catch (Exception cause) {
            LOGGER.error("Error occurred while sharing the update to case : {}", cause.getMessage(), cause);

            // Check if retry is allowed for exceptions as well
            if (Boolean.TRUE.equals(allowRetry)) {
                RemediationActions remediationActions = new RemediationActions(List.of(RemediationActionFactory
                        .createInformActionALLParticipants("Error occurred while  updating to the Feed of the Case", List.of())),
                        null);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Error occurred while sharing update to case.", System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, "");
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
     * Returns the file name of the given {@code File} object.
     *
     * @param file the {@code File} object; may be {@code null}
     * @return the file name if {@code file} is not {@code null}, otherwise an empty string
     */
    private String getFileName(File file) {
        if (file == null) {
            return "";
        } else {
            return file.getFileName();
        }
    }

    /**
     * Converts special characters in a message to their escaped versions.
     * For example, double quotes become \", new lines become \n, and so on.
     *
     * @param updateMessage the message to escape
     * @return the message with special characters replaced by escape codes
     */
    public static String getEscapeCharacterMessage(String updateMessage) {
        StringBuilder escapedMessage = new StringBuilder();

        for (int i = 0; i < updateMessage.length(); i++) {
            char ch = updateMessage.charAt(i);
            switch (ch) {
                case '\"':
                    escapedMessage.append("\\\"");
                    break;
                case '\n':
                    escapedMessage.append("\\n");
                    break;
                case '\\':
                    escapedMessage.append("\\\\");
                    break;
                case '\f':
                    escapedMessage.append("\\f");
                    break;
                case '\t':
                    escapedMessage.append("\\t");
                    break;
                case '\b':
                    escapedMessage.append("\\b");
                    break;
                case '\r':
                    escapedMessage.append("\\r");
                    break;
                default:
                    escapedMessage.append(ch);
                    break;
            }
        }
        return escapedMessage.toString();
    }

    /**
     * Sends an alert when a case is created or updated.
     * It checks if the event is a "Case Change Alert" and processes the case data.
     * Then it returns the updated case information.
     *
     * @param eventName the name of the event (should be "Case Change Alert")
     * @param eventData the details of the case event
     * @return a response with the updated case info or an error if something goes wrong
     */
    @CatalogRequest(
            id = "localDomainRequest_6efb4f6c-745d-4c2f-8582-f03e447c5e15",
            name = "Case Change Alert",
            description = "This request will alert the user whenever you create a case or update any case.",
            area = "Case management",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.Desc(name = "CaseResponse", type = "{ Case Id: Text, Case Contact Id: Text, Case Account Id: Text, Case Subject: Text, Case Is Closed: Boolean, Case Owner Id: Text, Case Created Date: Date, Case Last Modified Case: Date, Case Number: Number, Case Status: Text, Case Reason: Text, Case Origin: Text, Case Description: Paragraph, Case Priority: Text }", required = false)
    public ExtensionResponse caseChangeAlert(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData) {
        return audited("Case Change Alert", () -> caseChangeAlertImpl(eventName, eventData));
    }

    private ExtensionResponse caseChangeAlertImpl(String eventName, FreeForm eventData) {
        Map<String, Object> caseMap = new HashMap<>();
        try {
            if (!"Case Change Alert".equals(eventName)) {
                return ExtensionResponseFactory.create("Unsupported event name:" + eventName);
            }

            if (eventData == null || eventData.get("Id") == null) {
                return ExtensionResponseFactory.create("Missing required Case Id in eventData" + eventName);
            }

            caseMap.put("Case Id", eventData.get("Id"));
            upsertCaseMap(eventData, caseMap);
            updateCaseDetails(eventData, caseMap);
            return new ExtensionResponseBuilder().success(Map.of("CaseResponse", caseMap)).build();

        } catch (IllegalArgumentException cause) {
            RemediationActions remediationActions = new RemediationActions(List.of(RemediationActionFactory.createInformActionALLParticipants(
                    Constants.ERROR_CASE_CHANGE_ALERT, List.of())), null);
            ExtensionResponse.Error error = new ExtensionResponse.Error(Constants.ERROR_CASE_CHANGE_ALERT, System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, cause.getMessage());

            return new ExtensionResponseBuilder().failure(error, remediationActions, null).build();

        } catch (IOException | ExecutionException cause) {
            throw new IllegalStateException(cause.getMessage(), cause);
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(cause.getMessage(), cause);
        }
    }

    /**
     * Updates the case details in the given map by fetching recent case history.
     * It queries the last 20 changes for the case identified in eventData,
     * then updates the caseMap with the fetched data if applicable.
     *
     * @param eventData the event data containing the Case Id
     * @param caseMap   the map to update with case details
     * @throws IOException          if there is an input/output error during data fetching
     * @throws ExecutionException   if an error occurs during asynchronous execution
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private void updateCaseDetails(FreeForm eventData, Map<String, Object> caseMap) throws IOException,
            ExecutionException, InterruptedException {
        String query = "SELECT Fields(ALL) FROM CaseHistory WHERE CaseId = '" + eventData.get("Id") +
                "' ORDER BY CreatedDate DESC LIMIT 20";

        Map<String, Object> fetched = caseDao.getCaseUpdatedFields(query);
        if (fetched.size() != 1 || !fetched.containsKey(Constants.CREATED)) {
            upsertCaseMap(fetched, caseMap);
        }
    }
}

