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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Customer;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CustomerDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CustomerImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCustomer;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.CaseQueryBuilder.getQuery;

@Service
public final class CustomerStore implements EntityStore<Customer> {

    private final CustomerDaoImpl customerDao;

    @Inject
    private CustomerStore(CustomerDaoImpl customerDao) {
        this.customerDao = customerDao;
    }


    @Override
    public Customer create(Map<String, Object> entityDefinition) {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            System.err.println("Failed to create customer. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        try {
            String customerId = customerDao.createCustomer(entityDefinition);
            return get(customerId);
        } catch (IllegalArgumentException cause) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + cause.getMessage());
        }
        return new Customer();
    }

    @Override
    public Customer get(String primaryKey) {
        if (primaryKey == null || primaryKey.isBlank()) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + Constants.PRIMARY_KEY_IS_EMPTY);
        }
        try {
            return CatalogTypes.fromSFCustomer(customerDao.getCustomerById(primaryKey));
        } catch (IOException | ExecutionException | InterruptedException | IllegalArgumentException cause) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + cause.getMessage());
        }
        return new Customer();
    }

    @Override
    public Customer update(Customer customer) {
        if (customer == null) {
            System.err.println("Failed to update customer. Customer not found for update");
        }
        try {
            customerDao.updateCustomer(new CustomerImpl(SFCustomer.from(customer)));
            assert customer != null;
            return get(customer.id);
        } catch (IOException | ExecutionException | InterruptedException | IllegalAccessException |
                 IllegalArgumentException cause) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + cause.getMessage());
        }
        return new Customer();
    }

    @Override
    public void delete(String primaryKey) {
        if (primaryKey == null || primaryKey.isBlank()) {
            System.err.println("Failed to delete case." + Constants.PRIMARY_KEY_IS_EMPTY);
        }
        try {
            customerDao.deleteCustomerById(primaryKey);
        } catch (IOException | ExecutionException | InterruptedException cause) {
            System.err.println("Failed to delete" + cause.getMessage());
        }
    }

    @Override
    public boolean contains(String s) {
        return false;
    }

    @Override
    @Deprecated
    public List<Customer> search(List<SearchCondition> list, long l, int i) {
        return Collections.emptyList();
    }

    @Override
    public List<Customer> search(SearchQuery searchQuery, long pageIndex, int pageSize) {
        final List<SearchCondition> conditions = searchQuery.getSearchConditions();
        final List<QueryClause> queryClauses = searchQuery.getQueryClauses();

        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Failed to search for individuals. " + Constants.CONDITIONS_NOT_FOUND);
        }
        try {
            String orderByField = queryClauses.isEmpty() ? Constants.LASTMODIFIEDDATE : queryClauses.getFirst().getFieldName();
            String orderByOperand = queryClauses.isEmpty() ? Constants.DESC : queryClauses.getFirst().getOperand().toString();

            if (conditions.size() == 1) {
                return customerDao.getCustomers(pageSize, pageIndex, orderByField, orderByOperand).stream()
                        .map(CatalogTypes::fromSFCustomer).collect(Collectors.toList());
            } else {
                String query = getQuery(conditions, pageSize, pageIndex, orderByField, orderByOperand);
                return customerDao.getCustomerByQuery(query).stream()
                        .map(CatalogTypes::fromSFCustomer).collect(Collectors.toList());
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
}
