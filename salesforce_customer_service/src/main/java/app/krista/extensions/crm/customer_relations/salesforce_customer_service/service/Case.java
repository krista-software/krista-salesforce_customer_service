package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

public interface Case {
    String getId();
    String getContactId();
    String getAccountId();
    String getContactName();
    String getSubject();
    Boolean getIsClosed();
    String getOwnerId();
    String getAccountName();
    Long getCreatedDate();
    Long getLastModifiedDate();
    Double getCaseNumber();
    String getContactEmail();
    String getContactPhone();
    String getStatus();
    String getReason();
    String getOrigin();
    String getDescription();
    String getPriority();
}
