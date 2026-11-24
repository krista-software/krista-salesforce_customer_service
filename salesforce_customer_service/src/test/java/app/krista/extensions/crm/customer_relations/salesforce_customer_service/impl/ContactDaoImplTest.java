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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Contact;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContactDaoImpl}.
 *
 * <p>The DAO delegates all remote interaction to {@link SalesforceService}, which is mocked here.
 * Read methods run SOQL queries (even {@code getContactById} is implemented as a query), so the
 * tests stub realistic Salesforce list JSON payloads and assert on the resulting {@link Contact}
 * objects. Write/delete methods are verified through their interaction with the service.</p>
 */
public class ContactDaoImplTest {

    private static final String CONTACT = "Contact";
    private static final String CONTACT_ID = "003XXXXXXXXXXXXXXX";

    private static final String CONTACT_JSON =
            "{\"Id\":\"" + CONTACT_ID + "\",\"Name\":\"John Doe\",\"Email\":\"john@example.com\"," +
                    "\"Phone\":\"555-0111\",\"Title\":\"Manager\",\"AccountId\":\"001XXX\"," +
                    "\"Account\":{\"Name\":\"Acme Corp\"}}";

    @Mock
    private SalesforceService salesforceService;

    private ContactDaoImpl contactDao;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        contactDao = new ContactDaoImpl(salesforceService);
    }

    // ==================== getContactById ====================

    @Test
    public void testGetContactById_ReturnsContact() throws Exception {
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + CONTACT_JSON + "],\"totalSize\":1,\"done\":true}");

        Contact contact = contactDao.getContactById(CONTACT_ID);

        assertNotNull("Contact should not be null", contact);
        assertEquals("Id should match", CONTACT_ID, contact.getId());
        assertEquals("Name should match", "John Doe", contact.getName());
        assertEquals("Email should match", "john@example.com", contact.getEmail());
        assertEquals("Account name should match", "Acme Corp", contact.getAccountName());
    }

    @Test(expected = RuntimeException.class)
    public void testGetContactById_NullId_NotFound_ThrowsRuntime() throws Exception {
        // Null id enters the guard branch, then the empty result set triggers the not-found path.
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[]}");
        contactDao.getContactById(null);
    }

    @Test(expected = RuntimeException.class)
    public void testGetContactById_NotFound_ThrowsRuntime() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[]}");
        contactDao.getContactById("003NOTFOUND");
    }

    // ==================== getContactsByQuery ====================

    @Test
    public void testGetContactsByQuery_ReturnsContacts() throws Exception {
        String secondContact =
                "{\"Id\":\"003BBB\",\"Name\":\"Jane Smith\",\"Email\":\"jane@example.com\"}";
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + CONTACT_JSON + "," + secondContact + "],\"totalSize\":2,\"done\":true}");

        List<Contact> contacts = contactDao.getContactsByQuery("SELECT Id, Name FROM Contact");

        assertNotNull("Contacts list should not be null", contacts);
        assertEquals("Should return two contacts", 2, contacts.size());
        assertEquals("First contact id should match", CONTACT_ID, contacts.get(0).getId());
        assertEquals("Second contact name should match", "Jane Smith", contacts.get(1).getName());
    }

    @Test
    public void testGetContactsByQuery_EmptyRecords_ReturnsEmptyList() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[],\"totalSize\":0,\"done\":true}");

        List<Contact> contacts = contactDao.getContactsByQuery("SELECT Id FROM Contact");

        assertNotNull("Contacts list should not be null", contacts);
        assertTrue("Contacts list should be empty", contacts.isEmpty());
    }

    @Test
    public void testGetContactsByQuery_BlankQuery_HandlesGuard() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[]}");

        List<Contact> contacts = contactDao.getContactsByQuery("   ");

        assertNotNull("Contacts list should not be null", contacts);
        assertTrue("Contacts list should be empty", contacts.isEmpty());
    }

    // ==================== getContacts (paginated) ====================

    @Test
    public void testGetContacts_ReturnsContacts() throws Exception {
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + CONTACT_JSON + "],\"totalSize\":1,\"done\":true}");

        List<Contact> contacts = contactDao.getContacts(10, 1, "Name", "ASC");

        assertNotNull("Contacts list should not be null", contacts);
        assertEquals("Should return one contact", 1, contacts.size());
        assertEquals("Contact id should match", CONTACT_ID, contacts.get(0).getId());
    }

    // ==================== createContact ====================

    @Test
    public void testCreateContact_ReturnsId() throws Exception {
        Map<String, Object> entity = new HashMap<>();
        entity.put("Name", "John Doe");
        entity.put("Account Id", "001XXX");
        when(salesforceService.createRecord(anyString(), eq(CONTACT)))
                .thenReturn("{\"id\":\"003NEWCONTACT\",\"success\":true}");

        String id = contactDao.createContact(entity);

        assertEquals("Created contact id should match", "003NEWCONTACT", id);
        verify(salesforceService).createRecord(anyString(), eq(CONTACT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateContact_NullMap_ThrowsIllegalArgument() throws Exception {
        contactDao.createContact(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateContact_EmptyMap_ThrowsIllegalArgument() throws Exception {
        contactDao.createContact(new HashMap<>());
    }

    // ==================== updateContact ====================

    @Test
    public void testUpdateContact_InvokesUpdateRecord() throws Exception {
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + CONTACT_JSON + "],\"totalSize\":1,\"done\":true}");

        // Wrap the SAME record the service returns so the computed diff is empty and the test's
        // only observable effect is the delegation to updateRecord. (A bare Mockito mock yields
        // primitive-default Long/Date values that SalesforceService.getDifference cannot stringify.)
        Contact contact = new ContactImpl(Constants.GSON.fromJson(CONTACT_JSON, SFContact.class));

        contactDao.updateContact(contact);

        verify(salesforceService).updateRecord(eq(CONTACT_ID), anyString(), eq(CONTACT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateContact_NullContact_ThrowsIllegalArgument() throws Exception {
        contactDao.updateContact(null);
    }

    // ==================== deleteContactById ====================

    @Test
    public void testDeleteContactById_InvokesDelete() throws Exception {
        contactDao.deleteContactById(CONTACT_ID);

        verify(salesforceService).deleteRecordById(CONTACT_ID, CONTACT);
    }
}
