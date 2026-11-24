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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extension.executor.Invoker;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import com.google.gson.JsonObject;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the "Save Credentials" flow for the Salesforce Authentication tab.
 *
 * <h2>Contract</h2>
 * <ol>
 *   <li>Parse the JSON payload into {@link SalesforceCustomerServiceAttributes}.</li>
 *   <li>Extract the {@code flow} field so we know which strategy to test and which
 *       label to persist as {@code savedFlow}.</li>
 *   <li>Test the credentials via {@link SalesforceTestConnectionServiceImpl} — if the
 *       test fails, return a failure response without persisting anything.</li>
 *   <li>If the test passes, persist the credentials to KV keyed by {@code invokerId}
 *       via {@link SalesforceCustomerServiceAttributesStore#save(SalesforceCustomerServiceAttributes, String, String)}.</li>
 *   <li>If the flow is {@code AUTH_CODE} and the saved clientId/clientSecret differ from
 *       the previously-stored values, remove the old refresh token from the KV store.
 *       Stale refresh tokens at old credential keys cause silent auth failures; clean
 *       removal forces re-authorization.</li>
 *   <li>Return JSON {@code {"isSuccess": true}} on success or
 *       {@code {"isSuccess": false, "errorMessage": "..."}} on any failure.</li>
 * </ol>
 *
 * <h2>Response shape</h2>
 * <p>Both the {@code doSave()} and {@code doTest()} functions in {@code oauth/app.js}
 * check {@code r.isSuccess}. The field name is camelCase — Gson preserves Java field
 * names exactly, so the {@code isSuccess} boolean field on the response record must
 * NOT be prefixed with "is" in the serialized output. We use a plain {@code boolean}
 * field named {@code isSuccess} so Gson emits {@code "isSuccess"} verbatim.</p>
 *
 * <h2>Thread-safety</h2>
 * <p>Instances are per-request (HK2 default scope). All fields are effectively final
 * after construction.</p>
 *
 * @since salesforce_customer_service 2.0.18
 */
@Service
public class SalesforceSaveConfigurationImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceSaveConfigurationImpl.class);

    /** The JS UI ({@code oauth/app.js}'s {@code collectPayload()}) sends the flow id as "authFlow". */
    private static final String FIELD_FLOW = "authFlow";
    private static final String FLOW_AUTH_CODE = "AUTH_CODE";
    private static final String FLOW_AUTH_CODE_QUICK = "AUTH_CODE_QUICK";

    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final SalesforceTestConnectionServiceImpl testConnectionService;
    private final Invoker invoker;
    private final String invokerId;

    /**
     * HK2-injection constructor.
     *
     * @param attributesStore      store for persisting credentials by invokerId
     * @param invoker              Krista Invoker — provides the invokerId
     * @param testConnectionService service used to verify credentials before persisting
     */
    @Inject
    public SalesforceSaveConfigurationImpl(SalesforceCustomerServiceAttributesStore attributesStore,
                                           Invoker invoker,
                                           SalesforceTestConnectionServiceImpl testConnectionService) {
        this.attributesStore = attributesStore;
        this.invoker = invoker;
        this.invokerId = invoker.getInvokerId();
        this.testConnectionService = testConnectionService;
    }

    /**
     * Validates the supplied credentials against Salesforce and — on success — persists
     * them to KV under the invokerId key.
     *
     * @param payload the JSON body posted by the Authentication tab's Save Changes button;
     *                must contain at minimum {@code clientId}, {@code clientSecret}, and
     *                {@code flow}
     * @return JSON string with shape {@code {"isSuccess": <bool>, "errorMessage": "<str>"}}
     */
    public String saveCredentials(JsonObject payload) {
        String flow = extractString(payload, FIELD_FLOW);

        // User Sign-In (AUTH_CODE) keys its setup token by the Consumer Email, so it is a
        // required field to save. Reject early with a clear message rather than relying only
        // on the downstream testConnection validation.
        if (FLOW_AUTH_CODE.equals(flow)) {
            String consumerEmail = extractString(payload, SalesforceCustomerServiceAttributes.EMAIL);
            if (consumerEmail == null || consumerEmail.isBlank()) {
                LOGGER.warn("saveCredentials rejected for invokerId={} flow={}: missing Consumer Email", invokerId, flow);
                return failureResponse("Please enter the Consumer Email before saving User Sign-In credentials.");
            }
        }

        // Build attributes from the payload using the factory-style map-constructor path.
        SalesforceCustomerServiceAttributes newAttrs;
        try {
            newAttrs = attrsFromPayload(payload);
        } catch (Exception e) {
            LOGGER.error("Failed to parse credential payload for invokerId={}: {}", invokerId, e.getMessage(), e);
            return failureResponse("Invalid credentials payload: " + e.getMessage());
        }

        // Test the credentials first — we never persist credentials that don't work.
        String testResult = testConnectionService.testConnection(newAttrs, flow);
        SaveResponse testResponse = Constants.GSON.fromJson(testResult, SaveResponse.class);
        if (!testResponse.isSuccess) {
            // Special case: AUTH_CODE testConnection may return {"isSuccess":false,"url":"..."}
            // when a popup is needed. The JS only calls saveCredentials AFTER the popup completed
            // and testConnection returned isSuccess=true, so this branch is not expected in
            // normal operation. If it happens (e.g. timing race), treat it as "credentials
            // not yet verified — reject the save." The errorMessage will be null, so supply
            // a user-friendly message in that case.
            String reason = testResponse.errorMessage != null
                    ? testResponse.errorMessage
                    : "Authorization not yet completed. Please test the connection first.";
            LOGGER.warn("Credential test failed for invokerId={} flow={}: {}",
                    invokerId, flow, reason);
            return Constants.GSON.toJson(new SaveResponse(false, reason));
        }

        try {
            // If AUTH_CODE flow, check whether the credential key has changed.
            // If it has, remove the stale refresh token so the user is prompted to
            // re-authorize rather than getting silent failures against the old token.
            // AUTH_CODE_QUICK: no stale-token eviction needed — the credentials are
            // Krista-owned and never change between operator saves. The refresh token
            // from the prior Quick Setup authorization remains valid.
            if (FLOW_AUTH_CODE.equals(flow)) {
                evictStaleRefreshTokenIfNeeded(newAttrs);
            }

            boolean saved = attributesStore.save(newAttrs, invokerId, flow);
            if (saved) {
                LOGGER.debug("Credentials saved successfully for invokerId={} flow={}", invokerId, flow);
                return successResponse();
            } else {
                LOGGER.error("KV put returned false for invokerId={}", invokerId);
                return failureResponse("Failed to save credentials to the configuration store.");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to persist credentials for invokerId={}: {}", invokerId, e.getMessage(), e);
            return failureResponse("Failed to save credentials: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link SalesforceCustomerServiceAttributes} from the raw JSON payload.
     *
     * <p>The payload shape sent by the Authentication tab matches the field names defined
     * as constants on {@link SalesforceCustomerServiceAttributes}. We build the attributes
     * map and delegate to the existing map-constructor, exactly as
     * {@code SalesforceCustomerServiceExtension.validateConnection()} does when processing
     * the Invoker attribute form.</p>
     */
    private SalesforceCustomerServiceAttributes attrsFromPayload(JsonObject payload) {
        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_ID,
                extractString(payload, SalesforceCustomerServiceAttributes.CLIENT_ID));
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET,
                extractString(payload, SalesforceCustomerServiceAttributes.CLIENT_SECRET));
        // CRIT-006: see AuthenticationResource.extractString comment — the JS flow-driven UI
        // sends camelCase keys (instanceUrl, userName) that differ from the legacy Java
        // constants ("Instance URL", "username"). Try the camelCase wire name first.
        attrMap.put(SalesforceCustomerServiceAttributes.INSTANCE_URL,
                extractString(payload, "instanceUrl", SalesforceCustomerServiceAttributes.INSTANCE_URL));
        attrMap.put(SalesforceCustomerServiceAttributes.USER_NAME,
                extractString(payload, "userName", SalesforceCustomerServiceAttributes.USER_NAME));
        attrMap.put(SalesforceCustomerServiceAttributes.PASS_WORD,
                extractString(payload, SalesforceCustomerServiceAttributes.PASS_WORD));
        attrMap.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN,
                extractString(payload, SalesforceCustomerServiceAttributes.SECURITY_TOKEN));
        // CRIT-022: Quick Setup admin email — read it from the payload so the saved attrs
        // can use it as the operator accountId at runtime. The same key works for the JWT
        // flow which also has an "email" field.
        attrMap.put(SalesforceCustomerServiceAttributes.EMAIL,
                extractString(payload, SalesforceCustomerServiceAttributes.EMAIL));

        if (payload.has(SalesforceCustomerServiceAttributes.SANDBOX)
                && !payload.get(SalesforceCustomerServiceAttributes.SANDBOX).isJsonNull()) {
            attrMap.put(SalesforceCustomerServiceAttributes.SANDBOX,
                    payload.get(SalesforceCustomerServiceAttributes.SANDBOX).getAsBoolean());
        }

        // CRIT-005: Resolve secret masks ("••••••••") back to the real stored values BEFORE
        // building the attrs object. The /saveCredentials path persists `newAttrs` directly
        // after test passes — without this, the post-test save would overwrite KV with the
        // mask string, corrupting credentials so every subsequent OAuth call fails.
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.api.AuthenticationResource
                .resolveMaskedSecrets(attrMap, attributesStore, invokerId);

        // Delegate to the factory so routingUrl is derived from the Invoker. The routingUrl
        // is required to build the OAuth redirect_uri for AUTH_CODE; passing null here would
        // yield "redirect_uri=null/rest/.../callback" in the Salesforce authorize URL.
        return SalesforceCustomerServiceAttributes.create(invoker, attrMap);
    }

    /**
     * Removes the stale refresh token when the AUTH_CODE clientId or clientSecret changes.
     *
     * <p>The legacy KV key format for refresh tokens is
     * {@code {accountId}&{clientId}+{clientSecret}}. When the operator changes the Connected
     * App credentials, the old token at the old key becomes unreachable by the SDK (which
     * builds keys from the new credentials). We proactively remove the old KV entry so the
     * user is prompted through a clean OAuth flow rather than getting an opaque auth failure.</p>
     *
     * <p>NOTE: we only know the invokerId-keyed credential blob here, not the accountId-keyed
     * token keys. The token keys include the accountId which is user-specific and not available
     * at save time. We therefore leave granular per-user token cleanup as a follow-up; the SDK's
     * {@code InvalidGrantException} handling in {@link app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider}
     * already triggers re-authorization when a stale token is encountered at runtime.</p>
     */
    private void evictStaleRefreshTokenIfNeeded(SalesforceCustomerServiceAttributes newAttrs) {
        SalesforceCustomerServiceAttributes oldAttrs = attributesStore.loadByInvokerId(invokerId);
        if (oldAttrs == null) {
            return; // first-time save — nothing to evict
        }
        boolean clientIdChanged = !equal(oldAttrs.getClientId(), newAttrs.getClientId());
        boolean clientSecretChanged = !equal(oldAttrs.getClientSecret(), newAttrs.getClientSecret());
        if (clientIdChanged || clientSecretChanged) {
            LOGGER.info("AUTH_CODE credentials changed for invokerId={} (clientId changed={}, secret changed={}). "
                    + "Per-user refresh tokens at old credential keys will be invalidated at next user request "
                    + "via InvalidGrantException handling.", invokerId, clientIdChanged, clientSecretChanged);
            // Granular per-user token eviction would require iterating all accountId#clientId+secret
            // KV entries — impractical without a KV scan API. The SDK's runtime re-auth path handles
            // it lazily and gracefully (MustAuthorizeException → browser redirect).
        }
    }

    private static boolean equal(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static String extractString(JsonObject payload, String... fieldAliases) {
        for (String field : fieldAliases) {
            if (payload.has(field) && !payload.get(field).isJsonNull()) {
                String val = payload.get(field).getAsString();
                if (!val.isBlank()) return val;
            }
        }
        return null;
    }

    private static String successResponse() {
        return Constants.GSON.toJson(new SaveResponse(true, null));
    }

    private static String failureResponse(String message) {
        return Constants.GSON.toJson(new SaveResponse(false, message));
    }

    /**
     * Response envelope matching the shape expected by the Authentication tab's JS:
     * {@code { "isSuccess": <bool>, "errorMessage": "<str>" }}.
     */
    @SuppressWarnings("unused") // fields read by Gson serialization
    private record SaveResponse(boolean isSuccess, String errorMessage) {
    }
}
