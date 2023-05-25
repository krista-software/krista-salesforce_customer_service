package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface CaseDao {

    /**
     * Returns {@link Case} object of salesforce from the case id provided.
     *
     * @param id id of case to be searched
     * @return {@link Case} object of salesforce
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    Case getCaseById(String id) throws IOException, ExecutionException, InterruptedException;

    /**
     * Returns {@link List<Case>} list of object of salesforce.
     *
     * @param query query to be searched
     * @return {@link List<Case>} list of object of salesforce
     * @return empty list if does not find any records
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    List<Case> getCasesByQuery(String query) throws IOException, ExecutionException, InterruptedException;

    /**
     * Returns {@link List<Case>} list of object of salesforce.
     *
     * @param size           number of cases to be fetched
     * @param page           page number to be fetched
     * @param orderByField
     * @param orderByOperand
     * @return {@link List<Case>} list of object of salesforce
     * @return empty list if does not find any records
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    List<Case> getCases(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException;

    /**
     * Add {@link Case} to salesforce from provided input fields
     *
     * @param entityDefinition fields which are added in the {@link Case} object of salesforce to create
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    String createCase(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException;

    /**
     * Update {@link Case} to salesforce from provided input fields
     *
     * @param aCase {@link Case} object of salesforce to be updated
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    void updateCase(Case aCase) throws IOException, IllegalAccessException, ExecutionException, InterruptedException;

    /**
     * This function is used to delete Case from salesforce customer service instance
     *
     * @param primaryKey id of the account to be deleted
     */
    void deleteCaseById(String primaryKey) throws IOException, ExecutionException, InterruptedException;
}
