package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers;

import app.krista.extension.executor.*;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.CaseManagementAreaSubCatalogRequest;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.ExtensionResponseFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.NamedFieldFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.RemediationActionFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.ValidationOrchestrator;
import app.krista.model.field.NamedField;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Generates extension responses for the Salesforce Customer Service.
 * This class is responsible for creating appropriate extension responses based on
 * validation results, error conditions, and other contextual information.
 * It provides methods to generate various types of responses including confirmation responses,
 * fetch responses, and fetch deny responses.
 */
@Service
public class ExtensionResponseGenerator {

    private static final String ERROR_MESSAGE = "ErrorMessage";
    private static final String STEP_MESSAGE = "StepMessage";
    private static final String FIELD = "Field";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionResponseGenerator.class);

    /**
     * Generates a confirmation response based on validation results.
     * 
     * @param exceptionType The type of exception that occurred
     * @param validationResults The results of the validation process
     * @param subCatalogRequestName The name of the sub-catalog request
     * @param state The current state map containing context information
     * @return An ExtensionResponse configured for confirmation with appropriate remediation actions
     */
    public ExtensionResponse generateConfirmationResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state) {

        List<NamedField> fields = List.of(NamedFieldFactory.createSwitchField(CaseManagementAreaSubCatalogRequest.REENTER));
        Map<String, Object> stringStringMap = generateResponse(validationResults, false);

        ExtensionResponse.Error error = new ErrorBuilder().exceptionType(exceptionType).build();

        return new ExtensionResponseBuilder().failure(error,
                new RemediationActions(List.of(RemediationActionFactory.createAskAction((String) stringStringMap.get(STEP_MESSAGE), fields)),
                        subCatalogRequestName), state).build();
    }

    /**
     * Generates a fetch response based on validation results.
     * 
     * @param exceptionType The type of exception that occurred
     * @param validationResults The results of the validation process
     * @param subCatalogRequestName The name of the sub-catalog request
     * @param state The current state map containing context information
     * @return An ExtensionResponse configured for fetch operations with appropriate remediation actions
     */
    @SuppressWarnings("unchecked")
    public ExtensionResponse generateFetchResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state) {

        LOGGER.info("Validation failed for : {}", validationResults.size());
        Map<String, Object> stringStringMap = generateResponse(validationResults, true);
        return ExtensionResponseFactory.create((String) stringStringMap.get(ERROR_MESSAGE), exceptionType,
                List.of(RemediationActionFactory.createAskAction((String) stringStringMap.get(STEP_MESSAGE), (List<NamedField>) stringStringMap.get(FIELD))),
                subCatalogRequestName, state);
    }

    /**
     * Generates a response map containing error messages, step messages, and fields based on validation results.
     * 
     * @param validationResults The results of the validation process
     * @param fetchResponse Flag indicating whether this is for a fetch response (true) or confirmation response (false)
     * @return A map containing error messages, step messages, and fields
     */
    private Map<String, Object> generateResponse(List<ValidationOrchestrator.ValidationResult> validationResults, boolean fetchResponse) {
        StringBuilder stepMessage = new StringBuilder();
        StringBuilder errMessage = new StringBuilder();
        List<NamedField> fields = new ArrayList<>();
        for (ValidationOrchestrator.ValidationResult validationResult : validationResults) {
            stepMessage.append(!fetchResponse ? validationResult.confirmStepMessage() : validationResult.fetchStepMessage()).append("\n");
            errMessage.append(validationResult.errMessage()).append("\n");
            if (fetchResponse) {
                fields.add(NamedFieldFactory.createField(validationResult.fetchFieldName(), validationResult.fieldType()));
            }
        }
        return Map.of(STEP_MESSAGE, stepMessage.toString(), ERROR_MESSAGE, errMessage.toString(), FIELD, fields);
    }

    /**
     * Generates a fetch deny response based on validation results.
     * This response is used when a fetch operation is denied due to validation failures.
     * 
     * @param exceptionType The type of exception that occurred
     * @param validationResults The results of the validation process
     * @param subCatalogRequestName The name of the sub-catalog request
     * @param state The current state map containing context information
     * @return An ExtensionResponse configured to inform all participants about the denial
     */
    public ExtensionResponse generateFetchDenyResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state
    ) {
        StringJoiner stepMessage = new StringJoiner(", ", Constants.INVALID + " ", " values are provided.");
        StringBuilder errMessage = new StringBuilder();

        for (ValidationOrchestrator.ValidationResult validationResult : validationResults) {
            String fieldName = validationResult.fetchFieldName();
            if (fieldName != null && !fieldName.trim().isEmpty()) {
                stepMessage.add("'" + fieldName + "'");
            } else {
                stepMessage.add("'Unknown Field'");
            }
            errMessage.append(validationResult.errMessage()).append("\n");
        }

        return ExtensionResponseFactory.create(errMessage.toString(), exceptionType, List.of(RemediationActionFactory.createInformActionALLParticipants(stepMessage.toString(), List.of())),
                subCatalogRequestName, state
        );
    }
}
