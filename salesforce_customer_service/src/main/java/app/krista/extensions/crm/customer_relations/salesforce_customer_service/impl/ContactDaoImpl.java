/*
 * Salesforce Customer Service Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFContact;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Contact;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.ContactDao;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.SalesforceListResponse;
import com.google.gson.reflect.TypeToken;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of the ContactDao interface that interacts with Salesforce.
 * <p>
 * This class provides methods to create, read, update, and delete Contact records
 * in Salesforce. It handles the communication with the Salesforce API and converts
 * between the API's data format and the application's Contact objects.
 */
@Service
public final class ContactDaoImpl implements ContactDao {

    public static final String QUERY_FIELDS = " Account.Name,AccountId,Description,department,email,phone,title,reportsToId,name,CreatedDate,LastModifiedDate,Id ";
    private static final String CONTACT = "Contact";
    private final SalesforceService salesforceService;

    /**
     * Creates a new ContactDaoImpl with the required Salesforce service.
     *
     * @param salesforceService Service for interacting with Salesforce API
     */
    @Inject
    public ContactDaoImpl(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    /**
     * Gets a Contact by its ID.
     *
     * @param id The ID of the Contact to retrieve
     * @return The Contact with the specified ID
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     * @throws RuntimeException If the Contact is not found
     */
    @Override
    public Contact getContactById(String id) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank()) {
            System.err.println("Failed to get contact by id request. " + Constants.ID_IS_NULL_OR_EMPTY);
        }
        return getContactsByQuery("Select " + QUERY_FIELDS + "  from Contact where id = '" + id + "'").stream().findFirst().orElseThrow(() -> new RuntimeException(CONTACT + " Not found for id " + id));
    }

    /**
     * Gets Contacts by executing a SOQL query.
     *
     * @param query The SOQL query to execute
     * @return List of Contacts matching the query
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public List<Contact> getContactsByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            System.err.println("Failed to get case by query request. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
        assert query != null;
        String res = salesforceService.getRecordByQuery(query.trim());
        SalesforceListResponse<SFContact> oppList =
                Constants.GSON.fromJson(res, new TypeToken<SalesforceListResponse<SFContact>>() {
                }.getType());
        if (!oppList.records.isEmpty()) {
            return oppList.records.stream().map(ContactImpl::new).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    /**
     * Creates a new Contact in Salesforce.
     *
     * @param entityDefinition Map containing the Contact fields and values
     * @return The ID of the newly created Contact
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public String createContact(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            System.err.println("Failed to create contact. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        var response = Constants.GSON.fromJson(salesforceService.createRecord(Constants.GSON.toJson(SFContact.from(entityDefinition)), CONTACT), Map.class);
        return String.valueOf(response.get("id"));
    }

    /**
     * Updates an existing Contact in Salesforce.
     * Only changed fields will be updated.
     *
     * @param contact The Contact with updated field values
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     * @throws IllegalAccessException If there's a problem accessing Contact fields
     */
    @Override
    public void updateContact(Contact contact) throws IOException, ExecutionException, InterruptedException, IllegalAccessException {
        if (contact == null || contact.getId() == null || contact.getId().isEmpty())
            System.err.println("Failed to update contact. Contact not found with given Id.");
        SFContact updatedContact = SFContact.from(contact);
        SFContact oldContact = SFContact.from(getContactById(contact.getId()));
        Map<String, String> diff = SalesforceService.getDifference(oldContact, updatedContact);
        salesforceService.updateRecord(contact.getId(), Constants.GSON.toJson(diff), CONTACT);
    }

    /**
     * Gets a paginated list of Contacts sorted by the specified field.
     *
     * @param pageSize Number of Contacts to retrieve per page
     * @param pageIndex Page number to retrieve (1-based)
     * @param orderByField Field to sort by
     * @param orderByOperand Sort direction (ASC or DESC)
     * @return List of Contacts for the specified page
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public List<Contact> getContacts(int pageSize, long pageIndex, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException {
        return getContactsByQuery("Select " + QUERY_FIELDS + " from Contact ORDER BY " + orderByField + " " + orderByOperand + " LIMIT " + pageSize + " OFFSET " + Math.max((pageIndex - 1) * pageSize, 0));
    }

    /**
     * Deletes a Contact from Salesforce by its ID.
     *
     * @param contactId The ID of the Contact to delete
     * @throws IOException If there's a problem with the API connection
     * @throws ExecutionException If the API request fails
     * @throws InterruptedException If the API request is interrupted
     */
    @Override
    public void deleteContactById(String contactId) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(contactId, CONTACT);
    }
}
