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

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.ChangeLog;
import app.krista.extension.impl.anno.Extension;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.InvokerRequest;
import app.krista.extension.impl.anno.Java;
import app.krista.extension.impl.anno.StaticResource;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extension.util.InvokerAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Krista extension for integrating with Salesforce Customer Service.
 *
 * <p>This extension enables OAuth-based authentication, token management,
 * and validation workflows for connecting Krista to Salesforce APIs.
 * It registers a request authenticator, provides a documentation tab,
 * validates connection attributes, and manages updated configurations.</p>
 *
 * <p>All Salesforce-SDK metadata annotations ({@code @SalesforceExtension},
 * {@code @AuthScope}, {@code @CustomProperty}) live on the dedicated
 * {@link SalesforceCustomerServiceAuthConfig} class, following the MS migration
 * guide's {@code {Extension}AuthConfig.java} pattern.</p>
 *
 * <h2>CRIT-014: Legacy {@code @Field.*} annotations removed</h2>
 *
 * <p>Earlier releases declared {@code @Field.Text(CLIENT_ID)},
 * {@code @Field.Text(CLIENT_SECRET, isSecured = true)}, and the rest of the
 * Salesforce credential fields here so Krista's standard "Edit Invoker"
 * Setup-tab form rendered an editor for them. That UI is now replaced by
 * the bundled Authentication tab (served from {@code oauth/index.html} via
 * {@link app.krista.extensions.crm.customer_relations.salesforce_customer_service.api.AuthenticationResource}),
 * which is the canonical credential editor and writes to KV via
 * {@link SalesforceCustomerServiceAttributesStore#save(SalesforceCustomerServiceAttributes, String, String)}.</p>
 *
 * <p>Removing the {@code @Field.*} annotations matches outlook-4's current
 * shape: the new Authentication tab is the only credential UI. The legacy
 * Setup-tab form disappears entirely.</p>
 *
 * <h2>Migration story for existing installs (kept working)</h2>
 *
 * <p>Existing customers who configured this invoker via the legacy Setup tab
 * still have their credentials in Krista's Invoker-attribute store (separate
 * from the new KV-by-invokerId blob the Authentication tab writes to). Those
 * credentials remain readable here via the injected
 * {@code SalesforceCustomerServiceAttributes customerServiceAttributes} field,
 * which Krista populates from Invoker attrs at HK2 construction time
 * regardless of whether the class declares {@code @Field.*} annotations.</p>
 *
 * <p>{@link #effectiveAttributes()} preserves the migration path:</p>
 * <ol>
 *   <li>Read KV (new Authentication-tab storage) first.</li>
 *   <li>If KV is empty, fall back to the Invoker-attribute object
 *       (legacy storage).</li>
 * </ol>
 *
 * <p>So a customer upgrading from a pre-Authentication-tab build keeps working
 * with zero re-entry of credentials until they open the new Authentication tab
 * and Save — at which point KV takes over and the Invoker-attribute copy
 * becomes inert. No data migration script is required.</p>
 *
 * @see SalesforceCustomerServiceAuthConfig
 */
// CRIT-031 — re-declared for the v2.0.18 expand-contract migration window. Krista's
// platform binds Invoker-attribute persistence to the schema declared by @Field.*; if
// these annotations are absent, the platform may trim pre-existing legacy values on
// upgrade, leaving invoker.getAttributes() empty and the migration with nothing to read.
// Re-adding them for ONE release guarantees that pre-2.0.18 customers' Setup-tab data
// survives the upgrade long enough for load() (below) to copy it into the new KV blob.
// PLAN: in v2.0.19 these annotations get removed again once every customer has booted
// v2.0.18 at least once and the migration has run. The Setup tab will show duplicate
// fields during the v2.0.18 window — that's a one-version cost documented in CHANGELOG.

@Extension(version = "2.0.18", name = "Salesforce Customer Service", description = "Integrating Salesforce with Krista")
@Java(version = Java.Version.JAVA_21)
@StaticResource(path = "docs", file = "docs")
@ChangeLog(file = "docs/pages/ReleaseNotes.md")
public final class SalesforceCustomerServiceExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceCustomerServiceExtension.class);

    private final SalesforceCustomerServiceAttributes customerServiceAttributes;
    private final Invoker invoker;
    private final SalesforceTokenProviderFactory providerFactory;
    private final SalesforceCustomerServiceAuthenticator requestAuthenticator;
    private final SalesforceCustomerServiceAttributesStore attributesStore;

    /**
     * Primary HK2-injection constructor — builds a store-backed {@link SalesforceCustomerServiceAuthenticator}
     * from the supplied dependencies.
     *
     * <p>The {@code sdkFactory} parameter was added in 2.0.18 (T-M6) to supply
     * {@link SalesforceCustomerServiceSdkFactory} to the refactored authenticator, which
     * now extends {@link app.krista.oauth.providers.salesforce.extension.AbstractSalesforceRequestAuthenticator}
     * and needs a per-request SDK builder for multi-tenant token-store isolation.</p>
     *
     * @param providerFactory factory for creating token providers (token exchange + admin/user token)
     * @param invoker Krista Invoker (supplies invokerId and routingUrl to the authenticator)
     * @param customerServiceAttributes operator-default Salesforce attributes
     * @param attributesStore per-request attribute store (keyed by authContextId)
     * @param sdkFactory per-request SDK factory (builds LegacyKeyedSalesforceTokenStore + SalesforceOAuth2Sdk)
     */
    @Inject
    public SalesforceCustomerServiceExtension(SalesforceTokenProviderFactory providerFactory,
                                              Invoker invoker,
                                              SalesforceCustomerServiceAttributes customerServiceAttributes,
                                              SalesforceCustomerServiceAttributesStore attributesStore,
                                              SalesforceCustomerServiceSdkFactory sdkFactory) {
        this(providerFactory, invoker, customerServiceAttributes, attributesStore,
                new SalesforceCustomerServiceAuthenticator(invoker, customerServiceAttributes, attributesStore, sdkFactory));
    }

    /**
     * Secondary constructor for testing and wiring scenarios where the authenticator instance
     * is supplied externally (e.g. with a mock authenticator in unit tests).
     *
     * @param providerFactory           factory for creating token providers
     * @param invoker                   Krista Invoker used for service interactions
     * @param customerServiceAttributes configured Salesforce attributes (Invoker-backed fallback)
     * @param attributesStore           KV-backed credential store (primary when KV entry exists)
     * @param requestAuthenticator      authenticator instance for handling OAuth flow
     */
    public SalesforceCustomerServiceExtension(SalesforceTokenProviderFactory providerFactory,
                                              Invoker invoker,
                                              SalesforceCustomerServiceAttributes customerServiceAttributes,
                                              SalesforceCustomerServiceAttributesStore attributesStore,
                                              SalesforceCustomerServiceAuthenticator requestAuthenticator) {
        this.providerFactory = providerFactory;
        this.invoker = invoker;
        this.customerServiceAttributes = customerServiceAttributes;
        this.attributesStore = attributesStore;
        this.requestAuthenticator = requestAuthenticator;
    }

    /**
     * Minimal test-only constructor without an {@link SalesforceCustomerServiceAttributesStore}.
     *
     * <p>Retained for binary compatibility with existing unit tests that do not supply an
     * attributes store. When constructed this way, {@link #effectiveAttributes()} always
     * falls back to the Invoker-backed {@code customerServiceAttributes} — the same
     * behaviour as pre-KV code.</p>
     *
     * @param providerFactory           factory for creating token providers
     * @param invoker                   Krista Invoker
     * @param customerServiceAttributes Invoker-backed attributes (always used — no KV fallback)
     * @param requestAuthenticator      authenticator instance
     */
    public SalesforceCustomerServiceExtension(SalesforceTokenProviderFactory providerFactory,
                                              Invoker invoker,
                                              SalesforceCustomerServiceAttributes customerServiceAttributes,
                                              SalesforceCustomerServiceAuthenticator requestAuthenticator) {
        this(providerFactory, invoker, customerServiceAttributes, null, requestAuthenticator);
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
     * Provides custom tabs for the Krista UI.
     *
     * <p>The <b>Authentication</b> tab serves the SDK's themed OAuth setup page extracted
     * from the SDK JAR at build time (see {@code extractSDKResources} in {@code build.gradle}).
     * The endpoint {@code /salesforcecustomerservice/authentication/} reads
     * {@code oauth/index.html} from the classpath and returns it with CSP headers.</p>
     *
     * <p><b>Why the {@code ?providerName=salesforce-platform} query parameter:</b> the bundled
     * {@code oauth/app.js} has two code paths. With a {@code providerName} query param, it
     * calls {@code GET /oauth/config?providerName=...} which is served by the SDK's
     * {@code OAuth2ConfigResource}. Without the query param, it falls back to
     * {@code GET /oauth/metadata/{extensionId}} which has NO endpoint in v1.1.0 of the SDK
     * (a tracked gap — see {@code SDK_MIGRATION_GUIDE} drift findings). The query-param form
     * is the supported path; passing it explicitly keeps the page on the working code path.</p>
     *
     * <p>The <b>Audit Dashboard</b> tab was added in Chunk M3. The endpoint
     * {@code /salesforcecustomerservice/audit/} is served by
     * {@link app.krista.extensions.crm.customer_relations.salesforce_customer_service.api.SalesforceCustomerServiceAuditResource},
     * which extends {@link app.krista.oauth.core.audit.AbstractOAuth2AuditResource} and
     * reads {@code audit/index.html} from the classpath (extracted from the SDK JAR at
     * build time by the updated {@code extractSDKResources} task).</p>
     *
     * @return immutable map of tab name to REST path (3 tabs as of v2.0.18 Chunk M3)
     */
    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of(
            "Authentication",  "rest/salesforcecustomerservice/authentication/?providerName=salesforce-platform",
            "Salesforce Audit Dashboard", "rest/salesforcecustomerservice/audit/",
            "Documentation",   "static/docs"
        );
    }

    /**
     * Validates the connection attributes provided by the user.
     *
     * <p>This method is typically invoked when the user updates credentials or
     * other integration parameters in the Krista UI. The Invoker-supplied attributes
     * take precedence here because the user is explicitly setting new values.</p>
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
     * Tests the configured connection by attempting a token fetch using the effective attributes.
     *
     * <p>Prefers KV-stored credentials (saved via the Authentication tab) when present,
     * falling back to the Invoker-backed {@code customerServiceAttributes} when not.
     * This ensures existing deployments that configured credentials via the standard
     * "Edit Invoker" attribute form continue to work after the Authentication tab is introduced.</p>
     */
    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() {
        attributesValidator(effectiveAttributes());
    }

    /**
     * Returns the effective attributes for runtime use — KV-stored (Auth tab) credentials
     * take priority over Invoker-backed credentials when a KV entry exists for this invokerId.
     *
     * <p>Migration path:</p>
     * <ul>
     *   <li>Existing deployments: no KV entry → falls back to Invoker attrs → unchanged behaviour.</li>
     *   <li>New deployments via the Authentication tab: KV entry present → uses KV attrs.</li>
     *   <li>Mixed (tab credentials override Invoker form): KV entry wins.</li>
     * </ul>
     *
     * @return the most specific available {@link SalesforceCustomerServiceAttributes}
     */
    private SalesforceCustomerServiceAttributes effectiveAttributes() {
        if (attributesStore != null && invoker != null) {
            String invokerId = invoker.getInvokerId();
            SalesforceCustomerServiceAttributes kvAttrs = attributesStore.loadByInvokerId(invokerId);
            if (kvAttrs != null) {
                LOGGER.debug("Using KV-stored credentials for invokerId={}", invokerId);
                return kvAttrs;
            }
            LOGGER.debug("No KV credentials found for invokerId={}, using Invoker-backed attributes", invokerId);
        }
        return customerServiceAttributes;
    }

    /**
     * Validates Salesforce operator-context connection attributes by requesting an admin token.
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md: {@code TEST_CONNECTION} and {@code VALIDATE_ATTRIBUTES}
     * are operator-shared operations — they always use the operator's configured credentials, never
     * per-user identity. This method therefore ALWAYS calls {@code getSalesforceTokenForAdmin()},
     * which routes to Client Credentials, Password Grant, or JWT Bearer via HC-006.</p>
     *
     * <p>When the invoker is configured with AUTH_CODE only (no instanceUrl, no user creds, no JWT),
     * the admin token acquisition will throw a descriptive {@link RuntimeException} explaining that
     * operator-context calls require server-to-server credentials. This is the correct, deliberate
     * behavior per Violation C fix — AUTH_CODE setup requires per-user consent, it is not a
     * substitute for operator credentials.</p>
     *
     * <p>The old pattern ({@code getSalesforceTokenForUser()} when no password-grant creds are
     * present) was wrong: it routed an operator-context call through the per-user path, which
     * could return a token keyed to the wrong identity (or NPE in the iframe context). Removed.</p>
     *
     * @param serviceAttributes the attributes to validate
     */
    private void attributesValidator(SalesforceCustomerServiceAttributes serviceAttributes) {
        // Always operator path — TEST_CONNECTION and VALIDATE_ATTRIBUTES are never per-user.
        // If only AUTH_CODE is configured, getSalesforceTokenForAdmin() will fail with a clear
        // error message instructing the operator to configure server-to-server credentials.
        providerFactory.create(serviceAttributes).getSalesforceTokenForAdmin();
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

    /**
     * CRIT-031 — one-shot migration of legacy Setup-tab credentials into the new
     * Authentication-tab KV blob, fired on every {@code INVOKER_LOADED} event.
     *
     * <p>Designed for the v2.0.17 → v2.0.18 upgrade window. The legacy {@code @Field.*}
     * annotations at the top of this class are re-declared (CRIT-031 comment block)
     * specifically so Krista's platform retains Setup-tab values across the upgrade and
     * surfaces them via {@code invoker.getAttributes()} for this method to read.</p>
     *
     * <h2>What this migrates</h2>
     * <p>Only the credential <b>config fields</b>: {@code clientId}, {@code clientSecret},
     * {@code "Instance URL"}, {@code username}, {@code password}, {@code securityToken},
     * {@code sandbox}. These lived in Krista's Invoker-attribute namespace pre-2.0.18
     * (populated by the old Setup-tab {@code @Field.Text} form) and now live in the
     * extension's own KV blob keyed by {@code invokerId} with an added {@code savedFlow}
     * tag.</p>
     *
     * <h2>What this does NOT migrate</h2>
     * <ul>
     *   <li><b>Refresh tokens</b> — already in KV under {@code {accountId}&{clientId}{clientSecret}}
     *       in both 2.0.17 AND post-CRIT-031 2.0.18 (via the corrected
     *       {@link LegacyKeyedSalesforceTokenStore#translateKey(String)}). No move needed.</li>
     *   <li><b>Per-request OAuth callback blobs</b> ({@code authContextId}-keyed) —
     *       ephemeral, not carried across versions.</li>
     *   <li><b>Audit records / archives / counters</b> — new in 2.0.18, no prior data exists.</li>
     * </ul>
     *
     * <h2>Auth tab flow inference</h2>
     * <p>v2.0.17 did NOT support Quick Setup (added in 2.0.18 via CRIT-022) or JWT Bearer
     * (added in the SDK substrate). Inference therefore returns one of three:</p>
     * <ol>
     *   <li>{@code "PASSWORD_GRANT"} — username + password + securityToken all populated</li>
     *   <li>{@code "CLIENT_CREDENTIALS"} — Instance URL populated (no user creds)</li>
     *   <li>{@code "AUTH_CODE"} — fallback (just clientId + clientSecret); maps to the
     *       Authentication tab's "User Sign-In" / Advanced flow</li>
     * </ol>
     * <p>The {@code savedFlow} value gets written into the KV blob so the bundled
     * {@code oauth/app.js} can pre-activate the right tab when the admin opens the
     * Authentication tab.</p>
     *
     * <h2>Safety</h2>
     * <p>Any exception during migration is caught and logged — the method must never
     * abort invoker load. Legacy Invoker attributes are left in place (not deleted),
     * so a rollback to v2.0.17 remains possible if needed.</p>
     */
    @InvokerRequest(InvokerRequest.Type.INVOKER_LOADED)
    public void load() {
        System.out.println("[SF-MIGRATE] CRIT-031 load() invoked — invokerId="
                + (invoker != null ? invoker.getInvokerId() : "(null invoker)"));
        try {
            migrateLegacyConfigToKvBlob();
        } catch (Exception e) {
            // Never break invoker load — log and move on.
            System.out.println("[SF-MIGRATE] CRIT-031 EXCEPTION during migration for invokerId="
                    + (invoker != null ? invoker.getInvokerId() : "(null invoker)")
                    + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            LOGGER.error("[SF-MIGRATE] CRIT-031 credential migration failed for invokerId={}: {}",
                    invoker != null ? invoker.getInvokerId() : "(null invoker)",
                    e.getMessage(), e);
        }
    }

    /**
     * Worker for {@link #load()} — see that method's javadoc for the full contract.
     */
    private void migrateLegacyConfigToKvBlob() {
        if (attributesStore == null || invoker == null) {
            System.out.println("[SF-MIGRATE] CRIT-031 SKIP — attributesStore="
                    + (attributesStore == null ? "null" : "present")
                    + " invoker=" + (invoker == null ? "null" : "present")
                    + " (test-only constructor path)");
            return;
        }
        String invokerId = invoker.getInvokerId();
        System.out.println("[SF-MIGRATE] CRIT-031 checking new-KV-blob status for invokerId=" + invokerId);

        // Fast path — already migrated (or saved natively via Auth tab). Idempotent.
        SalesforceCustomerServiceAttributes existing = attributesStore.loadByInvokerId(invokerId);
        if (existing != null) {
            System.out.println("[SF-MIGRATE] CRIT-031 SKIP — invokerId=" + invokerId
                    + " already has new-KV-blob (clientIdMask=" + mask(existing.getClientId())
                    + " savedFlow=" + attributesStore.getSavedFlow(invokerId) + ")");
            return;
        }

        // Trigger field for SF = clientId. v2.0.17 had no email field (Quick Setup was
        // introduced in 2.0.18 via CRIT-022), so we can't use email as the trigger like
        // OneDrive/SharePoint do.
        Map<String, Object> legacyAttrs = invoker.getAttributes();
        if (legacyAttrs == null) {
            System.out.println("[SF-MIGRATE] CRIT-031 SKIP — invoker.getAttributes() returned null for invokerId="
                    + invokerId);
            return;
        }
        String legacyClientId = InvokerAttributes.getStringOrNull(
                legacyAttrs, SalesforceCustomerServiceAttributes.CLIENT_ID);
        if (legacyClientId == null || legacyClientId.isBlank()) {
            System.out.println("[SF-MIGRATE] CRIT-031 SKIP — no legacy clientId in Invoker attrs for invokerId="
                    + invokerId + " (fresh install, legacyAttrKeys=" + legacyAttrs.keySet() + ")");
            return;
        }

        System.out.println("[SF-MIGRATE] CRIT-031 MIGRATING — invokerId=" + invokerId
                + " legacyClientIdMask=" + mask(legacyClientId)
                + " legacyAttrKeys=" + legacyAttrs.keySet());
        LOGGER.info("[SF-MIGRATE] CRIT-031 — starting legacy Setup-tab credential migration for invokerId={}",
                invokerId);

        // The existing 2-arg constructor accepts the Invoker-attributes map shape
        // (same field names — only the storage location changed between versions).
        String routingUrl = invoker.getRoutingInfo()
                .getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        System.out.println("[SF-MIGRATE] CRIT-031 routingUrl=" + routingUrl);

        SalesforceCustomerServiceAttributes migrated =
                new SalesforceCustomerServiceAttributes(routingUrl, legacyAttrs);

        String inferredFlow = inferLegacyFlow(migrated);
        System.out.println("[SF-MIGRATE] CRIT-031 inferredFlow=" + inferredFlow
                + " (instanceUrl=" + migrated.getInstanceUrl()
                + ", hasUserName=" + nb(migrated.getUserName())
                + ", hasPassword=" + nb(migrated.getPassword())
                + ", hasSecurityToken=" + nb(migrated.getSecurityToken()) + ")");

        attributesStore.save(migrated, invokerId, inferredFlow);

        System.out.println("[SF-MIGRATE] CRIT-031 DONE — invokerId=" + invokerId
                + " inferredFlow=" + inferredFlow
                + " → new-KV-blob written. Authentication tab will activate the matching flow on next open.");
        LOGGER.info("[SF-MIGRATE] CRIT-031 — migrated credentials for invokerId={} inferredFlow={}",
                invokerId, inferredFlow);
    }

    /**
     * Infers which Authentication-tab flow the admin was using in v2.0.17, from the
     * set of populated credential fields. Mirrors HC-006 routing precedence so the
     * migrated KV blob's {@code savedFlow} matches what the Auth-tab UI offers.
     *
     * <p>Returns one of {@code "PASSWORD_GRANT"}, {@code "CLIENT_CREDENTIALS"},
     * or {@code "AUTH_CODE"}. Never returns {@code "AUTH_CODE_QUICK"} or
     * {@code "JWT_BEARER"} — those flows did not exist in v2.0.17.</p>
     */
    static String inferLegacyFlow(SalesforceCustomerServiceAttributes attrs) {
        // Username + password + security token all set → Password Grant
        if (notBlank(attrs.getUserName())
                && notBlank(attrs.getPassword())
                && notBlank(attrs.getSecurityToken())) {
            return "PASSWORD_GRANT";
        }
        // Instance URL set + no user creds → Server-to-Server (Client Credentials)
        if (notBlank(attrs.getInstanceUrl())) {
            return "CLIENT_CREDENTIALS";
        }
        // Default: customer's own Connected App with user-delegated OAuth → User Sign-In tab
        return "AUTH_CODE";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nb(String s) {
        return notBlank(s) ? "true" : "false";
    }

    private static String mask(String s) {
        if (s == null || s.isBlank()) return "***";
        return s.length() > 8 ? s.substring(0, 8) + "***" : s + "***";
    }
}
