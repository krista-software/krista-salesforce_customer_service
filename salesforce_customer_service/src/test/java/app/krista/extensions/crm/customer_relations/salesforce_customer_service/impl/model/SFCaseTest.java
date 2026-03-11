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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Case;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SFCase} model class.
 */
public class SFCaseTest {

    @Test
    public void testFrom_WithValidCase_ReturnsPopulatedSFCase() {
        Case caseEntity = new Case();
        caseEntity.id = "500XXXXXXXXXXXXXXX";
        caseEntity.subject = "Test Case";
        caseEntity.status = "New";
        caseEntity.priority = "High";

        SFCase result = SFCase.from(caseEntity);

        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", caseEntity.id, result.id);
        assertEquals("Subject should match", caseEntity.subject, result.subject);
        assertEquals("Status should match", caseEntity.status, result.status);
        assertEquals("Priority should match", caseEntity.priority, result.priority);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullCase_ThrowsException() {
        SFCase.from((Case) null);
    }

    @Test
    public void testFrom_WithValidMap_ReturnsPopulatedSFCase() {
        Map<String, Object> map = new HashMap<>();
        map.put("Subject", "Test Case");
        map.put("Status", "New");

        SFCase result = SFCase.from(map);

        assertNotNull("Result should not be null", result);
        assertEquals("Subject should match", "Test Case", result.subject);
        assertEquals("Status should match", "New", result.status);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullMap_ThrowsException() {
        SFCase.from((Map<String, Object>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithEmptyMap_ThrowsException() {
        SFCase.from(new HashMap<>());
    }
}

