package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Contact;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public final class SFContact {
    @SerializedName("AccountId")
    public String accountId;
    @SerializedName("Account")
    public Account account;
    @SerializedName("LastModifiedDate")
    public Date lastModifiedDate;
    @SerializedName("Department")
    public String department;
    @SerializedName("Email")
    public String email;
    @SerializedName("Description")
    public String description;
    @SerializedName("CreatedDate")
    public Date createdDate;
    @SerializedName("Phone")
    public String phone;
    @SerializedName("Title")
    public String title;
    @SerializedName("Id")
    public String id;
    @SerializedName("Name")
    public String name;
    @SerializedName("FirstName")
    public String firstName;
    @SerializedName("LastName")
    public String lastName;
    @SerializedName("ReportsToId")
    public String reportsToId;


    public static SFContact from(Contact sfContact) {
        /* this is used to pass data to update function */
        if (sfContact == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }

        SFContact contact = new SFContact();
        String[] name = sfContact.name.split(Constants.SPACE);
        assignFirstAndLastNames(contact, name);
        contact.department = sfContact.department;
        contact.description = sfContact.description;
        contact.email = sfContact.email;
        contact.id = sfContact.id;
        contact.phone = sfContact.phone != null ? sfContact.phone : "";
        contact.reportsToId = sfContact.reportsToId;
        contact.title = sfContact.title;
        checkAndAssignAccountIdInfo(sfContact, contact);
        contact.createdDate = sfContact.createdDate != null ? new Date(sfContact.createdDate) : null;
        contact.lastModifiedDate = sfContact.lastModifiedDate != null ? new Date(sfContact.lastModifiedDate) : null;
        return contact;
    }

    private static void checkAndAssignAccountIdInfo(Contact sfContact, SFContact contact) {
        String givenAccountId = sfContact.accountId;
        String givenAccountName = sfContact.accountName;
        if (givenAccountId == null || givenAccountId.isBlank()) {
            if (StringHelper.isStringAvailable(givenAccountName)) {
                throw new IllegalArgumentException(Constants.ACCOUNT_ID_IS_REQUIRED_TO_UPDATE_ACCOUNT_NAME);
            }
        } else {
            contact.accountId = givenAccountId;
        }
    }

    private static void assignFirstAndLastNames(SFContact contact, String[] name) {
        if (name.length == 1) contact.lastName = name[0];
        else if (name.length == 2) {
            contact.firstName = name[0];
            contact.lastName = name[1];
        } else {
            contact.firstName = String.join(" ", Arrays.copyOf(name, name.length - 1));
            contact.lastName = name[name.length - 1];
        }
    }

    public static SFContact from(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Contact sfContact) {
        /* used for updating the contact to get the new data and old data */
        if (sfContact == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        SFContact contact = new SFContact();
        if (sfContact.getName() != null) {
            String[] name = sfContact.getName().split(Constants.SPACE);
            assignFirstAndLastNames(contact, name);
        } else {
            contact.firstName = sfContact.getFirstName();
            contact.lastName = sfContact.getLastName();
        }
        contact.department = sfContact.getDepartment();
        contact.description = sfContact.getDescription();
        contact.email = sfContact.getEmail();
        contact.phone = sfContact.getPhone() != null ? sfContact.getPhone() : "";
        contact.reportsToId = sfContact.getReportsToId();
        contact.title = sfContact.getTitle();
        String givenAccountId = sfContact.getAccountId();
        String givenAccountName = sfContact.getAccountName();
        if (givenAccountId == null || givenAccountId.isBlank()) {
            if (StringHelper.isStringAvailable(givenAccountName)) {
                throw new IllegalArgumentException(Constants.ACCOUNT_ID_IS_REQUIRED_TO_UPDATE_ACCOUNT_NAME);
            } else {
                contact.accountId = "";
            }
        } else {
            contact.accountId = givenAccountId;
        }
        contact.createdDate = sfContact.getCreatedDate() != null ? new Date(sfContact.getCreatedDate()) : null;
        contact.lastModifiedDate = sfContact.getLastModifiedDate() != null ? new Date(sfContact.getLastModifiedDate()) : null;
        return contact;
    }

    public static SFContact from(Map<String, Object> entityDefinition) {
        /* this data is passed to create contact record */
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException(Constants.ENTITY_MAP_IS_NULL_OR_EMPTY);
        }
        SFContact contact = new SFContact();
        String[] name = String.valueOf(entityDefinition.get(Constants.NAME)).split(Constants.SPACE);
        assignFirstAndLastNames(contact, name);
        contact.accountId = (String) entityDefinition.get(Constants.ACCOUNT_ID);
        contact.department = (String) entityDefinition.get(Constants.DEPARTMENT);
        contact.description = (String) entityDefinition.get(Constants.DESCRIPTION);
        contact.email = (String) entityDefinition.get(Constants.EMAIL);
        contact.phone = entityDefinition.get(Constants.PHONE) != null ? (String) entityDefinition.get(Constants.PHONE) : null;
        contact.reportsToId = (String) entityDefinition.get(Constants.REPORTS_TO_ID);
        contact.title = (String) entityDefinition.get(Constants.TITLE);
        String givenAccountId = (String) entityDefinition.get(Constants.ACCOUNT_ID);
        String givenAccountName = (String) entityDefinition.get(Constants.ACCOUNT_NAME);
        if (givenAccountId == null || givenAccountId.isBlank() && (StringHelper.isStringAvailable(givenAccountName))) {
            throw new IllegalArgumentException(Constants.ACCOUNT_ID_IS_REQUIRED_TO_ADD_ACCOUNT_NAME);
        }
        contact.createdDate = entityDefinition.get(Constants.CREATED_DATE) != null ? new Date((Long) entityDefinition.get(Constants.CREATED_DATE)) : null;
        contact.lastModifiedDate = entityDefinition.get(Constants.LAST_MODIFIED_DATE) != null ? new Date((Long) entityDefinition.get(Constants.LAST_MODIFIED_DATE)) : null;
        return contact;
    }

    public static class Account {
        public final String Name;

        public Account(String name) {
            this.Name = name;
        }
    }
}
