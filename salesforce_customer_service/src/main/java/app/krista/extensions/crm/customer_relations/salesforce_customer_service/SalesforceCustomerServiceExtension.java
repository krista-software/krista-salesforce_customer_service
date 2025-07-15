package app.krista.extensions.crm.customer_relations.salesforce_customer_service;

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.*;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;

import javax.inject.Inject;
import java.util.Map;

/**
 * Krista extension for integrating with Salesforce Customer Service.
 *
 * <p>This extension enables OAuth-based authentication, token management,
 * and validation workflows for connecting Krista to Salesforce APIs.
 * It registers a request authenticator, provides a documentation tab,
 * validates connection attributes, and manages updated configurations.</p>
 */
@Field.Text(value = SalesforceCustomerServiceAttributes.CLIENT_ID)
@Field.Text(value = SalesforceCustomerServiceAttributes.CLIENT_SECRET, isSecured = true)
@Field.Text(value = SalesforceCustomerServiceAttributes.USER_NAME, required = false)
@Field.Text(value = SalesforceCustomerServiceAttributes.PASS_WORD, isSecured = true, required = false)
@Field.Text(value = SalesforceCustomerServiceAttributes.SECURITY_TOKEN, isSecured = true, required = false)
@Field.Boolean(value = SalesforceCustomerServiceAttributes.SANDBOX, required = false)
@Extension(version = "2.0.13", name = "Salesforce Customer Service", description = "Integrating Salesforce with Krista")
@Java(version = Java.Version.JAVA_21)
@StaticResource(path = "docs", file = "documentations")
public final class SalesforceCustomerServiceExtension {

    private final SalesforceCustomerServiceAttributes customerServiceAttributes;
    private final Invoker invoker;
    private final SalesforceTokenProviderFactory providerFactory;
    private final SalesforceCustomerServiceAuthenticator requestAuthenticator;

    /**
     * Constructs a new SalesforceCustomerServiceExtension using a store-backed authenticator.
     *
     * @param providerFactory factory for creating token providers
     * @param invoker Krista Invoker used for service interactions
     * @param customerServiceAttributes configured Salesforce attributes
     * @param attributesStore store for loading attributes for different auth contexts
     */
    @Inject
    public SalesforceCustomerServiceExtension(SalesforceTokenProviderFactory providerFactory, Invoker invoker,
                                              SalesforceCustomerServiceAttributes customerServiceAttributes,
                                              SalesforceCustomerServiceAttributesStore attributesStore) {
        this(providerFactory, invoker, customerServiceAttributes,
                new SalesforceCustomerServiceAuthenticator(customerServiceAttributes, attributesStore));
    }

    /**
     * Constructs a new SalesforceCustomerServiceExtension using a provided authenticator instance.
     *
     * @param providerFactory factory for creating token providers
     * @param invoker Krista Invoker used for service interactions
     * @param customerServiceAttributes configured Salesforce attributes
     * @param requestAuthenticator authenticator instance for handling OAuth flow
     */
    public SalesforceCustomerServiceExtension(SalesforceTokenProviderFactory providerFactory, Invoker invoker,
                                              SalesforceCustomerServiceAttributes customerServiceAttributes,
                                              SalesforceCustomerServiceAuthenticator requestAuthenticator) {
        this.providerFactory = providerFactory;
        this.invoker = invoker;
        this.customerServiceAttributes = customerServiceAttributes;
        this.requestAuthenticator = requestAuthenticator;
    }

    /**
     * Registers the request authenticator used for Salesforce OAuth flow.
     *
     * @return the authenticator instance
     */
    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    /**
     * Provides custom tabs for the Krista UI, such as documentation links.
     *
     * @return a map of tab name to static resource path
     */
    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of("Documentation", "static/docs");
    }

    /**
     * Validates the connection attributes provided by the user.
     *
     * <p>This method is typically invoked when the user updates credentials or
     * other integration parameters in the Krista UI.</p>
     *
     * @param connectionAttributes new attributes to validate
     */
    @InvokerRequest(InvokerRequest.Type.VALIDATE_ATTRIBUTES)
    public void validateConnection(Map<String, Object> connectionAttributes) {
        SalesforceCustomerServiceAttributes salesforceAttributes = SalesforceCustomerServiceAttributes
                .create(invoker, connectionAttributes);
        attributesValidator(salesforceAttributes);
    }

    /**
     * Tests the configured connection by attempting a token fetch using the stored attributes.
     *
     * <p>Will use either the user credentials or client credentials based on presence of username/password.</p>
     */
    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() {
        attributesValidator(customerServiceAttributes);
    }

    /**
     * Validates Salesforce connection attributes by requesting a token.
     *
     * <p>If user credentials are present, uses password grant for a user; otherwise, uses admin credentials.</p>
     *
     * @param serviceAttributes the attributes to validate
     */
    private void attributesValidator(SalesforceCustomerServiceAttributes serviceAttributes) {
        if (serviceAttributes.getUserName() == null || serviceAttributes.getUserName().isBlank() ||
                serviceAttributes.getPassword() == null || serviceAttributes.getPassword().isBlank() ||
                serviceAttributes.getSecurityToken() == null || serviceAttributes.getSecurityToken().isBlank()) {
            providerFactory.create(serviceAttributes).getSalesforceTokenForUser();
        } else {
            providerFactory.create(serviceAttributes).getSalesforceTokenForAdmin();
        }
    }

    /**
     * Handles updates to the extension's attributes.
     *
     * <p>This method updates the internal attribute state used by the extension,
     * ensuring future requests use the latest configuration.</p>
     *
     * @param oldAttributes the previous attribute map
     * @param newAttributes the new attribute map
     */
    @InvokerRequest(InvokerRequest.Type.INVOKER_UPDATED)
    public void attributesUpdated(Map<String, Object> oldAttributes, Map<String, Object> newAttributes) {
        customerServiceAttributes.update(newAttributes);
    }
}
