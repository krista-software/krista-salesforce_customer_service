package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Customer} entity class.
 */
public class CustomerTest {

    @Test
    public void testCustomerInstantiation() {
        Customer customer = new Customer();
        assertNotNull("Customer should not be null", customer);
    }

    @Test
    public void testCustomerFieldAssignment() {
        Customer customer = new Customer();
        customer.id = "CUST-001";
        customer.name = "Test Customer";
        customer.partyId = "PARTY-001";

        assertEquals("ID should match", "CUST-001", customer.id);
        assertEquals("Name should match", "Test Customer", customer.name);
        assertEquals("Party ID should match", "PARTY-001", customer.partyId);
    }

    @Test
    public void testCustomerWithNullValues() {
        Customer customer = new Customer();
        customer.id = null;
        customer.name = null;

        assertNull("ID should be null", customer.id);
        assertNull("Name should be null", customer.name);
    }
}

