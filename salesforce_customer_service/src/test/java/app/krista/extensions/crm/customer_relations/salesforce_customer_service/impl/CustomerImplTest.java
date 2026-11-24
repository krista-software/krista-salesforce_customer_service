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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCustomer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CustomerImpl}.
 */
public class CustomerImplTest {

    private SFCustomer sfCustomer;
    private CustomerImpl customerImpl;

    @Before
    public void setUp() {
        sfCustomer = new SFCustomer();
        sfCustomer.id = "001XXXXXXXXXXXXXXX";
        sfCustomer.partyId = "PARTY-001";
        sfCustomer.name = "Acme Corp";
        sfCustomer.customerStatusType = "Active";
        sfCustomer.totalLifeTimeValue = 50000.0;

        customerImpl = new CustomerImpl(sfCustomer);
    }

    @Test
    public void testGetId_ReturnsCorrectValue() {
        assertEquals("ID should match", "001XXXXXXXXXXXXXXX", customerImpl.getId());
    }

    @Test
    public void testGetPartyId_ReturnsCorrectValue() {
        assertEquals("Party ID should match", "PARTY-001", customerImpl.getPartyId());
    }

    @Test
    public void testGetName_ReturnsCorrectValue() {
        assertEquals("Name should match", "Acme Corp", customerImpl.getName());
    }

    @Test
    public void testGetCustomerStatusType_ReturnsCorrectValue() {
        assertEquals("Customer status type should match", "Active", customerImpl.getCustomerStatusType());
    }

    @Test
    public void testGetTotalLifeTimeValue_ReturnsCorrectValue() {
        assertEquals("Total lifetime value should match", Double.valueOf(50000.0), customerImpl.getTotalLifeTimeValue());
    }

    @Test
    public void testGetters_WhenFieldsNull_ReturnNull() {
        CustomerImpl impl = new CustomerImpl(new SFCustomer());
        assertNull("ID should be null", impl.getId());
        assertNull("Party ID should be null", impl.getPartyId());
        assertNull("Name should be null", impl.getName());
        assertNull("Customer status type should be null", impl.getCustomerStatusType());
        assertNull("Total lifetime value should be null", impl.getTotalLifeTimeValue());
    }
}
