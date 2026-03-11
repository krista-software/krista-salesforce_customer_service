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

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SFCaseHistory} model class.
 */
public class SFCaseHistoryTest {

    @Test
    public void testToMap_WithEmptyRecords_ReturnsEmptyMap() {
        SFCaseHistory history = new SFCaseHistory();
        history.records = new ArrayList<>();
        
        Map<String, Object> result = history.toMap();
        
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }

    @Test
    public void testToMap_WithNullRecords_ReturnsEmptyMap() {
        SFCaseHistory history = new SFCaseHistory();
        history.records = null;
        
        Map<String, Object> result = history.toMap();
        
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }

    @Test
    public void testToMap_WithSingleRecord_ReturnsMapWithOneEntry() {
        SFCaseHistory history = new SFCaseHistory();
        history.records = new ArrayList<>();
        
        SFCaseHistory.Record record = new SFCaseHistory.Record();
        record.Field = "Status";
        record.NewValue = "Closed";
        record.CreatedDate = new Date();
        
        history.records.add(record);
        
        Map<String, Object> result = history.toMap();
        
        assertNotNull("Result should not be null", result);
        assertEquals("Should have one entry", 1, result.size());
        assertEquals("Should contain Status field", "Closed", result.get("Status"));
    }

    @Test
    public void testToMap_WithMultipleRecordsSameTimestamp_ReturnsAllFields() {
        SFCaseHistory history = new SFCaseHistory();
        history.records = new ArrayList<>();
        
        Date sameDate = new Date();
        
        SFCaseHistory.Record record1 = new SFCaseHistory.Record();
        record1.Field = "Status";
        record1.NewValue = "Closed";
        record1.CreatedDate = sameDate;
        
        SFCaseHistory.Record record2 = new SFCaseHistory.Record();
        record2.Field = "Priority";
        record2.NewValue = "High";
        record2.CreatedDate = sameDate;
        
        history.records.add(record1);
        history.records.add(record2);
        
        Map<String, Object> result = history.toMap();
        
        assertNotNull("Result should not be null", result);
        assertEquals("Should have two entries", 2, result.size());
        assertEquals("Should contain Status field", "Closed", result.get("Status"));
        assertEquals("Should contain Priority field", "High", result.get("Priority"));
    }

    @Test
    public void testToMap_WithMultipleRecordsDifferentTimestamps_ReturnsOnlyLatestTimestamp() {
        SFCaseHistory history = new SFCaseHistory();
        history.records = new ArrayList<>();
        
        Date laterDate = new Date();
        Date earlierDate = new Date(laterDate.getTime() - 10000);
        
        SFCaseHistory.Record record1 = new SFCaseHistory.Record();
        record1.Field = "Status";
        record1.NewValue = "Closed";
        record1.CreatedDate = laterDate;
        
        SFCaseHistory.Record record2 = new SFCaseHistory.Record();
        record2.Field = "Priority";
        record2.NewValue = "High";
        record2.CreatedDate = earlierDate;
        
        history.records.add(record1);
        history.records.add(record2);
        
        Map<String, Object> result = history.toMap();
        
        assertNotNull("Result should not be null", result);
        assertEquals("Should have one entry", 1, result.size());
        assertEquals("Should contain only the latest record", "Closed", result.get("Status"));
        assertNull("Should not contain earlier record", result.get("Priority"));
    }

    @Test
    public void testRecordAttributes_CanBeSet() {
        SFCaseHistory.Record.Attributes attributes = new SFCaseHistory.Record.Attributes();
        attributes.type = "CaseHistory";
        attributes.url = "/services/data/v50.0/sobjects/CaseHistory/017XXXXXXXXXXXXXXX";
        
        assertEquals("Type should match", "CaseHistory", attributes.type);
        assertEquals("URL should match", "/services/data/v50.0/sobjects/CaseHistory/017XXXXXXXXXXXXXXX", attributes.url);
    }

    @Test
    public void testRecord_AllFieldsCanBeSet() {
        SFCaseHistory.Record record = new SFCaseHistory.Record();
        record.Id = "017XXXXXXXXXXXXXXX";
        record.IsDeleted = false;
        record.CreatedById = "005XXXXXXXXXXXXXXX";
        record.CreatedDate = new Date();
        record.Field = "Status";
        record.DataType = "String";
        record.OldValue = "New";
        record.NewValue = "Closed";
        
        assertNotNull("All fields should be set", record.Id);
        assertFalse("IsDeleted should be false", record.IsDeleted);
        assertNotNull("CreatedById should be set", record.CreatedById);
        assertNotNull("CreatedDate should be set", record.CreatedDate);
        assertEquals("Field should match", "Status", record.Field);
        assertEquals("DataType should match", "String", record.DataType);
        assertEquals("OldValue should match", "New", record.OldValue);
        assertEquals("NewValue should match", "Closed", record.NewValue);
    }
}

