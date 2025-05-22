package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores;

import app.krista.model.base.FreeForm;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Data Access Object implementation for handling content from Salesforce API responses.
 * <p>
 * This class provides functionality to parse JSON responses from Salesforce API
 * and convert them into FreeForm maps. It extracts specified custom fields from
 * the JSON response and stores their values in the resulting map.
 */
@Service
public class ContentDaoImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentDaoImpl.class);

    /**
     * Parses a JSON response string into a FreeForm map.
     * <p>
     * This method extracts values for the specified custom fields from the JSON response
     * and stores them in a FreeForm map. If the JSON response is invalid or does not
     * contain the expected structure, an empty map is returned.
     *
     * @param jsonResponse The JSON response string from Salesforce API
     * @param customFields List of field names to extract from the JSON response
     * @return A FreeForm map containing the extracted field values
     */
    public FreeForm parseJSONToMap(String jsonResponse, List<String> customFields) {
        FreeForm resultMap = new FreeForm();
        try {
            processJSONResponse(jsonResponse, customFields, resultMap);
        } catch (JSONException exception) {
            LOGGER.info("Exception occurred due to : {}", exception.getMessage(), exception);
        }
        return resultMap;
    }

    /**
     * Processes the JSON response and extracts field values.
     * <p>
     * This method parses the JSON response, looks for a "records" array,
     * and processes the first record if available.
     *
     * @param jsonResponse The JSON response string from Salesforce API
     * @param customFields List of field names to extract from the JSON response
     * @param resultMap The FreeForm map to store the extracted field values
     */
    private static void processJSONResponse(String jsonResponse, List<String> customFields, FreeForm resultMap) {
        if (jsonResponse != null) {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            if (jsonObject.has("records")) {
                JSONArray records = jsonObject.getJSONArray("records");

                if (!records.isEmpty()) {
                    JSONObject record = records.getJSONObject(0);

                    processRecordFields(customFields, resultMap, record);
                } else {
                    LOGGER.info("No records found in the JSON response.");
                }
            } else {
                LOGGER.info("No 'records' array found in the JSON response.");
            }
        } else {
            LOGGER.info("JSON response is null.");
        }
    }

    /**
     * Extracts specified fields from a JSON record.
     * <p>
     * This method iterates through the list of custom fields and extracts
     * their values from the JSON record if they exist.
     *
     * @param customFields List of field names to extract from the JSON record
     * @param resultMap The FreeForm map to store the extracted field values
     * @param record The JSON record object containing the field values
     */
    private static void processRecordFields(List<String> customFields, FreeForm resultMap, JSONObject record) {
        if (record != null) {
            for (String key : customFields) {
                if (record.has(key)) {
                    Object value = record.get(key);
                    resultMap.put(key, "Text", value.toString());
                }
            }
        } else {
            LOGGER.info("First record is null.");
        }
    }

}
