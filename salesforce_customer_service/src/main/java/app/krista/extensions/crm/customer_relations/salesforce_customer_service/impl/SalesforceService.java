package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import com.github.scribejava.apis.salesforce.SalesforceToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final String SALESFORCE_SOBJECTS_PATH = "/services/data/v53.0/sobjects/";
    public static final String FAILED_TO_ADD_UPDATE_TO_CASE = "Failed to add update to case. ";
    private static final int Bad_Request = 400;
    private final SalesforceTokenProvider tokenProvider;
    private final SalesforceCustomerServiceAttributes customerServiceAttributes;
    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceService.class);

    @Inject
    public SalesforceService(SalesforceTokenProviderFactory providerFactory, SalesforceCustomerServiceAttributes customerServiceAttributes) {
        this.tokenProvider = providerFactory.create();
        this.customerServiceAttributes = customerServiceAttributes;
    }

    public static Map<String, String> getDifference(Object old, Object newObj) throws IllegalAccessException {
        if (old == null || newObj == null) {
            LOGGER.info("Failed to get difference. Object is null.");
        }
        Map<String, String> values = new HashMap<>();
        for (Field field : old.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value1 = field.get(old);
            Object value2 = field.get(newObj);
            if (value1 != null && value2 != null) {
                if (!Objects.equals(value1, value2) && !field.getName().toLowerCase().contains(DATE) && !field.getName().equalsIgnoreCase("id")) {
                    values.put(field.getName(), getStringValue(value2));
                }
            } else if (value1 != null) {
                values.put(field.getName(), "");
            } else if (value2 != null) {
                values.put(field.getName(), getStringValue(value2));
            }
        }
        return values;
    }

    private static String getStringValue(Object value) {
        if (value instanceof Double) {
            return String.valueOf(((Double) value).intValue());
        }
        return (String) value;
    }

    public String getUserEmail() throws IOException, ExecutionException, InterruptedException {
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = "https://login.salesforce.com/services/oauth2/userinfo";
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    public String getRecordById(String id, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || sobject == null || sobject.isEmpty()) {
            LOGGER.info("Failed to get record by id. Input is null.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + sobject + "/" + id;
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    public void deleteRecordById(String id, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || sobject == null || sobject.isBlank()) {
            LOGGER.info("Failed to delete record by id. Input is null or empty.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + sobject + "/" + id;
        final OAuthRequest request = getOauthRequest(Verb.DELETE, null, url, salesforceToken);
        getResponse(salesforceToken, request);
    }

    public void updateRecord(String id, String body, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || body == null || body.isBlank() || sobject == null || sobject.isEmpty()) {
            LOGGER.info("Failed to update record by id. Input is null.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + sobject + "/" + id + "?_HttpMethod=PATCH";
        final OAuthRequest request = getOauthRequest(Verb.POST, body, url, salesforceToken);
        getResponse(salesforceToken, request);
    }

    public String getRecordByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            LOGGER.info("Failed to get record by query. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + "/services/data/v53.0/query?q=";
        final String queryEncoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url + queryEncoded, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    public String createRecord(String body, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (body == null || body.isBlank() || sobject == null || sobject.isEmpty()) {
            LOGGER.info("Failed to create record by id. Input is null.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + sobject;
        final OAuthRequest request = getOauthRequest(Verb.POST, body, url, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    public Response getFileResponse(String downloadUrl) {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
            String url = salesforceToken.getInstanceUrl() + downloadUrl;
            final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
            return getResponseForFile(salesforceToken, request);
        } catch (IOException | ExecutionException | InterruptedException cause) {
            LOGGER.error("Failed to get file response" + cause.getMessage(), cause);
        }
        return null;
    }

    public String getFeedsByCaseId(String caseId, int numberOfPosts) {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v48.0/chatter/feeds/record/" + caseId +
                    "/feed-elements?sortOrder=ASC&sortField=CreatedDate&pageSize=" + numberOfPosts;
            final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            LOGGER.error("Failed to get case feed posts. " + cause.getMessage(), cause);
        } catch (ExecutionException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        return "";
    }


    public String createFeedPost(String postData) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v48.0/chatter/feed-elements";
            final OAuthRequest request = getOauthRequest(Verb.POST, postData, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException(FAILED_TO_ADD_UPDATE_TO_CASE + cause.getMessage(), cause);
        }
    }

    public String createFeedPostForFile(String postData) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v58.0/sobjects/ContentVersion";
            final OAuthRequest request = getOauthRequest(Verb.POST, postData, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException(FAILED_TO_ADD_UPDATE_TO_CASE + cause.getMessage(), cause);
        }
    }

    public String createFeedPostForFeedItem(String postData) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v58.0/sobjects/FeedItem";
            final OAuthRequest request = getOauthRequest(Verb.POST, postData, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException(FAILED_TO_ADD_UPDATE_TO_CASE + cause.getMessage(), cause);
        }
    }

    public String createFeedPostForFeedAttachment(String postData) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v58.0/sobjects/FeedAttachment";
            final OAuthRequest request = getOauthRequest(Verb.POST, postData, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException(FAILED_TO_ADD_UPDATE_TO_CASE + cause.getMessage(), cause);
        }
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
            if (!response.isSuccessful()) {
                LOGGER.error("Bad Request, Incorrect input is provided. The error is" + response.getBody());
            }
            if (response.getBody().contains("\"errorCode\":\"STORAGE_LIMIT_EXCEEDED\"")) {
                throw new IllegalArgumentException("Storage limit exceeded.");
            }
            return response.getBody();
        }
    }

    private Response getResponseForFile(SalesforceToken salesforceToken, OAuthRequest request) throws IOException, ExecutionException, InterruptedException {
        customerServiceAttributes.getOauthService().signRequest(salesforceToken, request);
        try (com.github.scribejava.core.model.Response response = customerServiceAttributes.getOauthService().execute(request)) {
            if (response.getCode() >= Bad_Request) {
                throw new IllegalArgumentException("Bad Request, Please enter correct required data. The error is " + response.getBody());
            }
            return response;
        } catch (IOException | InterruptedException | ExecutionException cause) {
            LOGGER.error("Exception occurred due to :" + cause.getMessage(), cause);
        }
        return null;
    }
}
