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

import app.krista.model.base.File;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StateMapperUtil}.
 */
public class StateMapperUtilTest {

    @Test
    public void testAddGetCaseFeedItemsByCaseIdMetaToMap_WithAllValues() {
        Map<String, Object> result =
                StateMapperUtil.addGetCaseFeedItemsByCaseIdMetaToMap("case-123", 5.0, "state-1");

        assertNotNull("Result should not be null", result);
        assertEquals("Should contain three entries", 3, result.size());
        assertEquals("State id should match", "state-1", result.get(SalesforceErrorHandlingResource.STATE_ID));
        assertEquals("Case id should match", "case-123", result.get(SalesforceErrorHandlingResource.CASE_ID));
        assertEquals("Number of posts should match", 5.0, result.get(SalesforceErrorHandlingResource.NUMBER_OF_POSTS));
    }

    @Test
    public void testAddGetCaseFeedItemsByCaseIdMetaToMap_WithNullOptionalValues() {
        Map<String, Object> result =
                StateMapperUtil.addGetCaseFeedItemsByCaseIdMetaToMap("case-123", null, null);

        assertTrue("State id key should be present", result.containsKey(SalesforceErrorHandlingResource.STATE_ID));
        assertNull("State id value should be null", result.get(SalesforceErrorHandlingResource.STATE_ID));
        assertEquals("Case id should match", "case-123", result.get(SalesforceErrorHandlingResource.CASE_ID));
        assertTrue("Number of posts key should be present",
                result.containsKey(SalesforceErrorHandlingResource.NUMBER_OF_POSTS));
        assertNull("Number of posts value should be null", result.get(SalesforceErrorHandlingResource.NUMBER_OF_POSTS));
    }

    @Test
    public void testAddShareUpdateToCaseMetaToMap_WithNullFile_OmitsFileKey() {
        Map<String, Object> result =
                StateMapperUtil.addShareUpdateToCaseMetaToMap("case-9", "Hello", null, "state-2");

        assertEquals("Should contain three entries when no file", 3, result.size());
        assertEquals("State id should match", "state-2", result.get(SalesforceErrorHandlingResource.STATE_ID));
        assertEquals("Case id should match", "case-9", result.get(SalesforceErrorHandlingResource.CASE_ID));
        assertEquals("Update message should match", "Hello",
                result.get(SalesforceErrorHandlingResource.UPDATE_MESSAGE));
        assertFalse("File key should be absent", result.containsKey(SalesforceErrorHandlingResource.FILE));
    }

    @Test
    public void testAddShareUpdateToCaseMetaToMap_WithFile_IncludesFileKey() {
        File file = new File(1024L, "attachment", "txt", "media-1");

        Map<String, Object> result =
                StateMapperUtil.addShareUpdateToCaseMetaToMap("case-9", "Hello", file, "state-2");

        assertEquals("Should contain four entries when file present", 4, result.size());
        assertTrue("File key should be present", result.containsKey(SalesforceErrorHandlingResource.FILE));
        assertEquals("File value should be a singleton list of the file",
                List.of(file), result.get(SalesforceErrorHandlingResource.FILE));
    }

    @Test
    public void testAddCustomFieldsMetaToMap_WithAllValues() {
        List<String> customFields = List.of("Field__c", "Other__c");

        Map<String, Object> result =
                StateMapperUtil.addCustomFieldsMetaToMap("obj-1", "Case", customFields, "state-3");

        assertEquals("Should contain four entries", 4, result.size());
        assertEquals("State id should match", "state-3", result.get(SalesforceErrorHandlingResource.STATE_ID));
        assertEquals("Object id should match", "obj-1", result.get(SalesforceErrorHandlingResource.OBJECT_ID));
        assertEquals("Object type should match", "Case", result.get(SalesforceErrorHandlingResource.OBJECT_TYPE));
        assertEquals("Custom fields should match", customFields,
                result.get(SalesforceErrorHandlingResource.CUSTOM_FIELDS));
    }

    @Test
    public void testAddCustomFieldsMetaToMap_WithNullValues() {
        Map<String, Object> result =
                StateMapperUtil.addCustomFieldsMetaToMap(null, null, null, null);

        assertEquals("All four keys should still be present", 4, result.size());
        assertNull("Object id value should be null", result.get(SalesforceErrorHandlingResource.OBJECT_ID));
        assertNull("Object type value should be null", result.get(SalesforceErrorHandlingResource.OBJECT_TYPE));
        assertNull("Custom fields value should be null", result.get(SalesforceErrorHandlingResource.CUSTOM_FIELDS));
    }
}
