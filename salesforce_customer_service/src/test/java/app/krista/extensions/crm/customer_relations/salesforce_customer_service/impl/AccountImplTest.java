package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFAccount;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AccountImpl}.
 */
public class AccountImplTest {

    private SFAccount sfAccount;
    private AccountImpl accountImpl;

    @Before
    public void setUp() {
        sfAccount = new SFAccount();
        sfAccount.id = "001XXXXXXXXXXXXXXX";
        sfAccount.name = "Test Account";
        sfAccount.type = "Customer";
        sfAccount.phone = "555-1234";
        sfAccount.website = "https://test.com";
        sfAccount.fax = "555-5678";
        sfAccount.accountNumber = "ACC-001";
        sfAccount.sic = "1234";
        sfAccount.ownerId = "005XXXXXXXXXXXXXXX";
        sfAccount.createdById = "005XXXXXXXXXXXXXXX";
        sfAccount.cleanStatus = "Clean";
        sfAccount.customerPriority = "High";
        sfAccount.sLASerialNumber = "SLA-001";
        sfAccount.createdDate = new Date();
        sfAccount.billingStreet = "123 Main St";
        sfAccount.billingCity = "San Francisco";
        sfAccount.billingState = "CA";
        sfAccount.billingPostalCode = "94105";
        sfAccount.billingCountry = "USA";
        sfAccount.shippingStreet = "456 Oak Ave";
        sfAccount.shippingCity = "Los Angeles";
        sfAccount.shippingState = "CA";
        sfAccount.shippingPostalCode = "90001";
        sfAccount.shippingCountry = "USA";
        sfAccount.numberOfEmployees = 100.0;
        sfAccount.sLAExpirationDate = System.currentTimeMillis();
        sfAccount.ownership = "Private";
        sfAccount.industry = "Technology";

        accountImpl = new AccountImpl(sfAccount);
    }

    @Test
    public void testGetId_ReturnsCorrectValue() {
        assertEquals("ID should match", "001XXXXXXXXXXXXXXX", accountImpl.getId());
    }

    @Test
    public void testGetName_ReturnsCorrectValue() {
        assertEquals("Name should match", "Test Account", accountImpl.getName());
    }

    @Test
    public void testGetType_ReturnsCorrectValue() {
        assertEquals("Type should match", "Customer", accountImpl.getType());
    }

    @Test
    public void testGetPhone_ReturnsCorrectValue() {
        assertEquals("Phone should match", "555-1234", accountImpl.getPhone());
    }

    @Test
    public void testGetWebsite_ReturnsCorrectValue() {
        assertEquals("Website should match", "https://test.com", accountImpl.getWebsite());
    }

    @Test
    public void testGetFax_ReturnsCorrectValue() {
        assertEquals("Fax should match", "555-5678", accountImpl.getFax());
    }

    @Test
    public void testGetAccountNumber_ReturnsCorrectValue() {
        assertEquals("Account number should match", "ACC-001", accountImpl.getAccountNumber());
    }

    @Test
    public void testGetSic_ReturnsCorrectValue() {
        assertEquals("SIC should match", "1234", accountImpl.getSic());
    }

    @Test
    public void testGetOwnerId_ReturnsCorrectValue() {
        assertEquals("Owner ID should match", "005XXXXXXXXXXXXXXX", accountImpl.getOwnerId());
    }

    @Test
    public void testGetCreatedById_ReturnsCorrectValue() {
        assertEquals("Created by ID should match", "005XXXXXXXXXXXXXXX", accountImpl.getCreatedById());
    }

    @Test
    public void testGetCleanStatus_ReturnsCorrectValue() {
        assertEquals("Clean status should match", "Clean", accountImpl.getCleanStatus());
    }

    @Test
    public void testGetCustomerPriority_ReturnsCorrectValue() {
        assertEquals("Customer priority should match", "High", accountImpl.getCustomerPriority());
    }

    @Test
    public void testGetSLASerialNumber_ReturnsCorrectValue() {
        assertEquals("SLA serial number should match", "SLA-001", accountImpl.getSLASerialNumber());
    }

    @Test
    public void testGetCreatedDate_ReturnsCorrectValue() {
        assertNotNull("Created date should not be null", accountImpl.getCreatedDate());
        assertEquals("Created date should match", sfAccount.createdDate.getTime(), accountImpl.getCreatedDate().longValue());
    }

    @Test
    public void testGetCreatedDate_WithNullDate_ReturnsNull() {
        sfAccount.createdDate = null;
        AccountImpl impl = new AccountImpl(sfAccount);
        assertNull("Created date should be null", impl.getCreatedDate());
    }

    @Test
    public void testGetBillingStreet_ReturnsCorrectValue() {
        assertEquals("Billing street should match", "123 Main St", accountImpl.getBillingStreet());
    }

    @Test
    public void testGetBillingCity_ReturnsCorrectValue() {
        assertEquals("Billing city should match", "San Francisco", accountImpl.getBillingCity());
    }

    @Test
    public void testGetBillingState_ReturnsCorrectValue() {
        assertEquals("Billing state should match", "CA", accountImpl.getBillingState());
    }

    @Test
    public void testGetBillingPostalCode_ReturnsCorrectValue() {
        assertEquals("Billing postal code should match", "94105", accountImpl.getBillingPostalCode());
    }

    @Test
    public void testGetBillingCountry_ReturnsCorrectValue() {
        assertEquals("Billing country should match", "USA", accountImpl.getBillingCountry());
    }

    @Test
    public void testGetShippingStreet_ReturnsCorrectValue() {
        assertEquals("Shipping street should match", "456 Oak Ave", accountImpl.getShippingStreet());
    }

    @Test
    public void testGetShippingCity_ReturnsCorrectValue() {
        assertEquals("Shipping city should match", "Los Angeles", accountImpl.getShippingCity());
    }

    @Test
    public void testGetShippingState_ReturnsCorrectValue() {
        assertEquals("Shipping state should match", "CA", accountImpl.getShippingState());
    }

    @Test
    public void testGetShippingPostalCode_ReturnsCorrectValue() {
        assertEquals("Shipping postal code should match", "90001", accountImpl.getShippingPostalCode());
    }

    @Test
    public void testGetShippingCountry_ReturnsCorrectValue() {
        assertEquals("Shipping country should match", "USA", accountImpl.getShippingCountry());
    }

    @Test
    public void testGetNumberOfEmployees_ReturnsCorrectValue() {
        assertEquals("Number of employees should match", Double.valueOf(100.0), accountImpl.getNumberOfEmployees());
    }

    @Test
    public void testGetSLAExpirationDate_ReturnsCorrectValue() {
        assertNotNull("SLA expiration date should not be null", accountImpl.getSLAExpirationDate());
        assertEquals("SLA expiration date should match", sfAccount.sLAExpirationDate, accountImpl.getSLAExpirationDate());
    }

    @Test
    public void testGetOwnership_ReturnsCorrectValue() {
        assertEquals("Ownership should match", "Private", accountImpl.getOwnership());
    }

    @Test
    public void testGetIndustry_ReturnsCorrectValue() {
        assertEquals("Industry should match", "Technology", accountImpl.getIndustry());
    }
}

