package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Contact;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SFContact} model class.
 */
public class SFContactTest {

    @Test
    public void testFrom_WithValidContact_ReturnsPopulatedSFContact() {
        Contact contact = new Contact();
        contact.id = "003XXXXXXXXXXXXXXX";
        contact.name = "John Doe";
        contact.email = "john@example.com";
        contact.phone = "555-1234";

        SFContact result = SFContact.from(contact);

        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", contact.id, result.id);
        assertEquals("First name should be John", "John", result.firstName);
        assertEquals("Last name should be Doe", "Doe", result.lastName);
        assertEquals("Email should match", contact.email, result.email);
        assertEquals("Phone should match", contact.phone, result.phone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullContact_ThrowsException() {
        SFContact.from((Contact) null);
    }

    @Test
    public void testFrom_WithValidMap_ReturnsPopulatedSFContact() {
        Map<String, Object> map = new HashMap<>();
        map.put("Name", "John Doe");
        map.put("Email", "john@example.com");
        map.put("Account Id", "001XXXXXXXXXXXXXXX");

        SFContact result = SFContact.from(map);

        assertNotNull("Result should not be null", result);
        assertEquals("First name should be John", "John", result.firstName);
        assertEquals("Last name should be Doe", "Doe", result.lastName);
        assertEquals("Email should match", "john@example.com", result.email);
        assertEquals("Account ID should match", "001XXXXXXXXXXXXXXX", result.accountId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullMap_ThrowsException() {
        SFContact.from((Map<String, Object>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithEmptyMap_ThrowsException() {
        SFContact.from(new HashMap<>());
    }
}

