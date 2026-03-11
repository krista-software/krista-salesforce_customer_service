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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;


import app.krista.extension.impl.anno.*;


@Searchable
@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "a2be487a-9fda-488b-81a8-a469fe022987")
@Entity(name = "Customer", id = "localDomainEntity_3854445f-aea9-42e0-a494-0dfcb89c06b4", primaryKey = "Id", supportStore = true)
public class Customer {

    @ToString
    @Searchable
    @Field.Text(name = "Id", required = false)
    public String id;

    @Searchable
    @ToString
    @Field.Text(name = "Party Id")
    public String partyId;

    @Searchable
    @ToString
    @Field.Text(name = "Name")
    public String name;

    @ToString
    @Field.PickOne(name = "Customer Status Type", values = {"Active", "Inactive"}, required = false)
    public String customerStatusType;

    @Field(name = "Total Life Time Value", type = "Number", required = false)
    public Double totalLifeTimeValue;

}
