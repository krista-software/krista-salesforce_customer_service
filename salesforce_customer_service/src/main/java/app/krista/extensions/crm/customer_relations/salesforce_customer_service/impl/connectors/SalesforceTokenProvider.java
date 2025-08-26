package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.RefreshTokenStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.field.NamedValuedField;
import com.github.scribejava.apis.salesforce.SalesforceToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SalesforceTokenProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceTokenProvider.class);
    private final RefreshTokenStore refreshTokenStore;
    private final RequestContext requestContext;
    private final SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes;
    private final String authContextId;
    private final AuthorizationContext authorizationContext;

    /**
     * Constructs a SalesforceTokenProvider with the specified dependencies.
     *
     * @param refreshTokenStore The store used to persist refresh tokens
     * @param requestContext The context of the current request
     * @param salesforceCustomerServiceAttributes The Salesforce service configuration attributes
     * @param authorizationContext The authorization context for the current user
     */
    @Inject
    public SalesforceTokenProvider(RefreshTokenStore refreshTokenStore, RequestContext requestContext, SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes, AuthorizationContext authorizationContext) {
        this(refreshTokenStore, requestContext, salesforceCustomerServiceAttributes, authorizationContext, null);
    }

    /**
     * Constructs a SalesforceTokenProvider with the specified dependencies and authorization context ID.
     *
     * @param refreshTokenStore The store used to persist refresh tokens
     * @param requestContext The context of the current request
     * @param salesforceCustomerServiceAttributes The Salesforce service configuration attributes
     * @param authorizationContext The authorization context for the current user
     * @param authContextId The authorization context ID, if applicable
     */
    public SalesforceTokenProvider(RefreshTokenStore refreshTokenStore, RequestContext requestContext, SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes, AuthorizationContext authorizationContext, String authContextId) {
        this.requestContext = requestContext;
        this.refreshTokenStore = refreshTokenStore;
        this.salesforceCustomerServiceAttributes = salesforceCustomerServiceAttributes;
        this.authorizationContext = authorizationContext;
        this.authContextId = authContextId;
    }

    /**
     * Returns the Salesforce customer service attributes used by this token provider.
     *
     * @return The Salesforce customer service attributes
     */
    public SalesforceCustomerServiceAttributes getSalesforceCustomerServiceAttributes() {
        return salesforceCustomerServiceAttributes;
    }

    /**
     * Retrieves a Salesforce token for the current user.
     *
     * @return A valid Salesforce token for the current user
     * @throws MustAuthorizeException If authorization is required to obtain a new token
     */
    public SalesforceToken getSalesforceTokenForUser() {
        return getSalesforceToken(requestContext.invokeAsUser(), null);
    }

    /**
     * Retrieves a Salesforce token for admin operations.
     *
     * @throws MustAuthorizeException If authorization is required to obtain a new token
     */
    public void getSalesforceTokenForAdmin() {
        getSalesforceToken(false, null);
    }

    /**
     * Retrieves a Salesforce token based on the specified parameters.
     *
     * @param isAdmin Whether the token is for admin operations
     * @param accountId The account ID to use, or null to use the current authorized account
     * @return A valid Salesforce token
     * @throws IllegalArgumentException If the provided credentials are invalid
     * @throws MustAuthorizeException If authorization is required to obtain a new token
     */
    private SalesforceToken getSalesforceToken(boolean isAdmin, String accountId) throws IllegalArgumentException {
        if (SalesforceTokenProvider.validateUsingOAuth(isAdmin, salesforceCustomerServiceAttributes)) {
            try {
                String domain = salesforceCustomerServiceAttributes.getInstanceUrl();
                return getSalesforceTokenForUser(salesforceCustomerServiceAttributes.getOauthService(domain).getAccessTokenClientCredentialsGrant());
            } catch (Exception cause) {
//                throw new RuntimeException("Refresh Token not found. Please re authorize yourself. " + cause.getMessage(), cause);
                cause.printStackTrace();
                LOGGER.error("Failed to get access token for user. - {}", cause.getMessage(), cause);
                String message = this.getErrorMessage(cause);
                throw new IllegalArgumentException(message , cause);
            }
        } else {
            Object username = salesforceCustomerServiceAttributes.getUserName();
            Object password = salesforceCustomerServiceAttributes.getPassword();
            Object securityToken = salesforceCustomerServiceAttributes.getSecurityToken();

            try {
                return getSalesforceTokenForUser(
                        salesforceCustomerServiceAttributes.getOauthService().getAccessTokenPasswordGrant(String.valueOf(username),
                                password + String.valueOf(securityToken)));
            } catch (Exception cause) {
                String message = this.getErrorMessage(cause);
                throw new IllegalArgumentException(message , cause);
            }
        }
    }

    /**
     * Returns the value of the "error" key from the exception message if it is valid JSON.
     * If parsing fails, returns a generic error message.
     * <p>
     * This method parses Salesforce API error responses and provides user-friendly
     * error messages based on the error type.
     *
     * @param cause The exception to extract the error message from
     * @return The error value, or a generic message if not available
     */
    private String getErrorMessage(Exception cause) {
        String message = cause.getMessage();
        if (message != null) {
            try {
                // Attempt to parse the message as JSON
                JSONObject json = new JSONObject(message);
                if (json.has("error")) {
                    // Return the value of the "error" key directly
                    String errorMessage = json.getString("error");
                    return switch (errorMessage) {
                        // Thrown when the client ID or Client Secret is invalid
                        case "invalid_client" ->
                                "Authentication failed. Please check your Salesforce client credentials.";

                        //Thrown when the user credentials (username, password, or security token) are invalid
                        case "invalid_grant" ->
                                "Authentication failed. Please check your Salesforce username, password, and security token.";

                        //Handles any other authentication errors returned by Salesforce
                        default -> "Authentication failed: " + errorMessage;
                    };
                }
            } catch (Exception exception) {
                LOGGER.debug("Failed to parse error message as JSON: {}", exception.getMessage(), exception);
                return "Provided attributes are not valid.";
            }
        }
        return "An unexpected error occurred. Please contact support.";
    }

    /**
     * Generates a unique key for storing and retrieving refresh tokens.
     *
     * @param accountId The account ID associated with the refresh token
     * @return A unique key combining account ID and client credentials
     */
    private String getRefreshTokenKey(String accountId) {
        return accountId + "&" + salesforceCustomerServiceAttributes.getClientId() + salesforceCustomerServiceAttributes.getClientSecret();
    }

    /**
     * Validates and casts the given OAuth2 access token to a {@link SalesforceToken}.
     * If the provided token is an instance of {@code SalesforceToken}, it is returned.
     * Otherwise, a {@link MustAuthorizeException} is thrown to indicate that re-authorization is required.
     *
     * @param accessToken the OAuth2 access token to validate and cast
     * @return the {@link SalesforceToken} instance
     * @throws MustAuthorizeException if the token is not a {@code SalesforceToken}
     */
    private SalesforceToken getSalesforceTokenForUser(OAuth2AccessToken accessToken) {
        final SalesforceToken salesforceAccessToken;
        if (accessToken instanceof SalesforceToken) {
            salesforceAccessToken = (SalesforceToken) accessToken;
        } else if (accessToken.getAccessToken() != null) {
            salesforceAccessToken = new SalesforceToken(accessToken.getAccessToken(), salesforceCustomerServiceAttributes.getInstanceUrl(), accessToken.getRawResponse());
        } else {
            throw new IllegalArgumentException("Salesforce didn't return the correct token. Please provide valid credentials.");
        }
        return salesforceAccessToken;
    }

    /**
     * Creates a MustAuthorizeException with appropriate details for the authorization flow.
     *
     * @param reAuthentication Whether this is a re-authentication request
     * @param accountId The account ID requiring authentication
     * @return A MustAuthorizeException with appropriate details
     */
    private MustAuthorizeException createMustAuthorizationException(boolean reAuthentication, String accountId) {
        LOGGER.info(reAuthentication ? "Got error for Authentication. So sending for re authentication" : "Got error for Authentication. So sending for authentication");
        accountId = accountId == null ? authorizationContext.getAuthorizedAccount().getAccountId() : accountId;
        List<NamedValuedField> details = new ArrayList<>();
        NamedValuedField userIdField = new NamedValuedField("userId", "Text", accountId, new HashMap<>(), new HashMap<>());
        details.add(userIdField);
        if (authContextId != null) {
            NamedValuedField contextIdField = new NamedValuedField("authContextId", "Text", authContextId, new HashMap<>(), new HashMap<>());
            details.add(contextIdField);
        }
        return new MustAuthorizeException(reAuthentication ? "Refresh Token Expired. Please reauthorize yourself" : "Authorize yourself and click 'Validate Attributes' before saving changes to proceed further", details);
    }

    /**
     * Determines whether OAuth-based authentication should be used based on the context and attributes.
     * <p>
     * OAuth is used when in admin context or when username/password credentials are not fully provided.
     *
     * @param isAdmin Whether the context is admin
     * @param salesforceSalesAttributes The Salesforce attributes
     * @return True if OAuth should be used, false if password grant is sufficient
     */
    public static boolean validateUsingOAuth(boolean isAdmin, SalesforceCustomerServiceAttributes
            salesforceSalesAttributes) {
        return isAdmin || (salesforceSalesAttributes.getUserName() == null || salesforceSalesAttributes.getUserName().isBlank() ||
                salesforceSalesAttributes.getPassword() == null || salesforceSalesAttributes.getPassword().isBlank() ||
                salesforceSalesAttributes.getSecurityToken() == null || salesforceSalesAttributes.getSecurityToken().isBlank());
    }

}

