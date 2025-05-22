package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.stores;

import app.krista.extension.executor.QueryClause;
import app.krista.extension.executor.SearchCondition;
import app.krista.extension.executor.SearchQuery;
import app.krista.extension.util.EntityStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.CatalogTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Case;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCase;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.CaseQueryBuilder;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public final class CaseStore implements EntityStore<Case> {

    private final CaseDaoImpl caseDao;

    @Inject
    private CaseStore(CaseDaoImpl caseDao) {
        this.caseDao = caseDao;
    }

    /**
     * @param entityDefinition input map with {@link Case} entity fields.
     * @return {@link Case} for the given input
     */
    @Override
    public Case create(Map<String, Object> entityDefinition) throws InterruptedIOException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException("Failed to create case." + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        try {
            String caseId = caseDao.createCase(entityDefinition);
            return get(caseId);
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
     * @param primaryKey Id of the {@link Case}
     * @return {@link Case} for the given Id.
     */
    @Override
    public Case get(String primaryKey) throws InterruptedIOException {
        if (primaryKey == null || primaryKey.isBlank()) {
            throw new IllegalArgumentException("Failed to get case." + Constants.PRIMARY_KEY_IS_EMPTY);
        }
        try {
            return CatalogTypes.fromSFCase(caseDao.getCaseById(primaryKey));
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
     * @param aCase {@link Case} object is given as input to update.
     * @return returns the updated entity {@link Case}
     * @throws InterruptedIOException if there is interruption during execution of salesforce API
     */
    @Override
    public Case update(Case aCase) throws InterruptedIOException {
        if (aCase == null) {
            throw new IllegalArgumentException("Failed to update case. Case not found for update");
        }
        try {
            caseDao.updateCase(new CaseImpl(SFCase.from(aCase)));
            return get(aCase.id);
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
            caseDao.deleteCaseById(primaryKey);
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
    public List<Case> search(List<SearchCondition> conditions, long pageIndex, int pageSize) {
        return List.of();
    }

    /**
     * @param searchQuery list of search conditions and query clauses given as input
     * @param pageIndex   page number of the query results
     * @param pageSize    number of results per page
     * @return returns {@link List<Case>} as per the given query
     * @throws IOException if there is any error in the given input
     */
    @Override
    public List<Case> search(SearchQuery searchQuery, long pageIndex, int pageSize) throws IOException {
        final List<SearchCondition> conditions = searchQuery.getSearchConditions();
        final List<QueryClause> queryClauses = searchQuery.getQueryClauses();
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Failed to search case. " + Constants.CONDITIONS_NOT_FOUND);
        }
        try {
            String orderByField = queryClauses.isEmpty() ? Constants.LASTMODIFIEDDATE : queryClauses.getFirst().getFieldName();
            String orderByOperand = queryClauses.isEmpty() ? Constants.DESC : queryClauses.getFirst().getOperand().toString();
            if (conditions.size() == 1) {
                return caseDao.getCases(pageSize, pageIndex, orderByField, orderByOperand).stream().map(CatalogTypes::fromSFCase).collect(Collectors.toList());
            } else {
                String query = CaseQueryBuilder.getQuery(conditions, pageSize, pageIndex, orderByField, orderByOperand);
                return caseDao.getCasesByQuery(query).stream().map(CatalogTypes::fromSFCase).collect(Collectors.toList());
            }
        } catch (IOException | ExecutionException cause) {

            throw new RuntimeException(Constants.FAILED_TO_SEARCH + cause.getMessage());

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
}
