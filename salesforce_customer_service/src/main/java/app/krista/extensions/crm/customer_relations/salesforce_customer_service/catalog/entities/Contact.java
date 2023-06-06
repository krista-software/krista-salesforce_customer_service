package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;


import app.krista.extension.impl.anno.*;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper;

import java.util.Map;

@Searchable
@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "02f1a8e5-3d30-4717-89ad-535dce7c1789")
@Entity(name = "Contact", id = "localDomainEntity_2012895f-a739-4382-a060-91d786b22d97", primaryKey = "Id")
public class Contact {

    @ToString
    @Searchable
    @Field(name = "Name", type = "Text")
    public String name;

    @Field(name = "Title", type = "Text", required = false)
    public String title;

    @Field(name = "Reports To Id", type = "Text", required = false)
    public String reportsToId;

    @Field(name = "Id", type = "Text", required = false)
    public String id;

    @ToString
    @Searchable
    @Field(name = "Account Id", type = "Text", required = false)
    public String accountId;

    @Field(name = "Created Date", type = "Date", attributes = @Attribute(name = "includeTimeOfDay", value = "false"), required = false)
    public Long createdDate;

    @ToString
    @Searchable
    @Field(name = "Account Name", type = "Text", required = false)
    public String accountName;

    @Field(name = "Last Modified Date", type = "Date", attributes = @Attribute(name = "includeTimeOfDay", value = "false"), required = false)
    public Long lastModifiedDate;

    @Field(name = "Department", type = "Text", required = false)
    public String department;

    @ToString
    @Searchable
    @Field(name = "Phone", type = "Text", required = false)
    public String phone;

    @Field(name = "Description", type = "Paragraph", required = false)
    public String description;

    @ToString
    @Searchable
    @Field(name = "Email", type = "Email", required = false)
    public String email;

    /**
     * @param contact is {@link Map} of {@link Contact} entity fields
     * @return {@link Contact} entity for the given input map.
     */
    public static Contact fromFields(Map<String, Object> contact) {
        Contact newContact = new Contact();
        newContact.name = (String) contact.get(Constants.NAME);
        newContact.title = (String) contact.get(Constants.TITLE);
        newContact.reportsToId = (String) contact.get(Constants.REPORTS_TO_ID);
        newContact.id = (String) contact.get(Constants.ID);
        String givenAccountId = (String) contact.get(Constants.ACCOUNT_ID);
        String givenAccountName = (String) contact.get(Constants.ACCOUNT_NAME);
        if (StringHelper.isStringAvailable(givenAccountId)) {
            newContact.accountName = "";
            newContact.accountId = givenAccountId;
        } else {
            if (StringHelper.isStringAvailable(givenAccountName)) {
                throw new IllegalArgumentException(Constants.ACCOUNT_ID_IS_REQUIRED_TO_ADD_ACCOUNT_NAME);
            }
        }
        newContact.department = (String) contact.get(Constants.DEPARTMENT);
        newContact.phone = (String) contact.get(Constants.PHONE);
        newContact.description = (String) contact.get(Constants.DESCRIPTION);
        newContact.email = (String) contact.get(Constants.EMAIL);
        newContact.createdDate = (Long) contact.get(Constants.CREATED_DATE);
        newContact.lastModifiedDate = (Long) contact.get(Constants.LAST_MODIFIED_DATE);
        return newContact;
    }

}
