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
    public Customer create(Map<String, Object> entityDefinition) throws IOException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            System.err.println("Failed to create customer. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        try {
            String customerId = customerDao.createCustomer(entityDefinition);
            return get(customerId);
        } catch (IOException | IllegalArgumentException cause) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + cause.getMessage());
        }
        return new Customer();
    }

    @Override
    public Customer get(String primaryKey) throws IOException {
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
    public Customer update(Customer customer) throws IOException {
        if (customer == null) {
            System.err.println("Failed to update customer. Customer not found for update");
        }
        try {
            customerDao.updateCustomer(new CustomerImpl(SFCustomer.from(customer)));
            return get(customer.id);
        } catch (IOException | ExecutionException | InterruptedException | IllegalAccessException |
                 IllegalArgumentException cause) {
            System.err.println(Constants.FAILED_TO_GET_CUSTOMER + cause.getMessage());
        }
        return new Customer();
    }

    @Override
    public void delete(String primaryKey) throws IOException {
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
    public List<Customer> search(List<SearchCondition> list, long l, int i) throws IOException {
        return Collections.emptyList();
    }

    @Override
    public List<Customer> search(SearchQuery searchQuery, long pageIndex, int pageSize) throws IOException {
        final List<SearchCondition> conditions = searchQuery.getSearchConditions();
        final List<QueryClause> queryClauses = searchQuery.getQueryClauses();

        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Failed to search for individuals. " + Constants.CONDITIONS_NOT_FOUND);
        }
        try {
            String orderByField = queryClauses.isEmpty() ? Constants.LASTMODIFIEDDATE : queryClauses.get(0).getFieldName();
            String orderByOperand = queryClauses.isEmpty() ? Constants.DESC : queryClauses.get(0).getOperand().toString();

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
