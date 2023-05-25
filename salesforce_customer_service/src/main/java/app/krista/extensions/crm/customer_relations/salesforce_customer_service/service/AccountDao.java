package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface AccountDao {

    /**
     * Returns {@link Account} object of salesforce from the account id provided.
     *
     * @param id id of account to be searched
     * @return {@link Account} object of salesforce
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    Account getAccountById(String id) throws IOException, ExecutionException, InterruptedException;

    /**
     * Returns {@link List<Account>} list of object of salesforce.
     *
     * @param size           number of accounts to be fetched
     * @param page           page number to be fetched
     * @param orderByField
     * @param orderByOperand
     * @return {@link List<Account>} list of object of salesforce
     * @return empty list if does not find any records
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    List<Account> getAccounts(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException;

    /**
     * Returns {@link List<Account>} list of object of salesforce.
     *
     * @param query query to be searched
     * @return {@link List<Account>} list of object of salesforce
     * @return empty list if does not find any records
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    List<Account> getAccountByQuery(String query) throws IOException, ExecutionException, InterruptedException;

    /**
     * Add {@link Account} to salesforce from provided input fields
     *
     * @param entityDefinition fields which are added in the {@link Account} object of salesforce to create
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    String createAccount(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException;

    /**
     * Update {@link Account} to salesforce from provided input fields
     *
     * @param account {@link Account} object of salesforce to be updated
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    void updateAccount(Account account) throws IllegalAccessException, IOException, ExecutionException, InterruptedException;

    /**
     * This function is used to delete account from salesforce instance
     *
     * @param primaryKey key of account to be deleted
     */
    void deleteAccountById(String primaryKey) throws IOException, ExecutionException, InterruptedException;
}
