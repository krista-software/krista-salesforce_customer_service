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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCustomer;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer;

/**
 * Implementation of the {@link Customer} interface for Salesforce integration.
 * <p>
 * This class provides concrete implementations of methods defined in the Customer
 * interface, specifically tailored for Salesforce CRM. It acts as an adapter between
 * the application's Customer interface and the Salesforce-specific {@link SFCustomer} model.
 * <p>
 * The implementation handles data mapping and conversion between the application's
 * customer representation and Salesforce's customer data structure.
 *
 * <h2>Key Responsibilities:</h2>
 * <ul>
 *   <li>Provide access to customer properties such as ID, party ID, name, and other attributes</li>
 *   <li>Convert and map data between the application and Salesforce formats</li>
 *   <li>Encapsulate Salesforce-specific logic for customer data</li>
 * </ul>
 *
 * @see Customer
 * @see SFCustomer
 */
public class CustomerImpl implements Customer {

    private final SFCustomer sfCustomer;

    /**
     * Constructs a new CustomerImpl that wraps the provided SFCustomer instance.
     *
     * @param sfCustomer The Salesforce customer model to be wrapped by this implementation
     */
    public CustomerImpl(SFCustomer sfCustomer) {
        this.sfCustomer = sfCustomer;
    }

    /**
     * Retrieves the unique identifier of the customer.
     *
     * @return The Salesforce ID of the customer, or null if not set
     */
    @Override
    public String getId() {
        return sfCustomer.id;
    }

    /**
     * Retrieves the party identifier of the customer.
     * This ID is used to link the customer to other systems or entities.
     *
     * @return The party ID of the customer, or null if not set
     */
    @Override
    public String getPartyId() {
        return sfCustomer.partyId;
    }

    /**
     * Retrieves the name of the customer.
     *
     * @return The name of the customer, or null if not set
     */
    @Override
    public String getName() {
        return sfCustomer.name;
    }

    /**
     * Retrieves the status type of the customer.
     * This indicates the current state of the customer relationship.
     *
     * @return The customer status type, or null if not set
     */
    @Override
    public String getCustomerStatusType() {
        return sfCustomer.customerStatusType;
    }

    /**
     * Retrieves the total lifetime value of the customer.
     * This represents the total monetary value the customer has generated.
     *
     * @return The total lifetime value of the customer, or null if not set
     */
    @Override
    public Double getTotalLifeTimeValue() {
        return sfCustomer.totalLifeTimeValue;
    }
}
