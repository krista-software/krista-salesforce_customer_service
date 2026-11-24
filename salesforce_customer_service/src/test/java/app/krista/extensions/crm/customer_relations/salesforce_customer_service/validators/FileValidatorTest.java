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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for {@link FileValidator}.
 * Tests file-name extension validation:
 * - null / empty file names are treated as valid (no file provided)
 * - supported extensions pass (case-insensitive)
 * - unsupported extensions and missing extensions fail
 * as well as the field metadata accessors.
 */
public class FileValidatorTest {

    private FileValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @Before
    public void setUp() {
        validator = new FileValidator();
        context = new HashMap<>();
    }

    // ========== No-File Tests (treated as valid) ==========

    @Test
    public void testValidate_WithNullFileName_ReturnsTrue() {
        assertTrue("Null file name means no file provided and should be valid",
                validator.validate(null, context));
    }

    @Test
    public void testValidate_WithEmptyFileName_ReturnsTrue() {
        assertTrue("Empty file name means no file provided and should be valid",
                validator.validate("", context));
    }

    // ========== Supported Extension Tests ==========

    @Test
    public void testValidate_WithPdfExtension_ReturnsTrue() {
        assertTrue("PDF is a supported format", validator.validate("report.pdf", context));
    }

    @Test
    public void testValidate_WithDocxExtension_ReturnsTrue() {
        assertTrue("DOCX is a supported format", validator.validate("letter.docx", context));
    }

    @Test
    public void testValidate_WithXlsxExtension_ReturnsTrue() {
        assertTrue("XLSX is a supported format", validator.validate("data.xlsx", context));
    }

    @Test
    public void testValidate_WithImageExtension_ReturnsTrue() {
        assertTrue("PNG is a supported format", validator.validate("image.png", context));
    }

    @Test
    public void testValidate_WithUpperCaseExtension_ReturnsTrue() {
        assertTrue("Extension matching should be case-insensitive",
                validator.validate("REPORT.PDF", context));
    }

    @Test
    public void testValidate_WithMixedCaseExtension_ReturnsTrue() {
        assertTrue("Extension matching should be case-insensitive",
                validator.validate("photo.JpG", context));
    }

    @Test
    public void testValidate_WithMultipleDotsUsesLastExtension_ReturnsTrue() {
        assertTrue("The last extension segment should be used",
                validator.validate("archive.backup.zip", context));
    }

    // ========== Unsupported / Missing Extension Tests ==========

    @Test
    public void testValidate_WithUnsupportedExtension_ReturnsFalse() {
        assertFalse("EXE is not a supported format", validator.validate("malware.exe", context));
    }

    @Test
    public void testValidate_WithNoExtension_ReturnsFalse() {
        assertFalse("A file with no extension should be rejected",
                validator.validate("README", context));
    }

    @Test
    public void testValidate_WithTrailingDot_ReturnsFalse() {
        assertFalse("A file name ending with a dot has no extension and should be rejected",
                validator.validate("file.", context));
    }

    // ========== Field Metadata Tests ==========

    @Test
    public void testGetFetchFieldName_ReturnsFile() {
        assertEquals(SalesforceErrorHandlingResource.FILE, validator.getFetchFieldName());
    }

    @Test
    public void testGetFieldType_ReturnsFileFieldType() {
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
    }

    @Test
    public void testGetFetchStepMessage_ReturnsNonEmptyMessage() {
        String message = validator.getFetchStepMessage();
        assertNotNull("Fetch step message should not be null", message);
        assertFalse("Fetch step message should not be empty", message.trim().isEmpty());
        assertTrue("Message should mention file", message.toLowerCase().contains("file"));
    }

    @Test
    public void testGetConfirmationStepMessage_ContainsFileName() {
        String fileName = "malware.exe";

        String message = validator.getConfirmationStepMessage(fileName, context);

        assertNotNull("Confirmation message should not be null", message);
        assertTrue("Message should contain the file name", message.contains(fileName));
    }

    @Test
    public void testGetErrMessage_ContainsFileName() {
        String fileName = "bad.file.tar";

        String message = validator.getErrMessage(fileName);

        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain the file name", message.contains(fileName));
    }
}
