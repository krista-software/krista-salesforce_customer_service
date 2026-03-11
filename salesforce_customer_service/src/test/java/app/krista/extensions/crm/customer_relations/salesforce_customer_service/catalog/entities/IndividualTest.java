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
 * Unit tests for {@link Individual} entity class.
 */
public class IndividualTest {

    @Test
    public void testIndividualInstantiation() {
        Individual individual = new Individual();
        assertNotNull("Individual should not be null", individual);
    }

    @Test
    public void testIndividualFieldAssignment() {
        Individual individual = new Individual();
        individual.id = "IND-001";
        individual.firstName = "John";
        individual.lastName = "Doe";

        assertEquals("ID should match", "IND-001", individual.id);
        assertEquals("First name should match", "John", individual.firstName);
        assertEquals("Last name should match", "Doe", individual.lastName);
    }

    @Test
    public void testIndividualWithNullValues() {
        Individual individual = new Individual();
        individual.id = null;
        individual.firstName = null;

        assertNull("ID should be null", individual.id);
        assertNull("First name should be null", individual.firstName);
    }
}

