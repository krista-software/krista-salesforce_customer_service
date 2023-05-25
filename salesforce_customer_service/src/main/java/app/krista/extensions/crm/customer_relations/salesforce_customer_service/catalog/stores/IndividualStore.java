package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.stores;

import app.krista.extension.executor.SearchCondition;
import app.krista.extension.executor.SearchQuery;
import app.krista.extension.util.EntityStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.CatalogTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Individual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.IndividualDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.IndividualImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFIndividual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


@Service
public class IndividualStore implements EntityStore<Individual> {

    private final IndividualDaoImpl individualDao;

    @Inject
    private IndividualStore(IndividualDaoImpl individualDao) {
        this.individualDao = individualDao;
    }

    @Override
    public Individual create(Map<String, Object> entityDefinition) throws IOException {
        if (entityDefinition == null || entityDefinition.isEmpty()) {
            throw new IllegalArgumentException("Failed to create Individual. " + Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        }
        try {
            String individualId = individualDao.createIndividual(entityDefinition);
            return get(individualId);
        } catch (IOException | ExecutionException cause) {
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(interruptedException.getMessage());
        }
    }

    @Override
    public Individual get(String primaryKey) throws IOException {
        if (primaryKey == null || primaryKey.isBlank()) {
            throw new IllegalArgumentException("Failed to get individual. " + Constants.PRIMARY_KEY_IS_EMPTY);
        }
        try {
            return CatalogTypes.fromSFIndividual(individualDao.getIndividualById(primaryKey));
        } catch (IOException | ExecutionException cause) {
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(interruptedException.getMessage());
        }
    }

    @Override
    public Individual update(Individual individual) throws IOException {
        if (individual == null) {
            throw new IllegalArgumentException("Failed to update Individual. Individual not found for update.");
        }
        try {
            individualDao.updateIndividual(new IndividualImpl(SFIndividual.from(individual)));
            return get(individual.id);
        } catch (IOException | ExecutionException | IllegalAccessException cause) {
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(interruptedException.getMessage());
        }
    }

    @Override
    public void delete(String primaryKey) throws IOException {

        if (primaryKey == null || primaryKey.isBlank()) {
            throw new IllegalArgumentException("Failed to delete Individual. " + Constants.PRIMARY_KEY_IS_EMPTY);
        }
        try {
            individualDao.deleteIndividualById(primaryKey);
        } catch (IOException | ExecutionException cause) {
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interruptedException.getMessage());
        }
    }

    @Override
    public boolean contains(String s) throws IOException {
        return false;
    }

    @Override
    public List<Individual> search(List<SearchCondition> list, long l, int i) throws IOException {
        return null;
    }

    @Override
    public List<Individual> search(SearchQuery searchQuery, long l, int i) throws IOException {
        return null;
    }

    @Override
    public long count(List<SearchCondition> list) throws IOException {
        return 0;
    }

    @Override
    public List<String> lookup(Map<String, Object> map, List<String> list) throws IOException {
        return null;
    }

}
