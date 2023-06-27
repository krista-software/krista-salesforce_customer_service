package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCustomer;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.CustomerDao;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.SalesforceListResponse;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.lang.Math.max;

@Service
public class CustomerDaoImpl implements CustomerDao {

    public static final String CUSTOMER = "Customer";
    public static final String QUERY_FIELDS = "id, customerStatusType, name, partyId, totalLifeTimeValue";
    private final SalesforceService salesforceService;

    @Inject
    public CustomerDaoImpl(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }


    @Override
    public Customer getCustomerById(String id) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank()) {
            System.err.println("Failed to fetch Customer by id. " + Constants.ID_IS_NULL_OR_EMPTY);
        }
        return new CustomerImpl(Constants.GSON.fromJson(salesforceService.getRecordById(id, CUSTOMER),
                SFCustomer.class));
    }

    @Override
    public List<Customer> getCustomers(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException {
        return getCustomerByQuery("Select " + QUERY_FIELDS + " from Customer ORDER BY " + orderByField + " " + orderByOperand +
                " LIMIT " + size + " OFFSET " + max((page - 1) * size, 0));
    }

    @Override
    public List<Customer> getCustomerByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            System.err.println("Failed to fetch customers by query. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
        String result = salesforceService.getRecordByQuery(query.trim());
        SalesforceListResponse<SFCustomer> customersList =
                Constants.GSON.fromJson(result, new TypeToken<SalesforceListResponse<SFCustomer>>() {
                }.getType());
        if (customersList.records.size() > 0) {
            return customersList.records.stream().map(CustomerImpl::new).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public String createCustomer(Map<String, Object> entityDefinition) {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            System.err.println("Failed to create customer. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        try {
            var response = Constants.GSON.fromJson(salesforceService.createRecord(
                    Constants.GSON.toJson(SFCustomer.from(entityDefinition)), CUSTOMER), Map.class);
            return String.valueOf(response.get("id"));
        } catch (JsonSyntaxException cause) {
            System.err.println("Failed to create the customer record. Please ensure the provided information is correct and try again." + cause.getMessage());
        } catch (IOException | ExecutionException | InterruptedException cause) {
            System.err.println("An unexpected error occurred while creating the customer record. Please try again." + cause.getMessage());
        }
        return "";
    }

    @Override
    public void updateCustomer(Customer customer) throws IllegalAccessException, IOException, ExecutionException, InterruptedException {
        if (customer == null || customer.getId() == null || customer.getId().isBlank()) {
            System.err.println("Failed to update account. Account not found for update.");
        }
        SFCustomer updatedCustomer = SFCustomer.from(customer);
        SFCustomer oldCustomer = SFCustomer.from(getCustomerById(customer.getId()));
        Map<String, String> differences = SalesforceService.getDifference(oldCustomer, updatedCustomer);
        salesforceService.updateRecord(customer.getId(), Constants.GSON.toJson(differences), CUSTOMER);
    }

    @Override
    public void deleteCustomerById(String primaryKey) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(primaryKey, CUSTOMER);

    }
}
