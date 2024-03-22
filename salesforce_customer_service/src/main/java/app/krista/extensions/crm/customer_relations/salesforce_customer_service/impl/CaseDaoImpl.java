package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCase;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCaseHistory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.CaseDao;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.SalesforceListResponse;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper;
import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.GSON;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper.createFeedPostBodyForFile;

@Service
public final class CaseDaoImpl implements CaseDao {

    private static final String CASE = "Case";
    private static final String QUERY_FIELDS = " Contact.Name, ContactId, ContactEmail, ContactPhone, Account.Name,AccountId,Subject,Priority,Description,IsClosed,ClosedDate,OwnerId,CreatedById, CaseNumber,status,Origin,Type,CreatedDate,LastModifiedDate,Id ";
    private final SalesforceService salesforceService;
    private final FileRepository fileRepository;

    @Inject
    public CaseDaoImpl(SalesforceService salesforceService, FileRepository fileRepository) {
        this.salesforceService = salesforceService;
        this.fileRepository = fileRepository;
    }

    @Override
    public Case getCaseById(String id) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank()) {
            System.err.println("Failed get case by Id. " + Constants.ID_IS_NULL_OR_EMPTY);
        }
        return getCasesByQuery("Select " + QUERY_FIELDS + "  from Case where id = '" + id + "'").stream().findFirst().orElseThrow(() -> new RuntimeException(CASE + " Not found for id " + id));
    }

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

    @Override
    public List<Case> getCases(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException {
        return getCasesByQuery("Select " + QUERY_FIELDS + " from Case ORDER BY " + orderByField + " " + orderByOperand + " LIMIT " + size + " OFFSET " + Math.max((page - 1) * size, 0));
    }

    @Override
    public String createCase(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            System.err.println("Failed create case request. " + Constants.ENTITY_MAP_IS_NULL_OR_EMPTY);
        }
        var response = GSON.fromJson(salesforceService.createRecord(GSON.toJson(SFCase.from(entityDefinition)), CASE), Map.class);
        return String.valueOf(response.get("id"));
    }

    @Override
    public void updateCase(Case aCase) throws IOException, IllegalAccessException, ExecutionException, InterruptedException {
        if (aCase == null || aCase.getId() == null || aCase.getId().isEmpty())
            System.err.println("Failed to update case. Case not found to update.");
        SFCase updatedCase = SFCase.from(aCase);
        SFCase oldCase = SFCase.from(getCaseById(aCase.getId()));
        Map<String, String> diff = SalesforceService.getDifference(oldCase, updatedCase);
        salesforceService.updateRecord(aCase.getId(), GSON.toJson(diff), CASE);
    }

    @Override
    public void deleteCaseById(String caseId) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(caseId, CASE);
    }

    @Override
    public List<Map<String, Object>> getFeedItemsForCaseId(String caseId) throws ExecutionException, InterruptedException, JsonProcessingException {
        String response = salesforceService.getFeedsByCaseId(caseId);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response);
        JsonNode elementsNode = rootNode.get("elements");

        List<Map<String, Object>> feedPosts = new ArrayList<>();
        Map<String, Object> post = new LinkedHashMap<>(2);
        for (JsonNode elementNode : elementsNode) {
            String feedElementType = elementNode.get("feedElementType").asText();
            if (feedElementType.equalsIgnoreCase("FeedItem")) {
                JsonNode actorNode = elementNode.get("actor");
                if (actorNode != null) {
                    post.put("Actor Name", actorNode.get("name").asText());
                }
                JsonNode bodyNode = elementNode.get("body");
                if (bodyNode != null) {
                    post.put("Post Message", bodyNode.get("text").asText());
                }
                feedPosts.add(post);
            }
            post = new HashMap<>();
        }
        return feedPosts;
    }

    public Boolean addFeedUpdateToCaseById(String caseId, String updateMessage, String base64EncodedString, String fileName) throws ExecutionException, InterruptedException {
        String postData = StringHelper.createFeedPostBody(caseId, updateMessage);
        String response = salesforceService.createFeedPost(postData);
        final JsonObject responseObject = GSON.fromJson(response, JsonObject.class);

        if (base64EncodedString != null && fileName != null) {
            String requestBody = createFeedPostBodyForFile(caseId, base64EncodedString, fileName);
            String responseForFile = salesforceService.createFeedPostForFile(requestBody);
            final JsonObject responseObjectForFile = GSON.fromJson(responseForFile, JsonObject.class);
            if (responseObject.get("id") != null && responseObjectForFile.get("id") != null) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }

        if (responseObject.get("id") != null) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }


    public Map<String, Object> getCaseUpdatedFields(String query) throws IOException, ExecutionException, InterruptedException {
        String response = salesforceService.getRecordByQuery(query);
        SFCaseHistory sfCaseHistory = GSON.fromJson(response, SFCaseHistory.class);
        return sfCaseHistory.toMap();
    }


    public app.krista.model.base.File convertJavaFileToKristaFile(java.io.File file) throws IOException {

        try (final FileHandle outputFileHandle = this.fileRepository.createNewFileByName(file.getName())) {
            try {
                outputFileHandle.setContent(new FileInputStream(file));
            } catch (IOException cause) {
                throw new RuntimeException("Failed to store content to file handle", cause);
            }
            return outputFileHandle.getFile();
        }
    }

    public java.io.File convertKristaFileToJavaFile(app.krista.model.base.File file) throws IOException {

        final FileHandle inputFileHandle = this.fileRepository.getFile(file);

        final File tmpDir = new File(System.getProperty("java.io.tmpDir", "/tmp"));

        //System.out.println("Using tmp dir:{}", tmpDir.getAbsolutePath());

        if (tmpDir.exists() && !tmpDir.canWrite()) {
            // LOGGER.error("Unable to write to tmpDir: {}", tmpDir.getAbsolutePath());
            throw new IllegalArgumentException("Unable write to temporary file.");
        }

        final File input = new File(file.getFileName());
        return convertInputStreamToFile(inputFileHandle.getContent(), input);

    }

    public File convertInputStreamToFile(InputStream inputStream, File input) throws IOException {

        try (OutputStream outputStream = new FileOutputStream(input)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return input;
    }

    public String getBase64EncodedString(java.io.File file1) throws IOException {
        byte[] fileContent = Files.readAllBytes(file1.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }
}

