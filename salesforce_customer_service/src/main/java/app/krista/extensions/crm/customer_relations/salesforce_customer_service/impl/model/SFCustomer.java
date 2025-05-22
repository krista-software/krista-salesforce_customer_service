package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Customer;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public final class SFCustomer {

    @SerializedName("Name")
    public String name;

    @SerializedName("Id")
    public String id;

    @SerializedName("PartyId")
    public String partyId;

    @SerializedName("CustomerStatusType")
    public String customerStatusType;

    @SerializedName("TotalLifeTimeValue")
    public Double totalLifeTimeValue;

    public static SFCustomer from(Customer sfCustomer) {
        if (sfCustomer == null) {
            System.err.println(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        SFCustomer newCustomer = new SFCustomer();
        assert sfCustomer != null;
        newCustomer.id = sfCustomer.id;
        newCustomer.partyId = sfCustomer.partyId;
        newCustomer.name = sfCustomer.name;
        newCustomer.customerStatusType = sfCustomer.customerStatusType;
        newCustomer.totalLifeTimeValue = sfCustomer.totalLifeTimeValue;

        return newCustomer;
    }

    public static SFCustomer from(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer sfCustomer) {
        if (sfCustomer == null) {
            System.err.println(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        SFCustomer newCustomer = new SFCustomer();
        assert sfCustomer != null;
        newCustomer.id = sfCustomer.getId();
        newCustomer.name = sfCustomer.getName();
        newCustomer.partyId = sfCustomer.getPartyId();
        newCustomer.customerStatusType = sfCustomer.getCustomerStatusType();
        newCustomer.totalLifeTimeValue = sfCustomer.getTotalLifeTimeValue();

        return newCustomer;
    }

    public static SFCustomer from(Map<String, Object> entityDefinition) {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            System.err.println(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        SFCustomer newCustomer = new SFCustomer();
        assert entityDefinition != null;
        newCustomer.id = (String) entityDefinition.get("Id");
        newCustomer.partyId = (String) entityDefinition.get("Party Id");
        newCustomer.name = (String) entityDefinition.get("Name");
        newCustomer.customerStatusType = (String) entityDefinition.get("Customer Status Type");
        newCustomer.totalLifeTimeValue = (Double) entityDefinition.get("Total Life Time Value");

        return newCustomer;
    }
}
