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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test class for {@link CustomFieldsValidator}.
 * Tests the custom-field validation rules:
 * - null / blank input rejected
 * - every field name must end with "__c"
 * - object type must be present in the context
 * - every field must exist in Salesforce (verified via {@link SalesforceService})
 * as well as the field metadata accessors.
 */
public class CustomFieldsValidatorTest {

    @Mock
    private SalesforceService mockSalesforceService;

    private CustomFieldsValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new CustomFieldsValidator(mockSalesforceService);
        context = new HashMap<>();
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Account");
    }

    // ========== Null / Blank Input Tests ==========

    @Test
    public void testValidate_WithNullInput_ReturnsFalse() {
        assertFalse("Should return false for null input",
                validator.validate(null, context));
        verifyNoInteractions(mockSalesforceService);
    }

    @Test
    public void testValidate_WithBlankInput_ReturnsFalse() {
        assertFalse("Should return false for blank input",
                validator.validate("   ", context));
        verifyNoInteractions(mockSalesforceService);
    }

    // ========== Field Name Format Tests ==========

    @Test
    public void testValidate_WithNonCustomField_ReturnsFalse() {
        // "Name" does not end with "__c"
        assertFalse("Should return false when a field does not end with __c",
                validator.validate("Name", context));
        verifyNoInteractions(mockSalesforceService);
    }

    @Test
    public void testValidate_WithOneNonCustomFieldAmongMany_ReturnsFalse() {
        // Second field is not a custom field
        assertFalse("Should return false when any field does not end with __c",
                validator.validate("Region__c, Name", context));
        verifyNoInteractions(mockSalesforceService);
    }

    // ========== Object Type Context Tests ==========

    @Test
    public void testValidate_WithMissingObjectType_ReturnsFalse() {
        context.remove(Validator.ValidationResource.OBJECT_TYPE);

        assertFalse("Should return false when object type is missing from context",
                validator.validate("Region__c", context));
        verifyNoInteractions(mockSalesforceService);
    }

    @Test
    public void testValidate_WithBlankObjectType_ReturnsFalse() {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "   ");

        assertFalse("Should return false when object type is blank",
                validator.validate("Region__c", context));
        verifyNoInteractions(mockSalesforceService);
    }

    // ========== Salesforce Existence Tests ==========

    @Test
    public void testValidate_WhenFieldDoesNotExistInSalesforce_ReturnsFalse() {
        when(mockSalesforceService.checkFieldExistsInSalesforce("Account", "Region__c"))
                .thenReturn(false);

        assertFalse("Should return false when field does not exist in Salesforce",
                validator.validate("Region__c", context));
        verify(mockSalesforceService, times(1))
                .checkFieldExistsInSalesforce("Account", "Region__c");
    }

    @Test
    public void testValidate_WithSingleValidExistingField_ReturnsTrue() {
        when(mockSalesforceService.checkFieldExistsInSalesforce("Account", "Region__c"))
                .thenReturn(true);

        assertTrue("Should return true for a valid, existing custom field",
                validator.validate("Region__c", context));
        verify(mockSalesforceService, times(1))
                .checkFieldExistsInSalesforce("Account", "Region__c");
    }

    @Test
    public void testValidate_WithMultipleValidExistingFields_ReturnsTrue() {
        when(mockSalesforceService.checkFieldExistsInSalesforce("Account", "Region__c"))
                .thenReturn(true);
        when(mockSalesforceService.checkFieldExistsInSalesforce("Account", "Tier__c"))
                .thenReturn(true);

        assertTrue("Should return true when all fields are valid and exist",
                validator.validate("Region__c, Tier__c", context));
        verify(mockSalesforceService, times(1))
                .checkFieldExistsInSalesforce("Account", "Region__c");
        verify(mockSalesforceService, times(1))
                .checkFieldExistsInSalesforce("Account", "Tier__c");
    }

    @Test
    public void testValidate_WithMultipleFields_OneMissingInSalesforce_ReturnsFalse() {
        when(mockSalesforceService.checkFieldExistsInSalesforce("Account", "Region__c"))
                .thenReturn(true);
        when(mockSalesforceService.checkFieldExistsInSalesforce("Account", "Tier__c"))
                .thenReturn(false);

        assertFalse("Should return false when one field does not exist in Salesforce",
                validator.validate("Region__c, Tier__c", context));
    }

    // ========== Field Metadata Tests ==========

    @Test
    public void testGetFetchFieldName_ReturnsCustomFields() {
        assertEquals(SalesforceErrorHandlingResource.CUSTOM_FIELDS, validator.getFetchFieldName());
    }

    @Test
    public void testGetFieldType_ReturnsListField() {
        assertEquals(FieldTypes.LIST_FIELD, validator.getFieldType());
    }

    @Test
    public void testGetFetchStepMessage_ReturnsNonEmptyMessage() {
        String message = validator.getFetchStepMessage();
        assertNotNull("Fetch step message should not be null", message);
        assertFalse("Fetch step message should not be empty", message.trim().isEmpty());
        assertTrue("Message should mention __c", message.contains("__c"));
    }

    @Test
    public void testGetConfirmationStepMessage_ContainsInput() {
        String input = "Region__c";

        String message = validator.getConfirmationStepMessage(input, context);

        assertNotNull("Confirmation message should not be null", message);
        assertTrue("Message should contain the provided field names", message.contains(input));
    }

    @Test
    public void testGetErrMessage_ContainsInput() {
        String input = "BadField";

        String message = validator.getErrMessage(input);

        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain the provided field names", message.contains(input));
    }
}
