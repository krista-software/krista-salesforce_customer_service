package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCase;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.SalesforceListResponse;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.CaseDao;
import com.google.gson.reflect.TypeToken;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public final class CaseDaoImpl implements CaseDao {

    private static final String CASE = "Case";
    private static final String QUERY_FIELDS = " Contact.Name, ContactId, ContactEmail, ContactPhone, Account.Name,AccountId,Subject,Priority,Description,IsClosed,ClosedDate,OwnerId,CreatedById, CaseNumber,status,Origin,Type,CreatedDate,LastModifiedDate,Id ";
    private final SalesforceService salesforceService;

    @Inject
    public CaseDaoImpl(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    @Override
    public Case getCaseById(String id) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Failed get case by Id. " + Constants.ID_IS_NULL_OR_EMPTY);
        }
        return getCasesByQuery("Select " + QUERY_FIELDS + "  from Case where id = '" + id + "'").stream().findFirst().orElseThrow(() -> new RuntimeException(CASE + " Not found for id " + id));
    }

    @Override
    public List<Case> getCasesByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Failed get case by query request. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
        String res = salesforceService.getRecordByQuery(query);
        SalesforceListResponse<SFCase> oppList =
                Constants.GSON.fromJson(res, new TypeToken<SalesforceListResponse<SFCase>>() {
                }.getType());
        if (oppList.records.size() > 0) {
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
            throw new IllegalArgumentException("Failed create case request. " + Constants.ENTITY_MAP_IS_NULL_OR_EMPTY);
        }
        var response = Constants.GSON.fromJson(salesforceService.createRecord(Constants.GSON.toJson(SFCase.from(entityDefinition)), CASE), Map.class);
        return String.valueOf(response.get("id"));
    }

    @Override
    public void updateCase(Case aCase) throws IOException, IllegalAccessException, ExecutionException, InterruptedException {
        if (aCase == null || aCase.getId() == null || aCase.getId().isEmpty())
            throw new IllegalArgumentException("Failed to update case. Case not found to update.");
        SFCase updatedCase = SFCase.from(aCase);
        SFCase oldCase = SFCase.from(getCaseById(aCase.getId()));
        Map<String, String> diff = SalesforceService.getDifference(oldCase, updatedCase);
        salesforceService.updateRecord(aCase.getId(), Constants.GSON.toJson(diff), CASE);
    }

    @Override
    public void deleteCaseById(String caseId) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(caseId, CASE);
    }
}
