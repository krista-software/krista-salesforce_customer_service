package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Service class for interacting with Salesforce using OAuth authentication.
 * This class provides utility methods for performing CRUD operations on Salesforce sObjects,
 * posting feed elements (including attachments), and checking object metadata.
 * It communicates with Salesforce REST APIs using ScribeJava for OAuth authentication.
 * This class is intended to be used as a reusable Salesforce service component.
 */
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

    /**
     * Computes field-level differences between two objects of the same type.
     * Ignores fields containing "date" in the name and the "id" field.
     *
     * @param old the original object
     * @param newObj the updated object
     * @return a map containing changed field names and their new values
     * @throws IllegalAccessException if field access fails
     */
    public static Map<String, String> getDifference(Object old, Object newObj) throws IllegalAccessException {
        if (old == null || newObj == null) {
            LOGGER.info("Failed to get difference. Object is null.");
        }
        Map<String, String> values = new HashMap<>();
        assert old != null;
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

    /**
     * Converts a field value to a String, converting Doubles to integer strings.
     *
     * @param value the field value
     * @return the string representation
     */
    private static String getStringValue(Object value) {
        if (value instanceof Double) {
            return String.valueOf(((Double) value).intValue());
        }
        return (String) value;
    }

    /**
     * Retrieves a Salesforce record by ID.
     *
     * @param id the Salesforce record ID
     * @param subject the Salesforce object type (e.g., "Case", "Account")
     * @return the JSON response as a string
     */
    public String getRecordById(String id, String subject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || subject == null || subject.isEmpty()) {
            String errorMsg = "Cannot retrieve Salesforce record: ";
            if (id == null || id.isBlank()) {
                errorMsg += "Record ID is required. ";
            }
            if (subject == null || subject.isEmpty()) {
                errorMsg += "Salesforce object type (e.g., 'Case', 'Account') is required. ";
            }
            errorMsg += "Please provide valid parameters and try again.";
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
        String url = salesforceToken.getInstanceUrl() + Constants.SALESFORCE_SOBJECTS_PATH + subject + "/" + id;
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    /**
     * Deletes a Salesforce record by ID.
     *
     * @param id the Salesforce record ID
     * @param sobject the Salesforce object type
     */
    public void deleteRecordById(String id, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || sobject == null || sobject.isBlank()) {
            String errorMsg = "Cannot delete Salesforce record: ";
            if (id == null || id.isBlank()) {
                errorMsg += "Record ID is required. ";
            }
            if (sobject == null || sobject.isBlank()) {
                errorMsg += "Salesforce object type (e.g., 'Case', 'Account') is required. ";
            }
            errorMsg += "Please provide valid parameters and try again.";
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
        String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + sobject + "/" + id;
        final OAuthRequest request = getOauthRequest(Verb.DELETE, null, url, salesforceToken);
        getResponse(salesforceToken, request);
    }


    /**
     * Updates a Salesforce record using the provided body JSON.
     *
     * @param id the Salesforce record ID
     * @param body the update body in JSON format
     * @param sobject the Salesforce object type
     */
    public void updateRecord(String id, String body, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || body == null || body.isBlank() || sobject == null || sobject.isEmpty()) {
            String errorMsg = "Cannot update Salesforce record: ";
            if (id == null || id.isBlank()) {
                errorMsg += "Record ID is required. ";
            }
            if (body == null || body.isBlank()) {
                errorMsg += "Update data (JSON body) is required. ";
            }
            if (sobject == null || sobject.isEmpty()) {
                errorMsg += "Salesforce object type (e.g., 'Case', 'Account') is required. ";
            }
            errorMsg += "Please provide valid parameters and try again.";
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
        String url = salesforceToken.getInstanceUrl() + Constants.SALESFORCE_SOBJECTS_PATH + sobject + "/" + id + Constants.HTTP_METHOD_PATCH;
        final OAuthRequest request = getOauthRequest(Verb.POST, body, url, salesforceToken);
        getResponse(salesforceToken, request);
    }

    /**
     * Executes a SOQL query against Salesforce and returns the results.
     *
     * @param query the SOQL query string
     * @return the response body from Salesforce
     */
    public String getRecordByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            String errorMsg = "Cannot execute Salesforce query: SOQL query string is required. " +
                    "Please provide a valid SOQL query (e.g., 'SELECT Id, Name FROM Account LIMIT 10') and try again.";
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
        String url = salesforceToken.getInstanceUrl() + Constants.SALESFORCE_QUERY_PATH;
        assert query != null;
        final String queryEncoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url + queryEncoded, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    /**
     * Creates a new record in Salesforce.
     *
     * @param body the record body in JSON format
     * @param sobject the Salesforce object type
     * @return the response body from Salesforce
     */
    public String createRecord(String body, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (body == null || body.isBlank() || sobject == null || sobject.isEmpty()) {
            String errorMsg = "Cannot create Salesforce record: ";
            if (body == null || body.isBlank()) {
                errorMsg += "Record data (JSON body) is required. ";
            }
            if (sobject == null || sobject.isEmpty()) {
                errorMsg += "Salesforce object type (e.g., 'Case', 'Account') is required. ";
            }
            errorMsg += "Please provide valid parameters and try again.";
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
        String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + sobject;
        final OAuthRequest request = getOauthRequest(Verb.POST, body, url, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    /**
     * Downloads a file from Salesforce using the given download URL.
     *
     * @param downloadUrl the relative Salesforce download URL
     * @return the HTTP response containing file stream
     */
    public Response getFileResponse(String downloadUrl) {
        SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
        String url = salesforceToken.getInstanceUrl() + downloadUrl;
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
        return getResponseForFile(salesforceToken, request);
    }

    /**
     * Retrieves Chatter feed elements for a given Case ID.
     *
     * @param caseId the Case ID
     * @param numberOfPosts the number of posts to retrieve
     * @return the response body as a string
     */
    public String getFeedsByCaseId(String caseId, Double numberOfPosts) {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v48.0/chatter/feeds/record/" + caseId +
                    "/feed-elements?sortOrder=ASC&sortField=CreatedDate&pageSize=" + numberOfPosts.intValue();
            final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            LOGGER.error("Failed to get case feed posts. {}", cause.getMessage(), cause);
        } catch (ExecutionException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        return "";
    }


    /**
     * Creates a Chatter feed post in Salesforce.
     *
     * @param postData the post body in JSON format
     * @return the response from Salesforce
     */
    public String createFeedPost(String postData) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v48.0/chatter/feed-elements";
            final OAuthRequest request = getOauthRequest(Verb.POST, postData, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException(FAILED_TO_ADD_UPDATE_TO_CASE + cause.getMessage(), cause);
        }
    }


    /**
     * Creates a file-based Chatter post (ContentVersion) in Salesforce.
     *
     * @param postData the post body in JSON format
     * @return the response from Salesforce
     */
    public String createFeedPostForFile(String postData) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v58.0/sobjects/ContentVersion";
            final OAuthRequest request = getOauthRequest(Verb.POST, postData, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException(FAILED_TO_ADD_UPDATE_TO_CASE + cause.getMessage(), cause);
        }
    }


    /**
     * Creates a FeedItem post in Salesforce.
     *
     * @param postData the post body in JSON format
     * @return the response from Salesforce
     */
    public String createFeedPostForFeedItem(String postData) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v58.0/sobjects/FeedItem";
            final OAuthRequest request = getOauthRequest(Verb.POST, postData, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException(FAILED_TO_ADD_UPDATE_TO_CASE + cause.getMessage(), cause);
        }
    }

    /**
     * Creates a FeedAttachment in Salesforce.
     *
     * @param postData the post body in JSON format
     * @return the response from Salesforce
     */
    public String createFeedPostForFeedAttachment(String postData) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v58.0/sobjects/FeedAttachment";
            final OAuthRequest request = getOauthRequest(Verb.POST, postData, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException(FAILED_TO_ADD_UPDATE_TO_CASE + cause.getMessage(), cause);
        }
    }

    /**
     * Prepares an OAuthRequest for the given HTTP verb, body, URL, and token.
     *
     * @param verb the HTTP verb
     * @param body the request body (optional)
     * @param url the target URL
     * @param salesforceToken the authenticated token
     * @return an OAuthRequest object
     */
    private OAuthRequest getOauthRequest(Verb verb, String body, String url, SalesforceToken salesforceToken) {
        final OAuthRequest request = new OAuthRequest(verb, url);
        if (body != null) {
            request.setPayload(body);
        }
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", "Bearer " + salesforceToken.getAccessToken());
        return request;
    }

    /**
     * Executes an authenticated request and returns the response body as a string.
     *
     * @param salesforceToken the Salesforce token
     * @param request the OAuth request
     * @return the response body
     */
    private String getResponse(SalesforceToken salesforceToken, OAuthRequest request) throws IOException, ExecutionException, InterruptedException {
        customerServiceAttributes.getOauthService().signRequest(salesforceToken, request);
        try (com.github.scribejava.core.model.Response response = customerServiceAttributes.getOauthService().execute(request)) {
            if (!response.isSuccessful()) {
                LOGGER.error("Bad Request, Incorrect input is provided. The error is{}", response.getBody());
            }
            if (response.getBody().contains("\"errorCode\":\"STORAGE_LIMIT_EXCEEDED\"")) {
                throw new IllegalArgumentException("Salesforce storage limit exceeded. " +
                    "Your organization has reached its data storage limit. " +
                    "Please contact your Salesforce administrator to increase storage capacity or free up space by deleting unnecessary records.");
            }
            return response.getBody();
        }
    }

    /**
     * Executes a file-related OAuth request and returns the raw response.
     *
     * @param salesforceToken the Salesforce token
     * @param request the OAuth request
     * @return the response object or null in case of error
     */
    private Response getResponseForFile(SalesforceToken salesforceToken, OAuthRequest request) {
        customerServiceAttributes.getOauthService().signRequest(salesforceToken, request);
        try (com.github.scribejava.core.model.Response response = customerServiceAttributes.getOauthService().execute(request)) {
            if (response.getCode() >= Bad_Request) {
                throw new IllegalArgumentException("Salesforce API request failed (HTTP " + response.getCode() + "): " +
                        "The request was rejected by Salesforce. This typically indicates invalid data, missing required fields, " +
                        "or insufficient permissions. Error details: " + response.getBody() +
                        ". Please verify your data and permissions, then try again.");
            }
            return response;
        } catch (IOException | InterruptedException | ExecutionException cause) {
            LOGGER.error("Exception occurred due to :{}", cause.getMessage(), cause);
        }
        return null;
    }

    /**
     * Checks whether a given field exists on a Salesforce object type.
     *
     * @param objectType the object type (e.g., "Account", "Contact")
     * @param fieldName the API name of the field
     * @return true if the field exists; false otherwise
     */
    public boolean checkFieldExistsInSalesforce(String objectType, String fieldName) {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceTokenForUser();
            String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + objectType + "/describe";

            final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
            String responseBody = getResponse(salesforceToken, request);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);
            JsonNode fieldsNode = rootNode.get("fields");

            if (fieldsNode != null && fieldsNode.isArray()) {
                for (JsonNode fieldNode : fieldsNode) {
                    String apiName = fieldNode.get("name").asText();
                    if (fieldName.equalsIgnoreCase(apiName)) {
                        return true;
                    }
                }
            }
        } catch (Exception cause) {
            LOGGER.error("Error checking if field exists on Salesforce object. {}", cause.getMessage(), cause);
        }
        return false;
    }


}
