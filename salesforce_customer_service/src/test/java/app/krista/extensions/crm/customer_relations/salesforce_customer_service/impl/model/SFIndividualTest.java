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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Individual;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SFIndividual} model class.
 */
public class SFIndividualTest {

    @Test
    public void testFrom_WithValidIndividual_ReturnsPopulatedSFIndividual() {
        Individual individual = new Individual();
        individual.id = "IND-001";
        individual.firstName = "John";
        individual.lastName = "Doe";

        SFIndividual result = SFIndividual.from(individual);

        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", individual.id, result.id);
        assertEquals("First name should match", individual.firstName, result.firstName);
        assertEquals("Last name should match", individual.lastName, result.lastName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullIndividual_ThrowsException() {
        SFIndividual.from((Individual) null);
    }

    @Test
    public void testFrom_WithValidMap_ReturnsPopulatedSFIndividual() {
        Map<String, Object> map = new HashMap<>();
        map.put("First Name", "John");
        map.put("Last Name", "Doe");

        SFIndividual result = SFIndividual.from(map);

        assertNotNull("Result should not be null", result);
        assertEquals("First name should match", "John", result.firstName);
        assertEquals("Last name should match", "Doe", result.lastName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullMap_ThrowsException() {
        SFIndividual.from((Map<String, Object>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithEmptyMap_ThrowsException() {
        SFIndividual.from(new HashMap<>());
    }
}

