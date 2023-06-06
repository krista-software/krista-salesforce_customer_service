package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

public interface Account {
    String getId();

    String getName();

    String getType();

    String getFax();

    String getAccountNumber();

    String getWebsite();

    String getSic();

    String getOwnerId();

    String getCreatedById();

    String getCleanStatus();

    String getCustomerPriority();

    String getSLASerialNumber();

    Long getCreatedDate();

    String getBillingStreet();

    String getBillingCity();

    String getBillingState();

    String getBillingPostalCode();

    String getBillingCountry();

    String getShippingStreet();

    String getShippingCity();

    String getShippingState();

    String getShippingPostalCode();

    String getShippingCountry();

    String getPhone();

    Double getNumberOfEmployees();

    Long getSLAExpirationDate();

    String getOwnership();

    String getIndustry();
}
