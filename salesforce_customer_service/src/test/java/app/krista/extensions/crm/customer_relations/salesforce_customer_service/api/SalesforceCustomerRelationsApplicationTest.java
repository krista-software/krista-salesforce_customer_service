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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.api;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SalesforceCustomerRelationsApplication}.
 */
public class SalesforceCustomerRelationsApplicationTest {

    @Test
    public void testGetClasses_ReturnsAuthenticationResource() {
        SalesforceCustomerRelationsApplication app = new SalesforceCustomerRelationsApplication();
        
        Set<Class<?>> classes = app.getClasses();
        
        assertNotNull("Classes should not be null", classes);
        assertEquals("Should have one class", 1, classes.size());
        assertTrue("Should contain AuthenticationResource", 
            classes.contains(AuthenticationResource.class));
    }

    @Test
    public void testApplicationInstantiation() {
        SalesforceCustomerRelationsApplication app = new SalesforceCustomerRelationsApplication();
        assertNotNull("Application should not be null", app);
    }
}

