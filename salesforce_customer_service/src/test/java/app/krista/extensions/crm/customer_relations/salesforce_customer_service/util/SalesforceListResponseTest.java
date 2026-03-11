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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SalesforceListResponse} class.
 * Tests the generic list response wrapper.
 */
public class SalesforceListResponseTest {

    /**
     * Test creating SalesforceListResponse with String records.
     */
    @Test
    public void testSalesforceListResponse_WithStringRecords_Success() {
        // Arrange
        SalesforceListResponse<String> response = new SalesforceListResponse<>();
        List<String> records = Arrays.asList("record1", "record2", "record3");

        // Act
        response.records = records;

        // Assert
        assertNotNull("Records should not be null", response.records);
        assertEquals("Should have 3 records", 3, response.records.size());
        assertEquals("First record should match", "record1", response.records.get(0));
    }

    /**
     * Test creating SalesforceListResponse with Integer records.
     */
    @Test
    public void testSalesforceListResponse_WithIntegerRecords_Success() {
        // Arrange
        SalesforceListResponse<Integer> response = new SalesforceListResponse<>();
        List<Integer> records = Arrays.asList(1, 2, 3, 4, 5);

        // Act
        response.records = records;

        // Assert
        assertNotNull("Records should not be null", response.records);
        assertEquals("Should have 5 records", 5, response.records.size());
        assertEquals("First record should be 1", Integer.valueOf(1), response.records.get(0));
    }

    /**
     * Test creating SalesforceListResponse with empty list.
     */
    @Test
    public void testSalesforceListResponse_WithEmptyList_Success() {
        // Arrange
        SalesforceListResponse<String> response = new SalesforceListResponse<>();
        List<String> records = new ArrayList<>();

        // Act
        response.records = records;

        // Assert
        assertNotNull("Records should not be null", response.records);
        assertEquals("Should have 0 records", 0, response.records.size());
        assertTrue("Records should be empty", response.records.isEmpty());
    }

    /**
     * Test creating SalesforceListResponse with null records.
     */
    @Test
    public void testSalesforceListResponse_WithNullRecords_Success() {
        // Arrange
        SalesforceListResponse<String> response = new SalesforceListResponse<>();

        // Act
        response.records = null;

        // Assert
        assertNull("Records should be null", response.records);
    }

    /**
     * Test creating SalesforceListResponse with custom object.
     */
    @Test
    public void testSalesforceListResponse_WithCustomObject_Success() {
        // Arrange
        SalesforceListResponse<TestObject> response = new SalesforceListResponse<>();
        List<TestObject> records = Arrays.asList(
                new TestObject("id1", "name1"),
                new TestObject("id2", "name2")
        );

        // Act
        response.records = records;

        // Assert
        assertNotNull("Records should not be null", response.records);
        assertEquals("Should have 2 records", 2, response.records.size());
        assertEquals("First record id should match", "id1", response.records.get(0).id);
        assertEquals("First record name should match", "name1", response.records.get(0).name);
    }

    /**
     * Test modifying records after assignment.
     */
    @Test
    public void testSalesforceListResponse_ModifyRecordsAfterAssignment_Success() {
        // Arrange
        SalesforceListResponse<String> response = new SalesforceListResponse<>();
        List<String> records = new ArrayList<>(Arrays.asList("record1", "record2"));
        response.records = records;

        // Act
        response.records.add("record3");

        // Assert
        assertEquals("Should have 3 records after modification", 3, response.records.size());
        assertEquals("Last record should be record3", "record3", response.records.get(2));
    }

    /**
     * Test creating multiple SalesforceListResponse instances.
     */
    @Test
    public void testSalesforceListResponse_MultipleInstances_Independent() {
        // Arrange
        SalesforceListResponse<String> response1 = new SalesforceListResponse<>();
        SalesforceListResponse<String> response2 = new SalesforceListResponse<>();
        
        response1.records = Arrays.asList("a", "b");
        response2.records = Arrays.asList("x", "y", "z");

        // Assert
        assertNotEquals("Responses should have different record counts", 
                response1.records.size(), response2.records.size());
        assertEquals("Response1 should have 2 records", 2, response1.records.size());
        assertEquals("Response2 should have 3 records", 3, response2.records.size());
    }

    /**
     * Test SalesforceListResponse with null elements in list.
     */
    @Test
    public void testSalesforceListResponse_WithNullElementsInList_Success() {
        // Arrange
        SalesforceListResponse<String> response = new SalesforceListResponse<>();
        List<String> records = Arrays.asList("record1", null, "record3");

        // Act
        response.records = records;

        // Assert
        assertNotNull("Records should not be null", response.records);
        assertEquals("Should have 3 records", 3, response.records.size());
        assertNull("Second record should be null", response.records.get(1));
    }

    /**
     * Helper class for testing custom objects.
     */
    private static class TestObject {
        String id;
        String name;

        TestObject(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}

