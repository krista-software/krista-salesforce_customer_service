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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceSdkFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesforceTokenProviderFactory}.
 *
 * <p>The factory provides two creation paths:</p>
 * <ul>
 *   <li>{@link SalesforceTokenProviderFactory#create()} — returns the pre-built default
 *       provider (operator-context use).</li>
 *   <li>{@link SalesforceTokenProviderFactory#create(SalesforceCustomerServiceAttributes)} —
 *       saves the attrs into the per-request attribute store under a NEW authContextId and
 *       returns a provider bound to those attrs (multi-tenant per-request override).</li>
 * </ul>
 *
 * <p>Both paths must produce a provider whose attributes are exactly the ones passed in;
 * the per-attrs path must additionally persist into the attribute store so the OAuth
 * callback can route the response back to the right (clientId, clientSecret) pair.</p>
 */
public class SalesforceTokenProviderFactoryTest {

    private static final String CLIENT_ID = "3MVG9clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String ROUTING_URL = "https://test.krista.ai";
    private static final String INVOKER_ID = "salesforce-customer-service";
    private static final String NEW_AUTH_CTX_ID = "ctx_abc123";

    @Mock
    private RequestContext requestContext;
    @Mock
    private AuthorizationContext authorizationContext;
    @Mock
    private SalesforceCustomerServiceAttributesStore attributesStore;
    @Mock
    private SalesforceCustomerServiceSdkFactory sdkFactory;
    @Mock
    private SalesforceTokenProvider defaultProvider;

    private SalesforceTokenProviderFactory factory;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use the full (test-seam) constructor.
        factory = new SalesforceTokenProviderFactory(
                requestContext, authorizationContext, defaultProvider,
                attributesStore, sdkFactory, INVOKER_ID);
    }

    private SalesforceCustomerServiceAttributes attrs() {
        return new SalesforceCustomerServiceAttributes(
                CLIENT_ID, CLIENT_SECRET, null, null, null, null, false, ROUTING_URL);
    }

    // ── create() ────────────────────────────────────────────────────────────────────

    @Test
    public void createNoArgs_returnsTheDefaultProviderSuppliedAtConstruction() {
        SalesforceTokenProvider provider = factory.create();
        assertSame("create() must return the exact default provider supplied at construction",
                defaultProvider, provider);
    }

    // ── create(attrs) ───────────────────────────────────────────────────────────────

    @Test
    public void createWithAttrs_savesAttrsInStoreAndReturnsBoundProvider() {
        SalesforceCustomerServiceAttributes a = attrs();
        when(attributesStore.save(a)).thenReturn(NEW_AUTH_CTX_ID);

        SalesforceTokenProvider provider = factory.create(a);

        assertNotNull(provider);
        // Provider must carry the same attrs that were passed in.
        assertSame(a, provider.getSalesforceCustomerServiceAttributes());

        // Attrs must have been persisted into the per-request store so the OAuth callback
        // can route back to the right (clientId, clientSecret) on completion.
        verify(attributesStore, times(1)).save(a);
    }

    @Test
    public void createWithAttrs_eachCall_returnsDistinctProviderInstance() {
        SalesforceCustomerServiceAttributes a = attrs();
        when(attributesStore.save(any())).thenReturn(NEW_AUTH_CTX_ID, "ctx_other");

        SalesforceTokenProvider first  = factory.create(a);
        SalesforceTokenProvider second = factory.create(a);

        assertNotSame("Per-attrs providers must be per-call instances, not cached",
                first, second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullAttrs_throwsIllegalArgumentException() {
        factory.create(null);
    }

    @Test
    public void createWithNullAttrs_doesNotTouchAttributeStore() {
        try {
            factory.create(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        verifyNoInteractions(attributesStore);
    }

    // ── Multi-tenant safety ─────────────────────────────────────────────────────────

    /**
     * Two different attrs → two distinct providers, each bound to its OWN attrs.
     * This is the multi-tenant per-request override path — if the factory ever returned
     * the same provider for both, cross-tenant token bleed would occur.
     */
    @Test
    public void createWithAttrs_differentAttrs_bindEachProviderToItsOwnAttrs() {
        SalesforceCustomerServiceAttributes attrsA = new SalesforceCustomerServiceAttributes(
                "clientA", "secretA", null, null, null, null, false, ROUTING_URL);
        SalesforceCustomerServiceAttributes attrsB = new SalesforceCustomerServiceAttributes(
                "clientB", "secretB", null, null, null, null, false, ROUTING_URL);

        when(attributesStore.save(attrsA)).thenReturn("ctx_A");
        when(attributesStore.save(attrsB)).thenReturn("ctx_B");

        SalesforceTokenProvider providerA = factory.create(attrsA);
        SalesforceTokenProvider providerB = factory.create(attrsB);

        assertSame(attrsA, providerA.getSalesforceCustomerServiceAttributes());
        assertSame(attrsB, providerB.getSalesforceCustomerServiceAttributes());
        assertNotSame("Different attrs → different provider instances", providerA, providerB);
    }
}
