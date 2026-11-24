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

import app.krista.oauth.providers.salesforce.config.SalesforceAuthFlow;
import app.krista.oauth.providers.salesforce.extension.annotations.SalesforceExtension;
import app.krista.sdk.msauth.extension.annotations.AuthScope;
import app.krista.sdk.msauth.extension.annotations.CustomProperty;
import app.krista.sdk.msauth.extension.annotations.PropertyType;

/**
 * Salesforce Customer Service extension metadata configuration.
 *
 * <p>This class is <b>annotation-only</b> — it carries all Salesforce-SDK metadata
 * annotations in one place, keeping them separate from the lifecycle-hook-bearing
 * {@link SalesforceCustomerServiceExtension}. The pattern mirrors the MS migration
 * guide's recommended {@code {Extension}AuthConfig.java} class.</p>
 *
 * <h2>SDK discovery</h2>
 * <p>{@link SalesforceExtension} is itself meta-annotated with
 * {@code @OAuth2Extension(providerName="salesforce-platform")}, so
 * {@code OAuth2ExtensionScanner.scan(SalesforceCustomerServiceAuthConfig.class)}
 * discovers this class automatically via the meta-annotation chain — no
 * explicit SPI file or {@code Class.forName} hint is required.</p>
 *
 * <h2>Annotation availability in v1.1.0</h2>
 * <ul>
 *   <li>{@code @SalesforceExtension} — canonical SF annotation in
 *       {@code app.krista.oauth.providers.salesforce.extension.annotations}.</li>
 *   <li>{@code @AuthScope}, {@code @CustomProperty} — shared from
 *       {@code app.krista.sdk.msauth.extension.annotations}; the SDK Javadoc
 *       explicitly states these are reused for Salesforce — the SF scope strings
 *       fit the existing schema unchanged.</li>
 *   <li>{@code @AuditConfig}, {@code @AuditEventGroup} — MS-only in v1.1.0;
 *       no SF-equivalent annotations exist yet.
 *       TODO: add SF-equivalent {@code @AuditConfig} / {@code @AuditEventGroup}
 *       when the SDK adds Salesforce audit annotation support (post-v1.1.0 gap).</li>
 * </ul>
 *
 * <h2>Audit dashboard</h2>
 * <p>{@code enableAuditDashboard = true} as of Chunk M3 — the
 * {@code SalesforceCustomerServiceAuditService} + {@code SalesforceCustomerServiceAuditResource}
 * are now wired and the "Audit Dashboard" custom tab is registered in the extension.</p>
 *
 * @since 2.0.18 (microsoft-auth-sdk 1.1.0 migration)
 */
@SalesforceExtension(
    extensionType       = "salesforce-customer-service",
    callbackPathPrefix  = "/salesforcecustomerservice",
    supportedFlows      = {
        SalesforceAuthFlow.AUTH_CODE_QUICK,       // Quick Setup: Krista-managed Connected App
        SalesforceAuthFlow.AUTH_CODE,              // User Sign-In: customer's own Connected App
        SalesforceAuthFlow.CLIENT_CREDENTIALS,     // Server-to-Server
        SalesforceAuthFlow.PASSWORD_GRANT          // Username + Password (legacy)
    },
    usePkce             = false,           // PKCE off: preserve legacy Connected-App compatibility
    allowLegacyPasswordGrant = true,       // opt-in: extension explicitly supports password grant
    headerTitle         = "Salesforce Customer Service",
    headerSubtitle      = "Connect Krista to Salesforce",
    // Sidebar "Salesforce Setup Guide" link (rendered under the Advanced Setup card in
    // the SDK's two-level auth UI) → the in-platform Documentation tab's Docsify route
    // for the "Creating Salesforce Connected App" page. The path is relative to the auth
    // tab URL (.../rest/salesforcecustomerservice/authentication/): "../../../" climbs to
    // the extension base, then into the "static/docs" Documentation tab (see customTabs()).
    setupGuideUrl       = "../../../static/docs/#/pages/CreatingSalesforceApp",
    // showScopePanel intentionally omitted — picks up the @SalesforceExtension
    // default of false. Salesforce Connected Apps pin allowed scopes at the
    // app-config level, so letting the admin tick scopes on/off via UI doesn't
    // change what's actually granted. The standard scope set is sent automatically.
    supportsOwnCredentials = true,
    enableAuditDashboard = true            // Chunk M3: AuditService + AuditResource now wired
)
// ── OAuth 2.0 scope declarations ─────────────────────────────────────────────────────
// These match the scope string returned by SalesforceCustomerServiceAttributes.getScopeString()
// ("api refresh_token offline_access") plus the optional content-file scope.
@AuthScope(
    value       = "api",
    required    = true,
    description = "Full access to Salesforce REST APIs — required for all CRM catalog requests",
    catalogRequests = {
        "Fetch Cases", "Fetch Accounts", "Fetch Contacts",
        "Create Case", "Update Case", "Close Case",
        "Fetch Customers", "Fetch Individuals"
    }
)
@AuthScope(
    value       = "refresh_token",
    required    = true,
    description = "Persist authorization across sessions without repeated user sign-in"
)
@AuthScope(
    value       = "offline_access",
    required    = true,
    description = "Offline access alias — required by Salesforce for refresh-token issuance"
)
@AuthScope(
    value       = "content",
    required    = false,
    description = "Access Salesforce Files (Content) — required only for attachment/file catalog requests",
    catalogRequests = {"Fetch Content", "Upload Attachment"}
)
// ── Custom property declarations ──────────────────────────────────────────────────────
// Declares the extension-specific fields shown in the Auth UI setup tab.
// These mirror the @Field.* declarations on SalesforceCustomerServiceExtension; the
// @CustomProperty annotations here make them discoverable by the SDK's metadata scanner
// for Auth UI rendering, while @Field.* continues to drive the Krista attribute store.
@CustomProperty(
    key          = "sandbox",
    label        = "Use Sandbox",
    type         = PropertyType.BOOLEAN,
    defaultValue = "false",
    required     = false,
    section      = "Salesforce Environment",
    order        = 1,
    helpText     = "Enable to connect to a Salesforce sandbox org instead of production"
)
@CustomProperty(
    key          = "Instance URL",
    label        = "Instance URL",
    type         = PropertyType.URL,
    defaultValue = "",
    required     = false,
    section      = "Salesforce Environment",
    order        = 2,
    helpText     = "Custom Salesforce instance URL for Client Credentials flow "
                 + "(e.g. https://myorg.my.salesforce.com). Leave blank for standard production/sandbox."
)
public final class SalesforceCustomerServiceAuthConfig {

    private SalesforceCustomerServiceAuthConfig() {
        // Annotation-only utility class — never instantiated
    }
}
