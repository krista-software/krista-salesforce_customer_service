package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface ContactDao {

    /**
     * Returns {@link Contact} object of salesforce from the contact id provided.
     *
     * @param id id of contact to be searched
     * @return {@link Contact} object of salesforce
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    Contact getContactById(String id) throws IOException, ExecutionException, InterruptedException;

    /**
     * Returns {@link List<Contact>} list of object of salesforce.
     *
     * @param query query to be searched
     * @return {@link List<Contact>} list of object of salesforce
     * @return empty list if does not find any records
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    List<Contact> getContactsByQuery(String query) throws IOException, ExecutionException, InterruptedException;


    /**
     * Add {@link Contact} to salesforce from provided input fields
     *
     * @param entityDefinition fields which are added in the {@link Contact} object of salesforce to create
     * @return
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    String createContact(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException;

    /**
     * Update {@link Contact} to salesforce from provided input fields
     *
     * @param contact {@link Contact} object of salesforce to be updated
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    void updateContact(Contact contact) throws IOException, ExecutionException, InterruptedException, IllegalAccessException;


    /**
     * Returns {@link List<Contact>} list of object of salesforce.
     *
     * @param pageSize       number of contact to be fetched
     * @param pageIndex      page number to be fetched
     * @param orderByField
     * @param orderByOperand
     * @return {@link List<Contact>} list of object of salesforce
     * @return empty list if does not find any records
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    List<Contact> getContacts(int pageSize, long pageIndex, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException;

    /**
     * This function is used to delete contact from salesforce customer service instance
     *
     * @param primaryKey id of the contact to be deleted
     */
    void deleteContactById(String primaryKey) throws IOException, ExecutionException, InterruptedException;
}
