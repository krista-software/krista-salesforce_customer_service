package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import java.util.Map;
import java.util.Objects;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Validators.getDoubleOrZero;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Validators.getStringOrEmpty;

public class SFObjectHelper {
    private SFObjectHelper() {
    }

    /**
     * This method is used to put entity fields in map.
     *
     * @param caseData a map containing raw case field values, typically retrieved from an external source or input
     * @param caseMap  the map to be populated or updated with processed case fields and their values
     */
    public static void upsertCaseMap(Map<String, Object> caseData, Map<String, Object> caseMap) {
        if (caseData == null) {
            // Handle null caseData by setting default values
            caseMap.put("Case Contact Id", "");
            caseMap.put("Case Account Id", "");
            caseMap.put("Case Subject", "");
            caseMap.put("Case Is Closed", null);
            caseMap.put("Case Owner Id", "");
            caseMap.put("Case Created Date", null);
            caseMap.put("Case Last Modified Case", null);
            caseMap.put("Case Number", 0.0);
            caseMap.put("Case Status", "");
            caseMap.put("Case Reason", "");
            caseMap.put("Case Origin", "");
            caseMap.put("Case Description", "");
            caseMap.put("Case Priority", "");
            return;
        }

        caseMap.put("Case Contact Id", getStringOrEmpty(caseData.get("ContactId")));
        caseMap.put("Case Account Id", getStringOrEmpty(caseData.get("AccountId")));
        caseMap.put("Case Subject", getStringOrEmpty(caseData.get("Subject")));
        caseMap.put("Case Is Closed", caseData.get("IsClosed"));
        caseMap.put("Case Owner Id", getStringOrEmpty(caseData.get("OwnerId")));
        caseMap.put("Case Created Date", caseData.get("CreatedDate") != null
                ? DatePlus.fromYYYYMMDDToEpochMilli(caseData.get("CreatedDate").toString())
                : null);
        caseMap.put("Case Last Modified Case", caseData.get("LastModifiedDate") != null
                ? DatePlus.fromYYYYMMDDToEpochMilli(caseData.get("LastModifiedDate").toString())
                : null);
        caseMap.put("Case Number", getDoubleOrZero(caseData.get("CaseNumber")));
        caseMap.put("Case Status", getStringOrEmpty(caseData.get("Status")));
        caseMap.put("Case Reason", getStringOrEmpty(caseData.get("Reason")));
        caseMap.put("Case Origin", getStringOrEmpty(caseData.get("Origin")));
        caseMap.put("Case Description", getStringOrEmpty(caseData.get("Description")));
        caseMap.put("Case Priority", getStringOrEmpty(caseData.get("Priority")));
    }

}
