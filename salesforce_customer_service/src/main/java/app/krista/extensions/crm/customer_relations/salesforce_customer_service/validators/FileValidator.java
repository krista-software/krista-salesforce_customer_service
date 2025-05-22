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
        return "Please provide valid file.";
    }

    @Override
    public String getConfirmationStepMessage(String fileName, Map<ValidationResource, String> context) {
        return String.format("The provided file %s is not supported", fileName);
    }

    @Override
    public String getErrMessage(String fileName) {
        return String.format("The provided file %s is not supported", fileName);
    }
}