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
 * Unit tests for {@link Case} entity class.
 */
public class CaseTest {

    @Test
    public void testCaseInstantiation() {
        Case caseEntity = new Case();
        assertNotNull("Case should not be null", caseEntity);
    }

    @Test
    public void testCaseFieldAssignment() {
        Case caseEntity = new Case();
        caseEntity.id = "500XXXXXXXXXXXXXXX";
        caseEntity.subject = "Test Case";
        caseEntity.status = "New";
        caseEntity.priority = "High";
        caseEntity.origin = "Web";
        caseEntity.contactId = "003XXXXXXXXXXXXXXX";
        caseEntity.accountId = "001XXXXXXXXXXXXXXX";

        assertEquals("ID should match", "500XXXXXXXXXXXXXXX", caseEntity.id);
        assertEquals("Subject should match", "Test Case", caseEntity.subject);
        assertEquals("Status should match", "New", caseEntity.status);
        assertEquals("Priority should match", "High", caseEntity.priority);
        assertEquals("Origin should match", "Web", caseEntity.origin);
        assertEquals("Contact ID should match", "003XXXXXXXXXXXXXXX", caseEntity.contactId);
        assertEquals("Account ID should match", "001XXXXXXXXXXXXXXX", caseEntity.accountId);
    }

    @Test
    public void testCaseWithNullValues() {
        Case caseEntity = new Case();
        caseEntity.id = null;
        caseEntity.subject = null;

        assertNull("ID should be null", caseEntity.id);
        assertNull("Subject should be null", caseEntity.subject);
    }

    @Test
    public void testCaseWithBooleanField() {
        Case caseEntity = new Case();
        caseEntity.isClosed = true;

        assertTrue("Is closed should be true", caseEntity.isClosed);
    }
}

