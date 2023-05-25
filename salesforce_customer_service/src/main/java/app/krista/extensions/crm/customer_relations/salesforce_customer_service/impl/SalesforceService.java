package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import com.github.scribejava.apis.salesforce.SalesforceToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
public class SalesforceService {

    public static final String DATE = "date";
    private final SalesforceTokenProvider tokenProvider;
    private final SalesforceCustomerServiceAttributes customerServiceAttributes;
    @Inject
    public SalesforceService(SalesforceTokenProviderFactory providerFactory, SalesforceCustomerServiceAttributes customerServiceAttributes) {
        this.tokenProvider = providerFactory.create();
        this.customerServiceAttributes = customerServiceAttributes;
    }

    public static Map<String, String> getDifference(Object old, Object newObj) throws IllegalAccessException {
        if (old == null || newObj == null) {
            throw new IllegalArgumentException("Failed to get difference. Object is null.");
        }
        Map<String, String> values = new HashMap<>();
        for (Field field : old.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value1 = field.get(old);
            Object value2 = field.get(newObj);
            if (value1 != null && value2 != null) {
                if (!Objects.equals(value1, value2) && !field.getName().toLowerCase().contains(DATE) && !field.getName().equalsIgnoreCase("id")) {
                    values.put(field.getName(), (String) value2);
                }
            } else if (value1 != null) {
                values.put(field.getName(), null);
            } else if (value2 != null) {
                values.put(field.getName(), (String) value2);
            }
        }
        return values;
    }

    public String getUserEmail() throws IOException, ExecutionException, InterruptedException {
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = "https://login.salesforce.com/services/oauth2/userinfo";
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    public String getRecordById(String id, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || sobject == null || sobject.isEmpty()) {
            throw new IllegalArgumentException("Failed to get record by id. Input is null.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + "/services/data/v53.0/sobjects/" + sobject + "/" + id;
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    public void deleteRecordById(String id, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || sobject == null || sobject.isBlank()) {
            throw new IllegalArgumentException("Failed to delete record by id. Input is null or empty.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + "/services/data/v53.0/sobjects/" + sobject + "/" + id;
        final OAuthRequest request = getOauthRequest(Verb.DELETE, null, url, salesforceToken);
        getResponse(salesforceToken, request);
    }

    public void updateRecord(String id, String body, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || body == null || body.isBlank() || sobject == null || sobject.isEmpty()) {
            throw new IllegalArgumentException("Failed to update record by id. Input is null.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + "/services/data/v53.0/sobjects/" + sobject + "/" + id + "?_HttpMethod=PATCH";
        final OAuthRequest request = getOauthRequest(Verb.POST, body, url, salesforceToken);
        getResponse(salesforceToken, request);
    }

    public String getRecordByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Failed to get record by query. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + "/services/data/v53.0/query?q=";
        final String queryEncoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url + queryEncoded, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    public String createRecord(String body, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (body == null || body.isBlank() || sobject == null || sobject.isEmpty()) {
            throw new IllegalArgumentException("Failed to create record by id. Input is null.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        System.out.println("Sobject: " + sobject);
        String url = salesforceToken.getInstanceUrl() + "/services/data/v53.0/sobjects/" + sobject;
        final OAuthRequest request = getOauthRequest(Verb.POST, body, url, salesforceToken);
        System.out.println("Payload Body: " + body);
        return getResponse(salesforceToken, request);
    }

    private OAuthRequest getOauthRequest(Verb verb, String body, String url, SalesforceToken salesforceToken) {
        final OAuthRequest request = new OAuthRequest(verb, url);
        if (body != null) {
            request.setPayload(body);
        }
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", "Bearer " + salesforceToken.getAccessToken());
        return request;
    }

    private String getResponse(SalesforceToken salesforceToken, OAuthRequest request) throws IOException, ExecutionException, InterruptedException {
        customerServiceAttributes.getOauthService().signRequest(salesforceToken, request);
        try (com.github.scribejava.core.model.Response response = customerServiceAttributes.getOauthService().execute(request)) {
            System.out.println("Response: " + response.toString());
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException("Bad Request, Incorrect input is provided. The error is " + response.getBody());
            }
            return response.getBody();
        }
    }

    private static class Email {
        String email;
    }
}
