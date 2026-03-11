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



