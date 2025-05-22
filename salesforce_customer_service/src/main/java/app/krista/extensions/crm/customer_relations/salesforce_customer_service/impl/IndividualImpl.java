package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFIndividual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.DatePlus;

import java.util.Objects;

/**
 * Implementation of the Individual interface for Salesforce integration.
 * This class provides concrete implementations of methods defined in the Individual
 * interface, specifically tailored for Salesforce CRM. It acts as an adapter between
 * the application's Individual interface and the Salesforce-specific SFIndividual model.
 * The implementation handles data mapping and conversion between different date formats
 * and representations used by the application and Salesforce.
 *
 * @see Individual
 * @see SFIndividual
 * @see DatePlus
 */
public class IndividualImpl implements Individual {

    private final SFIndividual sfIndividual;

    /**
     * Constructs a new IndividualImpl that wraps the provided SFIndividual instance.
     *
     * @param sfIndividual The Salesforce individual model to be wrapped by this implementation
     */
    public IndividualImpl(SFIndividual sfIndividual) {
        this.sfIndividual = sfIndividual;
    }

    /**
     * Retrieves the unique identifier of the individual.
     *
     * @return The Salesforce ID of the individual, or null if not set
     */
    @Override
    public String getId() {
        return sfIndividual.id;
    }

    /**
     * Retrieves the first name of the individual.
     *
     * @return The first name of the individual, or null if not set
     */
    @Override
    public String getFirstName() {
        return sfIndividual.firstName;
    }

    /**
     * Retrieves the last name of the individual.
     *
     * @return The last name of the individual, or null if not set
     */
    @Override
    public String getLastName() {
        return sfIndividual.lastName;
    }

    /**
     * Retrieves the birthdate of the individual as a timestamp.
     * Converts the Salesforce YYYY-MM-DD date format to a Unix timestamp (milliseconds since epoch).
     *
     * @return The birthdate as milliseconds since epoch, or null if the birthdate is not set
     * @throws NullPointerException If the birthdate string is in an invalid format that cannot be parsed
     */
    @Override
    public Long getBirthDate() {
        return sfIndividual.birthDate != null
                ? Objects.requireNonNull(DatePlus.fromYYYYMMDD(sfIndividual.birthDate)).getTime()
                : null;
    }
    /**
     * Retrieves the creation date of the individual record as a timestamp.
     *
     * @return The creation date as milliseconds since epoch, or null if the creation date is not set
     */
    @Override
    public Long getCreatedDate() {
        return sfIndividual.createdDate != null ? sfIndividual.createdDate.getTime() : null;
    }

    /**
     * Retrieves the ID of the user who created the individual record.
     *
     * @return The Salesforce ID of the user who created this record, or null if not set
     */
    @Override
    public String getCreatedById() {
        return sfIndividual.createdById;
    }

    /**
     * Retrieves the last modification date of the individual record as a timestamp.
     *
     * @return The last modification date as milliseconds since epoch, or null if the last modification date is not set
     */
    @Override
    public Long getLastModifiedDate() {
        return sfIndividual.lastModifiedDate != null ? sfIndividual.lastModifiedDate.getTime() : null;
    }
}