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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.*;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CatalogTypes} utility class.
 */
public class CatalogTypesTest {

    @Test(expected = IllegalArgumentException.class)
    public void testFromSFCase_WithNull_ThrowsException() {
        CatalogTypes.fromSFCase(null);
    }

    @Test
    public void testFromSFCase_WithValidCase_ReturnsCase() {
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case sfCase = 
            mock(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case.class);
        
        when(sfCase.getId()).thenReturn("500XXXXXXXXXXXXXXX");
        when(sfCase.getSubject()).thenReturn("Test Case");
        when(sfCase.getStatus()).thenReturn("New");
        when(sfCase.getIsClosed()).thenReturn(false);
        
        Case result = CatalogTypes.fromSFCase(sfCase);
        
        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", "500XXXXXXXXXXXXXXX", result.id);
        assertEquals("Subject should match", "Test Case", result.subject);
        assertEquals("Status should match", "New", result.status);
        assertFalse("Is closed should be false", result.isClosed);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromSFContact_WithNull_ThrowsException() {
        CatalogTypes.fromSFContact(null);
    }

    @Test
    public void testFromSFContact_WithValidContact_ReturnsContact() {
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Contact sfContact = 
            mock(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Contact.class);
        
        when(sfContact.getId()).thenReturn("003XXXXXXXXXXXXXXX");
        when(sfContact.getName()).thenReturn("John Doe");
        when(sfContact.getEmail()).thenReturn("john@example.com");
        when(sfContact.getPhone()).thenReturn("555-1234");
        
        Contact result = CatalogTypes.fromSFContact(sfContact);
        
        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", "003XXXXXXXXXXXXXXX", result.id);
        assertEquals("Name should match", "John Doe", result.name);
        assertEquals("Email should match", "john@example.com", result.email);
        assertEquals("Phone should match", "555-1234", result.phone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromSFAccount_WithNull_ThrowsException() {
        CatalogTypes.fromSFAccount(null);
    }

    @Test
    public void testFromSFAccount_WithValidAccount_ReturnsAccount() {
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Account sfAccount = 
            mock(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Account.class);
        
        when(sfAccount.getId()).thenReturn("001XXXXXXXXXXXXXXX");
        when(sfAccount.getName()).thenReturn("Test Account");
        when(sfAccount.getPhone()).thenReturn("555-1234");
        when(sfAccount.getWebsite()).thenReturn("https://test.com");
        
        Account result = CatalogTypes.fromSFAccount(sfAccount);
        
        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", "001XXXXXXXXXXXXXXX", result.id);
        assertEquals("Name should match", "Test Account", result.name);
        assertEquals("Phone should match", "555-1234", result.phone);
        assertEquals("Website should match", "https://test.com", result.website);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromSFIndividual_WithNull_ThrowsException() {
        CatalogTypes.fromSFIndividual(null);
    }

    @Test
    public void testFromSFIndividual_WithValidIndividual_ReturnsIndividual() {
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual sfIndividual = 
            mock(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual.class);
        
        when(sfIndividual.getId()).thenReturn("IND-001");
        when(sfIndividual.getFirstName()).thenReturn("John");
        when(sfIndividual.getLastName()).thenReturn("Doe");
        
        Individual result = CatalogTypes.fromSFIndividual(sfIndividual);
        
        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", "IND-001", result.id);
        assertEquals("First name should match", "John", result.firstName);
        assertEquals("Last name should match", "Doe", result.lastName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromSFCustomer_WithNull_ThrowsException() {
        CatalogTypes.fromSFCustomer(null);
    }

    @Test
    public void testFromSFCustomer_WithValidCustomer_ReturnsCustomer() {
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer sfCustomer = 
            mock(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer.class);
        
        when(sfCustomer.getId()).thenReturn("CUST-001");
        when(sfCustomer.getName()).thenReturn("Test Customer");
        when(sfCustomer.getPartyId()).thenReturn("PARTY-001");
        
        Customer result = CatalogTypes.fromSFCustomer(sfCustomer);
        
        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", "CUST-001", result.id);
        assertEquals("Name should match", "Test Customer", result.name);
        assertEquals("Party ID should match", "PARTY-001", result.partyId);
    }
}

