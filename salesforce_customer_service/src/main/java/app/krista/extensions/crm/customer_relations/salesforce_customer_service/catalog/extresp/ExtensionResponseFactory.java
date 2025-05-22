package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.RemediationAction;
import app.krista.extension.executor.RemediationActions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Factory class for creating standardized ExtensionResponse objects.
 * <p>
 * This class provides methods to create various types of extension responses
 * for the Salesforce Customer Service integration. It encapsulates the logic
 * for constructing properly formatted responses with appropriate status codes,
 * messages, and remediation actions.
 * <p>
 * The factory helps maintain consistency in response formats across the application
 * and simplifies the creation of complex response objects.
 */
public class ExtensionResponseFactory {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ExtensionResponseFactory() {
    }

    /**
     * Creates a success response with the provided values.
     *
     * @param values The data to include in the success response
     * @return An ExtensionResponse object indicating successful processing
     */
    public static ExtensionResponse create(Map<String, Object> values) {
        return new ExtensionResponse(ExtensionResponse.Result.SUCCESS, values,
                null, null, null);
    }

    /**
     * Creates an error response with exception details.
     *
     * @param cause The exception that caused the error
     * @param message The error message describing what went wrong
     * @param exceptionType The type of exception that occurred
     * @return An ExtensionResponse object containing error details with inform remediation action
     */
    public static ExtensionResponse create(Exception cause,
                                           String message,
                                           ExtensionResponse.Error.ExceptionType exceptionType) {
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(),
                exceptionType, Arrays.toString(cause.getStackTrace()));
        RemediationActions remediationActions = new RemediationActions(List.of(RemediationActionFactory.createInformAction(message, List.of())), null);
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE,
                null, error, remediationActions, Map.of());
    }

    /**
     * Creates a failure response with custom remediation actions and state.
     *
     * @param message The error message describing what went wrong
     * @param exceptionType The type of exception that occurred
     * @param actions List of remediation actions to include
     * @param subCatalogRequestName Name of the sub-catalog request if applicable
     * @param state Map containing state information to be preserved
     * @return An ExtensionResponse object with the specified remediation actions and state
     */
    public static ExtensionResponse create(String message,
                                           ExtensionResponse.Error.ExceptionType exceptionType,
                                           List<RemediationAction> actions,
                                           String subCatalogRequestName,
                                           Map<String, Object> state) {
        RemediationActions remediationActions = new RemediationActions(actions, subCatalogRequestName);
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(),
                exceptionType, "");
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error,
                remediationActions, state);
    }

    /**
     * Creates a simple failure response with just an error message.
     *
     * @param message The error message describing what went wrong
     * @return An ExtensionResponse object containing the error message
     */
    public static ExtensionResponse create(String message) {
        RemediationActions remediationActions = new RemediationActions(null, null);
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(),
                null, "");
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error,
                remediationActions, null);
    }

    /**
     * Creates a failure response with an error message and exception type.
     *
     * @param message The error message describing what went wrong
     * @param exceptionType The type of exception that occurred
     * @return An ExtensionResponse object containing the error message and exception type
     */
    public static ExtensionResponse create(String message, ExtensionResponse.Error.ExceptionType exceptionType) {
        RemediationActions remediationActions = new RemediationActions(null, null);
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(),
                exceptionType, "");
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error,
                remediationActions, null);
    }
}
