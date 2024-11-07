package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

@Service
public class SalesforceCustomerServiceAttributesStore {
    private final KeyValueStore store;
    private final Invoker invoker;

    @Inject
    public SalesforceCustomerServiceAttributesStore(KeyValueStore store, Invoker invoker) {
        this.store = store;
        this.invoker = invoker;
    }

    public String save(SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes) {
        String authContextId = UUID.randomUUID().toString();
        store.put(authContextId, Constants.GSON.toJson(Map.of(SalesforceCustomerServiceAttributes.CLIENT_ID,
                salesforceCustomerServiceAttributes.getClientId(),
                SalesforceCustomerServiceAttributes.CLIENT_SECRET, salesforceCustomerServiceAttributes.getClientSecret(),
                SalesforceCustomerServiceAttributes.SANDBOX, salesforceCustomerServiceAttributes.isSandbox())));
        return authContextId;
    }

    public SalesforceCustomerServiceAttributes load(String authContextId) {
        var attributes = Constants.GSON.fromJson(store.get(authContextId).toString(), Map.class);
        return new SalesforceCustomerServiceAttributes((String) attributes.get(SalesforceCustomerServiceAttributes.CLIENT_ID),
                (String) attributes.get(SalesforceCustomerServiceAttributes.CLIENT_SECRET),
                (String) attributes.get(SalesforceCustomerServiceAttributes.USER_NAME),
                (String) attributes.get(SalesforceCustomerServiceAttributes.PASS_WORD),
                (String) attributes.get(SalesforceCustomerServiceAttributes.SECURITY_TOKEN),
                (Boolean) attributes.get(SalesforceCustomerServiceAttributes.SANDBOX),
                invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE));
    }

    public void remove(String authContextId) {
        store.remove(authContextId);
    }
}
