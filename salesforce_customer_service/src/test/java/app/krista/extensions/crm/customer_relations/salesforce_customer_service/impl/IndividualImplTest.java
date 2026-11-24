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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFIndividual;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link IndividualImpl}.
 */
public class IndividualImplTest {

    private SFIndividual sfIndividual;
    private IndividualImpl individualImpl;

    @Before
    public void setUp() {
        sfIndividual = new SFIndividual();
        sfIndividual.id = "0PKXXXXXXXXXXXXXXX";
        sfIndividual.firstName = "John";
        sfIndividual.lastName = "Doe";
        sfIndividual.birthDate = "1990-05-15";
        sfIndividual.createdById = "005XXXXXXXXXXXXXXX";
        sfIndividual.createdDate = new Date();
        sfIndividual.lastModifiedDate = new Date();

        individualImpl = new IndividualImpl(sfIndividual);
    }

    @Test
    public void testGetId_ReturnsCorrectValue() {
        assertEquals("ID should match", "0PKXXXXXXXXXXXXXXX", individualImpl.getId());
    }

    @Test
    public void testGetFirstName_ReturnsCorrectValue() {
        assertEquals("First name should match", "John", individualImpl.getFirstName());
    }

    @Test
    public void testGetLastName_ReturnsCorrectValue() {
        assertEquals("Last name should match", "Doe", individualImpl.getLastName());
    }

    @Test
    public void testGetBirthDate_ReturnsCorrectValue() {
        long expected = LocalDate.parse("1990-05-15")
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        assertNotNull("Birth date should not be null", individualImpl.getBirthDate());
        assertEquals("Birth date should match", expected, individualImpl.getBirthDate().longValue());
    }

    @Test
    public void testGetBirthDate_WithNullDate_ReturnsNull() {
        sfIndividual.birthDate = null;
        IndividualImpl impl = new IndividualImpl(sfIndividual);
        assertNull("Birth date should be null", impl.getBirthDate());
    }

    @Test
    public void testGetCreatedDate_ReturnsCorrectValue() {
        assertNotNull("Created date should not be null", individualImpl.getCreatedDate());
        assertEquals("Created date should match", sfIndividual.createdDate.getTime(), individualImpl.getCreatedDate().longValue());
    }

    @Test
    public void testGetCreatedDate_WithNullDate_ReturnsNull() {
        sfIndividual.createdDate = null;
        IndividualImpl impl = new IndividualImpl(sfIndividual);
        assertNull("Created date should be null", impl.getCreatedDate());
    }

    @Test
    public void testGetCreatedById_ReturnsCorrectValue() {
        assertEquals("Created by ID should match", "005XXXXXXXXXXXXXXX", individualImpl.getCreatedById());
    }

    @Test
    public void testGetLastModifiedDate_ReturnsCorrectValue() {
        assertNotNull("Last modified date should not be null", individualImpl.getLastModifiedDate());
        assertEquals("Last modified date should match", sfIndividual.lastModifiedDate.getTime(), individualImpl.getLastModifiedDate().longValue());
    }

    @Test
    public void testGetLastModifiedDate_WithNullDate_ReturnsNull() {
        sfIndividual.lastModifiedDate = null;
        IndividualImpl impl = new IndividualImpl(sfIndividual);
        assertNull("Last modified date should be null", impl.getLastModifiedDate());
    }
}
