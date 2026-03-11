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
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for all public static methods in StringHelper utility class.
 * Tests include edge cases: null, empty, invalid inputs, and boundary values.
 * No Mockito used - only real concrete implementations.
 */
public class StringHelperTest {

    // ==================== isStringAvailable() Tests ====================

    @Test
    public void testIsStringAvailable_WithValidString_ReturnsTrue() {
        assertTrue("Valid string should return true", StringHelper.isStringAvailable("Hello World"));
        assertTrue("Single character should return true", StringHelper.isStringAvailable("a"));
        assertTrue("Numeric string should return true", StringHelper.isStringAvailable("123"));
        assertTrue("Special characters should return true", StringHelper.isStringAvailable("!@#$%"));
        assertTrue("String with spaces should return true", StringHelper.isStringAvailable("Hello World"));
    }

    @Test
    public void testIsStringAvailable_WithNullString_ReturnsFalse() {
        assertFalse("Null string should return false", StringHelper.isStringAvailable(null));
    }

    @Test
    public void testIsStringAvailable_WithEmptyString_ReturnsFalse() {
        assertFalse("Empty string should return false", StringHelper.isStringAvailable(""));
    }

    @Test
    public void testIsStringAvailable_WithBlankString_ReturnsFalse() {
        assertFalse("Blank string should return false", StringHelper.isStringAvailable("   "));
        assertFalse("Tab string should return false", StringHelper.isStringAvailable("\t"));
        assertFalse("Newline string should return false", StringHelper.isStringAvailable("\n"));
        assertFalse("Mixed whitespace should return false", StringHelper.isStringAvailable("  \t\n  "));
    }

    // ==================== appendOrderByClause() Tests ====================

    @Test
    public void testAppendOrderByClause_WithValidInputs_AppendsCorrectly() {
        StringBuilder builder = new StringBuilder("SELECT Id FROM Case");
        StringHelper.appendOrderByClause(10, 1, "CreatedDate", "DESC", builder);
        
        String expected = "SELECT Id FROM Case ORDER BY CreatedDate DESC LIMIT 10 OFFSET 0";
        assertEquals("Should append correct ORDER BY clause", expected, builder.toString());
    }

    @Test
    public void testAppendOrderByClause_WithSecondPage_CalculatesOffsetCorrectly() {
        StringBuilder builder = new StringBuilder("SELECT Id FROM Case");
        StringHelper.appendOrderByClause(20, 3, "LastModifiedDate", "ASC", builder);
        
        String expected = "SELECT Id FROM Case ORDER BY LastModifiedDate ASC LIMIT 20 OFFSET 40";
        assertEquals("Should calculate offset correctly for page 3", expected, builder.toString());
    }

    @Test
    public void testAppendOrderByClause_WithZeroPage_UsesZeroOffset() {
        StringBuilder builder = new StringBuilder("SELECT Id FROM Case");
        StringHelper.appendOrderByClause(5, 0, "Name", "DESC", builder);
        
        String expected = "SELECT Id FROM Case ORDER BY Name DESC LIMIT 5 OFFSET 0";
        assertEquals("Should use zero offset for page 0", expected, builder.toString());
    }

    @Test
    public void testAppendOrderByClause_WithNegativePage_UsesZeroOffset() {
        StringBuilder builder = new StringBuilder("SELECT Id FROM Case");
        StringHelper.appendOrderByClause(15, -1, "Status", "ASC", builder);
        
        String expected = "SELECT Id FROM Case ORDER BY Status ASC LIMIT 15 OFFSET 0";
        assertEquals("Should use zero offset for negative page", expected, builder.toString());
    }

    @Test
    public void testAppendOrderByClause_WithLargeNumbers_HandlesCorrectly() {
        StringBuilder builder = new StringBuilder("SELECT Id FROM Case");
        StringHelper.appendOrderByClause(1000, 100, "Id", "DESC", builder);
        
        String expected = "SELECT Id FROM Case ORDER BY Id DESC LIMIT 1000 OFFSET 99000";
        assertEquals("Should handle large numbers correctly", expected, builder.toString());
    }

    @Test
    public void testAppendOrderByClause_WithEmptyBuilder_AppendsCorrectly() {
        StringBuilder builder = new StringBuilder();
        StringHelper.appendOrderByClause(5, 1, "Name", "ASC", builder);
        
        String expected = " ORDER BY Name ASC LIMIT 5 OFFSET 0";
        assertEquals("Should append to empty builder", expected, builder.toString());
    }

    // ==================== setQueryEndIndex() Tests ====================

    @Test
    public void testSetQueryEndIndex_WithTrailingWhere_RemovesWhere() {
        StringBuilder builder = new StringBuilder("SELECT Id FROM Case WHERE");
        StringHelper.setQueryEndIndex(builder);
        
        assertEquals("Should remove trailing WHERE", "SELECT Id FROM Case ", builder.toString());
    }

    @Test
    public void testSetQueryEndIndex_WithTrailingAnd_RemovesAnd() {
        StringBuilder builder = new StringBuilder("SELECT Id FROM Case WHERE Name = 'Test' AND");
        StringHelper.setQueryEndIndex(builder);
        
        assertEquals("Should remove trailing AND", "SELECT Id FROM Case WHERE Name = 'Test'", builder.toString());
    }

    @Test
    public void testSetQueryEndIndex_WithoutTrailingClause_NoChange() {
        StringBuilder builder = new StringBuilder("SELECT Id FROM Case WHERE Name = 'Test'");
        String original = builder.toString();
        StringHelper.setQueryEndIndex(builder);
        
        assertEquals("Should not change query without trailing clause", original, builder.toString());
    }

    @Test
    public void testSetQueryEndIndex_WithEmptyBuilder_NoException() {
        StringBuilder builder = new StringBuilder();
        // Should not throw exception
        StringHelper.setQueryEndIndex(builder);
        assertEquals("Empty builder should remain empty", "", builder.toString());
    }

    @Test
    public void testSetQueryEndIndex_WithShortString_NoException() {
        StringBuilder builder = new StringBuilder("ABC");
        // Should not throw exception even if string is shorter than WHERE/AND
        StringHelper.setQueryEndIndex(builder);
        assertEquals("Short string should remain unchanged", "ABC", builder.toString());
    }

    // ==================== createFeedPostBody() Tests ====================

    @Test
    public void testCreateFeedPostBody_WithValidInputs_CreatesCorrectJson() {
        String result = StringHelper.createFeedPostBody("500XX0000000001", "Test message");
        
        assertTrue("Should contain case ID", result.contains("500XX0000000001"));
        assertTrue("Should contain message", result.contains("Test message"));
        assertTrue("Should contain Text type", result.contains("\"type\": \"Text\""));
        assertTrue("Should contain FeedItem type", result.contains("\"feedElementType\": \"FeedItem\""));
        assertTrue("Should be valid JSON structure", result.startsWith("{") && result.endsWith("}"));
    }

    @Test
    public void testCreateFeedPostBody_WithEmptyMessage_CreatesJson() {
        String result = StringHelper.createFeedPostBody("500XX0000000001", "");
        
        assertTrue("Should handle empty message", result.contains("\"text\": \"\""));
        assertTrue("Should still contain case ID", result.contains("500XX0000000001"));
    }

    @Test
    public void testCreateFeedPostBody_WithSpecialCharacters_CreatesJson() {
        String result = StringHelper.createFeedPostBody("500XX0000000001", "Message with \"quotes\" and 'apostrophes'");
        
        assertTrue("Should handle special characters", result.contains("quotes"));
        assertTrue("Should contain case ID", result.contains("500XX0000000001"));
    }

    @Test
    public void testCreateFeedPostBody_WithNullInputs_CreatesJson() {
        String result = StringHelper.createFeedPostBody(null, null);
        
        assertTrue("Should handle null inputs", result.contains("null"));
        assertTrue("Should be valid JSON structure", result.startsWith("{") && result.endsWith("}"));
    }

    // ==================== createFeedPostBodyForFile() Tests ====================

    @Test
    public void testCreateFeedPostBodyForFile_WithValidInputs_CreatesCorrectJson() {
        String base64 = "SGVsbG8gV29ybGQ=";
        String fileName = "test.pdf";
        String result = StringHelper.createFeedPostBodyForFile(base64, fileName);
        
        String expected = "{\"Title\":\"test.pdf\",\"VersionData\":\"SGVsbG8gV29ybGQ=\",\"PathOnClient\":\"test.pdf\"}";
        assertEquals("Should create correct JSON for file upload", expected, result);
    }

    @Test
    public void testCreateFeedPostBodyForFile_WithEmptyInputs_CreatesJson() {
        String result = StringHelper.createFeedPostBodyForFile("", "");
        
        String expected = "{\"Title\":\"\",\"VersionData\":\"\",\"PathOnClient\":\"\"}";
        assertEquals("Should handle empty inputs", expected, result);
    }

    @Test
    public void testCreateFeedPostBodyForFile_WithNullInputs_CreatesJson() {
        String result = StringHelper.createFeedPostBodyForFile(null, null);
        
        String expected = "{\"Title\":\"null\",\"VersionData\":\"null\",\"PathOnClient\":\"null\"}";
        assertEquals("Should handle null inputs", expected, result);
    }

    @Test
    public void testCreateFeedPostBodyForFile_WithSpecialCharactersInFileName_CreatesJson() {
        String result = StringHelper.createFeedPostBodyForFile("data", "file with spaces & symbols.txt");
        
        assertTrue("Should handle special characters in filename", result.contains("file with spaces & symbols.txt"));
        assertTrue("Should contain the data", result.contains("data"));
    }

    // ==================== createFeedPostBodyForFeedItem() Tests ====================

    @Test
    public void testCreateFeedPostBodyForFeedItem_WithValidInputs_CreatesCorrectJson() {
        String result = StringHelper.createFeedPostBodyForFeedItem("Test body", "500XX0000000001");
        
        String expected = "{\"Body\":\"Test body\",\"ParentId\":\"500XX0000000001\"}";
        assertEquals("Should create correct JSON for FeedItem", expected, result);
    }

    @Test
    public void testCreateFeedPostBodyForFeedItem_WithEmptyInputs_CreatesJson() {
        String result = StringHelper.createFeedPostBodyForFeedItem("", "");
        
        String expected = "{\"Body\":\"\",\"ParentId\":\"\"}";
        assertEquals("Should handle empty inputs", expected, result);
    }

    @Test
    public void testCreateFeedPostBodyForFeedItem_WithNullInputs_CreatesJson() {
        String result = StringHelper.createFeedPostBodyForFeedItem(null, null);
        
        String expected = "{\"Body\":\"null\",\"ParentId\":\"null\"}";
        assertEquals("Should handle null inputs", expected, result);
    }

    // ==================== createFeedPostBodyForFeedAttachment() Tests ====================

    @Test
    public void testCreateFeedPostBodyForFeedAttachment_WithValidInputs_CreatesCorrectJson() {
        String result = StringHelper.createFeedPostBodyForFeedAttachment("0D5XX0000000001", "068XX0000000001", "document.pdf");
        
        String expected = "{\"FeedEntityId\":\"0D5XX0000000001\",\"RecordId\":\"068XX0000000001\",\"Title\":\"document.pdf\",\"Type\":\"CONTENT\"}";
        assertEquals("Should create correct JSON for FeedAttachment", expected, result);
    }

    @Test
    public void testCreateFeedPostBodyForFeedAttachment_WithQuotesInFeedId_RemovesQuotes() {
        String result = StringHelper.createFeedPostBodyForFeedAttachment("\"0D5XX0000000001\"", "\"068XX0000000001\"", "document.pdf");
        
        assertTrue("Should remove quotes from feedId", result.contains("\"FeedEntityId\":\"0D5XX0000000001\""));
        assertTrue("Should remove quotes from recordId", result.contains("\"RecordId\":\"068XX0000000001\""));
    }

    @Test
    public void testCreateFeedPostBodyForFeedAttachment_WithEmptyInputs_CreatesJson() {
        String result = StringHelper.createFeedPostBodyForFeedAttachment("", "", "");
        
        String expected = "{\"FeedEntityId\":\"\",\"RecordId\":\"\",\"Title\":\"\",\"Type\":\"CONTENT\"}";
        assertEquals("Should handle empty inputs", expected, result);
    }

    @Test
    public void testCreateFeedPostBodyForFeedAttachment_WithNullInputs_CreatesJson() {
        String result = StringHelper.createFeedPostBodyForFeedAttachment(null, null, null);
        
        String expected = "{\"FeedEntityId\":\"null\",\"RecordId\":\"null\",\"Title\":\"null\",\"Type\":\"CONTENT\"}";
        assertEquals("Should handle null inputs", expected, result);
    }
}
