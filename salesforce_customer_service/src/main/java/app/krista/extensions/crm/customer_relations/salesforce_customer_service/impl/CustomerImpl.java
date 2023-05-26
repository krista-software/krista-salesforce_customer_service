package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCustomer;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer;

public class CustomerImpl implements Customer {

    private final SFCustomer sfCustomer;

    public CustomerImpl(SFCustomer sfCustomer) {
        this.sfCustomer = sfCustomer;
    }

    @Override
    public String getId() {
        return sfCustomer.id;
    }

    @Override
    public String getPartyId() {
        return sfCustomer.partyId;
    }

    @Override
    public String getName() {
        return sfCustomer.name;
    }

    @Override
    public String getCustomerStatusType() {
        return sfCustomer.customerStatusType;
    }

    @Override
    public Double getTotalLifeTimeValue() {
        return sfCustomer.totalLifeTimeValue;
    }
}
