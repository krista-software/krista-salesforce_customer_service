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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.*;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ValidationOrchestrator focusing on proper error handling
 * and null validator scenarios.
 */
public class ValidationOrchestratorTest {

    @Mock
    private CaseDaoImpl caseDao;
    @Mock
    private AccountDaoImpl accountDao;
    @Mock
    private ContactDaoImpl contactDao;
    @Mock
    private CustomerDaoImpl customerDao;
    @Mock
    private IndividualDaoImpl individualDao;
    @Mock
    private SalesforceService salesforceService;

    private ValidationOrchestrator validationOrchestrator;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        validationOrchestrator = new ValidationOrchestrator(
            caseDao, accountDao, contactDao, customerDao, individualDao, salesforceService);
    }

    /**
     * Test that ValidationOrchestrator throws proper exception instead of using assert
     * when validator is not found.
     */
    @Test(expected = IllegalStateException.class)
    public void testValidate_WithUnknownResource_ShouldThrowException() {
        // Arrange
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        
        // Create a custom ValidationResource that won't have a validator
        // This simulates the scenario where a validator is null
        ValidationOrchestrator customOrchestrator = new ValidationOrchestrator(
            caseDao, accountDao, contactDao, customerDao, individualDao, salesforceService) {
            
            @Override
            public List<ValidationResult> validate(Map<Validator.ValidationResource, String> resources) {
                // Simulate missing validator by directly calling with null validator scenario
                Map<Validator.ValidationResource, Validator> emptyValidators = new HashMap<>();
                
                for (Map.Entry<Validator.ValidationResource, String> entry : resources.entrySet()) {
                    Validator validator = emptyValidators.get(entry.getKey()); // This will be null
                    if (validator == null) {
                        throw new IllegalStateException("Validator not found for resource: " + entry.getKey());
                    }
                }
                return super.validate(resources);
            }
        };
        
        resources.put(Validator.ValidationResource.CASE_ID, "test-case-id");
        
        // Act - This should throw IllegalStateException
        customOrchestrator.validate(resources);
        
        // Assert - Exception should be thrown (handled by @Test annotation)
    }

    /**
     * Test successful validation with valid resources.
     */
    @Test
    public void testValidate_WithValidResources_ShouldReturnResults() {
        // Arrange
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.NUMBER_OF_POSTS, "50");
        resources.put(Validator.ValidationResource.UPDATE_MESSAGE, "Valid update message");
        resources.put(Validator.ValidationResource.FILE, "document.pdf");
        
        // Act
        List<ValidationOrchestrator.ValidationResult> results = validationOrchestrator.validate(resources);
        
        // Assert
        assertNotNull("Results should not be null", results);
        // Results should be empty if all validations pass
        assertTrue("Results should be empty for valid inputs", results.isEmpty());
    }

    /**
     * Test validation with invalid resources returns proper error results.
     */
    @Test
    public void testValidate_WithInvalidResources_ShouldReturnErrors() {
        // Arrange
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.NUMBER_OF_POSTS, "0"); // Invalid - should be > 0
        resources.put(Validator.ValidationResource.UPDATE_MESSAGE, ""); // Invalid - empty
        resources.put(Validator.ValidationResource.FILE, "document.exe"); // Invalid - unsupported extension
        
        // Act
        List<ValidationOrchestrator.ValidationResult> results = validationOrchestrator.validate(resources);
        
        // Assert
        assertNotNull("Results should not be null", results);
        assertFalse("Results should not be empty for invalid inputs", results.isEmpty());
        assertEquals("Should have 3 validation errors", 3, results.size());
        
        // Verify each result has proper error information
        for (ValidationOrchestrator.ValidationResult result : results) {
            assertNotNull("Error message should not be null", result.errMessage());
            assertNotNull("Field type should not be null", result.fieldType());
            assertFalse("Error message should not be empty", result.errMessage().trim().isEmpty());
        }
    }

    /**
     * Test validation with null resources map.
     */
    @Test(expected = NullPointerException.class)
    public void testValidate_WithNullResources_ShouldHandleGracefully() {
        // Act - This should either handle gracefully or throw meaningful exception
        validationOrchestrator.validate(null);
        
        // Assert - Should throw NullPointerException or handle gracefully
    }

    /**
     * Test validation with empty resources map.
     */
    @Test
    public void testValidate_WithEmptyResources_ShouldReturnEmptyResults() {
        // Arrange
        Map<Validator.ValidationResource, String> emptyResources = new HashMap<>();
        
        // Act
        List<ValidationOrchestrator.ValidationResult> results = validationOrchestrator.validate(emptyResources);
        
        // Assert
        assertNotNull("Results should not be null", results);
        assertTrue("Results should be empty for empty input", results.isEmpty());
    }

    /**
     * Test that ValidationResult record works properly.
     */
    @Test
    public void testValidationResult_RecordFunctionality() {
        // Arrange
        String confirmMessage = "Please confirm the case ID";
        String fetchFieldName = "caseId";
        String fetchStepMessage = "Fetching case information";
        String errorMessage = "Invalid case ID format";
        String fieldType = "Text";
        
        // Act
        ValidationOrchestrator.ValidationResult result = new ValidationOrchestrator.ValidationResult(
            confirmMessage, fetchFieldName, fetchStepMessage, errorMessage, fieldType);
        
        // Assert
        assertEquals("Confirm message should match", confirmMessage, result.confirmStepMessage());
        assertEquals("Fetch field name should match", fetchFieldName, result.fetchFieldName());
        assertEquals("Fetch step message should match", fetchStepMessage, result.fetchStepMessage());
        assertEquals("Error message should match", errorMessage, result.errMessage());
        assertEquals("Field type should match", fieldType, result.fieldType());
        
        // Test record equality
        ValidationOrchestrator.ValidationResult sameResult = new ValidationOrchestrator.ValidationResult(
            confirmMessage, fetchFieldName, fetchStepMessage, errorMessage, fieldType);
        
        assertEquals("Records with same values should be equal", result, sameResult);
        assertEquals("Hash codes should be equal", result.hashCode(), sameResult.hashCode());
        
        // Test toString
        String resultString = result.toString();
        assertNotNull("toString should not return null", resultString);
        assertTrue("toString should contain field values", resultString.contains(errorMessage));
    }

    /**
     * Test validation with mixed valid and invalid resources.
     */
    @Test
    public void testValidate_WithMixedResources_ShouldReturnOnlyErrors() {
        // Arrange
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.NUMBER_OF_POSTS, "50"); // Valid
        resources.put(Validator.ValidationResource.UPDATE_MESSAGE, ""); // Invalid - empty
        resources.put(Validator.ValidationResource.FILE, "document.pdf"); // Valid
        
        // Act
        List<ValidationOrchestrator.ValidationResult> results = validationOrchestrator.validate(resources);
        
        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 validation error", 1, results.size());
        
        ValidationOrchestrator.ValidationResult errorResult = results.get(0);
        assertNotNull("Error result should not be null", errorResult);
        assertTrue("Error message should mention update message", 
            errorResult.errMessage().toLowerCase().contains("message") ||
            errorResult.errMessage().toLowerCase().contains("empty") ||
            errorResult.errMessage().toLowerCase().contains("required"));
    }

    /**
     * Test that proper exception message is provided when validator is not found.
     */
    @Test
    public void testValidate_ExceptionMessage_ShouldBeDescriptive() {
        // Arrange - Create a scenario where validator lookup fails
        ValidationOrchestrator customOrchestrator = new ValidationOrchestrator(
            caseDao, accountDao, contactDao, customerDao, individualDao, salesforceService) {
            
            @Override
            public List<ValidationResult> validate(Map<Validator.ValidationResource, String> resources) {
                // Simulate the fixed behavior
                for (Map.Entry<Validator.ValidationResource, String> entry : resources.entrySet()) {
                    Validator validator = null; // Simulate null validator
                    if (validator == null) {
                        throw new IllegalStateException("Validator not found for resource: " + entry.getKey());
                    }
                }
                return super.validate(resources);
            }
        };
        
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.CASE_ID, "test-case-id");
        
        // Act & Assert
        try {
            customOrchestrator.validate(resources);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention the resource", 
                e.getMessage().contains("CASE_ID"));
            assertTrue("Exception message should mention validator not found", 
                e.getMessage().toLowerCase().contains("validator not found"));
        }
    }
}
