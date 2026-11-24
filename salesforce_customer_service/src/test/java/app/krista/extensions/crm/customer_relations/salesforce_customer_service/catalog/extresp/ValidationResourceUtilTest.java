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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.Validator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ValidationResourceUtil}.
 */
public class ValidationResourceUtilTest {

    @Test
    public void testGenerateCaseValidationMap_WithBothValues() {
        Map<Validator.ValidationResource, String> result =
                ValidationResourceUtil.generateCaseValidationMap("case-1", 5.0);

        assertEquals("Case id should be mapped", "case-1",
                result.get(Validator.ValidationResource.CASE_ID));
        assertEquals("Number of posts should be its string value", "5.0",
                result.get(Validator.ValidationResource.NUMBER_OF_POSTS));
    }

    @Test
    public void testGenerateCaseValidationMap_WithNullCaseId_OmitsCaseId() {
        Map<Validator.ValidationResource, String> result =
                ValidationResourceUtil.generateCaseValidationMap(null, 3.0);

        assertFalse("Case id should be absent when null",
                result.containsKey(Validator.ValidationResource.CASE_ID));
        assertEquals("Number of posts should still be present", "3.0",
                result.get(Validator.ValidationResource.NUMBER_OF_POSTS));
    }

    @Test
    public void testGenerateCaseValidationMap_WithNullNumberOfPosts_MapsNullString() {
        // String.valueOf((Double) null) yields the string "null", which is not a Java null,
        // so the NUMBER_OF_POSTS entry is always added.
        Map<Validator.ValidationResource, String> result =
                ValidationResourceUtil.generateCaseValidationMap("case-2", null);

        assertEquals("Case id should be mapped", "case-2",
                result.get(Validator.ValidationResource.CASE_ID));
        assertTrue("Number of posts key should be present",
                result.containsKey(Validator.ValidationResource.NUMBER_OF_POSTS));
        assertEquals("Number of posts should be the string \"null\"", "null",
                result.get(Validator.ValidationResource.NUMBER_OF_POSTS));
    }

    @Test
    public void testGenerateContentFieldValidationMap_WithAllValues() {
        List<String> customFields = List.of("A__c", "B__c");

        Map<Validator.ValidationResource, String> result =
                ValidationResourceUtil.generateContentFieldValidationMap("obj-1", "Case", customFields);

        assertEquals("Object id should be mapped", "obj-1",
                result.get(Validator.ValidationResource.OBJECT_ID));
        assertEquals("Object type should be mapped", "Case",
                result.get(Validator.ValidationResource.OBJECT_TYPE));
        assertEquals("Custom fields should be comma-joined", "A__c, B__c",
                result.get(Validator.ValidationResource.CUSTOM_FIELDS));
    }

    @Test
    public void testGenerateContentFieldValidationMap_WithAllNulls_ReturnsEmptyMap() {
        Map<Validator.ValidationResource, String> result =
                ValidationResourceUtil.generateContentFieldValidationMap(null, null, null);

        assertTrue("Map should be empty when all inputs are null", result.isEmpty());
    }

    @Test
    public void testGenerateContentFieldValidationMap_WithEmptyCustomFields_OmitsCustomFields() {
        Map<Validator.ValidationResource, String> result =
                ValidationResourceUtil.generateContentFieldValidationMap("obj-2", "Account", new ArrayList<>());

        assertEquals("Object id should be mapped", "obj-2",
                result.get(Validator.ValidationResource.OBJECT_ID));
        assertEquals("Object type should be mapped", "Account",
                result.get(Validator.ValidationResource.OBJECT_TYPE));
        assertFalse("Custom fields should be absent when list is empty",
                result.containsKey(Validator.ValidationResource.CUSTOM_FIELDS));
    }

    @Test
    public void testGenerateContentFieldValidationMap_WithNullObjectIdOnly() {
        Map<Validator.ValidationResource, String> result =
                ValidationResourceUtil.generateContentFieldValidationMap(null, "Contact", List.of("X__c"));

        assertFalse("Object id should be absent when null",
                result.containsKey(Validator.ValidationResource.OBJECT_ID));
        assertEquals("Object type should be mapped", "Contact",
                result.get(Validator.ValidationResource.OBJECT_TYPE));
        assertEquals("Custom fields should be mapped", "X__c",
                result.get(Validator.ValidationResource.CUSTOM_FIELDS));
    }
}
