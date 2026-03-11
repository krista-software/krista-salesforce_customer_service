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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CustomerDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.*;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ObjectIdValidator implements Validator {

    private final AccountDao accountDao;
    private final CaseDao caseDao;
    private final ContactDao contactDao;
    private final IndividualDao individualDao;
    private final CustomerDaoImpl customerDao;
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectIdValidator.class);

    public ObjectIdValidator(AccountDao accountDao, CaseDao caseDao,
                             ContactDao contactDao, IndividualDao individualDao, CustomerDaoImpl customerDao) {
        this.accountDao = accountDao;
        this.caseDao = caseDao;
        this.contactDao = contactDao;
        this.individualDao = individualDao;
        this.customerDao = customerDao;
    }

    /**
     * Validates whether the given Account ID exists in the system.
     *
     * @param accountId the ID of the Account to validate
     * @return true if the Account exists; false otherwise or if an error occurs
     */
    private boolean validateAccountId(String accountId) {
        try {
            Account account = accountDao.getAccountById(accountId);
            return account != null;
        } catch (Exception cause) {
            LOGGER.error("Error validating Account ID: {}", cause.getMessage(), cause);
            return false;
        }
    }

    /**
     * Validates whether the given Case ID exists in the system.
     *
     * @param caseId the ID of the Case to validate
     * @return true if the Case exists; false otherwise or if an error occurs
     */
    private boolean validateCaseId(String caseId) {
        try {
            Case caseRecord = caseDao.getCaseById(caseId);
            return caseRecord != null;
        } catch (Exception cause) {
            LOGGER.error("Error validating Case ID: {}", cause.getMessage(), cause);
            return false;
        }
    }

    /**
     * Validates whether the given Contact ID exists in the system.
     *
     * @param contactId the ID of the Contact to validate
     * @return true if the Contact exists; false otherwise or if an error occurs
     */
    private boolean validateContactId(String contactId) {
        try {
            Contact contact = contactDao.getContactById(contactId);
            return contact != null;
        } catch (Exception cause) {
            LOGGER.error("Error validating Contact ID: {}", cause.getMessage(), cause);
            return false;
        }
    }

    /**
     * Validates whether the given Individual ID exists in the system.
     *
     * @param individualId the ID of the Individual to validate
     * @return true if the Individual exists; false otherwise or if an error occurs
     */
    private boolean validateIndividualId(String individualId) {
        try {
            Individual individual = individualDao.getIndividualById(individualId);
            return individual != null;
        } catch (Exception cause) {
            LOGGER.error("Error validating Individual ID: {}", cause.getMessage(), cause);
            return false;
        }
    }

    /**
     * Validates whether the given Customer ID exists in the system.
     *
     * @param customerId the ID of the Customer to validate
     * @return true if the Customer exists; false otherwise or if an error occurs
     */
    private boolean validateCustomerId(String customerId) {
        try {
            Customer customer = customerDao.getCustomerById(customerId);
            return customer != null;
        } catch (Exception cause) {
            LOGGER.error("Error validating Customer ID: {}", cause.getMessage(), cause);
            return false;
        }
    }

    @Override
    public Boolean validate(String objectId, Map<ValidationResource, String> objectIdContext) {
        String objectType = objectIdContext.get(ValidationResource.OBJECT_TYPE);

        if (objectType == null || objectId == null || objectId.isBlank()) {
            return false;
        }

        return switch (objectType) {
            case "Account" -> validateAccountId(objectId);
            case "Case" -> validateCaseId(objectId);
            case "Contact" -> validateContactId(objectId);
            case "Individual" -> validateIndividualId(objectId);
            case "Customer" -> validateCustomerId(objectId);
            default -> throw new IllegalArgumentException("Unknown object type: " + objectType);
        };
    }

    @Override
    public String getFetchFieldName() {
        return SalesforceErrorHandlingResource.OBJECT_ID;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Salesforce object ID for the selected object type.";
    }

    @Override
    public String getConfirmationStepMessage(String objectId, Map<ValidationResource, String> objectIdContext) {
        String objectType = objectIdContext.get(ValidationResource.OBJECT_TYPE);
        return String.format("Object validation failed because the provided %s ID '%s' does not exist in Salesforce. " +
                "Please verify the ID is correct and accessible in your Salesforce system.", objectType, objectId);
    }

    @Override
    public String getErrMessage(String objectId) {
        return String.format("Object operation failed because the object ID '%s' is invalid or does not exist in Salesforce. " +
                "A valid Salesforce object ID is required to perform this operation. " +
                "Please verify the object ID exists in your Salesforce system and you have access to it.", objectId);
    }
}
