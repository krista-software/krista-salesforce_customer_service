/*
 * Salesforce Customer Service Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
