package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.*;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;

public final class CatalogTypes {

    private CatalogTypes() {
    }

    public static Case fromSFCase(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case sfCase) {
        if (sfCase == null) {
            throw new IllegalArgumentException("Case entity is empty");
        }
        Case newCase = new Case();
        newCase.id = sfCase.getId();
        newCase.contactId = sfCase.getContactId();
        newCase.accountId = sfCase.getAccountId();
        newCase.contactName = sfCase.getContactName();
        newCase.subject = sfCase.getSubject();
        newCase.isClosed = sfCase.getIsClosed();
        newCase.ownerId = sfCase.getOwnerId();
        newCase.accountName = sfCase.getAccountName();
        newCase.createdDate = sfCase.getCreatedDate();
        newCase.lastModifiedDate = sfCase.getLastModifiedDate();
        newCase.caseNumber = sfCase.getCaseNumber();
        newCase.contactEmail = sfCase.getContactEmail();
        newCase.contactPhone = sfCase.getContactPhone();
        newCase.status = sfCase.getStatus();
        newCase.reason = sfCase.getReason();
        newCase.origin = sfCase.getOrigin();
        newCase.description = sfCase.getDescription();
        newCase.priority = sfCase.getPriority();
        return newCase;
    }

    public static Contact fromSFContact(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Contact sfContact) {
        if (sfContact == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Contact newContact =
                new app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Contact();
        newContact.name = sfContact.getName();
        newContact.title = sfContact.getTitle();
        newContact.reportsToId = sfContact.getReportsToId();
        newContact.id = sfContact.getId();
        newContact.accountId = sfContact.getAccountId();
        newContact.createdDate = sfContact.getCreatedDate();
        newContact.accountName = sfContact.getAccountName();
        newContact.lastModifiedDate = sfContact.getLastModifiedDate();
        newContact.department = sfContact.getDepartment();
        newContact.phone = sfContact.getPhone();
        newContact.description = sfContact.getDescription();
        newContact.email = sfContact.getEmail();
        return newContact;
    }

    public static Account fromSFAccount(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Account sfAccount) {
        if (sfAccount == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        Account account = new Account();
        account.id = sfAccount.getId();
        account.name = sfAccount.getName();
        account.type = sfAccount.getType();
        account.fax = sfAccount.getFax();
        account.accountNumber = sfAccount.getAccountNumber();
        account.website = sfAccount.getWebsite();
        account.sic = sfAccount.getSic();
        account.ownerId = sfAccount.getOwnerId();
        account.createdById = sfAccount.getCreatedById();
        account.cleanStatus = sfAccount.getCleanStatus();
        account.customerPriority = sfAccount.getCustomerPriority();
        account.sLASerialNumber = sfAccount.getSLASerialNumber();
        account.createdDate = sfAccount.getCreatedDate();
        account.billingStreet = sfAccount.getBillingStreet();
        account.billingCity = sfAccount.getBillingCity();
        account.billingState = sfAccount.getBillingState();
        account.billingPostalCode = sfAccount.getBillingPostalCode();
        account.billingCountry = sfAccount.getBillingCountry();
        account.shippingStreet = sfAccount.getShippingStreet();
        account.shippingCity = sfAccount.getShippingCity();
        account.shippingState = sfAccount.getShippingState();
        account.shippingPostalCode = sfAccount.getShippingPostalCode();
        account.shippingCountry = sfAccount.getShippingCountry();
        account.phone = sfAccount.getPhone();
        account.numberOfEmployees = sfAccount.getNumberOfEmployees();
        account.sLAExpirationDate = sfAccount.getSLAExpirationDate();
        account.ownership = sfAccount.getOwnership();
        account.industry = sfAccount.getIndustry();
        return account;
    }

    public static Individual fromSFIndividual(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual sfIndividual) {
        if (sfIndividual == null) {
            throw new IllegalArgumentException("Individual entity is empty!");
        }
        Individual newIndividual = new Individual();
        newIndividual.id = sfIndividual.getId();
        newIndividual.firstName = sfIndividual.getFirstName();
        newIndividual.lastName = sfIndividual.getLastName();
        newIndividual.birthDate = sfIndividual.getBirthDate();
        newIndividual.createdDate = sfIndividual.getCreatedDate();
        newIndividual.createdById = sfIndividual.getCreatedById();
        newIndividual.lastModifiedDate = sfIndividual.getLastModifiedDate();

        return newIndividual;
    }

    public static Customer fromSFCustomer(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer sfCustomer) {
        if (sfCustomer == null) {
            throw new IllegalArgumentException("Customer entity is empty!");
        }
        Customer newCustomer = new Customer();
        newCustomer.id = sfCustomer.getId();
        newCustomer.name = sfCustomer.getName();
        newCustomer.partyId = sfCustomer.getPartyId();
        newCustomer.customerStatusType = sfCustomer.getCustomerStatusType();
        newCustomer.totalLifeTimeValue = sfCustomer.getTotalLifeTimeValue();

        return newCustomer;
    }
}
