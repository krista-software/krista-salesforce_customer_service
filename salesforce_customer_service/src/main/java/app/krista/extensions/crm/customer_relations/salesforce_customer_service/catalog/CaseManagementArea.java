package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;


import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.model.field.util.FieldValues;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
}
