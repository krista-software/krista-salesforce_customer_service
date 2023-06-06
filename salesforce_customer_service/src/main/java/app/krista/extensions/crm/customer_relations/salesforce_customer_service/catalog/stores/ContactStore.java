package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.stores;

import app.krista.extension.executor.QueryClause;
import app.krista.extension.executor.SearchCondition;
import app.krista.extension.executor.SearchQuery;
import app.krista.extension.util.EntityStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.CatalogTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Contact;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.ContactDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.ContactImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFContact;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.ContactDaoImpl.QUERY_FIELDS;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.AND;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.WHERE;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.StringHelper.isStringAvailable;


@Service
public final class ContactStore implements EntityStore<Contact> {


    private final ContactDaoImpl contactDao;

    @Inject
    private ContactStore(ContactDaoImpl contactDao) {
        this.contactDao = contactDao;
    }

    /**
     * @param entityDefinition input map with {@link Contact} entity fields.
     * @return {@link Contact} for the given input
     * @throws InterruptedException if there is any interruption during execution of Salesforce API.
     */
    @Override
    public Contact create(Map<String, Object> entityDefinition) throws InterruptedIOException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException("Failed to create contact. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        try {
            String contactId = contactDao.createContact(entityDefinition);
            return get(contactId);
        } catch (IOException | ExecutionException cause) {
            throw new RuntimeException(cause.getMessage());
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(illegalArgumentException.getMessage());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(interruptedException.getMessage());
        }
    }

    /**
     * @param primaryKey Id of the {@link Contact}
     * @return {@link Contact} for the given Id.
     * @throws InterruptedException if there is any interruption during execution of Salesforce API.
     */
    @Override
    public Contact get(String primaryKey) throws InterruptedIOException {
        if (primaryKey == null || primaryKey.isBlank()) {
            throw new IllegalArgumentException("Failed to get contact. " + Constants.PRIMARY_KEY_IS_EMPTY);
        }
        try {
            return CatalogTypes.fromSFContact(contactDao.getContactById(primaryKey));
        } catch (IOException | ExecutionException cause) {
            throw new RuntimeException(cause.getMessage());
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(illegalArgumentException.getMessage());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(interruptedException.getMessage());
        }
    }

    /**
     * @param contact {@link Contact} object is given as input to update.
     * @return returns the updated entity {@link Contact}
     * @throws InterruptedIOException if there is interruption during execution of salesforce API
     */
    @Override
    public Contact update(Contact contact) throws InterruptedIOException {
        if (contact == null) {
            throw new IllegalArgumentException("Failed to update contact. Contact not found for update");
        }
        try {
            contactDao.updateContact(new ContactImpl(SFContact.from(contact)));
            return get(contact.id);
        } catch (IOException | ExecutionException | IllegalAccessException cause) {
            throw new RuntimeException(cause.getMessage());
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(illegalArgumentException.getMessage());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(interruptedException.getMessage());
        }
    }

    @Override
    public void delete(String primaryKey) {
        if (primaryKey == null || primaryKey.isBlank()) {
            throw new IllegalArgumentException("Failed to delete case." + Constants.PRIMARY_KEY_IS_EMPTY);
        }
        try {
            contactDao.deleteContactById(primaryKey);
        } catch (IOException | ExecutionException cause) {
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interruptedException.getMessage(), interruptedException);
        }
    }

    @Override
    public boolean contains(String s) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<Contact> search(List<SearchCondition> conditions, long pageIndex, int pageSize) {
        return List.of();
    }

    /**
     * @param searchQuery list of search conditions and query clauses given as input
     * @param pageIndex   page number of the query results
     * @param pageSize    number of results per page
     * @return returns {@link List<Contact>} as per the given query
     * @throws IOException if there is any error in the given input
     */
    @Override
    public List<Contact> search(SearchQuery searchQuery, long pageIndex, int pageSize) throws IOException {
        final List<SearchCondition> conditions = searchQuery.getSearchConditions();
        final List<QueryClause> queryClauses = searchQuery.getQueryClauses();
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Failed to search contact. Conditions not found.");
        }
        String orderByField = queryClauses.isEmpty() ? Constants.LASTMODIFIEDDATE : queryClauses.get(0).getFieldName();
        String orderByOperand = queryClauses.isEmpty() ? Constants.DESC : queryClauses.get(0).getOperand().toString();
        try {
            if (conditions.size() == 1) {
                return contactDao.getContacts(pageSize, pageIndex, orderByField, orderByOperand).stream().map(CatalogTypes::fromSFContact).collect(Collectors.toList());
            } else {
                String query = getQuery(conditions, pageSize, pageIndex, orderByField, orderByOperand);
                return contactDao.getContactsByQuery(query).stream().map(CatalogTypes::fromSFContact).collect(Collectors.toList());
            }
        } catch (IOException | ExecutionException cause) {
            throw new RuntimeException(cause.getMessage());
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(illegalArgumentException.getMessage());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(interruptedException.getMessage());
        }
    }

    @Override
    public long count(List<SearchCondition> list) {
        return 0;
    }

    @Override
    public List<String> lookup(Map<String, Object> map, List<String> list) {
        return Collections.emptyList();
    }

    private String getConditionValue(List<SearchCondition> conditions, String fieldName) {
        for (SearchCondition con : conditions) {
            if (fieldName.equalsIgnoreCase(con.getFieldName())) {
                return con.getOperand() != null ? String.valueOf(con.getOperand()).trim() : "";
            }
        }
        return "";
    }

    private String getQuery(List<SearchCondition> searchConditions, long size, long page, String orderByField, String orderByOperand) {
        String name = getConditionValue(searchConditions, Constants.NAME);
        String accountId = getConditionValue(searchConditions, Constants.ACCOUNT_ID);
        String accountName = getConditionValue(searchConditions, Constants.ACCOUNT_NAME);
        String phone = getConditionValue(searchConditions, Constants.PHONE);
        String email = getConditionValue(searchConditions, Constants.EMAIL);
        return createQuery(name, accountId, accountName, phone, email, size, page, orderByField, orderByOperand);
    }

    private String createQuery(String name, String accountId, String accountName, String phone, String email, long size, long page, String orderByField, String orderByOperand) {
        StringBuilder builder = new StringBuilder();
        builder.append("Select " + QUERY_FIELDS + " from Contact " + WHERE)
                .append(isStringAvailable(name) ? " Name = '" + name + "' " + AND : "")
                .append(isStringAvailable(accountId) ? " AccountId = '" + accountId + "' " + AND : "")
                .append(isStringAvailable(accountName) ? " Account.Name = '" + accountName + "' " + AND : "")
                .append((isStringAvailable(phone) ? " Phone LIKE '%" + phone + "%' " + AND : ""))
                .append((isStringAvailable(email) ? " Email LIKE '%" + email + "%' " + AND : ""));
        StringHelper.setQueryEndIndex(builder);
        StringHelper.appendOrderByClause(size, page, orderByField, orderByOperand, builder);
        return builder.toString();
    }

}
