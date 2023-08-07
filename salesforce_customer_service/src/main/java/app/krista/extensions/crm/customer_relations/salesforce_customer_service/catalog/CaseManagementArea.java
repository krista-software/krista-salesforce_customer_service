package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;


import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.DatePlus;
import app.krista.model.base.FreeForm;
import app.krista.model.field.util.FieldValues;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Validators.getValidatedDouble;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Validators.getValidatedString;

@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "0b565597-08f7-4ed8-b5d1-8a72322abdaa")
public class CaseManagementArea {
    private final CaseDaoImpl caseDao;

    app.krista.model.field.Field textField = new app.krista.model.field.Field("com.krista.fields.Text",
            Map.of(), Map.of());

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
        caseId = FieldValues.toString(textField, caseId);
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
        caseId = FieldValues.toString(textField, caseId);
        try {
            return caseDao.addFeedUpdateToCaseById(caseId, updateMessage);
        } catch (ExecutionException cause) {
            throw new RuntimeException(cause.getMessage(), cause);
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_6efb4f6c-745d-4c2f-8582-f03e447c5e15",
            name = "Case Change Alert",
            description = "This request is for the changes(creation and update) in the cases.",
            area = "Case management",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.Desc(name = "CaseResponse", type = "{ Case Id: Text, Case Contact Id: Text, Case Account Id: Text, Case Contact Name: Text, Case Subject: Text, Case Is Closed: Boolean, Case Owner Id: Text, Case Account Name: Text, Case Created Date: Date, Case Last Modified Case: Date, Case Number: Number, Case Contact Email: Email, Case Contact Phone: Phone, Case Status: Text, Case Reason: Text, Case Origin: Text, Case Description: Paragraph, Case Priority: Text }", required = false)
    public Map<String, Object> caseChangeAlert(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData) {
        try {
            System.out.println("In wait for event");
            if (eventName.equals("Case Change Alert")) {
                System.out.println("got event data");
                Map<String, Object> caseMap = new HashMap<>();
                System.out.println("created map");
                caseMap.put("Case Id", eventData.get("Id"));
                System.out.println("Added case id: " + eventData.get("Id"));
                setCaseDetails(eventData, caseMap);
                System.out.println("set default details");
                updateCaseDetails(eventData, caseMap);
                System.out.println("updated details");
                return caseMap;
            }
            return Map.of();
        } catch (IllegalArgumentException cause) {
            System.err.println(cause.getMessage());
            return Map.of();
        } catch (IOException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void updateCaseDetails(FreeForm eventData, Map<String, Object> caseMap) throws IOException, ExecutionException, InterruptedException {
        String query = "SELECT Fields(ALL) FROM CaseHistory WHERE CaseId = '" + eventData.get("Id")
                + "' ORDER BY CreatedDate DESC LIMIT 20";

        Map<String, Object> fetched = caseDao.getCaseUpdatedFields(query);
        if (fetched.size() != 1 || !fetched.containsKey(Constants.CREATED)) {
            setCaseDetails(fetched, caseMap);
        }
    }

    private static void setCaseDetails(Map<String, Object> caseData, Map<String, Object> caseMap) {
        /*String accountName = "";
        if (caseData.containsKey("Name")) {
            accountName = caseData.get("Name").toString();
        } else if (caseData.containsKey("TextName")) {
            accountName = caseData.get("TextName").toString();
        }.*/
        //caseMap.put("Account Name", accountName);
        //caseMap.put("Case Contact Id", getValidatedString(caseData.get("Fax")));
        caseMap.put("Case Account Id", getValidatedString(caseData.get("AccountId")));//
        //caseMap.put("Case Contact Name", getValidatedString(caseData.get("ContactEmail")));
        caseMap.put("Case Subject", getValidatedString(caseData.get("Subject")));//
        caseMap.put("Case Is Closed", getValidatedString(caseData.get("IsClosed")));//
        caseMap.put("Case Owner Id", getValidatedString(caseData.get("OwnerId")));//
        //caseMap.put("Case Account Name", getValidatedString(caseData.get("Email")));
        caseMap.put("Case Contact Id", getValidatedString(caseData.get("ContactId")));
        caseMap.put("Case Priority", getValidatedString(caseData.get("Priority")));
        //caseMap.put("Case Number", getValidatedString(caseData.get("MobilePhone")));
        caseMap.put("Case Contact Email", getValidatedString(caseData.get("ContactEmail")));//
        caseMap.put("Case Contact Phone", getValidatedString(caseData.get("ContactPhone")));//
        caseMap.put("Case Status", getValidatedString(caseData.get("Status")));//
        caseMap.put("Case Reason", getValidatedString(caseData.get("Reason")));//
        caseMap.put("Case Origin", getValidatedString(caseData.get("Origin")));//
        caseMap.put("Case Description", getValidatedString(caseData.get("Description")));//

        caseMap.put("Case Number", getValidatedDouble(caseData.get("CaseNumber")));
        caseMap.put("Case Created Date", caseData.get("CreatedDate") != null
                ? Objects.requireNonNull(
                DatePlus.fromYYYYMMDD(caseData.get("CreatedDate").toString())).getTime()
                : null);//
        caseMap.put("Case Last Modified Case", caseData.get("LastModifiedDate") != null
                ? Objects.requireNonNull(
                DatePlus.fromYYYYMMDD(caseData.get("LastModifiedDate").toString())).getTime()
                : null);//

        caseMap.forEach((k, v) -> {
            System.out.println(k + " : " + v);
        });

    }
}

