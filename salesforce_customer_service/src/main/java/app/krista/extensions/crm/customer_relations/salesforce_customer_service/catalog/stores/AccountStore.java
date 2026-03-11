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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.stores;

import app.krista.extension.executor.QueryClause;
import app.krista.extension.executor.SearchCondition;
import app.krista.extension.executor.SearchQuery;
import app.krista.extension.util.EntityStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.CatalogTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Account;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.AccountDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.AccountImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFAccount;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.AND;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.WHERE;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper.isStringAvailable;

@Service
public final class AccountStore implements EntityStore<Account> {

    private final AccountDaoImpl accountDao;

    @Inject
    private AccountStore(AccountDaoImpl accountDao) {
        this.accountDao = accountDao;
    }

    /**
     * @param entityDefinition input map with {@link Account} entity fields.
     * @return {@link Account} for the given input
     */
    @Override
    public Account create(Map<String, Object> entityDefinition) {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException("Failed to create customer. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        try {
            String customerId = accountDao.createAccount(entityDefinition);
            return get(customerId);
        } catch (IOException | ExecutionException | InterruptedException | IllegalArgumentException cause) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + cause.getMessage());
        }
        return new Account();
    }

    /**
     * @param primaryKey Id of the {@link Account}
     * @return {@link Account} for the given Id.
     */
    @Override
    public Account get(String primaryKey) {
        if (primaryKey == null || primaryKey.isBlank()) {
            throw new IllegalArgumentException(Constants.FAILED_TO_GET_CUSTOMER + Constants.PRIMARY_KEY_IS_EMPTY);
        }
        try {
            return CatalogTypes.fromSFAccount(accountDao.getAccountById(primaryKey));
        } catch (IOException | ExecutionException | InterruptedException | IllegalArgumentException cause) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + cause.getMessage());
        }
        return new Account();
    }

    /**
     * @param account {@link Account} object is given as input to update.
     * @return returns the updated entity {@link Account}
     */
    @Override
    public Account update(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Failed to update customer. Customer not found for update");
        }
        try {
            accountDao.updateAccount(new AccountImpl(SFAccount.from(account)));
            return get(account.id);
        } catch (IOException | ExecutionException | IllegalAccessException | InterruptedException |
                 IllegalArgumentException cause) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + cause.getMessage());
        }
        return new Account();
    }

    @Override
    public void delete(String primaryKey) {
        if (primaryKey == null || primaryKey.isBlank()) {
            throw new IllegalArgumentException("Failed to delete case." + Constants.PRIMARY_KEY_IS_EMPTY);
        }
        try {
            accountDao.deleteAccountById(primaryKey);
        } catch (IOException | ExecutionException | InterruptedException cause) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + cause.getMessage());
        }
    }

    @Override
    public boolean contains(String s) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<Account> search(List<SearchCondition> conditions, long pageIndex, int pageSize) {
        return List.of();
    }

    /**
     * @param searchQuery list of search conditions and query clauses given as input
     * @param pageIndex   page number of the query results
     * @param pageSize    number of results per page
     * @return returns {@link List<Account>} as per the given query
     */
    @Override
    public List<Account> search(SearchQuery searchQuery, long pageIndex, int pageSize) {
        final List<SearchCondition> conditions = searchQuery.getSearchConditions();
        final List<QueryClause> queryClauses = searchQuery.getQueryClauses();
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Failed to get Customers. " + Constants.CONDITIONS_NOT_FOUND);
        }
        try {
            String orderByField = queryClauses.isEmpty() ? Constants.LASTMODIFIEDDATE : queryClauses.getFirst().getFieldName();
            String orderByOperand = queryClauses.isEmpty() ? Constants.DESC : queryClauses.getFirst().getOperand().toString();
            if (conditions.size() == 1) {
                return accountDao.getAccounts(pageSize, pageIndex, orderByField, orderByOperand).stream().map(CatalogTypes::fromSFAccount).collect(Collectors.toList());
            } else {
                String query = getQuery(conditions, pageSize, pageIndex, orderByField, orderByOperand);
                return accountDao.getAccountByQuery(query).stream().map(CatalogTypes::fromSFAccount).collect(Collectors.toList());
            }
        } catch (IOException | ExecutionException | InterruptedException | IllegalArgumentException cause) {
            System.err.println(Constants.FAILED_TO_SEARCH + cause.getMessage());
        }
        return List.of();
    }

    @Override
    public long count(List<SearchCondition> list) {
        return 0;
    }

    @Override
    public List<String> lookup(Map<String, Object> map, List<String> list) {
        return Collections.emptyList();
    }

    private String getConditionValue(List<SearchCondition> conditions, String fieldName) {
        for (SearchCondition con : conditions) {
            if (fieldName.equalsIgnoreCase(con.getFieldName())) {
                return con.getOperand() != null ? String.valueOf(con.getOperand()).trim() : "";
            }
        }
        return "";
    }

    private String getQuery(List<SearchCondition> searchConditions, long size, long page, String orderByField, String orderByOperand) {
        String id = getConditionValue(searchConditions, Constants.ID);
        String name = getConditionValue(searchConditions, Constants.NAME);
        String accountNumber = getConditionValue(searchConditions, Constants.ACCOUNT_NUMBER);
        String phone = getConditionValue(searchConditions, Constants.PHONE);
        return createQuery(id, name, accountNumber, phone, size, page, orderByField, orderByOperand);
    }

    private String createQuery(String id, String name, String accountNumber, String phone, long size, long page, String orderByField, String orderByOperand) {
        StringBuilder builder = new StringBuilder();
        String QUERY_FIELDS = "id, createdDate, phone, type, website, Name, ownership, AccountNumber, Industry, NumberOfEmployees ,fax, sic, ownerId, createdById, cleanStatus, billingStreet, billingCity, billingState, billingPostalCode, billingCountry, shippingStreet, shippingCity, shippingState, shippingPostalCode, shippingCountry";
        builder.append("Select ")
                .append(QUERY_FIELDS)
                .append(" from Account ")
                .append(WHERE)
                .append(isStringAvailable(id) ? " Id = '" + id + "' " + AND : "")
                .append(isStringAvailable(name) ? " Name LIKE '%" + name + "%' " + AND : "")
                .append(isStringAvailable(accountNumber) ? " AccountNumber = '" + accountNumber + "' " + AND : "")
                .append((isStringAvailable(phone) ? " Phone LIKE '%" + phone + "%' " + AND : ""));
        StringHelper.setQueryEndIndex(builder);
        StringHelper.appendOrderByClause(size, page, orderByField, orderByOperand, builder);

        return builder.toString();
    }
}
