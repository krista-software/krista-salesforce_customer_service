package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceService;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.ContentDaoImpl;
import app.krista.model.base.FreeForm;
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
    private final ContentDaoImpl contentDao;

    @Inject
    public ContentArea(SalesforceService salesforceService, ContentDaoImpl contentDao) {
        this.salesforceService = salesforceService;
        this.contentDao = contentDao;
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
        StringBuilder customField = new StringBuilder();
        for (String str : customFields) {
            if (!str.endsWith("__c")) { // custom fields ends with __c
                LOGGER.info("{} is not a custom field", str);
                return new FreeForm();
            }
            customField.append(str).append(",");
        }
        if (!customFields.isEmpty()) {
            customField.deleteCharAt(customField.length() - 1);
        }
        String queryFields = customField.toString();
        String query = "Select " + queryFields + " from " + objectType + " where id = '" + objectId + "'";
        String recordByQuery = salesforceService.getRecordByQuery(query);
        if (recordByQuery != null) {
            return contentDao.parseJSONToMap(recordByQuery, customFields);
        }
        return new FreeForm();
    }

}