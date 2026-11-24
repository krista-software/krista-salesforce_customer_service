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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.api;

import app.krista.extension.executor.Invoker;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAuditService;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceSdkFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.oauth.core.audit.AbstractOAuth2AuditResource;
import app.krista.oauth.core.audit.AuditData;
import app.krista.oauth.core.audit.AuditRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS audit-dashboard resource for the {@code salesforce_customer_service} extension.
 *
 * <p>Extends the SDK's generic {@link AbstractOAuth2AuditResource}, which provides the
 * standard audit endpoints at the registered path prefix:</p>
 * <ul>
 *   <li>{@code GET  /salesforcecustomerservice/audit/}         — audit dashboard HTML</li>
 *   <li>{@code GET  /salesforcecustomerservice/audit/data}     — JSON records + summary</li>
 *   <li>{@code GET  /salesforcecustomerservice/audit/health}   — credential / connectivity status</li>
 *   <li>{@code DELETE /salesforcecustomerservice/audit/data}   — clear records in time range</li>
 *   <li>{@code GET  /salesforcecustomerservice/audit/archives} — archive index</li>
 *   <li>{@code GET  /salesforcecustomerservice/audit/archives/{key}} — archived records</li>
 *   <li>{@code DELETE /salesforcecustomerservice/audit/archives/{key}} — delete archive</li>
 *   <li>{@code POST /salesforcecustomerservice/audit/archiveNow} — manual archive trigger</li>
 *   <li>{@code GET  /salesforcecustomerservice/audit/globalCounter} — all-time counter</li>
 * </ul>
 *
 * <h2>invokerId threading</h2>
 * <p>The {@link AbstractOAuth2AuditResource} base class requires a stable {@code invokerId}
 * string supplied at construction time. This resource is registered as a JAX-RS singleton
 * via {@link SalesforceCustomerRelationsApplication#getSingletons()} and thus has the
 * same lifetime as the extension. The {@link Invoker} is HK2-provided and stable for the
 * extension's lifetime, so {@code invoker.getInvokerId()} is safe to call at construction.</p>
 *
 * <h2>Audit dashboard HTML</h2>
 * <p>The base class's {@link #getAuditHtmlPath()} returns {@code "audit/index.html"} —
 * the pre-built dashboard HTML shipped in the SDK JAR and extracted into the extension JAR
 * by the {@code extractSDKResources} Gradle task (see {@code build.gradle}).  The dashboard
 * is MS-themed in v1.1.0; a Salesforce-themed variant is deferred to post-v1.1.0 SDK work.</p>
 *
 * <h2>Health check</h2>
 * <p>{@link #buildHealthResponse()} attempts a token acquisition via the SDK's
 * {@link SalesforceCustomerServiceSdkFactory} to report live credential health.
 * The check is always defensive — any exception produces a {@code "status": "error"}
 * response rather than surfacing a stack trace.</p>
 *
 * @since 2.0.18 (microsoft-auth-sdk 1.1.0 migration, Chunk M3)
 */
@Path("/salesforcecustomerservice/audit")
public class SalesforceCustomerServiceAuditResource extends AbstractOAuth2AuditResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SalesforceCustomerServiceAuditResource.class);

    private final Invoker                                invoker;
    private final SalesforceCustomerServiceAuditService  auditService;
    private final SalesforceCustomerServiceAttributes    attributes;
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final SalesforceCustomerServiceSdkFactory    sdkFactory;

    /**
     * HK2 injection constructor.
     *
     * <p>The {@code invokerId} passed to the base class is obtained from the
     * {@link Invoker} — the same mechanism used by {@code SalesforceCustomerServiceAuthenticator}.
     * The base class stores this value and forwards it to every abstract hook call so
     * implementations do not have to manage it separately.</p>
     *
     * @param invoker         the Krista invoker (provides the stable invokerId)
     * @param auditService    the extension's audit recording + query service
     * @param attributes      operator-default Salesforce attributes (Invoker-attr-backed fallback)
     * @param attributesStore KV-backed credential store (admin's Auth-tab saved blob)
     * @param sdkFactory      factory that builds per-request SDK instances (used in health check)
     */
    @Inject
    public SalesforceCustomerServiceAuditResource(Invoker invoker,
                                                  SalesforceCustomerServiceAuditService auditService,
                                                  SalesforceCustomerServiceAttributes attributes,
                                                  SalesforceCustomerServiceAttributesStore attributesStore,
                                                  SalesforceCustomerServiceSdkFactory sdkFactory) {
        super(invoker.getInvokerId());
        this.invoker         = invoker;
        this.auditService    = auditService;
        this.attributes      = attributes;
        this.attributesStore = attributesStore;
        this.sdkFactory      = sdkFactory;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // AbstractOAuth2AuditResource — required abstract hooks
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link SalesforceCustomerServiceAuditService#getData}. Failures in
     * the service (KV read errors, deserialization) are already swallowed there; this hook
     * simply returns whatever the service produces.</p>
     */
    @Override
    protected AuditData getAuditData(String invokerId,
                                     Long from,
                                     Long to,
                                     String type,
                                     String status) {
        return auditService.getData(invokerId, from, to, type, status);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link SalesforceCustomerServiceAuditService#clearRecords}.</p>
     */
    @Override
    protected void clearAuditRecords(String invokerId, Long from, Long to) {
        auditService.clearRecords(invokerId, from, to);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // AbstractOAuth2AuditResource — overridable hooks
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Returns a JSON-serialisable map describing the current credential health.
     *
     * <p>Attempts a lightweight token acquisition via the SDK to determine whether
     * the stored credentials are valid. Any exception (SDK exception, KV error, network
     * timeout) produces a {@code "status": "error"} response — the endpoint must never
     * throw or produce a 5xx response from an uncaught exception.</p>
     *
     * <p>Reported fields:</p>
     * <ul>
     *   <li>{@code status}        — {@code "healthy"} or {@code "error"}</li>
     *   <li>{@code message}       — error message when {@code status = "error"}</li>
     *   <li>{@code instanceUrl}   — configured Salesforce instance URL (masked if blank)</li>
     *   <li>{@code sandbox}       — whether the sandbox flag is set</li>
     *   <li>{@code clientIdMask}  — first-8 + *** of the client ID (never the full value)</li>
     *   <li>{@code hasUserCredentials} — whether username + password + security token are set</li>
     * </ul>
     *
     * <p><b>MS-aliased fields (CRIT-030)</b> — added for the SDK's bundled audit dashboard
     * UI ({@code audit/index.html}), which hardcodes Microsoft field names. The dashboard
     * reads these to render the status-chip row at the top:</p>
     * <ul>
     *   <li>{@code authType}          — human-readable saved-flow label (e.g. {@code "Server-to-Server"})</li>
     *   <li>{@code email}             — operator identity (admin email / userName / jwtUser)</li>
     *   <li>{@code hasRefreshToken}   — {@code true} for AUTH_CODE flows, {@code "N/A"} for CC/JWT/Password (no RT), {@code false} on failure</li>
     *   <li>{@code clientSecretExpiry} — always {@code ""}; Salesforce Connected Apps don't expose expiry</li>
     * </ul>
     * <p>Provider-aware badges (Salesforce-themed dashboard) are scheduled for Stage 3
     * SDK work; until then, these aliases bridge the contract gap.</p>
     *
     * @return mutable map safe for Gson serialisation to JSON
     */
    @Override
    protected Map<String, Object> buildHealthResponse() {
        Map<String, Object> health = new HashMap<>();

        // CRIT-032 — read EFFECTIVE attrs: KV blob (admin's saved Auth-tab data) first,
        // fall back to the HK2-injected default (legacy Invoker-attr-backed). Without this,
        // the health check always saw the Public-Connected-App default and couldn't surface
        // the admin's chosen flow / email / instance URL.
        SalesforceCustomerServiceAttributes effective = effectiveAttributes();

        // Populate static credential metadata — these are safe even if the token
        // acquisition below fails.
        health.put("instanceUrl",         emptyIfNull(effective.getInstanceUrl()));
        health.put("sandbox",             effective.isSandbox());
        health.put("clientIdMask",        mask(effective.getClientId()));
        health.put("hasUserCredentials",  hasUserCredentials(effective));

        // CRIT-030 — MS-aliased fields for the bundled audit dashboard UI.
        String detectedFlow = detectSavedFlow(effective);
        health.put("authType",           detectedFlow);
        health.put("email",              emptyIfNull(resolveOperatorEmail(effective)));
        health.put("clientSecretExpiry", "");

        // CRIT-032 — provider hint so the SDK dashboard JS can hide MS-only panels
        // (Subscription, Mail Alert, "Tenant" label) when running for a Salesforce extension.
        health.put("provider", "salesforce");

        // CRIT-032 — health-check strategy is FLOW-AWARE:
        //
        //   • Quick Setup + Authorization Code (per-user AUTH_CODE flows): there is no
        //     "operator" token to fetch; the admin doesn't even have personal credentials
        //     stored. Calling acquireForAdmin() here throws "accountId required" because
        //     the per-user flow needs an end-user identity. Health for these flows is
        //     simply: do we have a valid Connected App config? clientId present + flow
        //     detected = healthy. Re-auth status per user lives in audit RECORDS, not here.
        //
        //   • Server-to-Server / Password Grant / JWT Bearer: these ARE operator flows.
        //     A live acquireForAdmin() probe is the right health signal — success means
        //     the admin's configured credentials still work end-to-end.
        boolean isPerUserFlow = "Quick Setup".equals(detectedFlow)
                             || "Authorization Code".equals(detectedFlow);
        boolean isConfigured  = !"Not configured".equals(detectedFlow);

        if (!isConfigured) {
            health.put("status",          "not configured");
            health.put("hasRefreshToken", Boolean.FALSE);
            health.put("message",         "No credentials configured. Open the Authentication tab to set up.");
            return health;
        }

        if (isPerUserFlow) {
            // Static health: config is valid; user-level RT presence is shown in audit records.
            health.put("status",          "healthy");
            health.put("hasRefreshToken", "N/A");  // per-user — not an operator-level token
            LOGGER.debug("[sfcs-audit] health: per-user flow ({}) — static-config healthy", detectedFlow);
            return health;
        }

        // Operator-flow probe (CC / Password / JWT).
        try {
            sdkFactory.newSdk(effective).acquireForAdmin();
            health.put("status",          "healthy");
            // Operator flows don't use refresh tokens — "N/A" tells the dashboard
            // to score them healthy on success.
            health.put("hasRefreshToken", "N/A");
            LOGGER.debug("[sfcs-audit] health: operator flow ({}) — token acquisition succeeded", detectedFlow);

        } catch (Exception e) {
            health.put("status",          "error");
            health.put("message",         e.getMessage() != null ? e.getMessage()
                                                                 : e.getClass().getSimpleName());
            health.put("hasRefreshToken", Boolean.FALSE);
            LOGGER.debug("[sfcs-audit] health: operator flow ({}) — token acquisition failed: {}",
                    detectedFlow, e.getMessage());
        }
        return health;
    }

    /**
     * Resolves the effective attributes for the current invoker — preferring the
     * Auth-tab KV blob (the admin's most recent save), falling back to the HK2-injected
     * Invoker-attr-backed default for invokers that haven't been migrated to the new tab yet.
     *
     * <p>Mirrors {@code SalesforceCustomerServiceExtension.effectiveAttributes()} so the
     * health check sees the same attrs as the runtime catalog requests.</p>
     */
    private SalesforceCustomerServiceAttributes effectiveAttributes() {
        if (attributesStore != null && invoker != null) {
            try {
                SalesforceCustomerServiceAttributes saved =
                        attributesStore.loadByInvokerId(invoker.getInvokerId());
                if (saved != null) {
                    return saved;
                }
            } catch (Exception e) {
                LOGGER.debug("[sfcs-audit] KV load failed for invoker={}, falling back to Invoker attrs: {}",
                        getInvokerId(), e.getMessage());
            }
        }
        return attributes;
    }

    /**
     * Detects the operator's saved authentication flow from the given attributes.
     *
     * <p>Returns a human-readable label suitable for direct display in the audit
     * dashboard's "Auth" badge. Detection order matches the HC-006 routing precedence
     * used by {@code SalesforceStrategyFactory}.</p>
     *
     * @return one of: "Not configured", "JWT Bearer", "Password Grant", "Quick Setup",
     *         "Server-to-Server", "Authorization Code"
     */
    private static String detectSavedFlow(SalesforceCustomerServiceAttributes attrs) {
        if (attrs.getClientId() == null || attrs.getClientId().isBlank()) {
            return "Not configured";
        }
        if (attrs.getJwtPrivateKeyPem() != null && !attrs.getJwtPrivateKeyPem().isBlank()
                && attrs.getJwtUser() != null && !attrs.getJwtUser().isBlank()) {
            return "JWT Bearer";
        }
        if (hasUserCredentials(attrs)) {
            return "Password Grant";
        }
        if (attrs.isPublicApp()) {
            return "Quick Setup";
        }
        if (attrs.getInstanceUrl() != null && !attrs.getInstanceUrl().isBlank()) {
            return "Server-to-Server";
        }
        return "Authorization Code";
    }

    /**
     * Resolves the operator's identity for display in the audit dashboard's "Email" badge.
     * Quick Setup → saved admin email; Password Grant → userName; JWT Bearer → jwtUser.
     * Server-to-Server and Authorization Code have no natural email field — returns empty.
     */
    private static String resolveOperatorEmail(SalesforceCustomerServiceAttributes attrs) {
        if (attrs.getEmail() != null && !attrs.getEmail().isBlank()) {
            return attrs.getEmail();
        }
        if (attrs.getUserName() != null && !attrs.getUserName().isBlank()) {
            return attrs.getUserName();
        }
        if (attrs.getJwtUser() != null && !attrs.getJwtUser().isBlank()) {
            return attrs.getJwtUser();
        }
        return "";
    }

    private static boolean hasUserCredentials(SalesforceCustomerServiceAttributes attrs) {
        return attrs.getUserName()      != null && !attrs.getUserName().isBlank()
            && attrs.getPassword()      != null && !attrs.getPassword().isBlank()
            && attrs.getSecurityToken() != null && !attrs.getSecurityToken().isBlank();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link SalesforceCustomerServiceAuditService#getArchiveIndex()}.</p>
     */
    @Override
    protected List<Map<String, Object>> getArchiveIndex() {
        return auditService.getArchiveIndex();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link SalesforceCustomerServiceAuditService#getArchivedRecords(String)}.</p>
     */
    @Override
    protected List<AuditRecord> getArchivedRecords(String archiveKey) {
        return auditService.getArchivedRecords(archiveKey);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link SalesforceCustomerServiceAuditService#deleteArchive(String)}.</p>
     */
    @Override
    protected void deleteArchiveEntry(String archiveKey) {
        auditService.deleteArchive(archiveKey);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link SalesforceCustomerServiceAuditService#getGlobalCounter()}.</p>
     */
    @Override
    protected Map<String, Object> getGlobalCounter() {
        return auditService.getGlobalCounter();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link SalesforceCustomerServiceAuditService#archiveAll()}.</p>
     */
    @Override
    protected void archiveAllRecords() {
        auditService.archiveAll();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────────

    private static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }
}
