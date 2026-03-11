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
        ecosystemVersion = "749622ef-f244-4727-b5b4-3f16d68f3ad3")
@Entity(name = "Individual", id = "localDomainEntity_c9073255-1153-4231-970b-b693048638b0", primaryKey = "Id", supportStore = true)
public class Individual {

    public static final String BIRTH_DATE = "Birth Date";
    @Searchable
    @ToString
    @Field.Text(name = "Id", required = false)
    public String id;

    @ToString
    @Field.Text(name = "First Name", required = false)
    public String firstName;
    @Searchable
    @ToString
    @Field.Text(name = "Last Name")
    public String lastName;

    @SuppressWarnings("DefaultAnnotationParam")
    @Field.Date(name = BIRTH_DATE, showHowManyDaysInViewer = 60, allowFuture = false, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public Long birthDate;

    @Field.Text(name = "Created By Id", required = false)
    public String createdById;

    @Field.Date(name = "Created Date", showHowManyDaysInViewer = 60, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public Long createdDate;

    @Field.Date(name = "Last Modified Date", showHowManyDaysInViewer = 60, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public Long lastModifiedDate;

}
