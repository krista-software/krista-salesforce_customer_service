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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Account;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SFAccount} model class.
 * Tests conversion methods from different source types.
 */
public class SFAccountTest {

    /**
     * Test from(Account) with valid account.
     */
    @Test
    public void testFrom_WithValidAccount_ReturnsPopulatedSFAccount() {
        // Arrange
        Account account = new Account();
        account.id = "001XXXXXXXXXXXXXXX";
        account.name = "Test Account";
        account.accountNumber = "ACC-12345";
        account.phone = "555-1234";
        account.website = "https://test.com";
        account.createdDate = System.currentTimeMillis();

        // Act
        SFAccount result = SFAccount.from(account);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", account.id, result.id);
        assertEquals("Name should match", account.name, result.name);
        assertEquals("Account number should match", account.accountNumber, result.accountNumber);
        assertEquals("Phone should match", account.phone, result.phone);
        assertEquals("Website should match", account.website, result.website);
        assertNotNull("Created date should not be null", result.createdDate);
    }

    /**
     * Test from(Account) with null account throws exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullAccount_ThrowsException() {
        // Act - should throw IllegalArgumentException
        SFAccount.from((Account) null);
    }

    /**
     * Test from(Account) with null phone sets empty string.
     */
    @Test
    public void testFrom_WithNullPhone_SetsEmptyString() {
        // Arrange
        Account account = new Account();
        account.id = "001XXXXXXXXXXXXXXX";
        account.name = "Test Account";
        account.phone = null;

        // Act
        SFAccount result = SFAccount.from(account);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Phone should be empty string", "", result.phone);
    }

    /**
     * Test from(Account) with null createdDate.
     */
    @Test
    public void testFrom_WithNullCreatedDate_HandlesGracefully() {
        // Arrange
        Account account = new Account();
        account.id = "001XXXXXXXXXXXXXXX";
        account.name = "Test Account";
        account.createdDate = null;

        // Act
        SFAccount result = SFAccount.from(account);

        // Assert
        assertNotNull("Result should not be null", result);
        assertNull("Created date should be null", result.createdDate);
    }

    /**
     * Test from(Map) with valid map.
     */
    @Test
    public void testFrom_WithValidMap_ReturnsPopulatedSFAccount() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.ID, "001XXXXXXXXXXXXXXX");
        map.put(Constants.NAME, "Test Account");
        map.put(Constants.ACCOUNT_NUMBER, "ACC-12345");
        map.put(Constants.PHONE, "555-1234");
        map.put(Constants.WEBSITE, "https://test.com");
        map.put(Constants.CREATED_DATE, System.currentTimeMillis());

        // Act
        SFAccount result = SFAccount.from(map);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "Test Account", result.name);
        assertEquals("Account number should match", "ACC-12345", result.accountNumber);
        assertEquals("Phone should match", "555-1234", result.phone);
        assertEquals("Website should match", "https://test.com", result.website);
        assertNotNull("Created date should not be null", result.createdDate);
    }

    /**
     * Test from(Map) with null map throws exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullMap_ThrowsException() {
        // Act - should throw IllegalArgumentException
        SFAccount.from((Map<String, Object>) null);
    }

    /**
     * Test from(Map) with empty map throws exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithEmptyMap_ThrowsException() {
        // Arrange
        Map<String, Object> map = new HashMap<>();

        // Act - should throw IllegalArgumentException
        SFAccount.from(map);
    }

    /**
     * Test from(Map) with null phone sets empty string.
     */
    @Test
    public void testFrom_WithMapNullPhone_SetsEmptyString() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.NAME, "Test Account");
        map.put(Constants.PHONE, null);

        // Act
        SFAccount result = SFAccount.from(map);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Phone should be empty string", "", result.phone);
    }

    /**
     * Test from(Map) with null created date.
     */
    @Test
    public void testFrom_WithMapNullCreatedDate_HandlesGracefully() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.NAME, "Test Account");
        map.put(Constants.CREATED_DATE, null);

        // Act
        SFAccount result = SFAccount.from(map);

        // Assert
        assertNotNull("Result should not be null", result);
        assertNull("Created date should be null", result.createdDate);
    }

    /**
     * Test from(Account) with all fields populated.
     */
    @Test
    public void testFrom_WithAllFieldsPopulated_CopiesAllFields() {
        // Arrange
        Account account = new Account();
        account.id = "001XXXXXXXXXXXXXXX";
        account.name = "Test Account";
        account.accountNumber = "ACC-12345";
        account.phone = "555-1234";
        account.website = "https://test.com";
        account.createdDate = System.currentTimeMillis();
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

        // Act
        SFAccount result = SFAccount.from(account);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", account.id, result.id);
        assertEquals("Name should match", account.name, result.name);
        assertEquals("Type should match", account.type, result.type);
        assertEquals("Fax should match", account.fax, result.fax);
        assertEquals("SIC should match", account.sic, result.sic);
        assertEquals("Owner ID should match", account.ownerId, result.ownerId);
        assertEquals("Created By ID should match", account.createdById, result.createdById);
        assertEquals("Clean Status should match", account.cleanStatus, result.cleanStatus);
        assertEquals("Ownership should match", account.ownership, result.ownership);
        assertEquals("Industry should match", account.industry, result.industry);
        assertEquals("Number of Employees should match", account.numberOfEmployees, result.numberOfEmployees);
        assertEquals("Billing City should match", account.billingCity, result.billingCity);
        assertEquals("Billing State should match", account.billingState, result.billingState);
        assertEquals("Billing Country should match", account.billingCountry, result.billingCountry);
        assertEquals("Billing Street should match", account.billingStreet, result.billingStreet);
        assertEquals("Billing Postal Code should match", account.billingPostalCode, result.billingPostalCode);
        assertEquals("Shipping City should match", account.shippingCity, result.shippingCity);
        assertEquals("Shipping State should match", account.shippingState, result.shippingState);
        assertEquals("Shipping Country should match", account.shippingCountry, result.shippingCountry);
        assertEquals("Shipping Street should match", account.shippingStreet, result.shippingStreet);
        assertEquals("Shipping Postal Code should match", account.shippingPostalCode, result.shippingPostalCode);
    }

    /**
     * Test from(Map) with all fields populated.
     */
    @Test
    public void testFrom_WithMapAllFieldsPopulated_CopiesAllFields() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.NAME, "Test Account");
        map.put(Constants.ACCOUNT_NUMBER, "ACC-12345");
        map.put(Constants.PHONE, "555-1234");
        map.put(Constants.WEBSITE, "https://test.com");
        map.put(Constants.TYPE, "Customer");
        map.put(Constants.FAX, "555-5678");
        map.put(Constants.SIC, "1234");
        map.put(Constants.CLEAN_STATUS, "Clean");
        map.put(Constants.OWNERSHIP, "Private");
        map.put(Constants.INDUSTRY, "Technology");
        map.put(Constants.NUMBER_OF_EMPLOYEES, 100.0);
        map.put(Constants.BILLING_CITY, "San Francisco");
        map.put(Constants.BILLING_STATE, "CA");
        map.put(Constants.BILLING_COUNTRY, "USA");
        map.put(Constants.BILLING_STREET, "123 Main St");
        map.put(Constants.BILLING_POSTAL_CODE, "94102");
        map.put(Constants.SHIPPING_CITY, "San Francisco");
        map.put(Constants.SHIPPING_STATE, "CA");
        map.put(Constants.SHIPPING_COUNTRY, "USA");
        map.put(Constants.SHIPPING_STREET, "123 Main St");
        map.put(Constants.SHIPPING_POSTAL_CODE, "94102");

        // Act
        SFAccount result = SFAccount.from(map);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "Test Account", result.name);
        assertEquals("Type should match", "Customer", result.type);
        assertEquals("Fax should match", "555-5678", result.fax);
        assertEquals("SIC should match", "1234", result.sic);
        assertEquals("Clean Status should match", "Clean", result.cleanStatus);
        assertEquals("Ownership should match", "Private", result.ownership);
        assertEquals("Industry should match", "Technology", result.industry);
        assertEquals("Number of Employees should match", Double.valueOf(100.0), result.numberOfEmployees);
        assertEquals("Billing City should match", "San Francisco", result.billingCity);
        assertEquals("Billing State should match", "CA", result.billingState);
    }
}

