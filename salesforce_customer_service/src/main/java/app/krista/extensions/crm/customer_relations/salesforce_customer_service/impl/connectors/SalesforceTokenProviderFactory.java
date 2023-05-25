package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.RefreshTokenStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;

@Service
public final class SalesforceTokenProviderFactory {

    private final RequestContext requestContext;
    private final RefreshTokenStore refreshTokenStore;
    private final SalesforceTokenProvider defaultSalesforceTokenProvider;
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final AuthorizationContext authorizationContext;

    @Inject
    public SalesforceTokenProviderFactory(RefreshTokenStore refreshTokenStore, RequestContext requestContext,
                                          SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes,
                                          SalesforceCustomerServiceAttributesStore attributesStore,
                                          AuthorizationContext authorizationContext) {
        this(refreshTokenStore, requestContext, authorizationContext,
                new SalesforceTokenProvider(refreshTokenStore, requestContext,
                        salesforceCustomerServiceAttributes, authorizationContext, null),
                attributesStore);
    }

    public SalesforceTokenProviderFactory(RefreshTokenStore refreshTokenStore, RequestContext requestContext,
                                          AuthorizationContext authorizationContext,
                                          SalesforceTokenProvider salesforceTokenProvider,
                                          SalesforceCustomerServiceAttributesStore attributesStore) {
        this.refreshTokenStore = refreshTokenStore;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.defaultSalesforceTokenProvider = salesforceTokenProvider;
        this.attributesStore = attributesStore;
    }

    public SalesforceTokenProvider create(SalesforceCustomerServiceAttributes salesforceSalesAttributes) throws IOException {
        if (salesforceSalesAttributes == null) {
            throw new IllegalArgumentException("Attributes not found");
        }
        String authContextId = attributesStore.save(salesforceSalesAttributes);
        return new SalesforceTokenProvider(refreshTokenStore, requestContext, salesforceSalesAttributes, authorizationContext, authContextId);
    }

    public SalesforceTokenProvider create() {
        return defaultSalesforceTokenProvider;
    }

}
