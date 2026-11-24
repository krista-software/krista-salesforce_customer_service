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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service;

import app.krista.extensions.util.KeyValueStore;
import app.krista.oauth.core.token.ITokenStore;
import app.krista.oauth.providers.salesforce.SalesforceOAuth2Sdk;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Per-request factory for {@link SalesforceOAuth2Sdk} instances backed by
 * {@link LegacyKeyedSalesforceTokenStore}.
 *
 * <h2>Multi-tenant safety</h2>
 *
 * <p>Each call to {@link #newSdk(SalesforceCustomerServiceAttributes)} produces a fresh
 * SDK instance bound to ONE specific (clientId, clientSecret) pair. For multi-tenant
 * flows where different requests use different OAuth apps (e.g. via authContextId-keyed
 * attribute overrides), call sites pass the request's resolved attributes — the factory
 * builds an SDK + adapter pair for that request only. No shared mutable state.</p>
 *
 * <h2>Token-store key isolation</h2>
 *
 * <p>The {@link LegacyKeyedSalesforceTokenStore} encodes (clientId, clientSecret) in the
 * KV key, so different OAuth apps for the same user resolve to different KV entries —
 * preserving the per-tenant isolation guarantee from the pre-SDK auth code.</p>
 *
 * <h2>Why a factory instead of a singleton SDK</h2>
 *
 * <p>The {@link SalesforceOAuth2Sdk} instance carries an immutable {@code SalesforceConfig}
 * snapshot. If we cached a singleton SDK, multi-tenant requests with different credentials
 * would either share the wrong config or require an in-SDK lookup table — both worse than
 * just building a new (cheap) SDK instance per request.</p>
 *
 * <h2>Thread-safety</h2>
 *
 * <p>This class is a stateless singleton — the only state is the {@link KeyValueStore}
 * reference injected at construction. Each {@link #newSdk} call is fully independent;
 * the returned SDK instances are not shared. Safe for concurrent use.</p>
 *
 * @since salesforce_customer_service 2.0.18 (microsoft-auth-sdk 1.1.0 migration)
 */
@Service
@Singleton
public class SalesforceCustomerServiceSdkFactory {

    private final KeyValueStore kv;

    @Inject
    public SalesforceCustomerServiceSdkFactory(KeyValueStore kv) {
        this.kv = kv;
    }

    /**
     * Build a fresh SDK instance for THIS request's resolved attributes.
     *
     * <p>Each call creates a new {@link LegacyKeyedSalesforceTokenStore} bound to the
     * (clientId, clientSecret) from {@code attrs}, then wraps it in a
     * {@link SalesforceOAuth2Sdk} built from {@code attrs.toSalesforceConfig()}. The
     * {@code toSalesforceConfig()} override on {@link SalesforceCustomerServiceAttributes}
     * reads all required getters and produces an immutable
     * {@link app.krista.oauth.providers.salesforce.config.SalesforceConfig} snapshot with
     * {@code usePkce=false} for Connected App compatibility.</p>
     *
     * @param attrs the per-request Salesforce attributes — typically loaded from the
     *              authContextId-keyed attribute store (multi-tenant) or the operator-default
     *              invokerId slot (single-tenant)
     * @return a configured {@link SalesforceOAuth2Sdk} ready to call
     *         {@code acquireForUser / acquireForAdmin / buildAuthorizationUrl / handleCallback / invalidate}
     */
    public SalesforceOAuth2Sdk newSdk(SalesforceCustomerServiceAttributes attrs) {
        ITokenStore tokenStore = buildTokenStore(attrs);
        return SalesforceOAuth2Sdk.builder()
                .config(attrs.toSalesforceConfig())
                .tokenStore(tokenStore)
                .build();
    }

    /**
     * Build a {@link LegacyKeyedSalesforceTokenStore} bound to the given attributes'
     * (clientId, clientSecret) pair.
     *
     * <p>Useful when the token store is needed independently from the full SDK construction
     * — for example, to satisfy the base class's {@link
     * app.krista.oauth.providers.salesforce.extension.AbstractSalesforceRequestAuthenticator#getTokenStore()}
     * contract while the per-request {@link
     * app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAuthenticator#newSdk}
     * override builds its own correctly-keyed store anyway.</p>
     *
     * @param attrs the attributes whose (clientId, clientSecret) bind the key translation
     * @return a non-null {@link ITokenStore} backed by the extension's {@link KeyValueStore}
     */
    public ITokenStore buildTokenStore(SalesforceCustomerServiceAttributes attrs) {
        return new LegacyKeyedSalesforceTokenStore(kv, attrs.getClientId(), attrs.getClientSecret());
    }
}
