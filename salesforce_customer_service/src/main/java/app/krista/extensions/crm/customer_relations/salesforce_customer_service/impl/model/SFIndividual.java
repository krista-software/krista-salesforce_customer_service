package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Individual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.DatePlus;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Map;

public final class SFIndividual {
    @SerializedName("Id")
    public String id;

    @SerializedName("FirstName")
    public String firstName;

    @SerializedName("LastName")
    public String lastName;

    @SerializedName("CreatedDate")
    public Date createdDate;

    @SerializedName("BirthDate")
    public String birthDate;

    @SerializedName("CreatedById")
    public String createdById;

    @SerializedName("LastModifiedDate")
    public Date lastModifiedDate;

    public static SFIndividual from(Individual sfIndividual) {
        if (sfIndividual == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        SFIndividual individual = new SFIndividual();
        individual.id = sfIndividual.id;
        individual.firstName = sfIndividual.firstName;
        individual.lastName = sfIndividual.lastName;
        if (sfIndividual.birthDate != null && sfIndividual.birthDate > 0) {
            individual.birthDate = DatePlus.getYYYYMMDD(sfIndividual.birthDate);
        }
        individual.createdById = sfIndividual.createdById;
        if (sfIndividual.lastModifiedDate != null) {
            individual.lastModifiedDate = new Date(sfIndividual.lastModifiedDate);
        }
        if (sfIndividual.createdDate != null) {
            individual.createdDate = new Date(sfIndividual.createdDate);
        }
        return individual;
    }

    public static SFIndividual from(app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual sfIndividual) {
        if (sfIndividual == null) {
            throw new IllegalArgumentException(Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        SFIndividual individual = new SFIndividual();
        individual.id = sfIndividual.getId();
        individual.firstName = sfIndividual.getFirstName();
        individual.lastName = sfIndividual.getLastName();
        individual.birthDate = DatePlus.getYYYYMMDD(sfIndividual.getBirthDate());
        individual.createdById = sfIndividual.getCreatedById();

        return individual;
    }

    public static SFIndividual from(Map<String, Object> entityDefinition) {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException("Individual entity is empty!");
        }
        SFIndividual individual = new SFIndividual();
        individual.id = (String) entityDefinition.get("Id");
        individual.firstName = (String) entityDefinition.get("First Name");
        individual.lastName = (String) entityDefinition.get("Last Name");
        Long parseBirthDate = (Long) entityDefinition.get("Birth Date");
        individual.birthDate = parseBirthDate != null && parseBirthDate > 0 ? DatePlus.getYYYYMMDD(parseBirthDate) : null;

        if (entityDefinition.get("Created Date") != null) {
            individual.createdDate = new Date((Long) entityDefinition.get("Created Date"));
        }
        individual.createdById = (String) entityDefinition.get("Created By Id");
        if (entityDefinition.get("Last Modified Date") != null) {
            individual.lastModifiedDate = new Date((Long) entityDefinition.get("Last Modified Date"));
        }

        return individual;
    }
}
