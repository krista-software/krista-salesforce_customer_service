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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.RemediationAction;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.ValidationOrchestrator;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ExtensionResponseGenerator}.
 */
public class ExtensionResponseGeneratorTest {

    private ExtensionResponseGenerator generator;
    private Map<String, Object> state;

    @Before
    public void setUp() {
        generator = new ExtensionResponseGenerator();
        state = new HashMap<>();
        state.put("stateKey", "stateValue");
    }

    private ValidationOrchestrator.ValidationResult result(String confirmMsg, String fetchFieldName,
                                                           String fetchStepMsg, String errMsg, String fieldType) {
        return new ValidationOrchestrator.ValidationResult(confirmMsg, fetchFieldName, fetchStepMsg, errMsg, fieldType);
    }

    @Test
    public void testGenerateConfirmationResponse_ReturnsFailureWithAskAction() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("Confirm the case id?", "Case Id", "Provide the case id", "Case id is invalid", FieldTypes.TEXT_FIELD));
        ExtensionResponse.Error.ExceptionType type = ExtensionResponse.Error.ExceptionType.INPUT_ERROR;

        ExtensionResponse response = generator.generateConfirmationResponse(type, results, "subReq", state);

        assertNotNull("Response should not be null", response);
        assertEquals("Result should be FAILURE", ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull("Error should not be null", response.getError());
        assertEquals("Exception type should match", type, response.getError().getExceptionType());
        assertEquals("SubCatalogRequestName should match", "subReq",
                response.getRemediationActions().getSubCatalogRequestName());
        assertEquals("Should have exactly one remediation action", 1,
                response.getRemediationActions().getRemediationActionList().size());
        RemediationAction action = response.getRemediationActions().getRemediationActionList().get(0);
        assertEquals("Ask action should target the active user",
                RemediationAction.RecipientType.ACTIVE_USER, action.getRecipientType());
        assertTrue("Ask message should include the confirm step message",
                action.getMessage().contains("Confirm the case id?"));
        assertEquals("State should be preserved", state, response.getState());
    }

    @Test
    public void testGenerateConfirmationResponse_WithMultipleResults_ConcatenatesMessages() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("First confirm", "F1", "fs1", "err1", FieldTypes.TEXT_FIELD),
                result("Second confirm", "F2", "fs2", "err2", FieldTypes.NUMBER_FIELD));

        ExtensionResponse response = generator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR, results, "subReq", state);

        RemediationAction action = response.getRemediationActions().getRemediationActionList().get(0);
        assertTrue("Message should contain first confirm message", action.getMessage().contains("First confirm"));
        assertTrue("Message should contain second confirm message", action.getMessage().contains("Second confirm"));
    }

    @Test
    public void testGenerateFetchResponse_ReturnsFailureWithErrorMessageAndAskAction() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("confirm", "Case Id", "Please provide a valid Case Id", "Case id is invalid", FieldTypes.TEXT_FIELD));
        ExtensionResponse.Error.ExceptionType type = ExtensionResponse.Error.ExceptionType.INPUT_ERROR;

        ExtensionResponse response = generator.generateFetchResponse(type, results, "fetchSub", state);

        assertEquals("Result should be FAILURE", ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals("Exception type should match", type, response.getError().getExceptionType());
        assertTrue("Error message should include the validation error message",
                response.getError().getErrorMessage().contains("Case id is invalid"));
        assertEquals("SubCatalogRequestName should match", "fetchSub",
                response.getRemediationActions().getSubCatalogRequestName());
        assertEquals("Should have exactly one remediation action", 1,
                response.getRemediationActions().getRemediationActionList().size());
        RemediationAction action = response.getRemediationActions().getRemediationActionList().get(0);
        assertEquals("Ask action should target the active user",
                RemediationAction.RecipientType.ACTIVE_USER, action.getRecipientType());
        assertTrue("Ask message should include the fetch step message",
                action.getMessage().contains("Please provide a valid Case Id"));
        assertEquals("State should be preserved", state, response.getState());
    }

    @Test
    public void testGenerateFetchResponse_WithFileFieldType_BuildsField() {
        // Exercises the FILE_FIELD branch of NamedFieldFactory.createField invoked from generateResponse.
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("confirm", "Attachment", "Upload a file", "File is required", FieldTypes.FILE_FIELD));

        ExtensionResponse response = generator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR, results, "fetchSub", state);

        assertEquals("Result should be FAILURE", ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals("Should have exactly one remediation action", 1,
                response.getRemediationActions().getRemediationActionList().size());
    }

    @Test
    public void testGenerateFetchDenyResponse_WithValidFieldName_InformsAllParticipants() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("confirm", "Case Id", "fetch", "Case id is invalid", FieldTypes.TEXT_FIELD));
        ExtensionResponse.Error.ExceptionType type = ExtensionResponse.Error.ExceptionType.INPUT_ERROR;

        ExtensionResponse response = generator.generateFetchDenyResponse(type, results, "denySub", state);

        assertEquals("Result should be FAILURE", ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals("Exception type should match", type, response.getError().getExceptionType());
        assertTrue("Error message should include the validation error message",
                response.getError().getErrorMessage().contains("Case id is invalid"));
        assertEquals("SubCatalogRequestName should match", "denySub",
                response.getRemediationActions().getSubCatalogRequestName());
        RemediationAction action = response.getRemediationActions().getRemediationActionList().get(0);
        assertEquals("Inform action should target all participants",
                RemediationAction.RecipientType.ALL_PARTICIPANTS, action.getRecipientType());
        assertTrue("Step message should mention Invalid", action.getMessage().contains("Invalid"));
        assertTrue("Step message should quote the field name", action.getMessage().contains("'Case Id'"));
        assertEquals("State should be preserved", state, response.getState());
    }

    @Test
    public void testGenerateFetchDenyResponse_WithNullFieldName_UsesUnknownField() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("confirm", null, "fetch", "Something is invalid", FieldTypes.TEXT_FIELD));

        ExtensionResponse response = generator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR, results, "denySub", state);

        RemediationAction action = response.getRemediationActions().getRemediationActionList().get(0);
        assertTrue("Step message should fall back to Unknown Field",
                action.getMessage().contains("'Unknown Field'"));
    }

    @Test
    public void testGenerateFetchDenyResponse_WithEmptyFieldName_UsesUnknownField() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("confirm", "   ", "fetch", "Blank field name", FieldTypes.TEXT_FIELD));

        ExtensionResponse response = generator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR, results, "denySub", state);

        RemediationAction action = response.getRemediationActions().getRemediationActionList().get(0);
        assertTrue("Blank/whitespace field name should fall back to Unknown Field",
                action.getMessage().contains("'Unknown Field'"));
    }
}
