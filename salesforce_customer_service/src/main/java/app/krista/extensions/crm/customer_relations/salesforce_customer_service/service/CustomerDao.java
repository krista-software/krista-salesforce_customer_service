package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface CustomerDao {
    Customer getCustomerById(String id) throws IOException, ExecutionException, InterruptedException;

    List<Customer> getCustomers(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException;

    List<Customer> getCustomerByQuery(String query) throws IOException, ExecutionException, InterruptedException;

    String createCustomer(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException;

    void updateCustomer(Customer customer) throws IllegalAccessException, IOException, ExecutionException, InterruptedException;

    void deleteCustomerById(String primaryKey) throws IOException, ExecutionException, InterruptedException;
}
