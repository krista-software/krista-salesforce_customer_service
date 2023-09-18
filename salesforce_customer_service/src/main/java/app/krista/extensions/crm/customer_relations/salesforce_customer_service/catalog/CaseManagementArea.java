package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;


import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.model.base.FreeForm;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.SFObjectHelper.upsertCaseMap;

@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "abc90187-31a8-460e-b19b-99ae146e6ee8")
public class CaseManagementArea {
    private final CaseDaoImpl caseDao;

    @Inject
    private CaseManagementArea(CaseDaoImpl caseDao) {
        this.caseDao = caseDao;
    }

    @CatalogRequest(
            id = "localDomainRequest_4adceae0-923e-4163-bcc0-1c0fd9cf8b19",
            name = "Get Case Feed Items By Case Id",
            description = "This request will fetch the list of post items for the given case Id",
            area = "Case management",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Feed Items", type = "[ { Actor Name: Text, Post Message: Text } ]", required = false)
    public List<Map<String, Object>> getCaseFeedItemsByCaseId(
            @Field.Text(name = "Case Id") String caseId) {
        try {
            return caseDao.getFeedItemsForCaseId(caseId);
        } catch (ExecutionException | JsonProcessingException cause) {
            throw new RuntimeException(cause);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interruptedException);
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_b3f52b0d-f4af-462b-a600-62d8afd83836",
            name = "Share Update to Case",
            description = "This request will add update to the Feed of the Case.",
            area = "Case management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Is Updated", required = false)
    public Boolean shareUpdateToCase(
            @Field.Text(name = "Case Id") String caseId,
            @Field(name = "Update Message", type = "Paragraph") String updateMessage) throws InterruptedException {
        try {
            return caseDao.addFeedUpdateToCaseById(caseId, getEscapeCharacterMessage(updateMessage));
        } catch (ExecutionException cause) {
            throw new RuntimeException(cause.getMessage(), cause);
        }
    }

    private static String getEscapeCharacterMessage(String updateMessage) {
        StringBuilder escapedMessage = new StringBuilder();

        for (int i = 0; i < updateMessage.length(); i++) {
            char ch = updateMessage.charAt(i);
            switch (ch) {
                case '\"':
                    escapedMessage.append("\\\"");
                    break;
                case '\n':
                    escapedMessage.append("\\n");
                    break;
                case '\\':
                    escapedMessage.append("\\\\");
                    break;
                case '\f':
                    escapedMessage.append("\\f");
                    break;
                case '\t':
                    escapedMessage.append("\\t");
                    break;
                case '\b':
                    escapedMessage.append("\\b");
                    break;
                case '\r':
                    escapedMessage.append("\\r");
                    break;
                default:
                    escapedMessage.append(ch);
                    break;
            }
        }
        return escapedMessage.toString();
    }

    @CatalogRequest(
            id = "localDomainRequest_6efb4f6c-745d-4c2f-8582-f03e447c5e15",
            name = "Case Change Alert",
            description = "This request will alert the user whenever you create a case or update any case.",
            area = "Case management",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.Desc(name = "CaseResponse", type = "{ Case Id: Text, Case Contact Id: Text, Case Account Id: Text, Case Subject: Text, Case Is Closed: Boolean, Case Owner Id: Text, Case Created Date: Date, Case Last Modified Case: Date, Case Number: Number, Case Status: Text, Case Reason: Text, Case Origin: Text, Case Description: Paragraph, Case Priority: Text }", required = false)
    public Map<String, Object> caseChangeAlert(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData) {
        Map<String, Object> caseMap = new HashMap<>();
        try {
            if (eventName.equals("Case Change Alert")) {
                caseMap.put("Case Id", eventData.get("Id"));
                upsertCaseMap(eventData, caseMap);
                updateCaseDetails(eventData, caseMap);
            }
        } catch (IllegalArgumentException cause) {
            System.err.println("This error is for case change alert." + cause.getMessage());
        } catch (IOException | ExecutionException cause) {
            throw new IllegalStateException(cause.getMessage(), cause);
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(cause.getMessage(), cause);
        }
        return caseMap;
    }

    private void updateCaseDetails(FreeForm eventData, Map<String, Object> caseMap) throws IOException,
            ExecutionException, InterruptedException {
        String query = "SELECT Fields(ALL) FROM CaseHistory WHERE CaseId = '" + eventData.get("Id") +
                "' ORDER BY CreatedDate DESC LIMIT 20";

        Map<String, Object> fetched = caseDao.getCaseUpdatedFields(query);
        if (fetched.size() != 1 || !fetched.containsKey(Constants.CREATED)) {
            upsertCaseMap(fetched, caseMap);
        }
    }

}

