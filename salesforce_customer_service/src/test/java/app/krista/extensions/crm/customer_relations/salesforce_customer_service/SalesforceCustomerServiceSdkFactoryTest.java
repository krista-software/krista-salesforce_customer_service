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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SalesforceCustomerServiceSdkFactory}.
 *
 * <p>The factory is the multi-tenant-safety boundary called out in its javadoc:
 * each call to {@link SalesforceCustomerServiceSdkFactory#newSdk newSdk} must produce
 * a FRESH SDK instance bound to the supplied attrs' (clientId, clientSecret) pair,
 * NOT a shared singleton. Same goes for
 * {@link SalesforceCustomerServiceSdkFactory#buildTokenStore buildTokenStore}.</p>
 *
 * <p>If the factory ever cached an SDK or token store, two requests with different
 * Connected Apps for the same KV-backed user would collide on each other's tokens —
 * a security and correctness bug.</p>
 *
 * <h2>What's verified</h2>
 * <ul>
 *   <li>Each {@code newSdk}/{@code buildTokenStore} call returns a NEW object (not
 *       reference-equal to a prior call) — prevents future cache regressions.</li>
 *   <li>The returned {@code ITokenStore} is a {@link LegacyKeyedSalesforceTokenStore}
 *       bound to the request's (clientId, clientSecret) — proven via
 *       {@code translateKey} embedding those credentials.</li>
 *   <li>{@code buildTokenStore} called twice with DIFFERENT credentials produces
 *       stores whose translated keys diverge — multi-tenant key isolation.</li>
 * </ul>
 */
public class SalesforceCustomerServiceSdkFactoryTest {

    private static final String CLIENT_ID_A = "3MVG9clientA";
    private static final String CLIENT_SECRET_A = "secretA";
    private static final String CLIENT_ID_B = "3MVG9clientB";
    private static final String CLIENT_SECRET_B = "secretB";
    private static final String ROUTING_URL = "https://test.krista.ai";

    @Mock
    private KeyValueStore kv;

    private SalesforceCustomerServiceSdkFactory factory;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        factory = new SalesforceCustomerServiceSdkFactory(kv);
    }

    private SalesforceCustomerServiceAttributes attrs(String clientId, String clientSecret) {
        // 8-arg constructor: (clientId, clientSecret, instanceUrl, userName, password,
        //                    securityToken, isSandbox, routingUrl)
        return new SalesforceCustomerServiceAttributes(
                clientId, clientSecret, null, null, null, null, false, ROUTING_URL);
    }

    // ── newSdk ──────────────────────────────────────────────────────────────────────

    @Test
    public void newSdk_returnsNonNullSdk() {
        SalesforceOAuth2Sdk sdk = factory.newSdk(attrs(CLIENT_ID_A, CLIENT_SECRET_A));
        assertNotNull("Factory must produce a non-null SDK", sdk);
    }

    /**
     * Multi-tenant safety property from the class javadoc: "If we cached a singleton SDK,
     * multi-tenant requests with different credentials would either share the wrong config
     * or require an in-SDK lookup table — both worse than just building a new (cheap) SDK
     * instance per request."
     */
    @Test
    public void newSdk_calledTwice_returnsDistinctInstances() {
        SalesforceOAuth2Sdk first = factory.newSdk(attrs(CLIENT_ID_A, CLIENT_SECRET_A));
        SalesforceOAuth2Sdk second = factory.newSdk(attrs(CLIENT_ID_A, CLIENT_SECRET_A));

        assertNotSame("Factory must return a fresh SDK on every call — never a singleton",
                first, second);
    }

    @Test
    public void newSdk_calledWithDifferentAttrs_returnsDistinctInstances() {
        SalesforceOAuth2Sdk sdkA = factory.newSdk(attrs(CLIENT_ID_A, CLIENT_SECRET_A));
        SalesforceOAuth2Sdk sdkB = factory.newSdk(attrs(CLIENT_ID_B, CLIENT_SECRET_B));

        assertNotSame(sdkA, sdkB);
    }

    // ── buildTokenStore ─────────────────────────────────────────────────────────────

    @Test
    public void buildTokenStore_returnsLegacyKeyedStoreBoundToAttrs() {
        ITokenStore store = factory.buildTokenStore(attrs(CLIENT_ID_A, CLIENT_SECRET_A));

        assertNotNull(store);
        assertTrue("Token store must be a LegacyKeyedSalesforceTokenStore so legacy KV " +
                        "keys remain readable post-SDK-migration",
                store instanceof LegacyKeyedSalesforceTokenStore);

        // Prove the credentials are wired through: translated key must embed them.
        String translated = ((LegacyKeyedSalesforceTokenStore) store)
                .translateKey("sfv1/acct/hash");
        assertTrue("translateKey must embed the request's clientId",
                translated.contains(CLIENT_ID_A));
        assertTrue("translateKey must embed the request's clientSecret",
                translated.contains(CLIENT_SECRET_A));
    }

    @Test
    public void buildTokenStore_calledTwice_returnsDistinctInstances() {
        ITokenStore first = factory.buildTokenStore(attrs(CLIENT_ID_A, CLIENT_SECRET_A));
        ITokenStore second = factory.buildTokenStore(attrs(CLIENT_ID_A, CLIENT_SECRET_A));

        assertNotSame("Token stores must be per-call instances, not cached",
                first, second);
    }

    /**
     * Token-store key isolation: same accountId, different (clientId, clientSecret) →
     * different translated KV keys. This is what prevents two Connected Apps for the
     * same user from aliasing onto the same refresh-token entry.
     */
    @Test
    public void buildTokenStore_differentCredentials_isolateKeyspaces() {
        LegacyKeyedSalesforceTokenStore storeA = (LegacyKeyedSalesforceTokenStore)
                factory.buildTokenStore(attrs(CLIENT_ID_A, CLIENT_SECRET_A));
        LegacyKeyedSalesforceTokenStore storeB = (LegacyKeyedSalesforceTokenStore)
                factory.buildTokenStore(attrs(CLIENT_ID_B, CLIENT_SECRET_B));

        String sdkKey = "sfv1/sharedAccountId/hash";
        String translatedA = storeA.translateKey(sdkKey);
        String translatedB = storeB.translateKey(sdkKey);

        assertNotEquals(
                "Different Connected Apps for the same user MUST resolve to different " +
                        "KV keys — otherwise customer A's token can be served to customer B.",
                translatedA, translatedB);
    }

    // ── Constructor / state ─────────────────────────────────────────────────────────

    @Test
    public void factory_isStateless_savedKvIsTheOnlyState() {
        // The class javadoc claims "stateless singleton — the only state is the KeyValueStore
        // reference injected at construction." Build two factories with the same KV; they
        // should be functionally indistinguishable (we can't assert equality on a factory
        // without an equals override, but we can verify each produces working stores from
        // the same KV).
        SalesforceCustomerServiceSdkFactory other = new SalesforceCustomerServiceSdkFactory(kv);

        ITokenStore fromFirst = factory.buildTokenStore(attrs(CLIENT_ID_A, CLIENT_SECRET_A));
        ITokenStore fromOther = other.buildTokenStore(attrs(CLIENT_ID_A, CLIENT_SECRET_A));

        assertNotNull(fromFirst);
        assertNotNull(fromOther);
        assertEquals(
                ((LegacyKeyedSalesforceTokenStore) fromFirst).translateKey("sfv1/x/h"),
                ((LegacyKeyedSalesforceTokenStore) fromOther).translateKey("sfv1/x/h"));
    }
}
