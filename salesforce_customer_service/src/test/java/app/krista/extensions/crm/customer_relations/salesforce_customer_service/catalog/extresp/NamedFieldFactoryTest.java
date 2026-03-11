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

import app.krista.model.field.NamedField;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NamedFieldFactory}.
 */
public class NamedFieldFactoryTest {

    @Test
    public void testCreateSwitchField_WithValidName_ReturnsNamedField() {
        String fieldName = "testSwitch";

        NamedField result = NamedFieldFactory.createSwitchField(fieldName);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }

    @Test
    public void testCreateSwitchField_WithEmptyName_ReturnsNamedField() {
        String fieldName = "";

        NamedField result = NamedFieldFactory.createSwitchField(fieldName);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should be empty", "", result.getName());
    }

    @Test
    public void testCreateField_WithTextFieldType_ReturnsNamedField() {
        String fieldName = "testField";
        String fieldType = FieldTypes.TEXT_FIELD;

        NamedField result = NamedFieldFactory.createField(fieldName, fieldType);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }

    @Test
    public void testCreateField_WithNumberFieldType_ReturnsNamedField() {
        String fieldName = "numberField";
        String fieldType = FieldTypes.NUMBER_FIELD;

        NamedField result = NamedFieldFactory.createField(fieldName, fieldType);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }

    @Test
    public void testCreateField_WithFileFieldType_ReturnsNamedField() {
        String fieldName = "fileField";
        String fieldType = FieldTypes.FILE_FIELD;

        NamedField result = NamedFieldFactory.createField(fieldName, fieldType);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }

    @Test
    public void testCreateField_WithListFieldType_ReturnsNamedField() {
        String fieldName = "listField";
        String fieldType = FieldTypes.LIST_FIELD;

        NamedField result = NamedFieldFactory.createField(fieldName, fieldType);

        assertNotNull("Result should not be null", result);
        assertEquals("Field name should match", fieldName, result.getName());
    }
}

