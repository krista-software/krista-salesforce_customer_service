package app.krista.extensions.crm.customer_relations.salesforce_customer_service;

import app.krista.extension.authorization.MustAuthenticateException;
import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.request.ProtoRequest;
import app.krista.extension.request.ProtoResponse;
import app.krista.extension.request.protos.http.HttpRequest;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;
import com.github.scribejava.core.oauth.OAuth20Service;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.*;

/**
 * Authenticator for handling Salesforce Customer Service authentication in a Krista extension.
 *
 * <p>This class implements the {@link RequestAuthenticator} interface and is responsible for:
 * <ul>
 *     <li>Handling the OAuth2 authentication callback from Salesforce.</li>
 *     <li>Extracting user context from callback requests.</li>
 *     <li>Redirecting users to the Salesforce authorization URL when needed.</li>
 * </ul>
 */
public class SalesforceCustomerServiceAuthenticator implements RequestAuthenticator {

    private final SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes;
    private final SalesforceCustomerServiceAttributesStore attributesStore;

    /**
     * Constructs a new authenticator using the default and stored Salesforce attribute sources.
     *
     * @param salesforceCustomerServiceAttributes default Salesforce configuration attributes
     * @param attributesStore store for loading Salesforce attributes by context
     */
    @Inject
    public SalesforceCustomerServiceAuthenticator(SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes,
                                                  SalesforceCustomerServiceAttributesStore attributesStore) {
        this.salesforceCustomerServiceAttributes = salesforceCustomerServiceAttributes;
        this.attributesStore = attributesStore;
    }

    /**
     * Returns the authentication scheme used. Returns {@code null} as this implementation uses OAuth redirection.
     *
     * @return null
     */
    @Override
    public String getScheme() {
        return null;
    }

    /**
     * Returns a set of supported protocols. This implementation returns an empty set.
     *
     * @return empty set
     */
    @Override
    public Set<String> getSupportedProtocols() {
        return Set.of();
    }

    /**
     * Extracts the authenticated account ID from the incoming callback request.
     *
     * <p>Parses the 'state' query parameter to retrieve the user ID or context-specific value.</p>
     *
     * @param protoRequest the request from which to extract the account ID
     * @return the user ID from the callback state parameter or {@code null} if not present or an error occurs
     */
    @Override
    public String getAuthenticatedAccountId(ProtoRequest protoRequest) {
        try {
            HttpRequest httpRequest = (HttpRequest) protoRequest;
            if (httpRequest.getUri().getPath().equals("/salesforcecustomerservice/callback")) {
                httpRequest.bufferBody();
                MultivaluedMap<String, String> queryParameters = httpRequest.getQueryParameters();
                String state = String.valueOf(queryParameters.get("state").getFirst());
                String[] parts = state.split("#");
                return parts[0];
            } else {
                return null;
            }
        } catch (RuntimeException | IOException cause) {
            return null;
        }
    }

    /**
     * This implementation does not support service-level authorization. Always returns {@code false}.
     *
     * @param s ignored
     * @return false
     */
    @Override
    public boolean setServiceAuthorization(String s) {
        return false;
    }

    /**
     * Returns the attribute fields used by the authenticator. This implementation returns an empty map.
     *
     * @return empty map
     */
    @Override
    public Map<String, NamedField> getAttributeFields() {
        return Map.of();
    }

    /**
     * Returns a response prompting the user to authenticate.
     * This version for ProtoRequest is unused and returns {@code null}.
     *
     * @param cause the exception requiring authentication
     * @param request the original request
     * @return {@code null}
     */
    @Override
    public ProtoResponse getMustAuthenticateResponse(MustAuthenticateException cause, ProtoRequest request) {
        return null;
    }

    /**
     * Returns a response prompting the user to authenticate.
     * This version is unused and returns {@code null}.
     *
     * @param cause the exception requiring authentication
     * @return {@code null}
     */
    @Override
    public AuthorizationResponse getMustAuthenticateResponse(MustAuthenticateException cause) {
        return null;
    }

    /**
     * Returns a response prompting the user to authorize access.
     * This version for ProtoRequest is unused and returns {@code null}.
     *
     * @param cause the exception requiring authorization
     * @param request the original request
     * @return {@code null}
     */
    @Override
    public ProtoResponse getMustAuthorizeResponse(MustAuthorizeException cause, ProtoRequest request) {
        return null;
    }

    /**
     * Constructs an OAuth2 authorization redirect response using the Salesforce configuration.
     *
     * <p>If an "authContextId" is present in the exception details, the corresponding Salesforce
     * attributes are loaded from the store; otherwise, defaults are used.</p>
     *
     * @param cause the authorization exception
     * @return a redirect response pointing to the Salesforce authorization URL
     */
    @Override
    public AuthorizationResponse getMustAuthorizeResponse(MustAuthorizeException cause) {
        String userId = (String) cause.getDetails().getFirst().getValue();
        Optional<NamedValuedField> authContextIdField = cause.getDetails().stream().filter(f -> Objects.equals(f.getName(), "authContextId")).findFirst();
        SalesforceCustomerServiceAttributes effectiveSalesforceCustomerServiceAttributes;
        String state;
        if (authContextIdField.isPresent()) {
            String authContextId = (String) authContextIdField.get().getValue();
            effectiveSalesforceCustomerServiceAttributes = attributesStore.load(authContextId);
            state = userId + "#" + authContextId;
        } else {
            effectiveSalesforceCustomerServiceAttributes = salesforceCustomerServiceAttributes;
            state = userId;
        }
        OAuth20Service salesforceSaleService = effectiveSalesforceCustomerServiceAttributes.getOauthService();
        String url = salesforceSaleService.getAuthorizationUrl(state);
        return new AuthorizationResponse(url, List.of());
    }
}
