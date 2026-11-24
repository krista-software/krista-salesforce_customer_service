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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAuditService;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceSdkFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * Factory class for creating {@link SalesforceTokenProvider} instances.
 *
 * <p>Each provider is constructed with the resolved per-request
 * {@link SalesforceCustomerServiceAttributes} and a reference to the
 * {@link SalesforceCustomerServiceSdkFactory} so that token-acquisition calls
 * can build a correctly-keyed {@link app.krista.oauth.providers.salesforce.SalesforceOAuth2Sdk}
 * backed by a {@link app.krista.extensions.crm.customer_relations.salesforce_customer_service.LegacyKeyedSalesforceTokenStore}
 * for each request.</p>
 */
@Service
public final class SalesforceTokenProviderFactory {

    private final RequestContext requestContext;
    private final SalesforceTokenProvider defaultSalesforceTokenProvider;
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final AuthorizationContext authorizationContext;
    private final SalesforceCustomerServiceSdkFactory sdkFactory;
    /** CRIT-008: forwarded into each TokenProvider so the runtime path can call
     *  {@code sdk.acquireForUser(invokerId)} for the AUTH_CODE-only setup case. */
    private final String invokerId;
    /** Audit recorder attached to every provider this factory builds — mirrors outlook-4's
     *  {@code GraphServiceClientProviderFactory.setAuditService(...)}. Non-final: set by the
     *  {@code @Inject} constructor after delegation; {@code null} on the test-seam path. */
    private SalesforceCustomerServiceAuditService auditService;

    /**
     * Production constructor injected by HK2.
     *
     * @param requestContext                      context of the current request
     * @param salesforceCustomerServiceAttributes operator-default Salesforce attributes
     * @param attributesStore                     per-request attribute store
     * @param authorizationContext                authorization context for the current request
     * @param sdkFactory                          factory that builds per-request SDK instances
     */
    @Inject
    public SalesforceTokenProviderFactory(RequestContext requestContext,
                                          SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes,
                                          SalesforceCustomerServiceAttributesStore attributesStore,
                                          AuthorizationContext authorizationContext,
                                          SalesforceCustomerServiceSdkFactory sdkFactory,
                                          app.krista.extension.executor.Invoker invoker,
                                          SalesforceCustomerServiceAuditService auditService) {
        this(requestContext, authorizationContext,
                buildDefault(requestContext, salesforceCustomerServiceAttributes,
                        authorizationContext, sdkFactory, invoker, attributesStore, auditService),
                attributesStore, sdkFactory,
                invoker != null ? invoker.getInvokerId() : null);
        this.auditService = auditService;
    }

    private static SalesforceTokenProvider buildDefault(RequestContext rc,
                                                        SalesforceCustomerServiceAttributes attrs,
                                                        AuthorizationContext ac,
                                                        SalesforceCustomerServiceSdkFactory sdkFactory,
                                                        app.krista.extension.executor.Invoker invoker,
                                                        SalesforceCustomerServiceAttributesStore attributesStore,
                                                        SalesforceCustomerServiceAuditService auditService) {
        SalesforceTokenProvider p = new SalesforceTokenProvider(rc, attrs, ac, null, sdkFactory, attributesStore);
        if (invoker != null) p.setInvokerId(invoker.getInvokerId());
        p.setAuditService(auditService);
        return p;
    }

    /**
     * Full constructor — supports test seams and the overloaded factory wiring path.
     */
    public SalesforceTokenProviderFactory(RequestContext requestContext,
                                          AuthorizationContext authorizationContext,
                                          SalesforceTokenProvider salesforceTokenProvider,
                                          SalesforceCustomerServiceAttributesStore attributesStore,
                                          SalesforceCustomerServiceSdkFactory sdkFactory,
                                          String invokerId) {
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.defaultSalesforceTokenProvider = salesforceTokenProvider;
        this.attributesStore = attributesStore;
        this.sdkFactory = sdkFactory;
        this.invokerId = invokerId;
    }

    /**
     * Creates a {@link SalesforceTokenProvider} with the specified Salesforce attributes.
     *
     * <p>Saves the attributes under a new authContextId in the attribute store (for
     * multi-tenant OAuth callback routing), then builds a provider bound to those attrs
     * and the shared {@link SalesforceCustomerServiceSdkFactory}.</p>
     *
     * @param salesforceSalesAttributes the Salesforce service attributes for this request
     * @return a configured {@link SalesforceTokenProvider} instance
     * @throws IllegalArgumentException if {@code salesforceSalesAttributes} is null
     */
    public SalesforceTokenProvider create(SalesforceCustomerServiceAttributes salesforceSalesAttributes) {
        if (salesforceSalesAttributes == null) {
            throw new IllegalArgumentException("Attributes not found");
        }
        String authContextId = attributesStore.save(salesforceSalesAttributes);
        SalesforceTokenProvider p = new SalesforceTokenProvider(requestContext, salesforceSalesAttributes,
                authorizationContext, authContextId, sdkFactory, attributesStore);
        p.setInvokerId(invokerId);
        p.setAuditService(auditService);
        return p;
    }

    /**
     * Creates a {@link SalesforceTokenProvider} using the operator-default configuration.
     *
     * @return the default {@link SalesforceTokenProvider} instance
     */
    public SalesforceTokenProvider create() {
        return defaultSalesforceTokenProvider;
    }
}
