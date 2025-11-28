package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification;

import app.krista.model.base.FreeForm;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SalesforceXMLCase}.
 */
public class SalesforceXMLCaseTest {

    @Test
    public void testToCaseFreeForm_WithAllFields_ReturnsPopulatedFreeForm() {
        SalesforceXMLCase xmlCase = new SalesforceXMLCase();
        xmlCase.Id = "500XXXXXXXXXXXXXXX";
        xmlCase.ContactId = "003XXXXXXXXXXXXXXX";
        xmlCase.AccountId = "001XXXXXXXXXXXXXXX";
        xmlCase.Subject = "Test Case";
        xmlCase.IsClosed = false;
        xmlCase.OwnerId = "005XXXXXXXXXXXXXXX";
        xmlCase.CreatedDate = System.currentTimeMillis();
        xmlCase.LastModifiedDate = System.currentTimeMillis();
        xmlCase.CaseNumber = 12345.0;
        xmlCase.Status = "New";
        xmlCase.Reason = "Test Reason";
        xmlCase.Origin = "Web";
        xmlCase.Description = "Test Description";
        xmlCase.Priority = "High";

        FreeForm result = xmlCase.toCaseFreeForm();

        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", "500XXXXXXXXXXXXXXX", result.get("Id"));
        assertEquals("ContactId should match", "003XXXXXXXXXXXXXXX", result.get("ContactId"));
        assertEquals("AccountId should match", "001XXXXXXXXXXXXXXX", result.get("AccountId"));
        assertEquals("Subject should match", "Test Case", result.get("Subject"));
        assertEquals("IsClosed should match", false, result.get("IsClosed"));
        assertEquals("Status should match", "New", result.get("Status"));
        assertEquals("Priority should match", "High", result.get("Priority"));
    }

    @Test
    public void testToCaseFreeForm_WithNullFields_HandlesGracefully() {
        SalesforceXMLCase xmlCase = new SalesforceXMLCase();
        xmlCase.Id = "500XXXXXXXXXXXXXXX";

        FreeForm result = xmlCase.toCaseFreeForm();

        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", "500XXXXXXXXXXXXXXX", result.get("Id"));
        assertNull("ContactId should be null", result.get("ContactId"));
    }

    @Test
    public void testFieldAssignment() {
        SalesforceXMLCase xmlCase = new SalesforceXMLCase();
        xmlCase.Id = "500XXXXXXXXXXXXXXX";
        xmlCase.Subject = "Test";

        assertEquals("ID should match", "500XXXXXXXXXXXXXXX", xmlCase.Id);
        assertEquals("Subject should match", "Test", xmlCase.Subject);
    }
}

