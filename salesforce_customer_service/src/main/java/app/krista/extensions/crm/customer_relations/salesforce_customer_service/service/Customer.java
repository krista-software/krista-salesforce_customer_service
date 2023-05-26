package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

public interface Customer {
    String getId();

    String getPartyId();

    String getName();

    String getCustomerStatusType();

    Double getTotalLifeTimeValue();
}
