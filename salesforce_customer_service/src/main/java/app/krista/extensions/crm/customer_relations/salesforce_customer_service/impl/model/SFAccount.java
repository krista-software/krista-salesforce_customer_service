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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Account;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Map;

public final class SFAccount {
    @SerializedName("AccountNumber")
    public String accountNumber;

    @SerializedName("Ownership")
    public String ownership;

    @SerializedName("CreatedDate")
    public Date createdDate;

    @SerializedName("Website")
    public String website;

    @SerializedName("SLAExpirationDate")
    public Long sLAExpirationDate;

    @SerializedName("Name")
    public String name;

    @SerializedName("Industry")
    public String industry;

    @SerializedName("NumberOfEmployees")
    public Double numberOfEmployees;

    @SerializedName("BillingCity")
    public String billingCity;

    @SerializedName("OwnerId")
    public String ownerId;

    @SerializedName("ShippingStreet")
    public String shippingStreet;

    @SerializedName("Phone")
    public String phone;

    @SerializedName("SLASerialNumber")
    public String sLASerialNumber;

    @SerializedName("ShippingState")
    public String shippingState;

    @SerializedName("CreatedById")
    public String createdById;

    @SerializedName("BillingState")
    public String billingState;

    @SerializedName("ShippingCity")
    public String shippingCity;

    @SerializedName("BillingCountry")
    public String billingCountry;

    @SerializedName("Sic")
    public String sic;

    @SerializedName("ShippingCountry")
    public String shippingCountry;

    @SerializedName("ShippingPostalCode")
    public String shippingPostalCode;

    @SerializedName("Type")
    public String type;

    @SerializedName("BillingStreet")
    public String billingStreet;

    @SerializedName("BillingPostalCode")
    public String billingPostalCode;

    @SerializedName("CustomerPriority")
    public String customerPriority;

    @SerializedName("CleanStatus")
    public String cleanStatus;

    @SerializedName("Id")
    public String id;

    @SerializedName("Fax")
    public String fax;

    public static SFAccount from(Account sfAccount) {
        if (sfAccount == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        SFAccount account = new SFAccount();
        account.accountNumber = sfAccount.accountNumber;
        account.ownership = sfAccount.ownership;
        if (sfAccount.createdDate != null)
            account.createdDate = new Date(sfAccount.createdDate);
        account.website = sfAccount.website;
        account.sLAExpirationDate = sfAccount.sLAExpirationDate;
        account.name = sfAccount.name;
        account.industry = sfAccount.industry;
        account.numberOfEmployees = sfAccount.numberOfEmployees;
        account.billingCity = sfAccount.billingCity;
        account.ownerId = sfAccount.ownerId;
        account.shippingStreet = sfAccount.shippingStreet;
        account.phone = sfAccount.phone != null ? sfAccount.phone : "";
        account.sLASerialNumber = sfAccount.sLASerialNumber;
        account.shippingState = sfAccount.shippingState;
        account.createdById = sfAccount.createdById;
        account.billingState = sfAccount.billingState;
        account.shippingCity = sfAccount.shippingCity;
        account.billingCountry = sfAccount.billingCountry;
        account.sic = sfAccount.sic;
        account.shippingCountry = sfAccount.shippingCountry;
        account.shippingPostalCode = sfAccount.shippingPostalCode;
        account.type = sfAccount.type;
        account.billingStreet = sfAccount.billingStreet;
        account.billingPostalCode = sfAccount.billingPostalCode;
        account.customerPriority = sfAccount.customerPriority;
        account.cleanStatus = sfAccount.cleanStatus;
        account.id = sfAccount.id;
        account.fax = sfAccount.fax;
        return account;
    }

    public static SFAccount from(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Account sfAccount) {
        if (sfAccount == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        SFAccount account = new SFAccount();
        account.accountNumber = sfAccount.getAccountNumber();
        account.ownership = sfAccount.getOwnership();
        account.website = sfAccount.getWebsite();
        account.sLAExpirationDate = sfAccount.getSLAExpirationDate();
        account.name = sfAccount.getName();
        account.industry = sfAccount.getIndustry();
        account.numberOfEmployees = sfAccount.getNumberOfEmployees();
        account.billingCity = sfAccount.getBillingCity();
        // TODO: 14/12/22 handle the case if there are multiple owners.
        //  account.ownerId = sfAccount.getOwnerId();
        account.shippingStreet = sfAccount.getShippingStreet();
        account.phone = sfAccount.getPhone() != null ? sfAccount.getPhone() : "";
        account.sLASerialNumber = sfAccount.getSLASerialNumber();
        account.shippingState = sfAccount.getShippingState();
        account.billingState = sfAccount.getBillingState();
        account.shippingCity = sfAccount.getShippingCity();
        account.billingCountry = sfAccount.getBillingCountry();
        account.sic = sfAccount.getSic();
        account.shippingCountry = sfAccount.getShippingCountry();
        account.shippingPostalCode = sfAccount.getShippingPostalCode();
        account.type = sfAccount.getType();
        account.billingStreet = sfAccount.getBillingStreet();
        account.billingPostalCode = sfAccount.getBillingPostalCode();
        account.customerPriority = sfAccount.getCustomerPriority();
        account.cleanStatus = sfAccount.getCleanStatus();
        account.fax = sfAccount.getFax();
        return account;
    }

    public static SFAccount from(Map<String, Object> entityDefinition) {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException("Customer map is empty");
        }
        SFAccount account = new SFAccount();
        account.accountNumber = (String) entityDefinition.get(Constants.ACCOUNT_NUMBER);
        account.ownership = (String) entityDefinition.get(Constants.OWNERSHIP);
        if (entityDefinition.get(Constants.CREATED_DATE) != null)
            account.createdDate = new Date((Long) entityDefinition.get(Constants.CREATED_DATE));
        account.website = (String) entityDefinition.get(Constants.WEBSITE);
        account.sLAExpirationDate = (Long) entityDefinition.get(Constants.SLA_EXPIRATION_DATE);
        account.name = (String) entityDefinition.get(Constants.NAME);
        account.industry = (String) entityDefinition.get(Constants.INDUSTRY);
        account.numberOfEmployees = (Double) entityDefinition.get(Constants.NUMBER_OF_EMPLOYEES);
        account.billingCity = (String) entityDefinition.get(Constants.BILLING_CITY);
        // TODO: 14/12/22 Handle the case if there are multiple owners.
        // TODO: account.ownerId = (String) entityDefinition.get("Owner Id");
        account.shippingStreet = (String) entityDefinition.get(Constants.SHIPPING_STREET);
        account.phone = entityDefinition.get(Constants.PHONE) != null ? (String) entityDefinition.get(Constants.PHONE) : "";
        account.sLASerialNumber = (String) entityDefinition.get(Constants.SLA_SERIAL_NUMBER);
        account.shippingState = (String) entityDefinition.get(Constants.SHIPPING_STATE);
        account.billingState = (String) entityDefinition.get(Constants.BILLING_STATE);
        account.shippingCity = (String) entityDefinition.get(Constants.SHIPPING_CITY);
        account.billingCountry = (String) entityDefinition.get(Constants.BILLING_COUNTRY);
        account.sic = (String) entityDefinition.get(Constants.SIC);
        account.shippingCountry = (String) entityDefinition.get(Constants.SHIPPING_COUNTRY);
        account.shippingPostalCode = (String) entityDefinition.get(Constants.SHIPPING_POSTAL_CODE);
        account.type = (String) entityDefinition.get(Constants.TYPE);
        account.billingStreet = (String) entityDefinition.get(Constants.BILLING_STREET);
        account.billingPostalCode = (String) entityDefinition.get(Constants.BILLING_POSTAL_CODE);
        account.customerPriority = (String) entityDefinition.get(Constants.CUSTOMER_PRIORITY);
        account.cleanStatus = (String) entityDefinition.get(Constants.CLEAN_STATUS);
        account.fax = (String) entityDefinition.get(Constants.FAX);
        return account;
    }
}
