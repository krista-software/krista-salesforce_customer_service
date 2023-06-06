package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;

import app.krista.extension.impl.anno.*;

import java.util.Map;

@Searchable
@Domain(id = "catEntryDomain_a66f83b1-fa58-427f-b8b9-0c83a401bf53",
        name = "Customer Relations",
        ecosystemId = "catEntryEcosystem_8a0e8ce7-6289-492f-9202-52284024280c",
        ecosystemName = "CRM",
        ecosystemVersion = "749622ef-f244-4727-b5b4-3f16d68f3ad3")
@Entity(name = "Individual", id = "localDomainEntity_c9073255-1153-4231-970b-b693048638b0", primaryKey = "Id")
public class Individual {

    public static final String BIRTH_DATE = "Birth Date";
    @Searchable
    @ToString
    @Field.Text(name = "Id", required = false)
    public String id;

    @ToString
    @Field.Text(name = "First Name", required = false)
    public String firstName;
    @Searchable
    @ToString
    @Field.Text(name = "Last Name")
    public String lastName;

    @Field.Date(name = BIRTH_DATE, showHowManyDaysInViewer = 60, allowFuture = false, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public Long birthDate;

    @Field.Text(name = "Created By Id", required = false)
    public String createdById;

    @Field.Date(name = "Created Date", showHowManyDaysInViewer = 60, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public Long createdDate;

    @Field.Date(name = "Last Modified Date", showHowManyDaysInViewer = 60, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public Long lastModifiedDate;

    public static Individual fromFields(Map<String, Object> individual) {
        Individual newIndividual = new Individual();
        newIndividual.id = (String) individual.get("Id");
        newIndividual.firstName = (String) individual.get("First Name");
        newIndividual.lastName = (String) individual.get("Last Name");
        if (individual.get(BIRTH_DATE) != null && (Long) individual.get(BIRTH_DATE) > 0) {
            newIndividual.birthDate = (Long) individual.get(BIRTH_DATE);
        }
        newIndividual.createdById = (String) individual.get("Created By Id");
        newIndividual.createdDate = (Long) individual.get("Created Date");
        newIndividual.lastModifiedDate = (Long) individual.get("Last Modified Date");

        return newIndividual;
    }
}
