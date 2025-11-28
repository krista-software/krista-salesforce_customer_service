package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Contact} entity class.
 */
public class ContactTest {

    @Test
    public void testContactInstantiation() {
        Contact contact = new Contact();
        assertNotNull("Contact should not be null", contact);
    }

    @Test
    public void testContactFieldAssignment() {
        Contact contact = new Contact();
        contact.id = "003XXXXXXXXXXXXXXX";
        contact.name = "John Doe";
        contact.email = "john@example.com";
        contact.phone = "555-1234";
        contact.title = "Manager";

        assertEquals("ID should match", "003XXXXXXXXXXXXXXX", contact.id);
        assertEquals("Name should match", "John Doe", contact.name);
        assertEquals("Email should match", "john@example.com", contact.email);
        assertEquals("Phone should match", "555-1234", contact.phone);
        assertEquals("Title should match", "Manager", contact.title);
    }

    @Test
    public void testContactWithNullValues() {
        Contact contact = new Contact();
        contact.id = null;
        contact.name = null;

        assertNull("ID should be null", contact.id);
        assertNull("Name should be null", contact.name);
    }
}

