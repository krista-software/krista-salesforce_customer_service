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

