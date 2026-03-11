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

