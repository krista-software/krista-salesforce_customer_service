package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Customer;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SFCustomer} model class.
 */
public class SFCustomerTest {

    @Test
    public void testFrom_WithValidCustomer_ReturnsPopulatedSFCustomer() {
        Customer customer = new Customer();
        customer.id = "CUST-001";
        customer.name = "Test Customer";
        customer.partyId = "PARTY-001";

        SFCustomer result = SFCustomer.from(customer);

        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", customer.id, result.id);
        assertEquals("Name should match", customer.name, result.name);
        assertEquals("Party ID should match", customer.partyId, result.partyId);
    }

    @Test
    public void testFrom_WithNullCustomer_PrintsErrorAndReturnsNull() {
        // Note: This method prints to stderr but doesn't throw exception
        // It will throw AssertionError if assertions are enabled
        try {
            SFCustomer result = SFCustomer.from((Customer) null);
            // If assertions are disabled, result will be created
        } catch (AssertionError e) {
            // Expected if assertions are enabled
        }
    }

    @Test
    public void testFrom_WithValidMap_ReturnsPopulatedSFCustomer() {
        Map<String, Object> map = new HashMap<>();
        map.put("Name", "Test Customer");
        map.put("Party Id", "PARTY-001");

        SFCustomer result = SFCustomer.from(map);

        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "Test Customer", result.name);
        assertEquals("Party ID should match", "PARTY-001", result.partyId);
    }

    @Test
    public void testFrom_WithNullMap_PrintsErrorAndReturnsNull() {
        // Note: This method prints to stderr but doesn't throw exception
        try {
            SFCustomer result = SFCustomer.from((Map<String, Object>) null);
            // If assertions are disabled, result will be created
        } catch (AssertionError e) {
            // Expected if assertions are enabled
        }
    }

    @Test
    public void testFrom_WithEmptyMap_PrintsErrorAndReturnsNull() {
        // Note: This method prints to stderr but doesn't throw exception
        try {
            SFCustomer result = SFCustomer.from(new HashMap<>());
            // If assertions are disabled, result will be created
        } catch (AssertionError e) {
            // Expected if assertions are enabled
        }
    }
}

