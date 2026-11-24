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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CustomerDaoImpl}.
 *
 * <p>The DAO delegates all remote interaction to {@link SalesforceService}, which is mocked here.
 * {@code getCustomerById} parses a single-record JSON payload, list operations parse Salesforce
 * list JSON, and create/update/delete are verified through their interaction with the service.
 * {@code createCustomer} swallows its exceptions internally, so those branches are asserted by
 * the returned empty string.</p>
 */
public class CustomerDaoImplTest {

    private static final String CUSTOMER = "Customer";
    private static final String CUSTOMER_ID = "0jrXXXXXXXXXXXXXXX";

    private static final String CUSTOMER_JSON =
            "{\"Id\":\"" + CUSTOMER_ID + "\",\"Name\":\"Acme Corp\",\"PartyId\":\"001PARTY\"," +
                    "\"CustomerStatusType\":\"Active\",\"TotalLifeTimeValue\":15000.0}";

    @Mock
    private SalesforceService salesforceService;

    private CustomerDaoImpl customerDao;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        customerDao = new CustomerDaoImpl(salesforceService);
    }

    // ==================== getCustomerById ====================

    @Test
    public void testGetCustomerById_ReturnsCustomer() throws Exception {
        when(salesforceService.getRecordById(CUSTOMER_ID, CUSTOMER)).thenReturn(CUSTOMER_JSON);

        Customer customer = customerDao.getCustomerById(CUSTOMER_ID);

        assertNotNull("Customer should not be null", customer);
        assertEquals("Id should match", CUSTOMER_ID, customer.getId());
        assertEquals("Name should match", "Acme Corp", customer.getName());
        assertEquals("Party id should match", "001PARTY", customer.getPartyId());
        assertEquals("Status should match", "Active", customer.getCustomerStatusType());
        assertEquals("Lifetime value should match", Double.valueOf(15000.0), customer.getTotalLifeTimeValue());
        verify(salesforceService).getRecordById(CUSTOMER_ID, CUSTOMER);
    }

    @Test
    public void testGetCustomerById_NullId_HandlesGuard() throws Exception {
        // Null id enters the guard branch; the DAO still delegates to the service.
        when(salesforceService.getRecordById(any(), any())).thenReturn(CUSTOMER_JSON);

        Customer customer = customerDao.getCustomerById(null);

        assertNotNull("A wrapper customer is always returned", customer);
        verify(salesforceService).getRecordById(null, CUSTOMER);
    }

    // ==================== getCustomers (paginated) ====================

    @Test
    public void testGetCustomers_ReturnsCustomersAndBuildsQuery() throws Exception {
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + CUSTOMER_JSON + "],\"totalSize\":1,\"done\":true}");

        List<Customer> customers = customerDao.getCustomers(25, 3, "Name", "ASC");

        assertNotNull("Customers list should not be null", customers);
        assertEquals("Should return one customer", 1, customers.size());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(salesforceService).getRecordByQuery(queryCaptor.capture());
        String query = queryCaptor.getValue();
        assertTrue("Query should sort", query.contains("ORDER BY Name ASC"));
        assertTrue("Query should page", query.contains("LIMIT 25"));
        assertTrue("Query should offset", query.contains("OFFSET 50"));
    }

    // ==================== getCustomerByQuery ====================

    @Test
    public void testGetCustomerByQuery_ReturnsCustomers() throws Exception {
        String second = "{\"Id\":\"0jrBBB\",\"Name\":\"Globex\"}";
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + CUSTOMER_JSON + "," + second + "],\"totalSize\":2,\"done\":true}");

        List<Customer> customers = customerDao.getCustomerByQuery("SELECT Id, Name FROM Customer");

        assertNotNull("Customers list should not be null", customers);
        assertEquals("Should return two customers", 2, customers.size());
        assertEquals("First customer id should match", CUSTOMER_ID, customers.get(0).getId());
        assertEquals("Second customer name should match", "Globex", customers.get(1).getName());
    }

    @Test
    public void testGetCustomerByQuery_EmptyRecords_ReturnsEmptyList() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[],\"totalSize\":0,\"done\":true}");

        List<Customer> customers = customerDao.getCustomerByQuery("SELECT Id FROM Customer");

        assertNotNull("Customers list should not be null", customers);
        assertTrue("Customers list should be empty", customers.isEmpty());
    }

    @Test
    public void testGetCustomerByQuery_BlankQuery_HandlesGuard() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[]}");

        List<Customer> customers = customerDao.getCustomerByQuery("   ");

        assertNotNull("Customers list should not be null", customers);
        assertTrue("Customers list should be empty", customers.isEmpty());
    }

    // ==================== createCustomer ====================

    @Test
    public void testCreateCustomer_ReturnsId() throws Exception {
        Map<String, Object> entity = new HashMap<>();
        entity.put("Name", "Acme Corp");
        entity.put("Party Id", "001PARTY");
        when(salesforceService.createRecord(anyString(), eq(CUSTOMER)))
                .thenReturn("{\"id\":\"0jrNEWCUSTOMER\",\"success\":true}");

        String id = customerDao.createCustomer(entity);

        assertEquals("Created customer id should match", "0jrNEWCUSTOMER", id);
        verify(salesforceService).createRecord(anyString(), eq(CUSTOMER));
    }

    @Test
    public void testCreateCustomer_EmptyMap_StillDelegates() throws Exception {
        // Empty map hits the guard branch but the record is still built and sent.
        when(salesforceService.createRecord(anyString(), eq(CUSTOMER)))
                .thenReturn("{\"id\":\"0jrEMPTY\",\"success\":true}");

        String id = customerDao.createCustomer(new HashMap<>());

        assertEquals("Created customer id should match", "0jrEMPTY", id);
    }

    @Test
    public void testCreateCustomer_MalformedResponse_ReturnsEmpty() throws Exception {
        Map<String, Object> entity = new HashMap<>();
        entity.put("Name", "Acme Corp");
        when(salesforceService.createRecord(anyString(), eq(CUSTOMER))).thenReturn("{\"id\":}");

        String id = customerDao.createCustomer(entity);

        assertEquals("JSON syntax error should yield empty string", "", id);
    }

    @Test
    public void testCreateCustomer_ServiceThrows_ReturnsEmpty() throws Exception {
        Map<String, Object> entity = new HashMap<>();
        entity.put("Name", "Acme Corp");
        when(salesforceService.createRecord(anyString(), eq(CUSTOMER))).thenThrow(new IOException("boom"));

        String id = customerDao.createCustomer(entity);

        assertEquals("Service failure should yield empty string", "", id);
    }

    // ==================== updateCustomer ====================

    @Test
    public void testUpdateCustomer_InvokesUpdateRecord() throws Exception {
        when(salesforceService.getRecordById(CUSTOMER_ID, CUSTOMER)).thenReturn(CUSTOMER_JSON);

        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(CUSTOMER_ID);
        when(customer.getName()).thenReturn("Renamed Corp");

        customerDao.updateCustomer(customer);

        verify(salesforceService).updateRecord(eq(CUSTOMER_ID), anyString(), eq(CUSTOMER));
    }

    // ==================== deleteCustomerById ====================

    @Test
    public void testDeleteCustomerById_InvokesDelete() throws Exception {
        customerDao.deleteCustomerById(CUSTOMER_ID);

        verify(salesforceService).deleteRecordById(CUSTOMER_ID, CUSTOMER);
    }
}
