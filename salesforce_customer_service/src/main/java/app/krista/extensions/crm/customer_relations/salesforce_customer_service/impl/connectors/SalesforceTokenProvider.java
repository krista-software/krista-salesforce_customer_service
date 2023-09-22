package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors;


import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.RefreshTokenStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.field.NamedValuedField;
import com.github.scribejava.apis.salesforce.SalesforceToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessTokenErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
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

    @Inject
    public SalesforceTokenProvider(RefreshTokenStore refreshTokenStore, RequestContext requestContext, SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes, AuthorizationContext authorizationContext) {
        this(refreshTokenStore, requestContext, salesforceCustomerServiceAttributes, authorizationContext, null);
    }

    public SalesforceTokenProvider(RefreshTokenStore refreshTokenStore, RequestContext requestContext, SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes, AuthorizationContext authorizationContext, String authContextId) {
        this.requestContext = requestContext;
        this.refreshTokenStore = refreshTokenStore;
        this.salesforceCustomerServiceAttributes = salesforceCustomerServiceAttributes;
        this.authorizationContext = authorizationContext;
        this.authContextId = authContextId;
    }

    public SalesforceCustomerServiceAttributes getSalesforceCustomerServiceAttributes() {
        return salesforceCustomerServiceAttributes;
    }

    public SalesforceToken getSalesforceToken() throws IOException {
        return getSalesforceToken(requestContext.invokeAsUser());
    }

    public SalesforceToken getSalesforceTokenForAdmin() throws IOException {
        return getSalesforceToken(false);
    }

    private SalesforceToken getSalesforceToken(boolean isAdmin) throws IOException {
        if (isAdmin) {
            String refreshToken = refreshTokenStore.get(authorizationContext.getAuthorizedAccount().getAccountId());
            if (refreshToken == null || refreshToken.isBlank()) {
                throw createMustAuthorizationException(false);
            }
            try {
                return getSalesforceToken(salesforceCustomerServiceAttributes.getOauthService().refreshAccessToken(refreshToken));
            } catch (MustAuthorizeException cause) {
                throw createMustAuthorizationException(true);
            } catch (OAuth2AccessTokenErrorResponse cause) {
                if (cause.getErrorDescription().equals("expired access/refresh token") && cause.getErrorDescription() != null) {
                    throw createMustAuthorizationException(true);
                }
                throw new RuntimeException("Something went wrong, please try again later");
            } catch (Exception errorResponse) {
                throw new RuntimeException("Refresh Token not found. Please re authorize yourself" + errorResponse.getMessage());
            }
        } else {
            Object username = salesforceCustomerServiceAttributes.getUserName();
            Object password = salesforceCustomerServiceAttributes.getPassword();
            Object securityToken = salesforceCustomerServiceAttributes.getSecurityToken();

            try {
                return getSalesforceToken(
                        salesforceCustomerServiceAttributes.getOauthService().getAccessTokenPasswordGrant(String.valueOf(username),
                                password + String.valueOf(securityToken)));
            } catch (Exception e) {
                throw new MustAuthorizeException(e.getMessage());
            }
        }
    }

    private SalesforceToken getSalesforceToken(OAuth2AccessToken accessToken) {
        final SalesforceToken salesforceAccessToken;
        if (accessToken instanceof SalesforceToken) {
            salesforceAccessToken = (SalesforceToken) accessToken;
        } else {
            throw new MustAuthorizeException("Salesforce didn't return the correct token. Please Reauthorize");
        }
        return salesforceAccessToken;
    }

    private MustAuthorizeException createMustAuthorizationException(boolean reAuthentication) {
        LOGGER.debug(reAuthentication ? "Got error for Authentication. So sending for re authentication" : "Got error for Authentication. So sending for authentication");
        List<NamedValuedField> details = new ArrayList<>();
        NamedValuedField userIdField = new NamedValuedField("userId", "Text", authorizationContext.getAuthorizedAccount().getAccountId(), new HashMap<>(), new HashMap<>());
        details.add(userIdField);
        if (authContextId != null) {
            NamedValuedField contextIdField = new NamedValuedField("authContextId", "Text", authContextId, new HashMap<>(), new HashMap<>());
            details.add(contextIdField);
        }
        return new MustAuthorizeException(reAuthentication ? "The refresh token is no longer valid. Please reauthorize yourself." : "You are not authorized. Please authorize yourself", details);
    }

}

