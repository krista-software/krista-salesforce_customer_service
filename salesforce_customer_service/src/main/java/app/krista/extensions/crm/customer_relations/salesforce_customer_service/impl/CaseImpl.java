package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCase;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case;

public class CaseImpl implements Case {

    private final SFCase sfCase;

    public CaseImpl(SFCase sfCase) {
        this.sfCase = sfCase;
    }

    @Override
    public String getId() {
        return sfCase.id;
    }

    @Override
    public String getContactId() {
        return sfCase.contactId;
    }

    @Override
    public String getAccountId() {
        return sfCase.accountId;
    }

    @Override
    public String getContactName() {
        return sfCase.Contact != null ? sfCase.Contact.Name : null;
    }

    @Override
    public String getSubject() {
        return sfCase.subject;
    }

    @Override
    public Boolean getIsClosed() {
        return sfCase.isClosed;
    }

    @Override
    public String getOwnerId() {
        return sfCase.ownerId;
    }

    @Override
    public String getAccountName() {
        return sfCase.Account != null ? sfCase.Account.Name : null;
    }

    @Override
    public Long getCreatedDate() {
        return sfCase.createdDate != null ? sfCase.createdDate.getTime() : null;
    }

    @Override
    public Long getLastModifiedDate() {
        return sfCase.lastModifiedDate != null ? sfCase.lastModifiedDate.getTime() : null;
    }

    @Override
    public Double getCaseNumber() {
        return sfCase.caseNumber;
    }

    @Override
    public String getContactEmail() {
        return sfCase.contactEmail;
    }

    @Override
    public String getContactPhone() {
        return sfCase.contactPhone;
    }

    @Override
    public String getStatus() {
        return sfCase.status;
    }

    @Override
    public String getReason() {
        return sfCase.reason;
    }

    @Override
    public String getOrigin() {
        return sfCase.origin;
    }

    @Override
    public String getDescription() {
        return sfCase.description;
    }

    @Override
    public String getPriority() {
        return sfCase.priority;
    }
}
