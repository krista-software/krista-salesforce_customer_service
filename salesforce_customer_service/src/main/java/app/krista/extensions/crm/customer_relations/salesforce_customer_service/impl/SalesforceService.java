package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import com.github.scribejava.apis.salesforce.SalesforceToken;
import com.github.scribejava.core.httpclient.multipart.MultipartPayload;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    private final SalesforceTokenProvider tokenProvider;
    private final SalesforceCustomerServiceAttributes customerServiceAttributes;

    @Inject
    public SalesforceService(SalesforceTokenProviderFactory providerFactory, SalesforceCustomerServiceAttributes customerServiceAttributes) {
        this.tokenProvider = providerFactory.create();
        this.customerServiceAttributes = customerServiceAttributes;
    }

    public static Map<String, String> getDifference(Object old, Object newObj) throws IllegalAccessException {
        if (old == null || newObj == null) {
            System.err.println("Failed to get difference. Object is null.");
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
            System.err.println("Failed to get record by id. Input is null.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + sobject + "/" + id;
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    public void deleteRecordById(String id, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || sobject == null || sobject.isBlank()) {
            System.err.println("Failed to delete record by id. Input is null or empty.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + sobject + "/" + id;
        final OAuthRequest request = getOauthRequest(Verb.DELETE, null, url, salesforceToken);
        getResponse(salesforceToken, request);
    }

    public void updateRecord(String id, String body, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank() || body == null || body.isBlank() || sobject == null || sobject.isEmpty()) {
            System.err.println("Failed to update record by id. Input is null.");
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + SALESFORCE_SOBJECTS_PATH + sobject + "/" + id + "?_HttpMethod=PATCH";
        final OAuthRequest request = getOauthRequest(Verb.POST, body, url, salesforceToken);
        getResponse(salesforceToken, request);
    }

    public String getRecordByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            System.err.println("Failed to get record by query. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + "/services/data/v53.0/query?q=";
        final String queryEncoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url + queryEncoded, salesforceToken);
        return getResponse(salesforceToken, request);
    }

    public String createRecord(String body, String sobject) throws IOException, ExecutionException, InterruptedException {
        if (body == null || body.isBlank() || sobject == null || sobject.isEmpty()) {
            System.err.println("Failed to create record by id. Input is null.");
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
            final OAuthRequest request = getOauthRequest(Verb.GET, null,url, salesforceToken);
            return getResponse1(salesforceToken, request);
        } catch (IOException | ExecutionException | InterruptedException cause) {
            //LOGGER.error("Failed to get file response" + cause.getMessage(), cause);
        }
        return null;
    }

//    public String getFeedsByCaseId(String caseId) throws ExecutionException, InterruptedException {
//        try {
//            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
//            String url = salesforceToken.getInstanceUrl() + "/services/data/v48.0/chatter/feeds/record/" +
//                    caseId + "/feed-elements";
//            final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
//            return getResponse(salesforceToken, request);
//        } catch (IOException cause) {
//            throw new IllegalArgumentException("Failed to get case feed posts. " + cause.getMessage(), cause);
//        }
//    }
public String getFeedsByCaseId(String caseId, int numberOfPosts) {
    try {
        SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
        String url = salesforceToken.getInstanceUrl() + "/services/data/v48.0/chatter/feeds/record/" + caseId +
                "/feed-elements?sortOrder=ASC&sortField=CreatedDate&pageSize=" + numberOfPosts;
        final OAuthRequest request = getOauthRequest(Verb.GET, null, url, salesforceToken);
        return getResponse(salesforceToken, request);
    } catch (IOException cause) {
       // LOGGER.error("Failed to get opportunity feed posts. " + cause.getMessage(), cause);
    } catch (ExecutionException e) {
        throw new RuntimeException(e);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
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
            throw new IllegalArgumentException("Failed to add update to case. " + cause.getMessage(), cause);
        }
    }
    public String createFeedPostForFile(String postData) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v58.0/sobjects/Attachment";
            final OAuthRequest request = getOauthRequest(Verb.POST, postData, url, salesforceToken);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException("Failed to add update to case. " + cause.getMessage(), cause);
        }
    }
    public String createFeedPost1(File javaFile) throws ExecutionException, InterruptedException {
        try {
            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
            String url = salesforceToken.getInstanceUrl() + "/services/data/v60.0/connect/files/users/me";

            final OAuthRequest request = getOauthRequest1(Verb.POST, url, salesforceToken,javaFile);
            return getResponse(salesforceToken, request);
        } catch (IOException cause) {
            throw new IllegalArgumentException("Failed to add update to case. " + cause.getMessage(), cause);
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
    private OAuthRequest getOauthRequest1(Verb verb, String url, SalesforceToken salesforceToken, File javaFile) throws FileNotFoundException {
        final OAuthRequest request = new OAuthRequest(verb, url);

        //1
//        request.addHeader("Authorization", "Bearer " + salesforceToken.getAccessToken());
//        request.addHeader("User-Agent", "Jakarta Commons-HttpClient/3.0.1");
//        FileInputStream fileInputStream = new FileInputStream(javaFile);
//        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//        builder.addBinaryBody("fileData", fileInputStream, ContentType.APPLICATION_OCTET_STREAM, javaFile.getName());
//        HttpEntity multipartEntity = builder.build();
//        request.setEntity(multipartEntity);

        //2
//        request.target(url)
//                .request(MediaType.MULTIPART_FORM_DATA)
//                .header("Authorization", "Bearer " + salesforceToken.getAccessToken())
//                .post(Entity.entity(javaFile, MediaType.APPLICATION_OCTET_STREAM));
//        request.addHeader("Authorization", "Bearer " + salesforceToken.getAccessToken());
//        request.addHeader("Content-Type", MediaType.MULTIPART_FORM_DATA);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("fileData", javaFile, ContentType.APPLICATION_OCTET_STREAM, javaFile.getName());

        request.setPayload(builder.build().toString());

        return request;
//3
//        request.setHeader("Authorization", accessToken);
//
//        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//        builder.addBinaryBody("fileData", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
//        HttpEntity multipartEntity = builder.build();
//        httpPost.setEntity(multipartEntity);

//        request.addHeader("Authorization", salesforceToken.getAccessToken());
//        MultipartPayload multipartPayload = new MultipartPayload();
//        multipartPayload.addFilePart("fileData", javaFile);
//
//        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//        builder.addBinaryBody("fileData", javaFile, ContentType.APPLICATION_OCTET_STREAM, javaFile.getName());
//        HttpEntity multipartEntity = builder.build();
//        request.setMultipartPayload((MultipartPayload) multipartEntity);

//        request.setPayload(String.valueOf((Entity.entity(javaFile, MediaType.APPLICATION_OCTET_STREAM))));
//
//        request.addHeader("Content-Type", MediaType.MULTIPART_FORM_DATA);
//        //MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//        request.addHeader("Authorization", "Bearer " + salesforceToken.getAccessToken());
//        request1.request(MediaType.MULTIPART_FORM_DATA)
//                .header("Authorization", salesforceToken.getAccessToken())
//                .post(Entity.entity(javaFile, MediaType.APPLICATION_OCTET_STREAM));


        //return request;
    }

    private String getResponse(SalesforceToken salesforceToken, OAuthRequest request) throws IOException, ExecutionException, InterruptedException {
        customerServiceAttributes.getOauthService().signRequest(salesforceToken, request);
        try (com.github.scribejava.core.model.Response response = customerServiceAttributes.getOauthService().execute(request)) {
            if (!response.isSuccessful()) {
                System.err.println("Bad Request, Incorrect input is provided. The error is" + response.getBody());
            }
            if (response.getBody().contains("\"errorCode\":\"STORAGE_LIMIT_EXCEEDED\"")) {
                throw new IllegalArgumentException("Storage limit exceeded.");
            }
            return response.getBody();
        }
    }
//    public Response getFileResponse(String downloadUrl) {
//        try {
//            SalesforceToken salesforceToken = tokenProvider.getSalesforceToken();
//            String url = salesforceToken.getInstanceUrl() + downloadUrl;
//            final OAuthRequest request = getOauthRequest(Verb.GET, null, url,salesforceToken);
//            return getResponse1(salesforceToken, request);
//        } catch (IOException | ExecutionException | InterruptedException cause) {
//           // LOGGER.error("Failed to get file response" + cause.getMessage(), cause);
//        }
//        return null;
//    }
//
    private Response getResponse1(SalesforceToken salesforceToken, OAuthRequest request) throws IOException, ExecutionException, InterruptedException {
        customerServiceAttributes.getOauthService().signRequest(salesforceToken, request);
        try (com.github.scribejava.core.model.Response response = customerServiceAttributes.getOauthService().execute(request)) {
            if (response.getCode() >= 400) {
                throw new IllegalArgumentException("Bad Request, Please enter correct required data. The error is " + response.getBody());
            }
            return response;
        } catch (IOException | InterruptedException | ExecutionException cause) {
           // LOGGER.error("Failed to get record " + cause.getMessage(), cause);
        }
        return null;
    }
}
