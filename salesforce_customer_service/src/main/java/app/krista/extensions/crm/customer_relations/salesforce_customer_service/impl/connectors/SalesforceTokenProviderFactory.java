package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.RefreshTokenStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * Factory class for creating SalesforceTokenProvider instances.
 * <p>
 * This factory is responsible for creating and configuring SalesforceTokenProvider
 * objects with the appropriate dependencies and settings. It centralizes the creation
 * logic and ensures that token providers are properly initialized with the necessary
 * components for Salesforce authentication.
 */
@Service
public final class SalesforceTokenProviderFactory {

    private final RequestContext requestContext;
    private final RefreshTokenStore refreshTokenStore;
    private final SalesforceTokenProvider defaultSalesforceTokenProvider;
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final AuthorizationContext authorizationContext;

    /**
     * Constructs a SalesforceTokenProviderFactory with the required dependencies.
     *
     * @param refreshTokenStore The store used to persist refresh tokens
     * @param requestContext The context of the current request
     * @param salesforceCustomerServiceAttributes The default Salesforce service attributes
     * @param attributesStore The store for Salesforce service configuration attributes
     * @param authorizationContext The authorization context for the current request
     */
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

    /**
     * Constructs a SalesforceTokenProviderFactory with the required dependencies and a pre-configured token provider.
     *
     * @param refreshTokenStore The store used to persist refresh tokens
     * @param requestContext The context of the current request
     * @param authorizationContext The authorization context for the current request
     * @param salesforceTokenProvider The pre-configured token provider to use as default
     * @param attributesStore The store for Salesforce service configuration attributes
     */
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

    /**
     * Creates a SalesforceTokenProvider with the specified Salesforce attributes.
     *
     * @param salesforceSalesAttributes The Salesforce service attributes to use for configuration
     * @return A configured SalesforceTokenProvider instance
     * @throws IllegalArgumentException if salesforceSalesAttributes is null
     */
    public SalesforceTokenProvider create(SalesforceCustomerServiceAttributes salesforceSalesAttributes) {
        if (salesforceSalesAttributes == null) {
            throw new IllegalArgumentException("Attributes not found");
        }
        String authContextId = attributesStore.save(salesforceSalesAttributes);
        return new SalesforceTokenProvider(refreshTokenStore, requestContext, salesforceSalesAttributes, authorizationContext, authContextId);
    }

    /**
     * Creates a SalesforceTokenProvider using the default configuration.
     *
     * @return The default SalesforceTokenProvider instance
     */
    public SalesforceTokenProvider create() {
        return defaultSalesforceTokenProvider;
    }

}
