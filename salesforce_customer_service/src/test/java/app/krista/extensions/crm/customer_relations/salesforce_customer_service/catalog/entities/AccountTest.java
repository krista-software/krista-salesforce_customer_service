package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Account} entity class.
 * Tests field assignments and basic functionality.
 */
public class AccountTest {

    /**
     * Test Account entity can be instantiated.
     */
    @Test
    public void testAccountInstantiation() {
        // Act
        Account account = new Account();

        // Assert
        assertNotNull("Account should not be null", account);
    }

    /**
     * Test Account entity fields can be set and retrieved.
     */
    @Test
    public void testAccountFieldAssignment() {
        // Arrange
        Account account = new Account();

        // Act
        account.id = "001XXXXXXXXXXXXXXX";
        account.name = "Test Account";
        account.accountNumber = "ACC-12345";
        account.phone = "555-1234";
        account.website = "https://test.com";
        account.type = "Customer";
        account.fax = "555-5678";

        // Assert
        assertEquals("ID should match", "001XXXXXXXXXXXXXXX", account.id);
        assertEquals("Name should match", "Test Account", account.name);
        assertEquals("Account number should match", "ACC-12345", account.accountNumber);
        assertEquals("Phone should match", "555-1234", account.phone);
        assertEquals("Website should match", "https://test.com", account.website);
        assertEquals("Type should match", "Customer", account.type);
        assertEquals("Fax should match", "555-5678", account.fax);
    }

    /**
     * Test Account entity with null values.
     */
    @Test
    public void testAccountWithNullValues() {
        // Arrange
        Account account = new Account();

        // Act
        account.id = null;
        account.name = null;
        account.phone = null;

        // Assert
        assertNull("ID should be null", account.id);
        assertNull("Name should be null", account.name);
        assertNull("Phone should be null", account.phone);
    }

    /**
     * Test Account entity with all fields populated.
     */
    @Test
    public void testAccountWithAllFields() {
        // Arrange
        Account account = new Account();

        // Act
        account.id = "001XXXXXXXXXXXXXXX";
        account.name = "Test Account";
        account.accountNumber = "ACC-12345";
        account.phone = "555-1234";
        account.website = "https://test.com";
        account.type = "Customer";
        account.fax = "555-5678";
        account.sic = "1234";
        account.ownerId = "005XXXXXXXXXXXXXXX";
        account.createdById = "005YYYYYYYYYYYYYYY";
        account.cleanStatus = "Clean";
        account.ownership = "Private";
        account.industry = "Technology";
        account.numberOfEmployees = 100.0;
        account.billingCity = "San Francisco";
        account.billingState = "CA";
        account.billingCountry = "USA";
        account.billingStreet = "123 Main St";
        account.billingPostalCode = "94102";
        account.shippingCity = "San Francisco";
        account.shippingState = "CA";
        account.shippingCountry = "USA";
        account.shippingStreet = "123 Main St";
        account.shippingPostalCode = "94102";
        account.customerPriority = "High";
        account.sLAExpirationDate = System.currentTimeMillis();
        account.sLASerialNumber = "SLA-12345";
        account.createdDate = System.currentTimeMillis();

        // Assert
        assertNotNull("All fields should be set", account.id);
        assertNotNull("Name should be set", account.name);
        assertNotNull("Billing city should be set", account.billingCity);
        assertNotNull("Shipping city should be set", account.shippingCity);
    }
}

