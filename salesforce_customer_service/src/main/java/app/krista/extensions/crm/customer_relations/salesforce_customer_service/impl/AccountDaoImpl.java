package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFAccount;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.SalesforceListResponse;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Account;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.AccountDao;
import com.google.gson.reflect.TypeToken;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public final class AccountDaoImpl implements AccountDao {

    public static final String ACCOUNT = "Account";
    private final SalesforceService salesforceService;

    @Inject
    public AccountDaoImpl(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    @Override
    public Account getAccountById(String id) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Failed to fetch account by Id. " + Constants.ID_IS_NULL_OR_EMPTY);
        }
        return new AccountImpl(Constants.GSON.fromJson(salesforceService.getRecordById(id, ACCOUNT), SFAccount.class));
    }

    @Override
    public List<Account> getAccounts(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException {
        String QUERY_FIELDS = "id,createdDate,phone,type,website,Name,ownership,AccountNumber,Industry,NumberOfEmployees";
        return getAccountByQuery("Select " + QUERY_FIELDS + " from Account  ORDER BY " + orderByField + " "+ orderByOperand +" LIMIT " + size + " OFFSET " + Math.max((page - 1) * size, 0));
    }

    @Override
    public List<Account> getAccountByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Failed fetch account by query request. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
        String res = salesforceService.getRecordByQuery(query.trim());
        SalesforceListResponse<SFAccount> oppList =
                Constants.GSON.fromJson(res, new TypeToken<SalesforceListResponse<SFAccount>>() {
                }.getType());
        if (oppList.records.size() > 0) {
            return oppList.records.stream().map(AccountImpl::new).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public String createAccount(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException("Failed create account request. " + Constants.ENTITY_MAP_IS_NULL_OR_EMPTY);
        }
        var response = Constants.GSON.fromJson(salesforceService.createRecord(Constants.GSON.toJson(SFAccount.from(entityDefinition)), ACCOUNT), Map.class);
        return String.valueOf(response.get("id"));
    }

    @Override
    public void updateAccount(Account account) throws IllegalAccessException, IOException, ExecutionException, InterruptedException, IllegalArgumentException {
        if (account == null || account.getId() == null || account.getId().isBlank()) {
            throw new IllegalArgumentException("Failed to update account. Account not found for update.");
        }
        SFAccount updatedOpportunity = SFAccount.from(account);
        SFAccount oldOpportunity1 = SFAccount.from(getAccountById(account.getId()));
        Map<String, String> diff = SalesforceService.getDifference(oldOpportunity1, updatedOpportunity);
        salesforceService.updateRecord(account.getId(), Constants.GSON.toJson(diff), ACCOUNT);
    }

    @Override
    public void deleteAccountById(String accountId) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(accountId, ACCOUNT);
    }
}
