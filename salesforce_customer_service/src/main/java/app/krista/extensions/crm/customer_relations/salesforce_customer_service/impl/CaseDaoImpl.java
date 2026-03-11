/*
 * Salesforce Customer Service Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCase;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCaseHistory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.KristaMediaClient;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.CaseDao;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.SalesforceListResponse;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.Response;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.GSON;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper.*;

/**
 * Implementation of the CaseDao interface that interacts with Salesforce.
 * <p>
 * This class provides methods to create, read, update, and delete Case records
 * in Salesforce. It also handles case feed posts, file attachments, and retrieving
 * case history information.
 */
@Service
public final class CaseDaoImpl implements CaseDao {

    private static final String CASE = "Case";
    private static final String QUERY_FIELDS = " Contact.Name, ContactId, ContactEmail, ContactPhone, Account.Name,AccountId,Subject,Priority,Description,IsClosed,ClosedDate,OwnerId,CreatedById, CaseNumber,status,Origin,Type,CreatedDate,LastModifiedDate,Id ";
    private static final String ACTOR_NAME = "Actor Name";
    private static final String POST_MESSAGE = "Post Message";
    private static final String FILE = "File";
    private final SalesforceService salesforceService;
    private static final String ELEMENTS = "elements";
    private static final String FEED_ELEMENT_TYPE = "feedElementType";
    private static final String ACTOR = "actor";
    private static final String NAME = "name";
    private static final String BODY = "body";
    private static final String MESSAGE_SEGMENTS = "messageSegments";
    private static final String TYPE = "type";
    private static final String INLINE_IMAGE = "InlineImage";
    private static final String DOWNLOAD_URL = "downloadUrl";
    private static final String ITEMS = "items";
    private static final String CAPABILITIES = "capabilities";
    private static final String FILES = "files";
    private final KristaMediaClient kristaMediaClient;

    /**
     * Creates a new CaseDaoImpl with the required services.
     *
     * @param salesforceService Service for interacting with Salesforce API
     * @param kristaMediaClient Client for handling media files
     */
    @Inject
    public CaseDaoImpl(SalesforceService salesforceService, KristaMediaClient kristaMediaClient) {
        this.salesforceService = salesforceService;
        this.kristaMediaClient = kristaMediaClient;
    }

    /**
     * Gets a Case by its ID.
     *
     * @param id The ID of the Case to retrieve
     * @return The Case with the specified ID
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     * @throws RuntimeException If the Case is not found
     */
    @Override
    public Case getCaseById(String id) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank()) {
            System.err.println("Failed get case by Id. " + Constants.ID_IS_NULL_OR_EMPTY);
        }
        return getCasesByQuery("Select " + QUERY_FIELDS + "  from Case where id = '" + id + "'").stream().findFirst().orElseThrow(() -> new RuntimeException(CASE + " Not found for id " + id));
    }

    /**
     * Gets Cases by executing a SOQL query.
     *
     * @param query The SOQL query to execute
     * @return List of Cases matching the query
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public List<Case> getCasesByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            System.err.println("Failed get case by query request. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
        String res = salesforceService.getRecordByQuery(query);
        SalesforceListResponse<SFCase> oppList =
                GSON.fromJson(res, new TypeToken<SalesforceListResponse<SFCase>>() {
                }.getType());
        if (!oppList.records.isEmpty()) {
            return oppList.records.stream().map(CaseImpl::new).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    /**
     * Gets a paginated list of Cases sorted by the specified field.
     *
     * @param size Number of Cases to retrieve per page
     * @param page Page number to retrieve (1-based)
     * @param orderByField Field to sort by
     * @param orderByOperand Sort direction (ASC or DESC)
     * @return List of Cases for the specified page
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public List<Case> getCases(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException {
        return getCasesByQuery("Select " + QUERY_FIELDS + " from Case ORDER BY " + orderByField + " " + orderByOperand + " LIMIT " + size + " OFFSET " + Math.max((page - 1) * size, 0));
    }

    /**
     * Creates a new Case in Salesforce.
     *
     * @param entityDefinition Map containing the Case fields and values
     * @return The ID of the newly created Case
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public String createCase(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            System.err.println("Failed create case request. " + Constants.ENTITY_MAP_IS_NULL_OR_EMPTY);
        }
        var response = GSON.fromJson(salesforceService.createRecord(GSON.toJson(SFCase.from(entityDefinition)), CASE), Map.class);
        return String.valueOf(response.get("id"));
    }

    /**
     * Updates an existing Case in Salesforce.
     * Only changed fields will be updated.
     *
     * @param aCase The Case with updated field values
     * @throws IOException If there's a problem with the API connection
     * @throws IllegalAccessException If there's a problem accessing Case fields
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public void updateCase(Case aCase) throws IOException, IllegalAccessException, ExecutionException, InterruptedException {
        if (aCase == null || aCase.getId() == null || aCase.getId().isEmpty())
            System.err.println("Failed to update case. Case not found to update.");
        SFCase updatedCase = SFCase.from(aCase);
        SFCase oldCase = SFCase.from(getCaseById(aCase.getId()));
        Map<String, String> diff = SalesforceService.getDifference(oldCase, updatedCase);
        salesforceService.updateRecord(aCase.getId(), GSON.toJson(diff), CASE);
    }

    /**
     * Deletes a Case from Salesforce by its ID.
     *
     * @param caseId The ID of the Case to delete
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public void deleteCaseById(String caseId) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(caseId, CASE);
    }

    /**
     * Gets feed items (posts and comments) for a Case.
     *
     * @param caseId The ID of the Case
     * @param numberOfPosts Maximum number of posts to retrieve
     * @return List of feed items as maps containing actor name, message, and files
     * @throws IOException If there's a problem with the API connection
     */
    @Override
    public List<Map<String, Object>> getFeedItemsForCaseId(String caseId, Double numberOfPosts) throws IOException {
        String response = salesforceService.getFeedsByCaseId(caseId, numberOfPosts);
        System.out.println("%Response :  " + response);
        if (response.contains("\"errorCode\":\"NOT_FOUND\"")) {
            return List.of();
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response);
        JsonNode elementsNode = rootNode.get(ELEMENTS);

        List<Map<String, Object>> feedPosts = new ArrayList<>();
        for (JsonNode elementNode : elementsNode) {
            Map<String, Object> post = parseElementToPost(elementNode);
            feedPosts.add(post);
        }
        return feedPosts;
    }

    /**
     * Parses a feed element node into a post map.
     *
     * @param elementNode The JSON node representing a feed element
     * @return Map containing actor name, message, and files
     * @throws IOException If there's a problem processing files
     */
    private Map<String, Object> parseElementToPost(JsonNode elementNode) throws IOException {
        String feedElementType = elementNode.get(FEED_ELEMENT_TYPE).asText();
        if (!"FeedItem".equalsIgnoreCase(feedElementType)) {
            return Collections.emptyMap();
        }

        Map<String, Object> post = new HashMap<>();
        post.put(ACTOR_NAME, getActorName(elementNode));
        post.put(POST_MESSAGE, getPostMessage(elementNode));
        post.put(FILE, getFiles(elementNode));
        return post;
    }

    /**
     * Gets the actor (author) name from a feed element.
     *
     * @param elementNode The JSON node representing a feed element
     * @return The name of the actor who created the post
     */
    private String getActorName(JsonNode elementNode) {
        JsonNode actorNode = elementNode.get(ACTOR);
        return actorNode != null ? actorNode.get(NAME).asText() : null;
    }

    /**
     * Gets the message text from a feed element.
     *
     * @param elementNode The JSON node representing a feed element
     * @return The text content of the post
     */
    private String getPostMessage(JsonNode elementNode) {
        JsonNode bodyNode = elementNode.get(BODY);
        if (bodyNode != null && bodyNode.has(MESSAGE_SEGMENTS)) {
            StringBuilder textBuilder = new StringBuilder();
            for (JsonNode segmentNode : bodyNode.get(MESSAGE_SEGMENTS)) {
                if ("Text".equals(segmentNode.get(TYPE).asText())) {
                    textBuilder.append(segmentNode.get("text").asText());
                }
            }
            return textBuilder.toString();
        }
        return null;
    }

    /**
     * Gets files attached to a feed element.
     *
     * @param elementNode The JSON node representing a feed element
     * @return List of File objects representing attached files
     * @throws IOException If there's a problem downloading files
     */
    private List<app.krista.model.base.File> getFiles(JsonNode elementNode) throws IOException {
        List<app.krista.model.base.File> files = new ArrayList<>();

        JsonNode bodyNode = elementNode.get(BODY);
        if (bodyNode != null && bodyNode.has(MESSAGE_SEGMENTS)) {
            for (JsonNode segmentNode : bodyNode.get(MESSAGE_SEGMENTS)) {
                if (isInlineImage(segmentNode)) {
                    files.add(downloadFile(segmentNode.get("url").asText()));
                }
            }
        }

        JsonNode filesNode = elementNode.get(CAPABILITIES).get(FILES);
        if (filesNode != null) {
            for (JsonNode fileNode : filesNode.get(ITEMS)) {
                files.add(downloadFile(fileNode.get(DOWNLOAD_URL).asText()));
            }
        }


        return files;
    }

    /**
     * Checks if a message segment is an inline image.
     *
     * @param segmentNode The JSON node representing a message segment
     * @return True if the segment is an inline image, false otherwise
     */
    private boolean isInlineImage(JsonNode segmentNode) {
        return segmentNode.has(TYPE) && segmentNode.get(TYPE).asText().equals(INLINE_IMAGE);
    }

    /**
     * Downloads a file from Salesforce.
     *
     * @param downloadUrl The URL to download the file from
     * @return A File object representing the downloaded file
     * @throws IOException If there's a problem downloading the file
     */
    private app.krista.model.base.File downloadFile(String downloadUrl) throws IOException {

        Response fileResponse = salesforceService.getFileResponse(downloadUrl);

        String contentDisposition = fileResponse.getHeader("Content-Disposition");
        Pattern pattern = Pattern.compile("filename=\"(.*)\"");
        Matcher matcher = pattern.matcher(contentDisposition);
        String fileName = matcher.find() ? matcher.group(1) : "unknown";

        File localFile = saveToLocalTempDir(fileResponse.getStream(), fileName);

        return kristaMediaClient.toKristaFile(localFile);

    }

    /**
     * Saves a file to a local temporary directory.
     *
     * @param inputStream The input stream containing the file data
     * @param filePath The path where the file should be saved
     * @return The saved File object
     * @throws IOException If there's a problem saving the file
     */
    private File saveToLocalTempDir(InputStream inputStream, String filePath) throws IOException {
        File localFile = new File(filePath);
        Files.copy(inputStream, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return localFile;
    }

    /**
     * Adds a feed update (post) to a Case, optionally with a file attachment.
     *
     * @param caseId The ID of the Case to add the update to
     * @param updateMessage The message text for the update
     * @param base64EncodedString Base64-encoded file content (or null if no file)
     * @param fileName Name of the file (or null if no file)
     * @return True if the update was successful, false otherwise
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    public Boolean addFeedUpdateToCaseById(String caseId, String updateMessage, String base64EncodedString, String fileName) throws ExecutionException, InterruptedException {
        if (base64EncodedString == null && fileName == null) {
            String postData = StringHelper.createFeedPostBody(caseId, updateMessage);
            String response = salesforceService.createFeedPost(postData);
            if (response.contains("\"errorCode\":\"INVALID_ID_FIELD\"")) {
                return Boolean.FALSE;
            }
            final JsonObject responseObject = GSON.fromJson(response, JsonObject.class);
            if (responseObject.get("id") != null) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
        return processFeedAndFileOperations(caseId, updateMessage, base64EncodedString, fileName);

    }

    /**
     * Processes feed and file operations for adding a post with a file attachment.
     *
     * @param caseId The ID of the Case
     * @param updateMessage The message text for the update
     * @param base64EncodedString Base64-encoded file content
     * @param fileName Name of the file
     * @return True if the operations were successful, false otherwise
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    private Boolean processFeedAndFileOperations(String caseId, String updateMessage, String base64EncodedString, String fileName) throws ExecutionException, InterruptedException {
        String bodyForFeedItem = createFeedPostBodyForFeedItem(updateMessage, caseId);
        String responseForFeedItem = salesforceService.createFeedPostForFeedItem(bodyForFeedItem);
        if (responseForFeedItem.contains("\"errorCode\":\"MALFORMED_ID\"")) {
            return Boolean.FALSE;
        }
        String requestBody = createFeedPostBodyForFile(base64EncodedString, fileName);
        String responseForFile = salesforceService.createFeedPostForFile(requestBody);

        final JsonObject responseObjectForFile = GSON.fromJson(responseForFile, JsonObject.class);
        JsonElement id = responseObjectForFile.get("id");

        final JsonObject responseObjectForFeedItem = GSON.fromJson(responseForFeedItem, JsonObject.class);
        String feedItemId = String.valueOf(responseObjectForFeedItem.get("id"));

        String bodyForFeedAttachment = createFeedPostBodyForFeedAttachment(feedItemId, String.valueOf(id), fileName);
        String responseForFeedAttachment = salesforceService.createFeedPostForFeedAttachment(bodyForFeedAttachment);
        final JsonObject responseObjectForFeedAttachment = GSON.fromJson(responseForFeedAttachment, JsonObject.class);

        if (responseObjectForFeedAttachment.get("id") != null) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * Gets the fields that were updated in a Case.
     *
     * @param query The SOQL query to retrieve case history
     * @return Map of field names to their new values
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    public Map<String, Object> getCaseUpdatedFields(String query) throws IOException, ExecutionException, InterruptedException {
        String response = salesforceService.getRecordByQuery(query);
        SFCaseHistory sfCaseHistory = GSON.fromJson(response, SFCaseHistory.class);
        return sfCaseHistory.toMap();
    }

    /**
     * Converts a file to a Base64-encoded string.
     *
     * @param file The file to convert
     * @return Base64-encoded string representation of the file
     * @throws IOException If there's a problem reading the file
     */
    public String getBase64EncodedString(File file) throws IOException {
        byte[] fileContent = FileUtils.readFileToByteArray(file);
        return Base64.getEncoder().encodeToString(fileContent);
    }
}