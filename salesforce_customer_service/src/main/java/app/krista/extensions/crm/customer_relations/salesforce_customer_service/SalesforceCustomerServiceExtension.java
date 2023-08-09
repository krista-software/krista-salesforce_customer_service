package app.krista.extensions.crm.customer_relations.salesforce_customer_service;


import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Extension;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.InvokerRequest;
import app.krista.extension.impl.anno.StaticResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

@Field.Text(value = SalesforceCustomerServiceAttributes.CLIENT_ID)
@Field.Text(value = SalesforceCustomerServiceAttributes.CLIENT_SECRET, isSecured = true)
@Field.Text(value = SalesforceCustomerServiceAttributes.USER_NAME, required = false)
@Field.Text(value = SalesforceCustomerServiceAttributes.PASS_WORD, isSecured = true, required = false)
@Field.Text(value = SalesforceCustomerServiceAttributes.SECURITY_TOKEN, isSecured = true, required = false)
@Extension(version = "2.0.6-rc1")
@StaticResource(path = "docs", file = "documentations")
public final class SalesforceCustomerServiceExtension {

    private final SalesforceCustomerServiceAttributes customerServiceAttributes;
    private final Invoker invoker;
    private final SalesforceTokenProviderFactory providerFactory;
    private final SalesforceCustomerServiceAuthenticator requestAuthenticator;

    @Inject
    public SalesforceCustomerServiceExtension(SalesforceTokenProviderFactory providerFactory, Invoker invoker,
                                              SalesforceCustomerServiceAttributes customerServiceAttributes,
                                              SalesforceCustomerServiceAttributesStore attributesStore) {
        this(providerFactory, invoker, customerServiceAttributes,
                new SalesforceCustomerServiceAuthenticator(customerServiceAttributes, attributesStore));
    }

    public SalesforceCustomerServiceExtension(SalesforceTokenProviderFactory providerFactory, Invoker invoker,
                                              SalesforceCustomerServiceAttributes customerServiceAttributes,
                                              SalesforceCustomerServiceAuthenticator requestAuthenticator) {
        this.providerFactory = providerFactory;
        this.invoker = invoker;
        this.customerServiceAttributes = customerServiceAttributes;
        this.requestAuthenticator = requestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of("Documentation", "static/docs");
    }

    @InvokerRequest(InvokerRequest.Type.VALIDATE_ATTRIBUTES)
    public void validateConnection(Map<String, Object> connectionAttributes) throws IOException {
        SalesforceCustomerServiceAttributes salesforceAttributes = SalesforceCustomerServiceAttributes
                .create(invoker, connectionAttributes);
        attributesValidator(salesforceAttributes);
    }

    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() throws IOException {
        attributesValidator(customerServiceAttributes);
    }

    private void attributesValidator(SalesforceCustomerServiceAttributes serviceAttributes) throws IOException {
        if (serviceAttributes.getUserName() == null || !serviceAttributes.getUserName().isBlank() ||
                serviceAttributes.getPassword() == null || serviceAttributes.getPassword().isBlank() ||
                serviceAttributes.getSecurityToken() == null || serviceAttributes.getSecurityToken().isBlank()) {
            providerFactory.create(serviceAttributes).getSalesforceToken();
        } else {
            providerFactory.create(serviceAttributes).getSalesforceTokenForAdmin();
        }
    }

    @InvokerRequest(InvokerRequest.Type.INVOKER_UPDATED)
    public void attributesUpdated(Map<String, Object> oldAttributes, Map<String, Object> newAttributes) {
        customerServiceAttributes.update(newAttributes);
    }
}
