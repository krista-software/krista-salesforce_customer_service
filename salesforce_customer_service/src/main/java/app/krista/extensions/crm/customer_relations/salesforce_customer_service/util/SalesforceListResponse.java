package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import java.util.List;

public final class SalesforceListResponse<T> {

    public int totalSize;
    public boolean done;
    public List<T> records;
}
