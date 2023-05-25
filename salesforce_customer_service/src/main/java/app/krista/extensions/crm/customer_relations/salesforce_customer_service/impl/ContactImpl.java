package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFContact;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Contact;

public class ContactImpl implements Contact {

    private final SFContact sfContact;

    public ContactImpl(SFContact sfContact) {
        this.sfContact = sfContact;
    }

    @Override
    public String getName() {
        return sfContact.name;
    }

    @Override
    public String getTitle() {
        return sfContact.title;
    }

    @Override
    public String getReportsToId() {
        return sfContact.reportsToId;
    }

    @Override
    public String getId() {
        return sfContact.id;
    }

    @Override
    public String getAccountId() {
        return sfContact.accountId;
    }

    @Override
    public Long getCreatedDate() {
        return sfContact.createdDate != null ? sfContact.createdDate.getTime() : null;
    }

    @Override
    public String getAccountName() {
        return sfContact.account != null ? sfContact.account.Name : null;
    }

    @Override
    public Long getLastModifiedDate() {
        return sfContact.lastModifiedDate != null ? sfContact.lastModifiedDate.getTime() : null;
    }

    @Override
    public String getDepartment() {
        return sfContact.department;
    }

    @Override
    public String getPhone() {
        return sfContact.phone;
    }

    @Override
    public String getDescription() {
        return sfContact.description;
    }

    @Override
    public String getEmail() {
        return sfContact.email;
    }

    @Override
    public String getFirstName() {
        return sfContact.firstName;
    }

    @Override
    public String getLastName() {
        return sfContact.lastName;
    }
}
