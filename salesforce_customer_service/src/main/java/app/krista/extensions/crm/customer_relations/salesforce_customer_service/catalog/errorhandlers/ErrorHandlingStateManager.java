package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers;

import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.GSON;

/**
 * Service class responsible for managing error handling state.
 * This class provides functionality to store and retrieve error state information
 * using a key-value store mechanism.
 */
@Service
public class ErrorHandlingStateManager {

    private final KeyValueStore internalState;
    private static final Map<String, Object> internalState2 = new HashMap<>();

    /**
     * Constructs an ErrorHandlingStateManager with the specified KeyValueStore.
     *
     * @param internalState The KeyValueStore used to persist error handling state information
     */
    @Inject
    public ErrorHandlingStateManager(KeyValueStore internalState) {
        this.internalState = internalState;
    }

    /**
     * Stores a value in the internal state with the specified key.
     *
     * @param key   The key used to identify the stored value
     * @param value The object to be stored
     */
    public void put(String key, Object value) {
        internalState.put(key, value);
    }

    /**
     * Retrieves a value from the internal state as a Map.
     * The stored value is converted from its string representation to a Map using GSON.
     *
     * @param key The key used to identify the stored value
     * @return A Map containing the stored value, deserialized from JSON
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String key) {
        final String map = String.valueOf(internalState.get(key));
        return GSON.fromJson(map, Map.class);
    }
}



