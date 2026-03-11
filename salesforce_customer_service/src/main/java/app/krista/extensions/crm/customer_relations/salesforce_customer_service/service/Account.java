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
