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

@Service
public class IndividualDaoImpl implements IndividualDao {

    public static final String INDIVIDUAL = "Individual";
    public final String QUERY_FIELDS = "id, createdDate, lastName, lastModifiedDate, birthDate, createdById";
    private final SalesforceService salesforceService;


    @Inject
    public IndividualDaoImpl(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    @Override
    public Individual getIndividualById(String id) throws IOException, ExecutionException, InterruptedException {
        return new IndividualImpl(Constants.GSON.fromJson(salesforceService.getRecordById(id, INDIVIDUAL), SFIndividual.class));
    }

    @Override
    public List<Individual> getIndividuals(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException {
        return getIndividualsByQuery("Select " + QUERY_FIELDS + " from Individual ORDER BY " + orderByField + " " +
                orderByOperand + " LIMIT " + size + " OFFSET " + Math.max((page - 1) * size, 0));
    }

    @Override
    public List<Individual> getIndividualsByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Failed to fetch individuals by query. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }

        String result = salesforceService.getRecordByQuery(query.trim());
        SalesforceListResponse<SFIndividual> individualsList =
                Constants.GSON.fromJson(result, new TypeToken<SalesforceListResponse<SFIndividual>>() {
                }.getType());
        if (individualsList.records.size() > 0) {
            return individualsList.records.stream().map(IndividualImpl::new).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public String createIndividual(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException("Failed to create Individual. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        var response = Constants.GSON.fromJson(salesforceService.createRecord(
                Constants.GSON.toJson(SFIndividual.from(entityDefinition)), INDIVIDUAL), Map.class);
        return String.valueOf(response.get("id"));
    }

    @Override
    public void updateIndividual(Individual individual) throws IOException, ExecutionException, InterruptedException, IllegalAccessException {
        if (individual == null || individual.getId() == null || individual.getId().isBlank()) {
            throw new IllegalAccessException("Failed to update Individual. " + "Individual not found to update.");
        }
        SFIndividual updatedIndividual = SFIndividual.from(individual);
        SFIndividual oldIndividual = SFIndividual.from(getIndividualById(individual.getId()));
        Map<String, String> differences = SalesforceService.getDifference(oldIndividual, updatedIndividual);
        salesforceService.updateRecord(individual.getId(), Constants.GSON.toJson(differences), INDIVIDUAL);

    }

    @Override
    public void deleteIndividualById(String primaryKey) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(primaryKey, INDIVIDUAL);
    }
}
