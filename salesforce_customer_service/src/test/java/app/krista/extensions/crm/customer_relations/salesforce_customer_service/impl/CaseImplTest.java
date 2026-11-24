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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCase;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CaseImpl}.
 */
public class CaseImplTest {

    private SFCase sfCase;
    private CaseImpl caseImpl;

    @Before
    public void setUp() {
        sfCase = new SFCase();
        sfCase.id = "500XXXXXXXXXXXXXXX";
        sfCase.contactId = "003XXXXXXXXXXXXXXX";
        sfCase.accountId = "001XXXXXXXXXXXXXXX";
        sfCase.subject = "Login issue";
        sfCase.isClosed = Boolean.TRUE;
        sfCase.ownerId = "005XXXXXXXXXXXXXXX";
        sfCase.createdDate = new Date();
        sfCase.lastModifiedDate = new Date();
        sfCase.caseNumber = 1001.0;
        sfCase.contactEmail = "john@test.com";
        sfCase.contactPhone = "555-1234";
        sfCase.status = "Open";
        sfCase.reason = "Technical";
        sfCase.origin = "Web";
        sfCase.description = "Cannot log in";
        sfCase.priority = "High";

        SFCase.AttributeClass contact = new SFCase.AttributeClass();
        contact.Name = "John Doe";
        sfCase.Contact = contact;

        SFCase.AttributeClass account = new SFCase.AttributeClass();
        account.Name = "Test Account";
        sfCase.Account = account;

        caseImpl = new CaseImpl(sfCase);
    }

    @Test
    public void testGetId_ReturnsCorrectValue() {
        assertEquals("ID should match", "500XXXXXXXXXXXXXXX", caseImpl.getId());
    }

    @Test
    public void testGetContactId_ReturnsCorrectValue() {
        assertEquals("Contact ID should match", "003XXXXXXXXXXXXXXX", caseImpl.getContactId());
    }

    @Test
    public void testGetAccountId_ReturnsCorrectValue() {
        assertEquals("Account ID should match", "001XXXXXXXXXXXXXXX", caseImpl.getAccountId());
    }

    @Test
    public void testGetContactName_ReturnsCorrectValue() {
        assertEquals("Contact name should match", "John Doe", caseImpl.getContactName());
    }

    @Test
    public void testGetContactName_WhenContactNull_ReturnsNull() {
        sfCase.Contact = null;
        CaseImpl impl = new CaseImpl(sfCase);
        assertNull("Contact name should be null", impl.getContactName());
    }

    @Test
    public void testGetSubject_ReturnsCorrectValue() {
        assertEquals("Subject should match", "Login issue", caseImpl.getSubject());
    }

    @Test
    public void testGetIsClosed_ReturnsCorrectValue() {
        assertEquals("IsClosed should match", Boolean.TRUE, caseImpl.getIsClosed());
    }

    @Test
    public void testGetOwnerId_ReturnsCorrectValue() {
        assertEquals("Owner ID should match", "005XXXXXXXXXXXXXXX", caseImpl.getOwnerId());
    }

    @Test
    public void testGetAccountName_ReturnsCorrectValue() {
        assertEquals("Account name should match", "Test Account", caseImpl.getAccountName());
    }

    @Test
    public void testGetAccountName_WhenAccountNull_ReturnsNull() {
        sfCase.Account = null;
        CaseImpl impl = new CaseImpl(sfCase);
        assertNull("Account name should be null", impl.getAccountName());
    }

    @Test
    public void testGetCreatedDate_ReturnsCorrectValue() {
        assertNotNull("Created date should not be null", caseImpl.getCreatedDate());
        assertEquals("Created date should match", sfCase.createdDate.getTime(), caseImpl.getCreatedDate().longValue());
    }

    @Test
    public void testGetCreatedDate_WithNullDate_ReturnsNull() {
        sfCase.createdDate = null;
        CaseImpl impl = new CaseImpl(sfCase);
        assertNull("Created date should be null", impl.getCreatedDate());
    }

    @Test
    public void testGetLastModifiedDate_ReturnsCorrectValue() {
        assertNotNull("Last modified date should not be null", caseImpl.getLastModifiedDate());
        assertEquals("Last modified date should match", sfCase.lastModifiedDate.getTime(), caseImpl.getLastModifiedDate().longValue());
    }

    @Test
    public void testGetLastModifiedDate_WithNullDate_ReturnsNull() {
        sfCase.lastModifiedDate = null;
        CaseImpl impl = new CaseImpl(sfCase);
        assertNull("Last modified date should be null", impl.getLastModifiedDate());
    }

    @Test
    public void testGetCaseNumber_ReturnsCorrectValue() {
        assertEquals("Case number should match", Double.valueOf(1001.0), caseImpl.getCaseNumber());
    }

    @Test
    public void testGetContactEmail_ReturnsCorrectValue() {
        assertEquals("Contact email should match", "john@test.com", caseImpl.getContactEmail());
    }

    @Test
    public void testGetContactPhone_ReturnsCorrectValue() {
        assertEquals("Contact phone should match", "555-1234", caseImpl.getContactPhone());
    }

    @Test
    public void testGetStatus_ReturnsCorrectValue() {
        assertEquals("Status should match", "Open", caseImpl.getStatus());
    }

    @Test
    public void testGetReason_ReturnsCorrectValue() {
        assertEquals("Reason should match", "Technical", caseImpl.getReason());
    }

    @Test
    public void testGetOrigin_ReturnsCorrectValue() {
        assertEquals("Origin should match", "Web", caseImpl.getOrigin());
    }

    @Test
    public void testGetDescription_ReturnsCorrectValue() {
        assertEquals("Description should match", "Cannot log in", caseImpl.getDescription());
    }

    @Test
    public void testGetPriority_ReturnsCorrectValue() {
        assertEquals("Priority should match", "High", caseImpl.getPriority());
    }
}
