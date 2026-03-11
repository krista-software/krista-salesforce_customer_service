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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Store for managing Salesforce Customer Service configuration attributes.
 * <p>
 * This class provides functionality to store, retrieve, and manage configuration
 * attributes required for connecting to and interacting with the Salesforce API.
 * It uses a key-value store to persist these attributes across requests.
 */
@Service
public class SalesforceCustomerServiceAttributesStore {
    private final KeyValueStore store;
    private final Invoker invoker;

    /**
     * Constructs a SalesforceCustomerServiceAttributesStore with the required dependencies.
     *
     * @param store   The key-value store used to persist attributes
     * @param invoker The invoker used for making API requests
     */
    @Inject
    public SalesforceCustomerServiceAttributesStore(KeyValueStore store, Invoker invoker) {
        this.store = store;
        this.invoker = invoker;
    }

    /**
     * Saves Salesforce Customer Service attributes to the store.
     * <p>
     * Generates a unique ID for the attributes and stores them in the key-value store.
     *
     * @param salesforceCustomerServiceAttributes The attributes to save
     * @return The generated unique ID (authContextId) for the saved attributes
     */
    public String save(SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes) {
        String authContextId = UUID.randomUUID().toString();
        store.put(authContextId, getAttributesJson(salesforceCustomerServiceAttributes));
        return authContextId;
    }

    private static String getAttributesJson(SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SalesforceCustomerServiceAttributes.CLIENT_ID,
                salesforceCustomerServiceAttributes.getClientId());
        attributes.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, salesforceCustomerServiceAttributes.getClientSecret());
        attributes.put(SalesforceCustomerServiceAttributes.SANDBOX, salesforceCustomerServiceAttributes.isSandbox());
        if (salesforceCustomerServiceAttributes.getInstanceUrl() != null) {
            attributes.put(SalesforceCustomerServiceAttributes.INSTANCE_URL, salesforceCustomerServiceAttributes.getInstanceUrl());
        }
        return Constants.GSON.toJson(attributes);
    }


    /**
     * Loads Salesforce Customer Service attributes from the store.
     * <p>
     * Retrieves the attributes associated with the given authContextId and
     * constructs a SalesforceCustomerServiceAttributes object.
     *
     * @param authContextId The unique ID for the stored attributes
     * @return The retrieved SalesforceCustomerServiceAttributes
     */
    public SalesforceCustomerServiceAttributes load(String authContextId) {
        var attributes = Constants.GSON.fromJson(store.get(authContextId).toString(), Map.class);
        return new SalesforceCustomerServiceAttributes((String) attributes.get(SalesforceCustomerServiceAttributes.CLIENT_ID),
                (String) attributes.get(SalesforceCustomerServiceAttributes.CLIENT_SECRET),
                (String) attributes.get(SalesforceCustomerServiceAttributes.INSTANCE_URL),
                (String) attributes.get(SalesforceCustomerServiceAttributes.USER_NAME),
                (String) attributes.get(SalesforceCustomerServiceAttributes.PASS_WORD),
                (String) attributes.get(SalesforceCustomerServiceAttributes.SECURITY_TOKEN),
                (Boolean) attributes.get(SalesforceCustomerServiceAttributes.SANDBOX),
                invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE));
    }

    /**
     * Removes Salesforce Customer Service attributes from the store.
     * <p>
     * Deletes the attributes associated with the given authContextId.
     *
     * @param authContextId The unique ID for the attributes to remove
     */
    public void remove(String authContextId) {
        store.remove(authContextId);
    }
}
