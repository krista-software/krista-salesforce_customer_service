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

import app.krista.extension.executor.SearchCondition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CaseQueryBuilder} utility class.
 * Tests SOQL query building for Salesforce Case objects.
 */
public class CaseQueryBuilderTest {

    /**
     * Helper method to create a mock SearchCondition with proper operator toString().
     */
    private SearchCondition createMockCondition(String fieldName, String operatorString, Object operand) {
        SearchCondition condition = mock(SearchCondition.class);
        SearchCondition.Operator operator = mock(SearchCondition.Operator.class);

        when(condition.getFieldName()).thenReturn(fieldName);
        when(condition.getOperator()).thenReturn(operator);
        when(operator.toString()).thenReturn(operatorString);
        when(condition.getOperand()).thenReturn(operand);

        return condition;
    }

    /**
     * Test getQuery with null conditions throws IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetQuery_WithNullConditions_ThrowsException() {
        // Act - should throw IllegalArgumentException
        CaseQueryBuilder.getQuery(null, 10, 0, "CreatedDate", "DESC");
    }

    /**
     * Test getQuery with empty conditions throws IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetQuery_WithEmptyConditions_ThrowsException() {
        // Arrange
        List<SearchCondition> conditions = new ArrayList<>();

        // Act - should throw IllegalArgumentException
        CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");
    }

    /**
     * Test getQuery with valid ID condition.
     */
    @Test
    public void testGetQuery_WithIdCondition_ReturnsQueryWithId() {
        // Arrange
        SearchCondition condition = createMockCondition("Id", "equals", "001XXXXXXXXXXXXXXX");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain SELECT", query.contains("Select"));
        assertTrue("Query should contain FROM Case", query.contains("from Case"));
        assertTrue("Query should contain Id condition", query.contains("Id = '001XXXXXXXXXXXXXXX'"));
    }

    /**
     * Test getQuery with contact ID condition.
     */
    @Test
    public void testGetQuery_WithContactIdCondition_ReturnsQueryWithContactId() {
        // Arrange
        SearchCondition condition = createMockCondition("Contact Id", "equals", "003XXXXXXXXXXXXXXX");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain ContactId condition", query.contains("ContactId = '003XXXXXXXXXXXXXXX'"));
    }

    /**
     * Test getQuery with account ID condition.
     */
    @Test
    public void testGetQuery_WithAccountIdCondition_ReturnsQueryWithAccountId() {
        // Arrange
        SearchCondition condition = createMockCondition("Account Id", "equals", "001YYYYYYYYYYYYYYY");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain AccountId condition", query.contains("AccountId = '001YYYYYYYYYYYYYYY'"));
    }

    /**
     * Test getQuery with contact name condition (LIKE operator).
     */
    @Test
    public void testGetQuery_WithContactNameCondition_ReturnsQueryWithLike() {
        // Arrange
        SearchCondition condition = createMockCondition("Contact Name", "equals", "John Doe");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain Contact.Name LIKE", query.contains("Contact.Name LIKE '%John Doe%'"));
    }

    /**
     * Test getQuery with subject condition.
     */
    @Test
    public void testGetQuery_WithSubjectCondition_ReturnsQueryWithSubject() {
        // Arrange
        SearchCondition condition = createMockCondition("Subject", "equals", "Test Subject");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain Subject LIKE", query.contains("Subject LIKE '%Test Subject%'"));
    }

    /**
     * Test getQuery with status condition.
     */
    @Test
    public void testGetQuery_WithStatusCondition_ReturnsQueryWithStatus() {
        // Arrange
        SearchCondition condition = createMockCondition("Status", "equals", "Open");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain Status condition", query.contains("Status = 'Open'"));
    }

    /**
     * Test getQuery with origin condition.
     */
    @Test
    public void testGetQuery_WithOriginCondition_ReturnsQueryWithOrigin() {
        // Arrange
        SearchCondition condition = createMockCondition("Origin", "equals", "Web");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain Origin condition", query.contains("Origin = 'Web'"));
    }

    /**
     * Test getQuery with case number condition (Double type).
     */
    @Test
    public void testGetQuery_WithCaseNumberCondition_ReturnsQueryWithCaseNumber() {
        // Arrange
        SearchCondition condition = createMockCondition("Case Number", "equals", 12345.0);
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain CaseNumber LIKE", query.contains("CaseNumber LIKE '%12345.0%'"));
    }

    /**
     * Test getQuery with owner ID condition.
     */
    @Test
    public void testGetQuery_WithOwnerIdCondition_ReturnsQueryWithOwnerId() {
        // Arrange
        SearchCondition condition = createMockCondition("Owner Id", "equals", "005XXXXXXXXXXXXXXX");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain OwnerId condition", query.contains("OwnerId = '005XXXXXXXXXXXXXXX'"));
    }

    /**
     * Test getQuery with multiple conditions.
     */
    @Test
    public void testGetQuery_WithMultipleConditions_ReturnsQueryWithAllConditions() {
        // Arrange
        SearchCondition condition1 = createMockCondition("Status", "equals", "Open");
        SearchCondition condition2 = createMockCondition("Origin", "equals", "Web");
        List<SearchCondition> conditions = Arrays.asList(condition1, condition2);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain Status condition", query.contains("Status = 'Open'"));
        assertTrue("Query should contain Origin condition", query.contains("Origin = 'Web'"));
        assertTrue("Query should contain AND", query.contains("AND"));
    }

    /**
     * Test getQuery with null operand returns query without that condition.
     */
    @Test
    public void testGetQuery_WithNullOperand_ReturnsQueryWithoutCondition() {
        // Arrange
        SearchCondition condition = createMockCondition("Status", "equals", null);
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain SELECT", query.contains("Select"));
        // Should not contain Status condition since operand is null
    }

    /**
     * Test getQuery with contact email condition.
     */
    @Test
    public void testGetQuery_WithContactEmailCondition_ReturnsQueryWithEmail() {
        // Arrange
        SearchCondition condition = createMockCondition("Contact Email", "equals", "test@example.com");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain ContactEmail LIKE", query.contains("ContactEmail LIKE '%test@example.com%'"));
    }

    /**
     * Test getQuery with contact phone condition.
     */
    @Test
    public void testGetQuery_WithContactPhoneCondition_ReturnsQueryWithPhone() {
        // Arrange
        SearchCondition condition = createMockCondition("Contact Phone", "equals", "555-1234");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain ContactPhone LIKE", query.contains("ContactPhone LIKE '%555-1234%'"));
    }

    /**
     * Test getQuery with account name condition.
     */
    @Test
    public void testGetQuery_WithAccountNameCondition_ReturnsQueryWithAccountName() {
        // Arrange
        SearchCondition condition = createMockCondition("Account Name", "equals", "Acme Corp");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertNotNull("Query should not be null", query);
        assertTrue("Query should contain Account.Name condition", query.contains("Account.Name = 'Acme Corp'"));
    }

    /**
     * Test getQuery includes all required fields in SELECT clause.
     */
    @Test
    public void testGetQuery_IncludesAllRequiredFields() {
        // Arrange
        SearchCondition condition = createMockCondition("Id", "equals", "001XXXXXXXXXXXXXXX");
        List<SearchCondition> conditions = Arrays.asList(condition);

        // Act
        String query = CaseQueryBuilder.getQuery(conditions, 10, 0, "CreatedDate", "DESC");

        // Assert
        assertTrue("Query should contain Contact.Name", query.contains("Contact.Name"));
        assertTrue("Query should contain ContactId", query.contains("ContactId"));
        assertTrue("Query should contain Account.Name", query.contains("Account.Name"));
        assertTrue("Query should contain Subject", query.contains("Subject"));
        assertTrue("Query should contain Priority", query.contains("Priority"));
        assertTrue("Query should contain Status", query.contains("Status"));
        assertTrue("Query should contain CaseNumber", query.contains("CaseNumber"));
    }
}

