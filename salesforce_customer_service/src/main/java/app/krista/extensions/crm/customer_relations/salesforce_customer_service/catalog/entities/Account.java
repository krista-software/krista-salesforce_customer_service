package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;


import app.krista.extension.impl.anno.*;

@Searchable
@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "02f1a8e5-3d30-4717-89ad-535dce7c1789")
@Entity(name = "Account", id = "localDomainEntity_414ed916-6f06-4544-9ce4-cb47f5516d6a", primaryKey = "Id", supportStore = true)
public class Account {

    @Searchable
    @ToString
    @Field(name = "Id", type = "Text", required = false)
    public String id;

    @Searchable
    @ToString
    @Field(name = "Name", type = "Text")
    public String name;

    @Field(name = "Type", type = "Text", required = false)
    public String type;

    @Field(name = "Fax", type = "Text", required = false)
    public String fax;

    @Searchable
    @ToString
    @Field(name = "Account Number", type = "Text", required = false)
    public String accountNumber;

    @Field(name = "Website", type = "Text", required = false)
    public String website;

    @Field(name = "Sic", type = "Text", required = false)
    public String sic;

    @Field(name = "Owner Id", type = "Text", required = false)
    public String ownerId;

    @Field(name = "Created By Id", type = "Text", required = false)
    public String createdById;

    @Field(name = "Clean Status", type = "Text", required = false)
    public String cleanStatus;

    @Field(name = "CustomerPriority", type = "Text", required = false)
    public String customerPriority;

    @Field(name = "SLA Serial Number", type = "Text", required = false)
    public String sLASerialNumber;

    @Field(name = "Created Date", type = "Date", attributes = @Attribute(name = "includeTimeOfDay", value = "false"), required = false)
    public Long createdDate;

    @Field(name = "Billing Street", type = "Street", required = false)
    public String billingStreet;

    @Field(name = "Billing City", type = "City", required = false)
    public String billingCity;

    @Field(name = "Billing State", type = "State", required = false)
    public String billingState;

    @Field(name = "Billing Postal Code", type = "PostalCode", required = false)
    public String billingPostalCode;

    @Field(name = "Billing Country", type = "Country", required = false)
    public String billingCountry;

    @Field(name = "Shipping Street", type = "Street", required = false)
    public String shippingStreet;

    @Field(name = "Shipping City", type = "City", required = false)
    public String shippingCity;

    @Field(name = "Shipping State", type = "State", required = false)
    public String shippingState;

    @Field(name = "Shipping Postal Code", type = "PostalCode", required = false)
    public String shippingPostalCode;

    @Field(name = "Shipping Country", type = "Country", required = false)
    public String shippingCountry;

    @Searchable
    @ToString
    @Field(name = "Phone", type = "Text", required = false)
    public String phone;

    @Field(name = "Number Of Employees", type = "Number", required = false)
    public Double numberOfEmployees;

    @Field(name = "SLA Expiration Date", type = "Date", attributes = @Attribute(name = "includeTimeOfDay", value = "false"), required = false)
    public Long sLAExpirationDate;

    @Field(name = "Ownership", type = "Text", required = false)
    public String ownership;

    @Field(name = "Industry", type = "Text", required = false)
    public String industry;

}
