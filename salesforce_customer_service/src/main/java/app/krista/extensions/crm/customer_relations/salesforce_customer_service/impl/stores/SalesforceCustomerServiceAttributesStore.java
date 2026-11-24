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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Store for managing Salesforce Customer Service configuration attributes.
 *
 * <p>Two distinct storage patterns are supported:</p>
 * <ol>
 *   <li><b>Per-request OAuth callback storage</b> ({@link #save(SalesforceCustomerServiceAttributes)}
 *       / {@link #load(String)}): keyed by a generated UUID ({@code authContextId}). Used during the
 *       OAuth authorization-code flow to carry per-request attributes across the Salesforce redirect.</li>
 *   <li><b>Persistent credential storage</b> ({@link #save(SalesforceCustomerServiceAttributes, String, String)}
 *       / {@link #loadByInvokerId(String)}): keyed by {@code invokerId}. Used by the Authentication tab's
 *       Save Credentials flow to persist the operator's chosen flow and credentials in KV so the
 *       extension runtime can load them without going through the Invoker attribute form.</li>
 * </ol>
 *
 * <p><b>Thread-safety:</b> instances are per-request (HK2 default scope). All fields are
 * effectively final after construction.</p>
 */
@Service
public class SalesforceCustomerServiceAttributesStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceCustomerServiceAttributesStore.class);

    /**
     * JSON field name for the last-saved flow identifier (e.g. {@code "CLIENT_CREDENTIALS"}).
     * Stored only in the invokerId-keyed blob, not in the per-request authContextId blob.
     */
    static final String SAVED_FLOW = "savedFlow";

    private final KeyValueStore store;
    private final Invoker invoker;

    /**
     * Constructs a SalesforceCustomerServiceAttributesStore with the required dependencies.
     *
     * @param store   The key-value store used to persist attributes
     * @param invoker The invoker used for routing URL and invokerId resolution
     */
    @Inject
    public SalesforceCustomerServiceAttributesStore(KeyValueStore store, Invoker invoker) {
        this.store = store;
        this.invoker = invoker;
    }

    // -------------------------------------------------------------------------
    // Per-request OAuth callback storage (authContextId-keyed)
    // -------------------------------------------------------------------------

    /**
     * Saves Salesforce Customer Service attributes under a newly-generated UUID key.
     *
     * <p>Used exclusively by the OAuth authorization-code flow before the Salesforce redirect.
     * The generated key is placed in the OAuth {@code state} parameter so the callback handler
     * can look up the per-request attributes from the multi-tenant store on return.</p>
     *
     * @param salesforceCustomerServiceAttributes The attributes to save
     * @return The generated authContextId (UUID string) used as the KV key
     */
    public String save(SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes) {
        // CRIT-026 — mirror outlook-4's `authContextId_<uuid>` format (NOT bare UUID).
        // The Krista platform's central OAuth callback handler at
        // /extension/api/rest/v3/oauth/callback recognizes the `authContextId_` prefix in
        // the OAuth state's authContextId segment to identify which segment of the
        // hash-split state is the authContext UUID. With a bare UUID the central handler
        // can't distinguish it from other opaque segments and fails to preserve the user's
        // session through the forward to /v3/oauth/callback — causing
        // context.isAuthenticated()=false at our callback and the
        // authorizationListener.authorized() to receive an HTTP 500 from
        // /accesspoint/ksdk/invoke.
        //
        // Side-by-side proof from QA logs:
        //   outlook-4 state: ...#authContextId_<uuid>#... → callback authenticated → listener succeeds → auto-resume
        //   SF state (old): ...#<bare-uuid>#...           → callback unauthenticated → listener 500 → manual re-click
        //
        // Mirrors outlook-4's OutlookAttributeStore.save():
        //   String authContextId = Constants.AUTH_CONTEXT_ID + Constants.UNDER_SCORE + UUID.randomUUID();
        String authContextId = "authContextId_" + UUID.randomUUID();
        store.put(authContextId, buildAttributesJson(salesforceCustomerServiceAttributes, null));
        return authContextId;
    }

    /**
     * Loads Salesforce Customer Service attributes from the per-request store.
     *
     * <p>Retrieves the attributes associated with the given {@code authContextId} and
     * deserializes them into a {@link SalesforceCustomerServiceAttributes} instance.
     *
     * @param authContextId The UUID key generated by {@link #save(SalesforceCustomerServiceAttributes)}
     * @return The retrieved attributes, or {@code null} if the entry is absent or unparseable
     */
    @SuppressWarnings("unchecked")
    public SalesforceCustomerServiceAttributes load(String authContextId) {
        Object raw = store.get(authContextId);
        if (raw == null) {
            return null;
        }
        try {
            Map<String, Object> attributes = Constants.GSON.fromJson(raw.toString(), Map.class);
            return fromMap(attributes);
        } catch (Exception e) {
            LOGGER.warn("Failed to deserialize attributes for authContextId {}: {}", authContextId, e.getMessage());
            return null;
        }
    }

    /**
     * Removes Salesforce Customer Service attributes from the store.
     *
     * @param authContextId The key to remove (either authContextId UUID or invokerId)
     */
    public void remove(String authContextId) {
        store.remove(authContextId);
    }

    // -------------------------------------------------------------------------
    // Persistent credential storage (invokerId-keyed)
    // -------------------------------------------------------------------------

    /**
     * Persists the operator's credentials under the {@code invokerId} key so that the
     * extension runtime can load them on every request without relying on the Invoker
     * attribute form.
     *
     * <p>The blob includes all Salesforce credential fields plus a {@code savedFlow} entry
     * recording which OAuth flow was last saved. This is the 3-argument primary form;
     * the 2-argument form delegates here with {@code flowId = null}.</p>
     *
     * @param attrs     the credentials to persist
     * @param invokerId the Krista invokerId that uniquely identifies this extension instance
     * @param flowId    the flow identifier string (e.g. {@code "CLIENT_CREDENTIALS"},
     *                  {@code "AUTH_CODE"}, {@code "PASSWORD_GRANT"}), or {@code null} to
     *                  leave any previously-stored flow value unchanged
     * @return {@code true} always — the underlying KV put does not surface a failure signal;
     *         any KV exception propagates as an unchecked exception to the caller
     */
    public boolean save(SalesforceCustomerServiceAttributes attrs, String invokerId, String flowId) {
        if (attrs == null) {
            throw new IllegalArgumentException("SalesforceCustomerServiceAttributes cannot be null");
        }
        store.put(invokerId, buildAttributesJson(attrs, flowId));
        LOGGER.debug("Persisted credentials for invokerId={} flow={}", invokerId, flowId);
        return true;
    }

    /**
     * Persists the operator's credentials under the {@code invokerId} key without
     * recording a flow identifier.
     *
     * <p>Convenience overload for back-compat callers that do not track flow selection.
     * Delegates to {@link #save(SalesforceCustomerServiceAttributes, String, String)} with
     * {@code flowId = null}.</p>
     *
     * @param attrs     the credentials to persist
     * @param invokerId the Krista invokerId
     * @return {@code true} always
     */
    public boolean save(SalesforceCustomerServiceAttributes attrs, String invokerId) {
        return save(attrs, invokerId, null);
    }

    /**
     * Loads credentials previously saved by {@link #save(SalesforceCustomerServiceAttributes, String, String)}.
     *
     * @param invokerId the Krista invokerId that was used as the KV key
     * @return the deserialized attributes, or {@code null} if no entry exists for this invokerId
     *         or the stored value cannot be parsed
     */
    @SuppressWarnings("unchecked")
    public SalesforceCustomerServiceAttributes loadByInvokerId(String invokerId) {
        Object raw = store.get(invokerId);
        if (raw == null) {
            return null;
        }
        try {
            Map<String, Object> attributes = Constants.GSON.fromJson(raw.toString(), Map.class);
            return fromMap(attributes);
        } catch (Exception e) {
            LOGGER.warn("Failed to deserialize KV credentials for invokerId {}: {}", invokerId, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the flow identifier stored in the invokerId-keyed credential blob, or
     * {@code null} if no credentials have been saved yet or the blob has no
     * {@code savedFlow} entry.
     *
     * @param invokerId the Krista invokerId
     * @return the saved flow string (e.g. {@code "CLIENT_CREDENTIALS"}), or {@code null}
     */
    @SuppressWarnings("unchecked")
    public String getSavedFlow(String invokerId) {
        Object raw = store.get(invokerId);
        if (raw == null) {
            return null;
        }
        try {
            Map<String, Object> attributes = Constants.GSON.fromJson(raw.toString(), Map.class);
            Object flow = attributes.get(SAVED_FLOW);
            return flow instanceof String ? (String) flow : null;
        } catch (Exception e) {
            LOGGER.warn("Failed to read savedFlow for invokerId {}: {}", invokerId, e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Serializes a {@link SalesforceCustomerServiceAttributes} instance plus an optional
     * {@code savedFlow} tag into a JSON string suitable for KV storage.
     *
     * @param attrs  the attributes to serialize
     * @param flowId the flow identifier to embed, or {@code null} to omit the field
     * @return a JSON string
     */
    private static String buildAttributesJson(SalesforceCustomerServiceAttributes attrs, String flowId) {
        Map<String, Object> map = new HashMap<>();
        map.put(SalesforceCustomerServiceAttributes.CLIENT_ID, attrs.getClientId());
        map.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET, attrs.getClientSecret());
        map.put(SalesforceCustomerServiceAttributes.SANDBOX, attrs.isSandbox());
        if (attrs.getInstanceUrl() != null) {
            map.put(SalesforceCustomerServiceAttributes.INSTANCE_URL, attrs.getInstanceUrl());
        }
        if (attrs.getUserName() != null) {
            map.put(SalesforceCustomerServiceAttributes.USER_NAME, attrs.getUserName());
        }
        if (attrs.getPassword() != null) {
            map.put(SalesforceCustomerServiceAttributes.PASS_WORD, attrs.getPassword());
        }
        if (attrs.getSecurityToken() != null) {
            map.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN, attrs.getSecurityToken());
        }
        // CRIT-022: persist the Quick Setup admin email so it survives KV round-trips
        // (required so getCredentials can pre-fill the email field after refresh, and so
        // operator-context runtime calls can re-derive the right token key).
        if (attrs.getEmail() != null) {
            map.put(SalesforceCustomerServiceAttributes.EMAIL, attrs.getEmail());
        }
        if (flowId != null) {
            map.put(SAVED_FLOW, flowId);
        }
        return Constants.GSON.toJson(map);
    }

    /**
     * Deserializes a raw attribute map (from GSON) into a
     * {@link SalesforceCustomerServiceAttributes} instance.
     *
     * @param attributes the deserialized map
     * @return a fully-constructed attributes instance
     */
    private SalesforceCustomerServiceAttributes fromMap(Map<String, Object> attributes) {
        Object sandboxRaw = attributes.get(SalesforceCustomerServiceAttributes.SANDBOX);
        Boolean sandbox = sandboxRaw instanceof Boolean ? (Boolean) sandboxRaw : null;
        // CRIT-022: pass through the persisted Quick Setup admin email via the 9-arg
        // constructor overload. Advanced flows store no email (null) so this is a no-op
        // for them.
        return new SalesforceCustomerServiceAttributes(
                (String) attributes.get(SalesforceCustomerServiceAttributes.CLIENT_ID),
                (String) attributes.get(SalesforceCustomerServiceAttributes.CLIENT_SECRET),
                (String) attributes.get(SalesforceCustomerServiceAttributes.INSTANCE_URL),
                (String) attributes.get(SalesforceCustomerServiceAttributes.USER_NAME),
                (String) attributes.get(SalesforceCustomerServiceAttributes.PASS_WORD),
                (String) attributes.get(SalesforceCustomerServiceAttributes.SECURITY_TOKEN),
                sandbox,
                invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE),
                (String) attributes.get(SalesforceCustomerServiceAttributes.EMAIL));
    }
}
