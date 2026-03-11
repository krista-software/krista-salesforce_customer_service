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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Case;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Map;

public final class SFCase {
    @SerializedName("Status")
    public String status;

    @SerializedName("Origin")
    public String origin;

    @SerializedName("AccountId")
    public String accountId;

    @SerializedName("LastModifiedDate")
    public Date lastModifiedDate;

    @SerializedName("Description")
    public String description;

    @SerializedName("CreatedDate")
    public Date createdDate;

    @SerializedName("Priority")
    public String priority;

    @SerializedName("ContactPhone")
    public String contactPhone;

    @SerializedName("Reason")
    public String reason;

    @SerializedName("Subject")
    public String subject;

    @SerializedName("IsClosed")
    public Boolean isClosed;

    @SerializedName("CaseNumber")
    public Double caseNumber;

    @SerializedName("ContactEmail")
    public String contactEmail;

    @SerializedName("OwnerId")
    public String ownerId;

    @SerializedName("Id")
    public String id;

    @SerializedName("ContactId")
    public String contactId;

    public AttributeClass Contact;
    public AttributeClass Account;

    public static SFCase from(Case sfCase) {
        /* this case will have input data given for updating the case. */
        if (sfCase == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }

        SFCase cases = new SFCase();
        cases.status = sfCase.status;
        cases.origin = sfCase.origin;
        String givenAccountId = sfCase.accountId;
        String givenAccountName = sfCase.accountName;

        if (givenAccountId == null || givenAccountId.isBlank()) {
            if (StringHelper.isStringAvailable(givenAccountName)) {
                throw new IllegalArgumentException(Constants.ACCOUNT_ID_IS_REQUIRED_TO_ADD_ACCOUNT_NAME);
            } else {
                cases.accountId = "";
            }
        } else {
            cases.accountId = givenAccountId;
        }

        if (sfCase.lastModifiedDate != null) {
            cases.lastModifiedDate = new Date(sfCase.lastModifiedDate);
        }
        if (sfCase.createdDate != null) {
            cases.createdDate = new Date(sfCase.createdDate);
        }

        String givenContactId = sfCase.contactId;
        String givenContactName = sfCase.contactName;
        String givenContactPhone = sfCase.contactPhone;
        String givenContactEmail = sfCase.contactEmail;
        if (givenContactId == null || givenContactId.isBlank()) {
            if (StringHelper.isStringAvailable(givenContactName) || StringHelper.isStringAvailable(givenContactPhone) || StringHelper.isStringAvailable(givenContactEmail)) {
                throw new IllegalArgumentException(Constants.CONTACT_ID_IS_REQUIRED_TO_ADD_CONTACT_INFORMATION);
            }
        } else {
            cases.contactId = givenContactId;
        }

        cases.description = sfCase.description;
        cases.priority = sfCase.priority;
        cases.reason = sfCase.reason;
        cases.subject = sfCase.subject;
        cases.caseNumber = sfCase.caseNumber;
        // TODO: 14/12/22 check if case number change is working
        // TODO: 14/12/22 Handle case for multiple owners.
        cases.ownerId = sfCase.ownerId;
        cases.id = sfCase.id;

        return cases;
    }

    public static SFCase from(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case sfCase) {
        /* get updated case and old case diff */
        if (sfCase == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }

        SFCase cases = new SFCase();
        cases.status = sfCase.getStatus();
        cases.origin = sfCase.getOrigin();

        String givenAccountId = sfCase.getAccountId();
        String givenAccountName = sfCase.getAccountName();
        if (givenAccountId == null || givenAccountId.isBlank()) {
            if (StringHelper.isStringAvailable(givenAccountName)) {
                throw new IllegalArgumentException(Constants.ACCOUNT_ID_IS_REQUIRED_TO_UPDATE_ACCOUNT_NAME);
            } else {
                cases.accountId = "";
            }
        } else {
            cases.accountId = givenAccountId;
        }

        checkAndAssignContactId(sfCase, cases);
        cases.description = sfCase.getDescription();
        cases.priority = sfCase.getPriority();
        cases.reason = sfCase.getReason();
        cases.subject = sfCase.getSubject();

        // TODO: 14/12/22 handle owner id cases
        cases.ownerId = sfCase.getOwnerId();

        return cases;
    }

    private static void checkAndAssignContactId(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case sfCase, SFCase cases) {

        String givenContactId = sfCase.getContactId();
        String givenContactName = sfCase.getContactName();
        String givenContactPhone = sfCase.getContactPhone();
        String givenContactEmail = sfCase.getContactEmail();
        if (givenContactId == null || givenContactId.isBlank()) {
            if (StringHelper.isStringAvailable(givenContactName) || StringHelper.isStringAvailable(givenContactPhone) || StringHelper.isStringAvailable(givenContactEmail)) {
                throw new IllegalArgumentException(Constants.CONTACT_ID_IS_REQUIRED_TO_ADD_CONTACT_INFORMATION);
            } else {
                cases.contactId = "";
            }
        } else {
            cases.contactId = givenContactId;
        }
    }

    public static SFCase from(Map<String, Object> entityDefinition) {
        /* This is used only while creating a case. so all user input not required can be ignored. */
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException(Constants.ENTITY_MAP_IS_NULL_OR_EMPTY);
        }
        SFCase cases = new SFCase();
        cases.status = (String) entityDefinition.get(Constants.STATUS);
        cases.origin = (String) entityDefinition.get(Constants.ORIGIN);

        String givenAccountId = (String) entityDefinition.get(Constants.ACCOUNT_ID);
        String givenAccountName = (String) entityDefinition.get(Constants.ACCOUNT_NAME);
        if (givenAccountId == null || givenAccountId.isBlank()) {
            if (StringHelper.isStringAvailable(givenAccountName)) {
                throw new IllegalArgumentException(Constants.ACCOUNT_ID_IS_REQUIRED_TO_ADD_ACCOUNT_NAME);
            }
        } else {
            cases.accountId = givenAccountId;
        }

        String givenContactId = (String) entityDefinition.get(Constants.CONTACT_ID);
        String givenContactName = (String) entityDefinition.get(Constants.CONTACT_NAME);
        String givenContactPhone = (String) entityDefinition.get(Constants.CONTACT_PHONE);
        String givenContactEmail = (String) entityDefinition.get(Constants.CONTACT_EMAIL);
        if (givenContactId == null || givenContactId.isBlank()) {
            if (StringHelper.isStringAvailable(givenContactName) || StringHelper.isStringAvailable(givenContactPhone) || StringHelper.isStringAvailable(givenContactEmail)) {
                throw new IllegalArgumentException(Constants.CONTACT_ID_IS_REQUIRED_TO_ADD_CONTACT_INFORMATION);
            }
        } else {
            cases.contactId = givenContactId;
        }

        cases.description = (String) entityDefinition.get(Constants.DESCRIPTION);
        cases.priority = (String) entityDefinition.get(Constants.PRIORITY);
        cases.reason = (String) entityDefinition.get(Constants.REASON);
        cases.subject = (String) entityDefinition.get(Constants.SUBJECT);
        // TODO: 14/12/22 cases.ownerId = (String) entityDefinition.get("Owner Id");
        //  Handle multiple owners case.
        //  * Owner Id should not be passed unless the organization has permission to change owner.
        //  * This is not possible for all organizations.

        return cases;
    }

    public static class AttributeClass {
        public String Name;
    }

}
