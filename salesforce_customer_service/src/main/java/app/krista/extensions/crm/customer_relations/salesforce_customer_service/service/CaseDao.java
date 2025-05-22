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
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    Case getCaseById(String id) throws IOException, ExecutionException, InterruptedException;

    /**
     * Returns {@link List<Case>} list of object of salesforce.
     *
     * @param query query to be searched
     * @return empty list if it does not find any records
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    List<Case> getCasesByQuery(String query) throws IOException, ExecutionException, InterruptedException;

    /**
     * Returns {@link List<Case>} list of object of salesforce.
     *
     * @param size           number of cases to be fetched
     * @param page           page number to be fetched
     * @param orderByField   field name used for sorting the results
     * @param orderByOperand sorting direction, e.g., "ASC" or "DESC"
     * @return empty list if it does not find any records
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    List<Case> getCases(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException;

    /**
     * Add {@link Case} to salesforce from provided input fields
     *
     * @param entityDefinition fields which are added in the {@link Case} object of salesforce to create
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    String createCase(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException;

    /**
     * Update {@link Case} to salesforce from provided input fields
     *
     * @param aCase {@link Case} object of salesforce to be updated
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    void updateCase(Case aCase) throws IOException, IllegalAccessException, ExecutionException, InterruptedException;

    /**
     * This function is used to delete Case from salesforce customer service instance
     *
     * @param primaryKey id of the account to be deleted
     */
    void deleteCaseById(String primaryKey) throws IOException, ExecutionException, InterruptedException;

    /**
     * This function is used to get the feed updates for the given case id.
     *
     * @param caseId case entity id
     * @return list of Feed items.
     */
    List<Map<String, Object>> getFeedItemsForCaseId(String caseId, Double numberOfPosts) throws IOException;

    /**
     * This function will add update to the feed posts.
     *
     * @param caseId              case entity id.
     * @param updateMessage       message to be added to the post
     * @param base64EncodedString base64EncodedString of file to be added.
     * @param fileName            filename to be added to the post
     * @return {@link Boolean} value of updated or not
     */
    Boolean addFeedUpdateToCaseById(String caseId, String updateMessage, String base64EncodedString, String fileName) throws ExecutionException, InterruptedException;
}
