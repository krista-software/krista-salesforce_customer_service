package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFAccount;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Account;

public class AccountImpl implements Account {

    private final SFAccount sfAccount;

    public AccountImpl(SFAccount sfAccount) {
        this.sfAccount = sfAccount;
    }

    @Override
    public String getId() {
        return sfAccount.id;
    }

    @Override
    public String getName() {
        return sfAccount.name;
    }

    @Override
    public String getType() {
        return sfAccount.type;
    }

    @Override
    public String getFax() {
        return sfAccount.fax;
    }

    @Override
    public String getAccountNumber() {
        return sfAccount.accountNumber;
    }

    @Override
    public String getWebsite() {
        return sfAccount.website;
    }

    @Override
    public String getSic() {
        return sfAccount.sic;
    }

    @Override
    public String getOwnerId() {
        return sfAccount.ownerId;
    }

    @Override
    public String getCreatedById() {
        return sfAccount.createdById;
    }

    @Override
    public String getCleanStatus() {
        return sfAccount.cleanStatus;
    }

    @Override
    public String getCustomerPriority() {
        return sfAccount.customerPriority;
    }

    @Override
    public String getSLASerialNumber() {
        return sfAccount.sLASerialNumber;
    }

    @Override
    public Long getCreatedDate() {
        return sfAccount.createdDate != null ? sfAccount.createdDate.getTime() : null;
    }

    @Override
    public String getBillingStreet() {
        return sfAccount.billingStreet;
    }

    @Override
    public String getBillingCity() {
        return sfAccount.billingCity;
    }

    @Override
    public String getBillingState() {
        return sfAccount.billingState;
    }

    @Override
    public String getBillingPostalCode() {
        return sfAccount.billingPostalCode;
    }

    @Override
    public String getBillingCountry() {
        return sfAccount.billingCountry;
    }

    @Override
    public String getShippingStreet() {
        return sfAccount.shippingStreet;
    }

    @Override
    public String getShippingCity() {
        return sfAccount.shippingCity;
    }

    @Override
    public String getShippingState() {
        return sfAccount.shippingState;
    }

    @Override
    public String getShippingPostalCode() {
        return sfAccount.shippingPostalCode;
    }

    @Override
    public String getShippingCountry() {
        return sfAccount.shippingCountry;
    }

    @Override
    public String getPhone() {
        return sfAccount.phone;
    }

    @Override
    public Double getNumberOfEmployees() {
        return sfAccount.numberOfEmployees;
    }

    @Override
    public Long getSLAExpirationDate() {
        return sfAccount.sLAExpirationDate;
    }

    @Override
    public String getOwnership() {
        return sfAccount.ownership;
    }

    @Override
    public String getIndustry() {
        return sfAccount.industry;
    }
}
