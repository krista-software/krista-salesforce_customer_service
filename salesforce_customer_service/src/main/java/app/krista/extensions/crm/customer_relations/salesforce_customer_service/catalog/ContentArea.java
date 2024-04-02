package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceService;
import app.krista.model.base.FreeForm;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "b7a2dadc-2b0c-4f59-a4f9-befbae5897e9")
public class ContentArea {
    private final SalesforceService salesforceService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentArea.class);

    @Inject
    public ContentArea(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    @CatalogRequest(
            id = "localDomainRequest_519557fc-c610-4769-8a3c-cfe3c984fb94",
            name = "Retrieve Custom Fields For Given Object",
            description = "Retrieve Custom Fields For Given Object",
            area = "Content",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field(name = "Field Details", type = "FreeForm", required = false, attributes = {}, options = {})
    public FreeForm retrieveCustomFieldsForGivenObject(
            @Field.Text(name = "Object Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String objectId,
            @Field.PickOne(name = "Object Type", values = {"Account", "Case", "Contact", "Customer", "Individual"}, required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String objectType,
            @Field.Desc(name = "Custom Fields", type = "[ Text ]", required = true) List<String> customFields) throws IOException, ExecutionException, InterruptedException {
        StringBuilder stringBuilderForCustomFields = new StringBuilder();
        for (String str : customFields) {
            if (!str.endsWith("__c")) { // custom fields ends with __c
                LOGGER.info("{} is not a custom field", str);
                return new FreeForm();
            }
            stringBuilderForCustomFields.append(str).append(",");
        }
        if (!customFields.isEmpty()) {
            stringBuilderForCustomFields.deleteCharAt(stringBuilderForCustomFields.length() - 1);
        }
        String queryFields = stringBuilderForCustomFields.toString();
        String query = "Select " + queryFields + " from " + objectType + " where id = '" + objectId + "'";
        String recordByQuery = salesforceService.getRecordByQuery(query);
        if (recordByQuery != null) {
            return parseJSONToMap(recordByQuery, customFields);
        }
        return new FreeForm();
    }

    private static FreeForm parseJSONToMap(String jsonResponse, List<String> customFields) {
        FreeForm resultMap = new FreeForm();
        try {
            processJSONResponse(jsonResponse, customFields, resultMap);
        } catch (JSONException exception) {
            LOGGER.info("Exception occurred due to :" + exception.getMessage(), exception);
        }
        return resultMap;
    }

    private static void processJSONResponse(String jsonResponse, List<String> customFields, FreeForm resultMap) {
        if (jsonResponse != null) {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            if (jsonObject.has("records")) {
                JSONArray records = jsonObject.getJSONArray("records");

                if (records.length() > 0) {
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

    private static void processRecordFields(List<String> customFields, FreeForm resultMap, JSONObject record) {
        if (record != null) {
            for (String key : customFields) {
                if (record.has(key)) {
                    Object value = record.get(key);

                    if (value.equals(null)) {
                        resultMap.put(key, "Text", "");
                    } else {
                        resultMap.put(key, "Text", value.toString());
                    }
                }
            }
        } else {
            LOGGER.info("First record is null.");
        }
    }
}