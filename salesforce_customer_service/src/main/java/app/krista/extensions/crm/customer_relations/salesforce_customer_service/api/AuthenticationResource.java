package app.krista.extensions.crm.customer_relations.salesforce_customer_service.api;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.RefreshTokenStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.XMlResponseParser;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.CaseNotifications;
import app.krista.extensions.util.EventHandler;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.model.base.FreeForm;
import com.github.scribejava.core.model.OAuth2AccessToken;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.INVALID_STATE_PARAMETERS;

@Path("/salesforcecustomerservice")
public final class AuthenticationResource {
    public static final String USER_AUTHENTICATED_SUCCESSFULLY = "User authenticated successfully.";
    public static final String SAVE_THE_CHANGES = " Save the changes.";
    private static final String CODE = "code";
    private static final String STATE = "state";
    private final RefreshTokenStore refreshTokenStore;
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final SalesforceTokenProviderFactory providerFactory;
    private final AuthorizationContext context;
    private final AuthorizationListener authorizationListener;
    private final EventHandler eventHandler;


    @Inject
    public AuthenticationResource(RefreshTokenStore refreshTokenStore, SalesforceCustomerServiceAttributesStore attributesStore,
                                  SalesforceTokenProviderFactory providerFactory, AuthorizationContext context,
                                  AuthorizationListener authorizationListener, EventHandler eventHandler) {
        this.refreshTokenStore = refreshTokenStore;
        this.attributesStore = attributesStore;
        this.providerFactory = providerFactory;
        this.authorizationListener = authorizationListener;
        this.context = context;
        this.eventHandler = eventHandler;
    }

    @GET
    @Path("/callback")
    public String getCallBack(@QueryParam(CODE) String code, @QueryParam(STATE) String state) throws IOException {
        Objects.requireNonNull(code);
        String[] parts = state.split("#");
        if (parts[0].isBlank() || parts.length > 2) {
            throw new BadRequestException(INVALID_STATE_PARAMETERS);
        }
        String key = parts[0];
        String authContextId = parts.length == 2 ? parts[1] : null;
        Map<String, Object> authTokenResponse = getAuthTokenResponse(code, authContextId);
        refreshTokenStore.put(key, (String) authTokenResponse.get(Constants.REFRESH_TOKEN));
        if (context.isAuthenticated()) {
            authorizationListener.authorized();
            return USER_AUTHENTICATED_SUCCESSFULLY;
        }
        return USER_AUTHENTICATED_SUCCESSFULLY + SAVE_THE_CHANGES;
    }

    private Map<String, Object> getAuthTokenResponse(String code, String authContextId) throws IOException {
        Map<String, Object> response = new HashMap<>();
        try {
            Objects.requireNonNull(code);
            SalesforceTokenProvider tokenProvider;
            if (authContextId == null) {
                tokenProvider = providerFactory.create();
            } else if (authContextId.isBlank()) {
                throw new BadRequestException(INVALID_STATE_PARAMETERS);
            } else {
                SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes = attributesStore.load(authContextId);
                tokenProvider = providerFactory.create(salesforceCustomerServiceAttributes);
            }
            OAuth2AccessToken accessToken = tokenProvider.getSalesforceCustomerServiceAttributes().getOauthService().getAccessToken(code);
            response.put(Constants.REFRESH_TOKEN, accessToken.getRefreshToken());
        } catch (Exception cause) {
            System.err.println("Exception occurred due to :" + cause.getMessage());
            throw new IllegalStateException("Error occurred during authorization", cause.getCause());
        } finally {
            if (authContextId != null) {
                attributesStore.remove(authContextId);
            }
        }
        return response;
    }

    @POST
    @Path("/caseChanged")
    public Response handleCaseChangedEvent(String xml) throws Exception {
        JAXBElement<CaseNotifications> caseNotifications = XMlResponseParser.parseCaseXmlResponse(xml);
        if (caseNotifications != null) {
            FreeForm caseNotificationSObject = caseNotifications.getValue().getCaseNotification().getSalesforceXMLCase()
                    .toCaseFreeForm();
            eventHandler.handleEvent("Case Change Alert", caseNotificationSObject);
        }
        return Response.ok().build();
    }
}
