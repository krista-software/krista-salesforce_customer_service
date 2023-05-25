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

public class SalesforceCustomerServiceAuthenticator implements RequestAuthenticator {

    private final SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes;
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    @Inject
    public SalesforceCustomerServiceAuthenticator(SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes,
                                                  SalesforceCustomerServiceAttributesStore attributesStore) {
        this.salesforceCustomerServiceAttributes = salesforceCustomerServiceAttributes;
        this.attributesStore = attributesStore;
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public Set<String> getSupportedProtocols() {
        return Set.of();
    }

    @Override
    public String getAuthenticatedAccountId(ProtoRequest protoRequest) {
        try {
            HttpRequest httpRequest = (HttpRequest) protoRequest;
            if (httpRequest.getUri().getPath().equals("/salesforcecustomerservice/callback")) {
                httpRequest.bufferBody();
                MultivaluedMap<String, String> queryParameters = httpRequest.getQueryParameters();
                String state = String.valueOf(queryParameters.get("state").get(0));
                String[] parts = state.split("#");
                return parts[0];
            } else {
                return null;
            }
        } catch (RuntimeException | IOException cause) {
            return null;
        }
    }

    @Override
    public boolean setServiceAuthorization(String s) {
        return false;
    }

    @Override
    public Map<String, NamedField> getAttributeFields() {
        return Map.of();
    }

    @Override
    public ProtoResponse getMustAuthenticateResponse(MustAuthenticateException cause, ProtoRequest request) {
        return null;
    }

    @Override
    public AuthorizationResponse getMustAuthenticateResponse(MustAuthenticateException cause) {
        return null;
    }

    @Override
    public ProtoResponse getMustAuthorizeResponse(MustAuthorizeException cause, ProtoRequest request) {
        return null;
    }

    @Override
    public AuthorizationResponse getMustAuthorizeResponse(MustAuthorizeException cause) {
        try {
            String userId = (String) cause.getDetails().get(0).getValue();
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
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to get authorize response", ioException);
        }
    }
}
