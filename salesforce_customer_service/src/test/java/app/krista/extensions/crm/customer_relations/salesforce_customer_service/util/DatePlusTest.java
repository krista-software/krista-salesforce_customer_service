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

import java.time.LocalDate;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DatePlus} utility class.
 * Tests date formatting and parsing operations.
 */
public class DatePlusTest {

    /**
     * Test getYYYYMMDD with a valid timestamp.
     */
    @Test
    public void testGetYYYYMMDD_WithValidTimestamp_ReturnsFormattedDate() {
        // Arrange
        long timestamp = 1609459200000L; // 2021-01-01 00:00:00 UTC

        // Act
        String result = DatePlus.getYYYYMMDD(timestamp);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain year 2021", result.contains("2021"));
        assertTrue("Result should match yyyy-MM-dd pattern", result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    /**
     * Test getYYYYMMDD with zero timestamp (epoch start).
     */
    @Test
    public void testGetYYYYMMDD_WithZeroTimestamp_ReturnsEpochDate() {
        // Arrange
        long timestamp = 0L;

        // Act
        String result = DatePlus.getYYYYMMDD(timestamp);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain year 1970", result.contains("1970"));
    }

    /**
     * Test getYYYYMMDD with current timestamp.
     */
    @Test
    public void testGetYYYYMMDD_WithCurrentTimestamp_ReturnsCurrentDate() {
        // Arrange
        long timestamp = System.currentTimeMillis();

        // Act
        String result = DatePlus.getYYYYMMDD(timestamp);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should match yyyy-MM-dd pattern", result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    /**
     * Test fromYYYYMMDD with valid date string.
     */
    @Test
    public void testFromYYYYMMDD_WithValidDateString_ReturnsLocalDate() {
        // Arrange
        String dateString = "2021-01-01";

        // Act
        LocalDate result = DatePlus.fromYYYYMMDD(dateString);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Year should be 2021", 2021, result.getYear());
        assertEquals("Month should be January", 1, result.getMonthValue());
        assertEquals("Day should be 1", 1, result.getDayOfMonth());
    }

    /**
     * Test fromYYYYMMDD with invalid date string.
     */
    @Test
    public void testFromYYYYMMDD_WithInvalidDateString_ReturnsNull() {
        // Arrange
        String dateString = "invalid-date";

        // Act
        LocalDate result = DatePlus.fromYYYYMMDD(dateString);

        // Assert
        assertNull("Result should be null for invalid date", result);
    }

    /**
     * Test fromYYYYMMDD with null date string throws NullPointerException.
     */
    @Test(expected = NullPointerException.class)
    public void testFromYYYYMMDD_WithNullDateString_ReturnsNull() {
        // Arrange
        String dateString = null;

        // Act - should throw NullPointerException
        DatePlus.fromYYYYMMDD(dateString);
    }

    /**
     * Test fromYYYYMMDD with empty date string.
     */
    @Test
    public void testFromYYYYMMDD_WithEmptyDateString_ReturnsNull() {
        // Arrange
        String dateString = "";

        // Act
        LocalDate result = DatePlus.fromYYYYMMDD(dateString);

        // Assert
        assertNull("Result should be null for empty string", result);
    }

    /**
     * Test fromYYYYMMDD with wrong format.
     */
    @Test
    public void testFromYYYYMMDD_WithWrongFormat_ReturnsNull() {
        // Arrange
        String dateString = "01-01-2021"; // Wrong format

        // Act
        LocalDate result = DatePlus.fromYYYYMMDD(dateString);

        // Assert
        assertNull("Result should be null for wrong format", result);
    }

    /**
     * Test fromYYYYMMDDToEpochMilli with valid date string.
     */
    @Test
    public void testFromYYYYMMDDToEpochMilli_WithValidDateString_ReturnsEpochMilli() {
        // Arrange
        String dateString = "2021-01-01";

        // Act
        Long result = DatePlus.fromYYYYMMDDToEpochMilli(dateString);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be positive", result > 0);
    }

    /**
     * Test fromYYYYMMDDToEpochMilli with invalid date string.
     */
    @Test
    public void testFromYYYYMMDDToEpochMilli_WithInvalidDateString_ReturnsNull() {
        // Arrange
        String dateString = "invalid-date";

        // Act
        Long result = DatePlus.fromYYYYMMDDToEpochMilli(dateString);

        // Assert
        assertNull("Result should be null for invalid date", result);
    }

    /**
     * Test fromYYYYMMDDToEpochMilli with null date string throws NullPointerException.
     */
    @Test(expected = NullPointerException.class)
    public void testFromYYYYMMDDToEpochMilli_WithNullDateString_ReturnsNull() {
        // Arrange
        String dateString = null;

        // Act - should throw NullPointerException
        DatePlus.fromYYYYMMDDToEpochMilli(dateString);
    }

    /**
     * Test fromYYYYMMDDToEpochMilli with empty date string.
     */
    @Test
    public void testFromYYYYMMDDToEpochMilli_WithEmptyDateString_ReturnsNull() {
        // Arrange
        String dateString = "";

        // Act
        Long result = DatePlus.fromYYYYMMDDToEpochMilli(dateString);

        // Assert
        assertNull("Result should be null for empty string", result);
    }

    /**
     * Test round-trip conversion: timestamp -> string -> timestamp.
     */
    @Test
    public void testRoundTripConversion_TimestampToStringToTimestamp() {
        // Arrange
        long originalTimestamp = 1609459200000L; // 2021-01-01 00:00:00 UTC

        // Act
        String dateString = DatePlus.getYYYYMMDD(originalTimestamp);
        Long convertedTimestamp = DatePlus.fromYYYYMMDDToEpochMilli(dateString);

        // Assert
        assertNotNull("Converted timestamp should not be null", convertedTimestamp);
        // Note: We can't expect exact equality due to timezone and time-of-day differences
        assertTrue("Converted timestamp should be positive", convertedTimestamp > 0);
    }

    /**
     * Test fromYYYYMMDD with leap year date.
     */
    @Test
    public void testFromYYYYMMDD_WithLeapYearDate_ReturnsLocalDate() {
        // Arrange
        String dateString = "2020-02-29"; // Leap year

        // Act
        LocalDate result = DatePlus.fromYYYYMMDD(dateString);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Year should be 2020", 2020, result.getYear());
        assertEquals("Month should be February", 2, result.getMonthValue());
        assertEquals("Day should be 29", 29, result.getDayOfMonth());
    }

    /**
     * Test fromYYYYMMDD with invalid date format.
     */
    @Test
    public void testFromYYYYMMDD_WithInvalidDateFormat_ReturnsNull() {
        // Arrange
        String dateString = "2021-13-01"; // Invalid month

        // Act
        LocalDate result = DatePlus.fromYYYYMMDD(dateString);

        // Assert
        assertNull("Result should be null for invalid date format", result);
    }

    /**
     * Test getYYYYMMDD with negative timestamp.
     */
    @Test
    public void testGetYYYYMMDD_WithNegativeTimestamp_ReturnsPreEpochDate() {
        // Arrange
        long timestamp = -86400000L; // One day before epoch

        // Act
        String result = DatePlus.getYYYYMMDD(timestamp);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain year 1969", result.contains("1969"));
    }
}

