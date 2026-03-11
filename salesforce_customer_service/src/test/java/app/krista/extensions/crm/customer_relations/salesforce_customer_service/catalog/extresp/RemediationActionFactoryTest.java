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

import app.krista.extension.executor.RemediationAction;
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RemediationActionFactory}.
 */
public class RemediationActionFactoryTest {

    @Test
    public void testCreateAskAction_WithValidParameters_ReturnsRemediationAction() {
        String message = "Please provide the following information";
        List<NamedField> fields = new ArrayList<>();
        fields.add(new NamedField("field1", FieldTypes.TEXT_FIELD, null, null));

        RemediationAction result = RemediationActionFactory.createAskAction(message, fields);

        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testCreateAskAction_WithEmptyFields_ReturnsRemediationAction() {
        String message = "Test message";
        List<NamedField> fields = new ArrayList<>();

        RemediationAction result = RemediationActionFactory.createAskAction(message, fields);

        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testCreateInformAction_WithValidParameters_ReturnsRemediationAction() {
        String message = "Here is the information";
        List<NamedValuedField> fields = new ArrayList<>();
        fields.add(new NamedValuedField("field1", FieldTypes.TEXT_FIELD, "value1", null, null));

        RemediationAction result = RemediationActionFactory.createInformAction(message, fields);

        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testCreateInformAction_WithEmptyFields_ReturnsRemediationAction() {
        String message = "Test message";
        List<NamedValuedField> fields = new ArrayList<>();

        RemediationAction result = RemediationActionFactory.createInformAction(message, fields);

        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testCreateInformAction_WithMultipleFields_ReturnsRemediationAction() {
        String message = "Multiple fields";
        List<NamedValuedField> fields = new ArrayList<>();
        fields.add(new NamedValuedField("field1", FieldTypes.TEXT_FIELD, "value1", null, null));
        fields.add(new NamedValuedField("field2", FieldTypes.NUMBER_FIELD, 123, null, null));

        RemediationAction result = RemediationActionFactory.createInformAction(message, fields);

        assertNotNull("Result should not be null", result);
    }
}

