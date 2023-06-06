package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFIndividual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.DatePlus;

import java.util.Objects;

public class IndividualImpl implements Individual {

    private final SFIndividual sfIndividual;

    public IndividualImpl(SFIndividual sfIndividual) {
        this.sfIndividual = sfIndividual;
    }

    @Override
    public String getId() {
        return sfIndividual.id;
    }

    @Override
    public String getFirstName() {
        return sfIndividual.firstName;
    }

    @Override
    public String getLastName() {
        return sfIndividual.lastName;
    }

    @Override
    public Long getBirthDate() {
        return sfIndividual.birthDate != null
                ? Objects.requireNonNull(DatePlus.fromYYYYMMDD(sfIndividual.birthDate)).getTime()
                : null;
    }

    @Override
    public Long getCreatedDate() {
        return sfIndividual.createdDate != null ? sfIndividual.createdDate.getTime() : null;
    }

    @Override
    public String getCreatedById() {
        return sfIndividual.createdById;
    }

    @Override
    public Long getLastModifiedDate() {
        return sfIndividual.lastModifiedDate != null ? sfIndividual.lastModifiedDate.getTime() : null;
    }
}
