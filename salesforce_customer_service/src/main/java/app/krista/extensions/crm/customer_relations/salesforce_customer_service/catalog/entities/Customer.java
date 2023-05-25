package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;


import app.krista.extension.impl.anno.*;

@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "a2be487a-9fda-488b-81a8-a469fe022987")
@Entity(name = "Customer", id = "localDomainEntity_3854445f-aea9-42e0-a494-0dfcb89c06b4", primaryKey = "Id")
public class Customer {

    @ToString
    @Field.Text(name = "Id", required = false)
    public String id;

    @Searchable
    @ToString
    @Field.Text(name = "Party")
    public String party;

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
