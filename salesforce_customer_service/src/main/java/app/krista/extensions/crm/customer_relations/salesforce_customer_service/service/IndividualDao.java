package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface IndividualDao {

    Individual getIndividualById(String id) throws IOException, ExecutionException, InterruptedException;

    List<Individual> getIndividuals(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException;

    List<Individual> getIndividualsByQuery(String query) throws IOException, ExecutionException, InterruptedException;

    String createIndividual(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException;

    void updateIndividual(Individual individual) throws IOException, ExecutionException, InterruptedException, IllegalAccessException;

    void deleteIndividualById(String primaryKey) throws IOException, ExecutionException, InterruptedException;

}
