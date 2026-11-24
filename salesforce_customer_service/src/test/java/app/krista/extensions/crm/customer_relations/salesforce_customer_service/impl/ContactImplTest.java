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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFContact;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ContactImpl}.
 */
public class ContactImplTest {

    private SFContact sfContact;
    private ContactImpl contactImpl;

    @Before
    public void setUp() {
        sfContact = new SFContact();
        sfContact.name = "John Doe";
        sfContact.title = "Manager";
        sfContact.reportsToId = "003XXXXXXXXXXXXXXX";
        sfContact.id = "003YYYYYYYYYYYYYYY";
        sfContact.accountId = "001XXXXXXXXXXXXXXX";
        sfContact.createdDate = new Date();
        sfContact.lastModifiedDate = new Date();
        sfContact.department = "Support";
        sfContact.phone = "555-1234";
        sfContact.description = "Primary contact";
        sfContact.email = "john@test.com";
        sfContact.firstName = "John";
        sfContact.lastName = "Doe";
        sfContact.account = new SFContact.Account("Acme Corp");

        contactImpl = new ContactImpl(sfContact);
    }

    @Test
    public void testGetName_ReturnsCorrectValue() {
        assertEquals("Name should match", "John Doe", contactImpl.getName());
    }

    @Test
    public void testGetTitle_ReturnsCorrectValue() {
        assertEquals("Title should match", "Manager", contactImpl.getTitle());
    }

    @Test
    public void testGetReportsToId_ReturnsCorrectValue() {
        assertEquals("Reports to ID should match", "003XXXXXXXXXXXXXXX", contactImpl.getReportsToId());
    }

    @Test
    public void testGetId_ReturnsCorrectValue() {
        assertEquals("ID should match", "003YYYYYYYYYYYYYYY", contactImpl.getId());
    }

    @Test
    public void testGetAccountId_ReturnsCorrectValue() {
        assertEquals("Account ID should match", "001XXXXXXXXXXXXXXX", contactImpl.getAccountId());
    }

    @Test
    public void testGetCreatedDate_ReturnsCorrectValue() {
        assertNotNull("Created date should not be null", contactImpl.getCreatedDate());
        assertEquals("Created date should match", sfContact.createdDate.getTime(), contactImpl.getCreatedDate().longValue());
    }

    @Test
    public void testGetCreatedDate_WithNullDate_ReturnsNull() {
        sfContact.createdDate = null;
        ContactImpl impl = new ContactImpl(sfContact);
        assertNull("Created date should be null", impl.getCreatedDate());
    }

    @Test
    public void testGetAccountName_ReturnsCorrectValue() {
        assertEquals("Account name should match", "Acme Corp", contactImpl.getAccountName());
    }

    @Test
    public void testGetAccountName_WhenAccountNull_ReturnsNull() {
        sfContact.account = null;
        ContactImpl impl = new ContactImpl(sfContact);
        assertNull("Account name should be null", impl.getAccountName());
    }

    @Test
    public void testGetLastModifiedDate_ReturnsCorrectValue() {
        assertNotNull("Last modified date should not be null", contactImpl.getLastModifiedDate());
        assertEquals("Last modified date should match", sfContact.lastModifiedDate.getTime(), contactImpl.getLastModifiedDate().longValue());
    }

    @Test
    public void testGetLastModifiedDate_WithNullDate_ReturnsNull() {
        sfContact.lastModifiedDate = null;
        ContactImpl impl = new ContactImpl(sfContact);
        assertNull("Last modified date should be null", impl.getLastModifiedDate());
    }

    @Test
    public void testGetDepartment_ReturnsCorrectValue() {
        assertEquals("Department should match", "Support", contactImpl.getDepartment());
    }

    @Test
    public void testGetPhone_ReturnsCorrectValue() {
        assertEquals("Phone should match", "555-1234", contactImpl.getPhone());
    }

    @Test
    public void testGetDescription_ReturnsCorrectValue() {
        assertEquals("Description should match", "Primary contact", contactImpl.getDescription());
    }

    @Test
    public void testGetEmail_ReturnsCorrectValue() {
        assertEquals("Email should match", "john@test.com", contactImpl.getEmail());
    }

    @Test
    public void testGetFirstName_ReturnsCorrectValue() {
        assertEquals("First name should match", "John", contactImpl.getFirstName());
    }

    @Test
    public void testGetLastName_ReturnsCorrectValue() {
        assertEquals("Last name should match", "Doe", contactImpl.getLastName());
    }
}
