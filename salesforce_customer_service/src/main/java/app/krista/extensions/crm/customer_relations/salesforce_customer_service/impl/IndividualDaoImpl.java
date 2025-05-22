package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFIndividual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.IndividualDao;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.SalesforceListResponse;
import com.google.gson.reflect.TypeToken;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of {@link IndividualDao} that handles CRUD operations for Individuals
 * using the SalesforceService for integration with Salesforce data.
 */
@Service
public class IndividualDaoImpl implements IndividualDao {

    public static final String INDIVIDUAL = "Individual";
    public static final String QUERY_FIELDS = "id, createdDate, firstName, lastName, lastModifiedDate, birthDate, createdById";
    private final SalesforceService salesforceService;

    /**
     * Constructs a new IndividualDaoImpl with the given SalesforceService.
     *
     * @param salesforceService the Salesforce service used to perform data operations
     */
    @Inject
    public IndividualDaoImpl(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    /**
     * Retrieves an Individual by ID.
     *
     * @param id the ID of the Individual
     * @return the Individual object
     * @throws IOException              if a network or IO error occurs
     * @throws ExecutionException       if the Salesforce call fails
     * @throws InterruptedException     if the operation is interrupted
     */
    @Override
    public Individual getIndividualById(String id) throws IOException, ExecutionException, InterruptedException {
        return new IndividualImpl(Constants.GSON.fromJson(salesforceService.getRecordById(id, INDIVIDUAL), SFIndividual.class));
    }

    /**
     * Retrieves a paginated list of Individuals ordered by the given field and operand.
     *
     * @param size           number of records to fetch
     * @param page           page number (1-based index)
     * @param orderByField   the field to sort by
     * @param orderByOperand the sorting direction (ASC or DESC)
     * @return a list of Individual objects
     * @throws IOException              if a network or IO error occurs
     * @throws ExecutionException       if the Salesforce call fails
     * @throws InterruptedException     if the operation is interrupted
     */
    @Override
    public List<Individual> getIndividuals(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException {
        return getIndividualsByQuery("Select " + QUERY_FIELDS + " from Individual ORDER BY " + orderByField + " " +
                orderByOperand + " LIMIT " + size + " OFFSET " + Math.max((page - 1) * size, 0));
    }

    /**
     * Executes a custom SOQL query to retrieve Individuals.
     *
     * @param query the SOQL query to execute
     * @return a list of Individuals matching the query
     * @throws IOException              if a network or IO error occurs
     * @throws ExecutionException       if the Salesforce call fails
     * @throws InterruptedException     if the operation is interrupted
     * @throws IllegalArgumentException if the query is null or blank
     */
    @Override
    public List<Individual> getIndividualsByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Failed to fetch individuals by query. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }

        String result = salesforceService.getRecordByQuery(query.trim());
        SalesforceListResponse<SFIndividual> individualsList =
                Constants.GSON.fromJson(result, new TypeToken<SalesforceListResponse<SFIndividual>>() {
                }.getType());
        if (!individualsList.records.isEmpty()) {
            return individualsList.records.stream().map(IndividualImpl::new).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Creates a new Individual record in Salesforce.
     *
     * @param entityDefinition a map of field names and values for the new Individual
     * @return the ID of the created Individual
     * @throws IOException              if a network or IO error occurs
     * @throws ExecutionException       if the Salesforce call fails
     * @throws InterruptedException     if the operation is interrupted
     * @throws IllegalArgumentException if the input entityDefinition is null or empty
     */
    @Override
    public String createIndividual(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException("Failed to create Individual. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        var response = Constants.GSON.fromJson(salesforceService.createRecord(
                Constants.GSON.toJson(SFIndividual.from(entityDefinition)), INDIVIDUAL), Map.class);
        return String.valueOf(response.get("id"));
    }

    /**
     * Updates an existing Individual in Salesforce.
     *
     * @param individual the Individual object containing updated data
     * @throws IOException              if a network or IO error occurs
     * @throws ExecutionException       if the Salesforce call fails
     * @throws InterruptedException     if the operation is interrupted
     * @throws IllegalAccessException   if the individual or ID is null/blank
     */
    @Override
    public void updateIndividual(Individual individual) throws IOException, ExecutionException, InterruptedException, IllegalAccessException {
        if (individual == null || individual.getId() == null || individual.getId().isBlank()) {
            throw new IllegalAccessException("Failed to update Individual. Individual not found to update.");
        }
        SFIndividual updatedIndividual = SFIndividual.from(individual);
        SFIndividual oldIndividual = SFIndividual.from(getIndividualById(individual.getId()));
        Map<String, String> differences = SalesforceService.getDifference(oldIndividual, updatedIndividual);
        salesforceService.updateRecord(individual.getId(), Constants.GSON.toJson(differences), INDIVIDUAL);
    }

    /**
     * Deletes an Individual record by primary key (ID).
     *
     * @param primaryKey the ID of the Individual to delete
     * @throws IOException              if a network or IO error occurs
     * @throws ExecutionException       if the Salesforce call fails
     * @throws InterruptedException     if the operation is interrupted
     */
    @Override
    public void deleteIndividualById(String primaryKey) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(primaryKey, INDIVIDUAL);
    }
}
