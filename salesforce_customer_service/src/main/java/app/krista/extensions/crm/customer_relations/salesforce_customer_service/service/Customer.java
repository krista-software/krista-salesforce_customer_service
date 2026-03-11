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

public interface Customer {
    /**
     * This function will return the id of the Customer entity
     *
     * @return id of the customer entity
     */
    String getId();

    /**
     * This function will return individual entity id for the customer instance
     *
     * @return id of the individual
     */
    String getPartyId();

    /**
     * This function will return the name of the customer entity
     *
     * @return name of the customer
     */
    String getName();

    /**
     * This function will return the status of the customer entity
     *
     * @return status of the customer
     */
    String getCustomerStatusType();

    /**
     * This function will return the lifetime value of the customer entity
     *
     * @return total lifetime of the customer
     */
    Double getTotalLifeTimeValue();
}
