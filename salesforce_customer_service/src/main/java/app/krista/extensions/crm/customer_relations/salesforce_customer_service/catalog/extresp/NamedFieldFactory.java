package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp;

import app.krista.model.field.NamedField;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating NamedField objects.
 * <p>
 * This class provides utility methods to create and manipulate NamedField objects
 * used in the Salesforce Customer Service extension. NamedFields are used to represent
 * form fields with different types and configurations.
 * <p>
 * The factory encapsulates the complexity of creating properly formatted NamedField objects
 * with appropriate metadata and ensures consistency across the application.
 */
public class NamedFieldFactory {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private NamedFieldFactory() {
    }

    /**
     * Creates a switch field with the specified name.
     * <p>
     * Switch fields are toggle fields that can be turned on or off.
     *
     * @param fieldName The name of the switch field
     * @return A NamedField object configured as a switch field
     */
    public static NamedField createSwitchField(String fieldName) {
        return new NamedField(fieldName, FieldTypes.SWITCH_FIELD, Map.of(), Map.of());
    }

    /**
     * Creates a field with the specified name and type.
     * <p>
     * This method handles special configuration for different field types:
     * - File fields: Configured with file upload settings
     * - List fields: Configured to hold text fields
     * - Other fields: Created with default configuration
     *
     * @param fieldName The name of the field
     * @param fieldType The type of the field (from FieldTypes)
     * @return A NamedField object configured according to the specified type
     */
    public static NamedField createField(String fieldName, String fieldType) {
        if (fieldType.equalsIgnoreCase(FieldTypes.FILE_FIELD)) {
            // Create the Map<String, Object> to represent the JSON structure
            Map<String, Object> fileUploadConfig = new HashMap<>();

            fileUploadConfig.put("visualWidth", "S");
            fileUploadConfig.put("multipleFileUpload", false);
            fileUploadConfig.put("minFileSizeUnit", "KB");
            fileUploadConfig.put("maxFileSizeUnit", "MB");
            fileUploadConfig.put("fileTypes", "[\"Document\",\"Image gallery\",\"Camera\",\"Contact\",\"Audio\"]");

            return new NamedField(fieldName, FieldTypes.FILE_FIELD, fileUploadConfig, Map.of());
        }

        if (fieldType.equals(FieldTypes.LIST_FIELD)) {
            Map<String, Object> innerAttributes = new HashMap<>(); // optional config

            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("type", "Field");
            fieldMap.put("fieldType", "com.krista.fields.Text");
            fieldMap.put("attributes", innerAttributes);

            // This defines that the list holds Text fields
            String fieldMapJson = convertMapToJson(fieldMap);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("field", fieldMapJson);

            return new NamedField(fieldName, FieldTypes.LIST_FIELD, attributes, Map.of());
        }

        return new NamedField(fieldName, fieldType, Map.of(), Map.of());
    }


    /**
     * Converts a map to a JSON string.
     * <p>
     * This utility method is used internally to serialize configuration maps
     * to JSON strings for field attributes.
     *
     * @param map The map to convert.
     * @return The JSON string representation of the map.
     * @throws RuntimeException if the conversion fails.
     */
    private static String convertMapToJson(Map<String, Object> map) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (Exception cause) {
            throw new RuntimeException("Failed to convert map to JSON string", cause);
        }
    }
}
