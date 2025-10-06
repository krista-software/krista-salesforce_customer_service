package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;


import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.Searchable;
import app.krista.extension.impl.anno.ToString;

@Searchable
@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53", name = "Customer Relations", ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c", ecosystemName = "CRM", ecosystemVersion = "02f1a8e5-3d30-4717-89ad-535dce7c1789")
@Entity(name = "Case", id = "localDomainEntity_a0d5ba68-bab0-4146-abb7-a2cc8cedfb69", primaryKey = "Id", supportStore = true)
public class Case {

    @Searchable
    @Field(name = "Id", type = "Text", required = false)
    public String id;

    @Searchable
    @Field(name = "Contact Id", type = "Text", required = false)
    public String contactId;

    @Searchable
    @Field(name = "Account Id", type = "Text", required = false)
    public String accountId;

    @Searchable
    @ToString
    @Field(name = "Contact Name", type = "Text", required = false)
    public String contactName;

    @ToString
    @Searchable
    @Field(name = "Subject", type = "Text", required = false)
    public String subject;

    @Field(name = "Is Closed", type = "Switch", required = false)
    public Boolean isClosed;

    @Searchable
    @Field(name = "Owner Id", type = "Text", required = false)
    public String ownerId;

    @Searchable
    @ToString
    @Field(name = "Account Name", type = "Text", required = false)
    public String accountName;

    @Searchable
    @ToString
    @Field(name = "Created Date", type = "Date", attributes = @Attribute(name = "includeTimeOfDay", value = "false"), required = false)
    public Long createdDate;

    @Searchable
    @Field(name = "Last Modified Date", type = "Date", attributes = @Attribute(name = "includeTimeOfDay", value = "false"), required = false)
    public Long lastModifiedDate;

    @Searchable
    @ToString
    @Field(name = "Case Number", type = "Number", required = false)
    public Double caseNumber;

    @Searchable
    @ToString
    @Field(name = "Contact Email", type = "Email", required = false)
    public String contactEmail;

    @Searchable
    @ToString
    @Field(name = "Contact Phone", type = "Text", required = false)
    public String contactPhone;

    @Searchable
    @ToString
    @Field(name = "Status", type = "Text")
    public String status;

    @Field(name = "Reason", type = "Text", required = false)
    public String reason;

    @Field(name = "Origin", type = "Text")
    public String origin;

    @Field(name = "Description", type = "Paragraph", required = false)
    public String description;

    @Field(name = "Priority", type = "Text", required = false)
    public String priority;

}
