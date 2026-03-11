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

/**
 * Implementation of the CustomerDao interface that interacts with Salesforce.
 * <p>
 * This class provides methods to create, read, update, and delete Customer records
 * in Salesforce. It handles the communication with the Salesforce API and converts
 * between the API's data format and the application's Customer objects.
 */
@Service
public class CustomerDaoImpl implements CustomerDao {

    public static final String CUSTOMER = "Customer";
    public static final String QUERY_FIELDS = "id, customerStatusType, name, partyId, totalLifeTimeValue";
    private final SalesforceService salesforceService;

    /**
     * Creates a new CustomerDaoImpl with the required Salesforce service.
     *
     * @param salesforceService Service for interacting with Salesforce API
     */
    @Inject
    public CustomerDaoImpl(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }


    /**
     * Gets a Customer by its ID.
     *
     * @param id The ID of the Customer to retrieve
     * @return The Customer with the specified ID
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public Customer getCustomerById(String id) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank()) {
            System.err.println("Failed to fetch Customer by id. " + Constants.ID_IS_NULL_OR_EMPTY);
        }
        return new CustomerImpl(Constants.GSON.fromJson(salesforceService.getRecordById(id, CUSTOMER),
                SFCustomer.class));
    }

    /**
     * Gets a paginated list of Customers sorted by the specified field.
     *
     * @param size Number of Customers to retrieve per page
     * @param page Page number to retrieve (1-based)
     * @param orderByField Field to sort by
     * @param orderByOperand Sort direction (ASC or DESC)
     * @return List of Customers for the specified page
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public List<Customer> getCustomers(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException {
        return getCustomerByQuery("Select " + QUERY_FIELDS + " from Customer ORDER BY " + orderByField + " " + orderByOperand +
                " LIMIT " + size + " OFFSET " + max((page - 1) * size, 0));
    }

    /**
     * Gets Customers by executing a SOQL query.
     *
     * @param query The SOQL query to execute
     * @return List of Customers matching the query
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public List<Customer> getCustomerByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            System.err.println("Failed to fetch customers by query. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
        assert query != null;
        String result = salesforceService.getRecordByQuery(query.trim());
        SalesforceListResponse<SFCustomer> customersList =
                Constants.GSON.fromJson(result, new TypeToken<SalesforceListResponse<SFCustomer>>() {
                }.getType());
        if (!customersList.records.isEmpty()) {
            return customersList.records.stream().map(CustomerImpl::new).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Creates a new Customer in Salesforce.
     *
     * @param entityDefinition Map containing the Customer fields and values
     * @return The ID of the newly created Customer, or empty string if creation fails
     */
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

    /**
     * Updates an existing Customer in Salesforce.
     * Only changed fields will be updated.
     *
     * @param customer The Customer with updated field values
     * @throws IllegalAccessException If there's a problem accessing Customer fields
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public void updateCustomer(Customer customer) throws IllegalAccessException, IOException, ExecutionException, InterruptedException {
        if (customer == null || customer.getId() == null || customer.getId().isBlank()) {
            System.err.println("Failed to update account. Account not found for update.");
        }
        SFCustomer updatedCustomer = SFCustomer.from(customer);
        assert customer != null;
        SFCustomer oldCustomer = SFCustomer.from(getCustomerById(customer.getId()));
        Map<String, String> differences = SalesforceService.getDifference(oldCustomer, updatedCustomer);
        salesforceService.updateRecord(customer.getId(), Constants.GSON.toJson(differences), CUSTOMER);
    }

    /**
     * Deletes a Customer from Salesforce by its ID.
     *
     * @param primaryKey The ID of the Customer to delete
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public void deleteCustomerById(String primaryKey) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(primaryKey, CUSTOMER);
    }
}
