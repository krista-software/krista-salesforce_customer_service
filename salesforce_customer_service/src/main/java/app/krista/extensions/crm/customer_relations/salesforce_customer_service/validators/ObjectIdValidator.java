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
        return "Please enter the object id for the selected type.";
    }

    @Override
    public String getConfirmationStepMessage(String objectId, Map<ValidationResource, String> objectIdContext) {
        return String.format("The provided object id: %s is invalid.", objectId);
    }

    @Override
    public String getErrMessage(String objectId) {
        return "The provided object id \"" + objectId + "\" is " + Constants.INVALID + " or does not exist in Salesforce.";
    }
}
