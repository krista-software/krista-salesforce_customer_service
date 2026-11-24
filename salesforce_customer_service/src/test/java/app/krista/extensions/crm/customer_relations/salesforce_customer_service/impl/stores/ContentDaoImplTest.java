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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores;

import app.krista.model.base.FreeForm;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ContentDaoImpl#parseJSONToMap(String, List)}.
 *
 * <p>{@code ContentDaoImpl} is a pure JSON-to-{@link FreeForm} mapper with no runtime
 * collaborators, so it is exercised directly with representative and malformed JSON.</p>
 */
public class ContentDaoImplTest {

    private ContentDaoImpl dao;

    @Before
    public void setUp() {
        dao = new ContentDaoImpl();
    }

    @Test
    public void testParseJSONToMap_WithValidRecord_ExtractsRequestedFields() {
        String json = "{\"records\":[{\"Name\":\"Acme\",\"Age\":42,\"Active\":true}]}";
        List<String> fields = Arrays.asList("Name", "Age", "Missing");

        FreeForm result = dao.parseJSONToMap(json, fields);

        assertNotNull(result);
        assertEquals("Only present requested fields are extracted", 2, result.size());
        assertEquals("String value extracted", "Acme", result.get("Name"));
        assertEquals("Numeric value converted to string", "42", result.get("Age"));
        assertFalse("Field not in record is skipped", result.containsKey("Missing"));
        assertFalse("Field not requested is skipped", result.containsKey("Active"));
    }

    @Test
    public void testParseJSONToMap_WithBooleanValue_ConvertsToString() {
        String json = "{\"records\":[{\"Active\":true}]}";

        FreeForm result = dao.parseJSONToMap(json, Collections.singletonList("Active"));

        assertEquals("true", result.get("Active"));
    }

    @Test
    public void testParseJSONToMap_WithMultipleRecords_UsesFirstRecord() {
        String json = "{\"records\":[{\"Name\":\"First\"},{\"Name\":\"Second\"}]}";

        FreeForm result = dao.parseJSONToMap(json, Collections.singletonList("Name"));

        assertEquals("Only the first record is processed", "First", result.get("Name"));
        assertEquals(1, result.size());
    }

    @Test
    public void testParseJSONToMap_WithEmptyRecordsArray_ReturnsEmptyMap() {
        String json = "{\"records\":[]}";

        FreeForm result = dao.parseJSONToMap(json, Collections.singletonList("Name"));

        assertNotNull(result);
        assertTrue("No records means empty map", result.isEmpty());
    }

    @Test
    public void testParseJSONToMap_WithoutRecordsKey_ReturnsEmptyMap() {
        String json = "{\"totalSize\":0,\"done\":true}";

        FreeForm result = dao.parseJSONToMap(json, Collections.singletonList("Name"));

        assertNotNull(result);
        assertTrue("Missing 'records' key means empty map", result.isEmpty());
    }

    @Test
    public void testParseJSONToMap_WithNullResponse_ReturnsEmptyMap() {
        FreeForm result = dao.parseJSONToMap(null, Collections.singletonList("Name"));

        assertNotNull(result);
        assertTrue("Null response means empty map", result.isEmpty());
    }

    @Test
    public void testParseJSONToMap_WithInvalidJson_ReturnsEmptyMap() {
        FreeForm result = dao.parseJSONToMap("not json {", Collections.singletonList("Name"));

        assertNotNull("Should recover from JSONException", result);
        assertTrue("Invalid JSON yields empty map", result.isEmpty());
    }

    @Test
    public void testParseJSONToMap_WithEmptyCustomFields_ReturnsEmptyMap() {
        String json = "{\"records\":[{\"Name\":\"Acme\"}]}";

        FreeForm result = dao.parseJSONToMap(json, Collections.emptyList());

        assertNotNull(result);
        assertTrue("No fields requested means empty map", result.isEmpty());
    }
}
