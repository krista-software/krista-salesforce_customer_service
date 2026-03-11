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
 * Unit tests for {@link Customer} entity class.
 */
public class CustomerTest {

    @Test
    public void testCustomerInstantiation() {
        Customer customer = new Customer();
        assertNotNull("Customer should not be null", customer);
    }

    @Test
    public void testCustomerFieldAssignment() {
        Customer customer = new Customer();
        customer.id = "CUST-001";
        customer.name = "Test Customer";
        customer.partyId = "PARTY-001";

        assertEquals("ID should match", "CUST-001", customer.id);
        assertEquals("Name should match", "Test Customer", customer.name);
        assertEquals("Party ID should match", "PARTY-001", customer.partyId);
    }

    @Test
    public void testCustomerWithNullValues() {
        Customer customer = new Customer();
        customer.id = null;
        customer.name = null;

        assertNull("ID should be null", customer.id);
        assertNull("Name should be null", customer.name);
    }
}

