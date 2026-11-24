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

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.RemediationAction;
import app.krista.model.field.NamedField;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ExtensionResponseFactory}.
 */
public class ExtensionResponseFactoryTest {

    @Test
    public void testCreate_WithValuesMap_ReturnsSuccessResponse() {
        Map<String, Object> values = new HashMap<>();
        values.put("key1", "value1");
        values.put("key2", 42);

        ExtensionResponse result = ExtensionResponseFactory.create(values);

        assertNotNull("Result should not be null", result);
        assertEquals("Result should be SUCCESS", ExtensionResponse.Result.SUCCESS, result.getResult());
        assertEquals("Response value should match the supplied map", values, result.getResponseValue());
        assertNull("Error should be null on success", result.getError());
        assertNull("Remediation actions should be null on success", result.getRemediationActions());
        assertNull("State should be null on success", result.getState());
    }

    @Test
    public void testCreate_WithEmptyValuesMap_ReturnsSuccessResponse() {
        Map<String, Object> values = new HashMap<>();

        ExtensionResponse result = ExtensionResponseFactory.create(values);

        assertEquals("Result should be SUCCESS", ExtensionResponse.Result.SUCCESS, result.getResult());
        assertTrue("Response value should be empty", result.getResponseValue().isEmpty());
    }

    @Test
    public void testCreate_WithException_ReturnsFailureWithInformAction() {
        Exception cause = new RuntimeException("boom");
        String message = "Something went wrong";
        ExtensionResponse.Error.ExceptionType type = ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR;

        ExtensionResponse result = ExtensionResponseFactory.create(cause, message, type);

        assertNotNull("Result should not be null", result);
        assertEquals("Result should be FAILURE", ExtensionResponse.Result.FAILURE, result.getResult());
        assertNotNull("Error should not be null", result.getError());
        assertEquals("Error message should match", message, result.getError().getErrorMessage());
        assertEquals("Exception type should match", type, result.getError().getExceptionType());
        assertNotNull("Remediation actions should not be null", result.getRemediationActions());
        assertEquals("Should have exactly one remediation action", 1,
                result.getRemediationActions().getRemediationActionList().size());
        assertNull("SubCatalogRequestName should be null", result.getRemediationActions().getSubCatalogRequestName());
        assertNotNull("State should not be null", result.getState());
        assertTrue("State should be empty", result.getState().isEmpty());
    }

    @Test
    public void testCreate_WithActionsAndState_ReturnsFailureWithRemediation() {
        String message = "Validation failed";
        ExtensionResponse.Error.ExceptionType type = ExtensionResponse.Error.ExceptionType.INPUT_ERROR;
        List<NamedField> fields = new ArrayList<>();
        fields.add(new NamedField("field1", FieldTypes.TEXT_FIELD, null, null));
        List<RemediationAction> actions = new ArrayList<>();
        actions.add(RemediationActionFactory.createAskAction("Please re-enter", fields));
        String subCatalogRequestName = "mySubRequest";
        Map<String, Object> state = new HashMap<>();
        state.put("stateKey", "stateValue");

        ExtensionResponse result = ExtensionResponseFactory.create(message, type, actions, subCatalogRequestName, state);

        assertEquals("Result should be FAILURE", ExtensionResponse.Result.FAILURE, result.getResult());
        assertEquals("Error message should match", message, result.getError().getErrorMessage());
        assertEquals("Exception type should match", type, result.getError().getExceptionType());
        assertEquals("SubCatalogRequestName should match", subCatalogRequestName,
                result.getRemediationActions().getSubCatalogRequestName());
        assertEquals("Actions list should match", actions,
                result.getRemediationActions().getRemediationActionList());
        assertEquals("State should match", state, result.getState());
    }

    @Test
    public void testCreate_WithMessageOnly_ReturnsFailureWithoutExceptionType() {
        String message = "Simple failure";

        ExtensionResponse result = ExtensionResponseFactory.create(message);

        assertEquals("Result should be FAILURE", ExtensionResponse.Result.FAILURE, result.getResult());
        assertEquals("Error message should match", message, result.getError().getErrorMessage());
        assertNull("Exception type should be null", result.getError().getExceptionType());
        assertNotNull("Remediation actions should not be null", result.getRemediationActions());
        assertNull("Remediation action list should be null", result.getRemediationActions().getRemediationActionList());
        assertNull("SubCatalogRequestName should be null", result.getRemediationActions().getSubCatalogRequestName());
        assertNull("State should be null", result.getState());
    }

    @Test
    public void testCreate_WithMessageAndExceptionType_ReturnsFailure() {
        String message = "Typed failure";
        ExtensionResponse.Error.ExceptionType type = ExtensionResponse.Error.ExceptionType.AUTHENTICATION_ERROR;

        ExtensionResponse result = ExtensionResponseFactory.create(message, type);

        assertEquals("Result should be FAILURE", ExtensionResponse.Result.FAILURE, result.getResult());
        assertEquals("Error message should match", message, result.getError().getErrorMessage());
        assertEquals("Exception type should match", type, result.getError().getExceptionType());
        assertNotNull("Remediation actions should not be null", result.getRemediationActions());
        assertNull("Remediation action list should be null", result.getRemediationActions().getRemediationActionList());
        assertNull("State should be null", result.getState());
    }
}
