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

@Service
public final class ContactDaoImpl implements ContactDao {

    public static final String QUERY_FIELDS = " Account.Name,AccountId,Description,department,email,phone,title,reportsToId,name,CreatedDate,LastModifiedDate,Id ";
    private static final String CONTACT = "Contact";
    private final SalesforceService salesforceService;

    @Inject
    public ContactDaoImpl(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    @Override
    public Contact getContactById(String id) throws IOException, ExecutionException, InterruptedException {
        if (id == null || id.isBlank()) {
            System.err.println("Failed to get contact by id request. " + Constants.ID_IS_NULL_OR_EMPTY);
        }
        return getContactsByQuery("Select " + QUERY_FIELDS + "  from Contact where id = '" + id + "'").stream().findFirst().orElseThrow(() -> new RuntimeException(CONTACT + " Not found for id " + id));
    }

    @Override
    public List<Contact> getContactsByQuery(String query) throws IOException, ExecutionException, InterruptedException {
        if (query == null || query.isBlank()) {
            System.err.println("Failed to get case by query request. " + Constants.QUERY_IS_NULL_OR_EMPTY);
        }
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

    @Override
    public String createContact(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            System.err.println("Failed to create contact. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        var response = Constants.GSON.fromJson(salesforceService.createRecord(Constants.GSON.toJson(SFContact.from(entityDefinition)), CONTACT), Map.class);
        return String.valueOf(response.get("id"));
    }

    @Override
    public void updateContact(Contact contact) throws IOException, ExecutionException, InterruptedException, IllegalAccessException {
        if (contact == null || contact.getId() == null || contact.getId().isEmpty())
            System.err.println("Failed to update contact. Contact not found with given Id.");
        SFContact updatedContact = SFContact.from(contact);
        SFContact oldContact = SFContact.from(getContactById(contact.getId()));
        Map<String, String> diff = SalesforceService.getDifference(oldContact, updatedContact);
        salesforceService.updateRecord(contact.getId(), Constants.GSON.toJson(diff), CONTACT);
    }

    @Override
    public List<Contact> getContacts(int pageSize, long pageIndex, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException {
        return getContactsByQuery("Select " + QUERY_FIELDS + " from Contact ORDER BY " + orderByField + " " + orderByOperand + " LIMIT " + pageSize + " OFFSET " + Math.max((pageIndex - 1) * pageSize, 0));
    }

    @Override
    public void deleteContactById(String contactId) throws IOException, ExecutionException, InterruptedException {
        salesforceService.deleteRecordById(contactId, CONTACT);
    }
}
