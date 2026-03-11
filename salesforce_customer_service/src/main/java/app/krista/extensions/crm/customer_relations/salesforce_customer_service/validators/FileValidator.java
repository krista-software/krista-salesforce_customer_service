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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class FileValidator implements Validator {

    private final static Set<String> validFileExtensions = Set.of(
            // Document formats
            "pdf", "doc", "docx", "rtf", "txt",
            // Spreadsheet formats
            "xls", "xlsx", "csv",
            // Presentation formats
            "ppt", "pptx",
            // Image formats
            "png", "jpg", "jpeg", "gif", "bmp", "tif", "tiff",
            // Archive formats
            "zip", "rar"
            // Other common formats - commenting below formats as they are restricted by krista
//            "xml", "html", "htm"
    );

    private final Logger logger = LoggerFactory.getLogger(FileValidator.class);

    @Override
    public Boolean validate(String fileName, Map<ValidationResource, String> context) {
        if (fileName == null || fileName.isEmpty()) {
            return true;
        } else if (validFileExtensions.contains(getFileExtension(fileName).toLowerCase())) {
            return true;
        } else {
            logger.warn("Invalid file provided: {}", fileName);
            return false;
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return Constants.INVALID;
        }

        return fileName.substring(lastDotIndex + 1);
    }

    @Override
    public String getFetchFieldName() {
        return SalesforceErrorHandlingResource.FILE;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.FILE_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please provide a valid file to attach to the case (supported formats: PDF, DOC, DOCX, XLS, XLSX, TXT, PNG, JPG, JPEG).";
    }

    @Override
    public String getConfirmationStepMessage(String fileName, Map<ValidationResource, String> context) {
        return String.format("File validation failed because the file '%s' has an unsupported format. " +
                "Please select a file with a supported format (PDF, DOC, DOCX, XLS, XLSX, TXT, PNG, JPG, JPEG).", fileName);
    }

    @Override
    public String getErrMessage(String fileName) {
        return String.format("File attachment failed because the file '%s' has an unsupported format. " +
                "Only the following file formats are supported: PDF, DOC, DOCX, XLS, XLSX, TXT, PNG, JPG, JPEG. " +
                "Please select a file with a supported format and try again.", fileName);
    }
}