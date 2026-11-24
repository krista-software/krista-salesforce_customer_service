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
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for {@link ObjectTypeValidator}.
 * Tests the simple non-null / non-blank validation rule for object type input
 * and the field metadata accessors.
 */
public class ObjectTypeValidatorTest {

    private ObjectTypeValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @Before
    public void setUp() {
        validator = new ObjectTypeValidator();
        context = new HashMap<>();
    }

    // ========== validate() Tests ==========

    @Test
    public void testValidate_WithValidObjectType_ReturnsTrue() {
        assertTrue("Should return true for a non-blank object type",
                validator.validate("Account", context));
    }

    @Test
    public void testValidate_WithAnotherValidObjectType_ReturnsTrue() {
        assertTrue("Should return true for any non-blank input",
                validator.validate("Case", context));
    }

    @Test
    public void testValidate_WithNullObjectType_ReturnsFalse() {
        assertFalse("Should return false for null input",
                validator.validate(null, context));
    }

    @Test
    public void testValidate_WithEmptyObjectType_ReturnsFalse() {
        assertFalse("Should return false for empty input",
                validator.validate("", context));
    }

    @Test
    public void testValidate_WithBlankObjectType_ReturnsFalse() {
        assertFalse("Should return false for whitespace-only input",
                validator.validate("   ", context));
    }

    @Test
    public void testValidate_IgnoresContext() {
        // validate() does not read the context; a null context value should still work
        assertTrue("Validation depends only on the input value",
                validator.validate("Contact", context));
    }

    // ========== Field Metadata Tests ==========

    @Test
    public void testGetFetchFieldName_ReturnsObjectType() {
        assertEquals(SalesforceErrorHandlingResource.OBJECT_TYPE, validator.getFetchFieldName());
    }

    @Test
    public void testGetFieldType_ReturnsPickOne() {
        assertEquals(FieldTypes.PICK_ONE, validator.getFieldType());
    }

    @Test
    public void testGetFetchStepMessage_ReturnsNonEmptyMessage() {
        String message = validator.getFetchStepMessage();
        assertNotNull("Fetch step message should not be null", message);
        assertFalse("Fetch step message should not be empty", message.trim().isEmpty());
        assertTrue("Message should mention object type", message.toLowerCase().contains("object type"));
    }

    @Test
    public void testGetConfirmationStepMessage_ContainsInput() {
        String invalidType = "Widget";

        String message = validator.getConfirmationStepMessage(invalidType, context);

        assertNotNull("Confirmation message should not be null", message);
        assertTrue("Message should contain the invalid object type", message.contains(invalidType));
    }

    @Test
    public void testGetErrMessage_ContainsInput() {
        String invalidType = "Gadget";

        String message = validator.getErrMessage(invalidType);

        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain the invalid object type", message.contains(invalidType));
    }
}
