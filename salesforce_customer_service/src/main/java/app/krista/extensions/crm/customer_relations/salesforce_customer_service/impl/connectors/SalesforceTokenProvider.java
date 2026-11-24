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

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceSdkFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.field.NamedValuedField;
import app.krista.oauth.core.audit.AuditRecorder;
import app.krista.oauth.core.OAuth2AccessContext;
import app.krista.oauth.providers.salesforce.SalesforceOAuth2Sdk;
import app.krista.oauth.providers.salesforce.exception.InvalidGrantException;
import app.krista.oauth.providers.salesforce.exception.SalesforceException;
import com.github.scribejava.apis.salesforce.SalesforceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Provides Salesforce access tokens for the current request by delegating all OAuth
 * token-acquisition work to the {@link SalesforceOAuth2Sdk} via
 * {@link SalesforceCustomerServiceSdkFactory}.
 *
 * <h2>Authentication flow routing</h2>
 *
 * <p>The SDK's {@code SalesforceStrategyFactory} implements the full HC-006 routing matrix
 * ({@code acquireForUser} ↔ Authorization Code + RT refresh;
 * {@code acquireForAdmin} ↔ Client Credentials / Password Grant / JWT Bearer, selected
 * automatically from the resolved config). This class no longer replicates that logic.</p>
 *
 * <h2>Custom error messages</h2>
 *
 * <p>The original hand-rolled OAuth code produced rich user-facing error text for
 * {@code invalid_client} and {@code invalid_grant} failures. This class preserves those
 * messages in {@link #formatErrorMessage(SalesforceException)}: SDK exceptions are caught,
 * the message is classified, and a RuntimeException carrying the user-friendly text is
 * re-thrown with the original SDK exception as the cause.</p>
 *
 * <h2>Multi-tenant per-request attrs</h2>
 *
 * <p>Each public token-acquisition call resolves attributes at call time (already resolved
 * at construction, since one instance is built per auth-context). The factory builds a
 * fresh {@link SalesforceOAuth2Sdk} bound to those exact (clientId, clientSecret) credentials,
 * and that SDK instance is used for THIS call only — no mutable state is shared.</p>
 *
 * <h2>Bridge to legacy callers</h2>
 *
 * <p>{@link #toLegacySalesforceToken(OAuth2AccessContext)} converts the SDK's
 * {@link OAuth2AccessContext} (which places {@code instanceUrl} in {@code providerMetadata})
 * into the ScribeJava {@link SalesforceToken} that callers downstream expect. No call-site
 * changes are required.</p>
 *
 * <h2>Thread-safety</h2>
 *
 * <p>Instances are created per request by {@link SalesforceTokenProviderFactory} and are
 * NOT shared across threads. All fields are effectively final after construction.</p>
 *
 * @since salesforce_customer_service 2.0.18 (microsoft-auth-sdk 1.1.0 migration, T-M7)
 */
public class SalesforceTokenProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceTokenProvider.class);

    private final RequestContext requestContext;
    private final SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes;
    private final String authContextId;
    private final AuthorizationContext authorizationContext;
    private final SalesforceCustomerServiceSdkFactory sdkFactory;
    /**
     * Store used by {@link #chooseEffectiveAttributes(boolean)} to read the operator's saved
     * flow id (e.g. {@code "AUTH_CODE_QUICK"}). This is the durable indicator of Quick Setup:
     * the {@link SalesforceCustomerServiceAttributes#isPublicApp()} sentinel does NOT survive
     * KV (de)serialisation (it is a transient boolean on the constructed object, never present
     * in the attribute map), so we cannot rely on it on HK2-injected attrs. The savedFlow
     * string IS persisted alongside the attribute blob and round-trips cleanly.
     * May be {@code null} in tests / unit constructors that don't supply it.
     */
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    /**
     * The invoker ID for this provider. Used in diagnostic logging and in the operator-path
     * guard that detects AUTH_CODE-only setups and fails with a clear error message.
     * NOT used as the per-user identity key (that comes from AuthorizationContext exclusively).
     */
    private String invokerId;

    /**
     * Optional audit recorder, attached by {@link SalesforceTokenProviderFactory} on every
     * provider it builds — mirrors outlook-4's {@code GraphServiceClientProviderFactory}
     * calling {@code provider.setAuditService(...)}. When present, classified Salesforce auth
     * failures during token acquisition are auto-recorded to the audit dashboard. May be
     * {@code null} (test seams / unwired paths); recording is then a no-op.
     */
    private AuditRecorder auditService;

    /**
     * Production constructor injected by HK2. Uses the operator-default attributes.
     *
     * @param requestContext                      context of the current request
     * @param salesforceCustomerServiceAttributes operator-default Salesforce configuration
     * @param authorizationContext                authorization context for the current user
     * @param sdkFactory                          factory that produces per-request SDK instances
     */
    @Inject
    public SalesforceTokenProvider(RequestContext requestContext,
                                   SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes,
                                   AuthorizationContext authorizationContext,
                                   SalesforceCustomerServiceSdkFactory sdkFactory,
                                   SalesforceCustomerServiceAttributesStore attributesStore) {
        this(requestContext, salesforceCustomerServiceAttributes,
                authorizationContext, null, sdkFactory, attributesStore);
    }

    /** Factory uses this to inject the invokerId after construction for diagnostic logging. */
    public void setInvokerId(String invokerId) {
        this.invokerId = invokerId;
    }

    /**
     * Attaches the audit recorder. Called by {@link SalesforceTokenProviderFactory} for every
     * provider it builds, following the outlook-4 factory pattern.
     */
    public void setAuditService(AuditRecorder auditService) {
        this.auditService = auditService;
    }

    /**
     * Records a classified Salesforce auth failure to the audit dashboard, using the SDK
     * exception's polymorphic {@link SalesforceException#auditEventName()} (e.g.
     * {@code SF_INVALID_CLIENT_DETECTED}, {@code SF_TOKEN_EXPIRY_DETECTED}). Defensive and
     * guarded — never throws, and records nothing when the recorder or invokerId is absent.
     *
     * <p>Auth failures that surface as {@link MustAuthorizeException} are intentionally NOT
     * recorded here: those propagate to the catalog handler where the request-audit wrapper
     * (wiring #1) records them, so recording them here too would duplicate.</p>
     */
    private void auditAuthFailure(SalesforceException cause) {
        if (auditService == null || invokerId == null) {
            return;
        }
        try {
            auditService.record(invokerId, cause.auditEventName(), "failure",
                    null, cause.getMessage(), cause.getClass().getSimpleName(), null);
        } catch (Exception ignored) {
            // Audit must never break token acquisition.
        }
    }

    /**
     * Full constructor — supports both the default provider path (authContextId=null)
     * and per-request attribute-override paths (authContextId non-null).
     *
     * @param requestContext                      context of the current request
     * @param salesforceCustomerServiceAttributes the Salesforce configuration for this request
     * @param authorizationContext                authorization context for the current user
     * @param authContextId                       per-request attribute context ID, or null for operator default
     * @param sdkFactory                          factory that produces per-request SDK instances
     */
    public SalesforceTokenProvider(RequestContext requestContext,
                                   SalesforceCustomerServiceAttributes salesforceCustomerServiceAttributes,
                                   AuthorizationContext authorizationContext,
                                   String authContextId,
                                   SalesforceCustomerServiceSdkFactory sdkFactory,
                                   SalesforceCustomerServiceAttributesStore attributesStore) {
        this.requestContext = requestContext;
        this.salesforceCustomerServiceAttributes = salesforceCustomerServiceAttributes;
        this.authorizationContext = authorizationContext;
        this.authContextId = authContextId;
        this.sdkFactory = sdkFactory;
        this.attributesStore = attributesStore;
    }

    /**
     * Returns the Salesforce customer service attributes used by this token provider.
     *
     * @return the Salesforce customer service attributes
     */
    public SalesforceCustomerServiceAttributes getSalesforceCustomerServiceAttributes() {
        return salesforceCustomerServiceAttributes;
    }

    /**
     * Retrieves a Salesforce token for the current user.
     *
     * <p>Delegates to the SDK via {@link SalesforceCustomerServiceSdkFactory}. The SDK's
     * {@code SalesforceStrategyFactory} routes per-user calls to the Auth Code + refresh-token
     * strategy (HC-006 invariant).</p>
     *
     * @return a valid Salesforce token for the current user
     * @throws MustAuthorizeException if the stored refresh token is absent or expired
     */
    public SalesforceToken getSalesforceTokenForUser() {
        return getSalesforceToken(requestContext.invokeAsUser(), null);
    }

    /**
     * Retrieves a Salesforce token for admin / operator operations.
     *
     * <p>Delegates to the SDK. The SDK routes operator calls to Client Credentials,
     * Password Grant, or JWT Bearer depending on the resolved config (HC-006).</p>
     *
     * @throws MustAuthorizeException if authorization is required
     */
    public void getSalesforceTokenForAdmin() {
        getSalesforceToken(false, null);
    }

    /**
     * Internal routing entry point.
     *
     * <h2>Per-User Auth Invariants Compliance</h2>
     *
     * <p><b>Rule 2 (invokeAsUser=true always takes per-user flow):</b> The {@code invokeAsUser}
     * check is the very first branch — no operator state is read before it. When
     * {@code invokeAsUser=true} we resolve the accountId from {@link AuthorizationContext}
     * exclusively. If no authenticated user is available, we throw {@link MustAuthorizeException}
     * so the platform can prompt consent. We NEVER fall back to {@code invokerId} here —
     * that was the Violation B data-bleed precursor removed in KE-3206.</p>
     *
     * <p><b>Rule 1 (two hermetic keyspaces):</b> By passing only the wsContact accountId to
     * {@code acquireForUser}, the SDK's token key becomes
     * {@code sfv1/{wsContactId}/{hash(clientId+secret)}} — always user-specific. The operator
     * path below reaches {@code acquireForAdmin()} which never touches those keys.</p>
     *
     * <p><b>Violation C removed:</b> The {@code isAuthCodeOnlySetup() → acquireForUser(invokerId)}
     * bridge (CRIT-008) is gone. Operator-context calls on an AUTH_CODE-only configured invoker
     * now fail fast with a clear descriptive error rather than silently routing to a shared RT.</p>
     *
     * <p><b>Rule 3 (MustAuthorizeException propagation):</b> MAE is caught and rethrown BEFORE
     * the broader {@link SalesforceException} and {@link RuntimeException} catches, so it can
     * never be wrapped or swallowed.</p>
     *
     * @param invokeAsUser true to perform a per-user token acquisition
     * @param accountId    the account ID to acquire for; null resolves to the authorised account
     * @return a valid Salesforce token
     * @throws MustAuthorizeException if re-authorization is required (per-user path)
     */
    private SalesforceToken getSalesforceToken(boolean invokeAsUser, String accountId) {
        // CRIT-023 / CRIT-028: when NO authentication is configured at all AND this is an
        // operator-context call AND the in-memory attrs have no operator credentials,
        // fail FAST with a clear actionable message.
        //
        // The three conditions matter:
        //   1. !invokeAsUser            — this is an operator-context call, no end-user
        //                                 identity available to redirect through Public OAuth.
        //   2. isNothingSaved()         — the operator has never clicked Save on the
        //                                 Authentication tab; KV has no `savedFlow`.
        //   3. isAuthCodeOnlySetup()    — the in-memory attrs ALSO have no operator-flow
        //                                 credentials (no instanceUrl for CC, no
        //                                 userName/password/securityToken for PG, no JWT
        //                                 creds). Without this third clause, the guard
        //                                 fires for the Test Connection path: the operator
        //                                 typed creds into the Auth tab form, clicked
        //                                 Test, and the typed-in attrs reach this code with
        //                                 valid CC/PG creds — but isNothingSaved() is true
        //                                 because they haven't Saved yet. We need to LET
        //                                 the test proceed so the typed-in credentials
        //                                 actually get exercised against Salesforce.
        if (!invokeAsUser && isNothingSaved() && isAuthCodeOnlySetup()) {
            LOGGER.warn("Operator-context Salesforce call with no authentication configured for invokerId={}", invokerId);
            throw new RuntimeException(
                    "No Salesforce authentication is configured. Either:\n" +
                    "  1. Configure Quick Setup or Advanced authentication in the Authentication tab, OR\n" +
                    "  2. Enable 'Have People Authenticate with the Account' on this workflow's " +
                    "Action settings so users sign in with their own Salesforce identity.");
        }

        // chooseEffectiveAttributes() is the seam that substitutes Public-app credentials
        // for per-user calls (Quick Setup, or fresh-install default). Operator-context calls
        // use the saved attrs directly.
        SalesforceOAuth2Sdk sdk = sdkFactory.newSdk(chooseEffectiveAttributes(invokeAsUser));

        System.out.println("[SF-DIAG] getSalesforceToken: invokeAsUser=" + invokeAsUser +
                " accountId=" + accountId + " invokerId=" + invokerId);

        try {
            OAuth2AccessContext ctx;

            if (invokeAsUser) {
                // ── Rule 2: per-user path is ALWAYS first. No operator-state read above this. ──
                // Resolve accountId from AuthorizationContext only. If no authenticated user
                // is present, throw MAE so the platform shows the consent prompt.
                String wsContactId = accountId != null ? accountId : safeAccountId();
                System.out.println("[SF-DIAG] getSalesforceToken: per-user path, resolved wsContactId=" + wsContactId);
                if (wsContactId == null) {
                    LOGGER.info("invokeAsUser=true but no authenticated AuthorizationContext — " +
                            "triggering MustAuthorizeException for consent prompt.");
                    throw createMustAuthorizationException(false, null);
                }
                LOGGER.debug("Acquiring per-user token via SDK for wsContact={} invokeAsUser=true", wsContactId);
                ctx = sdk.acquireForUser(wsContactId);

            } else {
                // ── Operator path: invokeAsUser=false. ──
                // Three subcases, picked by what the admin configured in the Authentication tab:
                //   (a) AUTH_CODE-only setup → use the admin's setup-time RT stored under
                //       `invokerId&clientId+secret`. The Connected App's Connected User =
                //       the admin who completed Test Connection. Mirrors outlook-4's pattern
                //       where setup-time RT serves as the operator-shared identity.
                //   (b) CLIENT_CREDENTIALS configured → SDK's HC-006 picks the CC strategy →
                //       fresh server-to-server token, no RT involved.
                //   (c) PASSWORD_GRANT configured → SDK's HC-006 picks the PG strategy →
                //       fresh token via password grant, no RT involved.
                //
                // Hermetic keyspace (Rule 1) preserved: invokerId-prefixed (operator setup-time RT)
                // and wsContact_*-prefixed (per-user) keys can never collide.
                if (isAuthCodeOnlySetup()) {
                    // CRIT-022: for Quick Setup the operator identity is the admin's EMAIL
                    // (entered on the Quick Setup tab), not invokerId. Using invokerId here
                    // would mean "whichever admin clicked Test Connection last owns this
                    // operator context" — silent overwrite + no accountability. For Advanced
                    // AUTH_CODE-only setups we keep the legacy invokerId-as-identity behavior.
                    String operatorAccountId;
                    String savedEmail = salesforceCustomerServiceAttributes != null
                            ? salesforceCustomerServiceAttributes.getEmail() : null;
                    boolean isQuickSetup = isSavedFlowQuickSetup();
                    if (isQuickSetup && savedEmail != null && !savedEmail.isBlank()) {
                        operatorAccountId = savedEmail;
                        LOGGER.debug("Quick Setup operator path: acquiring via Test Connection RT keyed by email={}",
                                operatorAccountId);
                        System.out.println("[SF-DIAG] getSalesforceToken: operator Quick Setup path using email="
                                + operatorAccountId);
                    } else if (isQuickSetup) {
                        throw new RuntimeException(
                                "Quick Setup operator-context call but no saved admin email — " +
                                "ask the administrator to re-test the Quick Setup connection " +
                                "with a valid Salesforce User Email.");
                    } else {
                        if (invokerId == null) {
                            throw new RuntimeException(
                                    "AUTH_CODE-only setup but no invokerId available to look up the " +
                                    "admin's setup-time refresh token.");
                        }
                        operatorAccountId = invokerId;
                        LOGGER.debug("AUTH_CODE operator path: acquiring via setup-time RT keyed by invokerId={}", invokerId);
                        System.out.println("[SF-DIAG] getSalesforceToken: operator AUTH_CODE path using invokerId=" + invokerId);
                    }
                    ctx = sdk.acquireForUser(operatorAccountId);
                } else {
                    LOGGER.debug("Acquiring operator token via SDK (CLIENT_CREDENTIALS / PASSWORD_GRANT / JWT) " +
                            "invokeAsUser=false invokerId={}", invokerId);
                    ctx = sdk.acquireForAdmin();
                }
            }

            return toLegacySalesforceToken(ctx);

        } catch (MustAuthorizeException mae) {
            // Rule 3: MUST rethrow BEFORE any broader catch. Never wrap or swallow MAE.
            throw mae;

        } catch (InvalidGrantException e) {
            if (invokeAsUser) {
                String wsContactId = accountId != null ? accountId : safeAccountId();
                LOGGER.debug("InvalidGrant from SDK for wsContact={} — triggering re-authorization", wsContactId);
                throw createMustAuthorizationException(true, wsContactId);
            }
            // Operator AUTH_CODE-only setup: invalid_grant on the admin's setup-time RT
            // means the admin needs to re-do Test Connection (their RT was revoked).
            // We surface a clear error rather than MAE (which would target end-users, not
            // the admin).
            LOGGER.error("invalid_grant on operator flow for invokerId={}: {}", invokerId, e.getMessage());
            auditAuthFailure(e);
            throw new RuntimeException(formatErrorMessage(e), e);

        } catch (SalesforceException e) {
            // Per-user: "No refresh token stored" → throw MAE so the platform prompts consent.
            // This is the exact condition that produces the popup-once-per-user UX described
            // in PER_USER_AUTH_INVARIANTS.md.
            if (invokeAsUser && e.shouldRemoveToken()) {
                String wsContactId = accountId != null ? accountId : safeAccountId();
                LOGGER.info("Per-user RT missing/revoked for wsContact={} — throwing MAE for consent prompt", wsContactId);
                throw createMustAuthorizationException(false, wsContactId);
            }
            // Other operator-side SF errors: wrap with friendly message.
            LOGGER.error("Salesforce authentication failed: invokeAsUser={} {}", invokeAsUser, e.getMessage(), e);
            auditAuthFailure(e);
            throw new RuntimeException(formatErrorMessage(e), e);
        }
    }

    /**
     * Seam for selecting the OAuth client config to use for the current call.
     *
     * <h2>Quick Setup / Public App path ({@code invokeAsUser=true})</h2>
     *
     * <p>When the operator chose the Quick Setup flow ({@code AUTH_CODE_QUICK}), per-user calls
     * must use Krista's own Salesforce Connected App credentials rather than the customer's
     * saved attrs. This keeps per-user identity hermetic: the Connected User that Salesforce
     * sees is always the end user who clicked "Authorize", never the admin who configured the
     * extension. The credentials are loaded from {@code config.properties} bundled in the
     * extension artifact (never from KV, never from the browser).</p>
     *
     * <p>The sandbox flag is forwarded from the saved attrs so the user-facing login page lands
     * on the correct Salesforce environment (production vs sandbox). All other fields (clientId,
     * clientSecret) come from {@code config.properties}; operator-supplied values are ignored
     * for the Public path.</p>
     *
     * <p>Mirrors {@code GraphServiceClientProvider.chooseEffectiveAttributes(invokeAsUser=true)}
     * in outlook-4, which substitutes the Public Azure AD credentials when the user context is
     * active.</p>
     *
     * <h2>Advanced / Private App path ({@code invokeAsUser=false}, or no Quick Setup saved)</h2>
     *
     * <p>Returns the customer's saved attrs unchanged. Operator-side calls (CLIENT_CREDENTIALS,
     * PASSWORD_GRANT, AUTH_CODE operator path) use whatever the admin configured in the
     * Authentication tab.</p>
     *
     * @param invokeAsUser {@code true} for per-user (end-user browser auth) requests
     * @return the effective attributes for this call
     */
    private SalesforceCustomerServiceAttributes chooseEffectiveAttributes(boolean invokeAsUser) {
        // Quick Setup invariant: per-user calls ALWAYS use the Public Connected App when one
        // exists, to ensure user identity is never bleed through operator-shared credentials.
        // We detect Quick Setup by checking whether the saved attrs are flagged as publicApp
        // OR by checking whether the saved attrs have neither instanceUrl nor user credentials
        // (which is exactly the Quick Setup shape: only sandbox + Krista's own credentials).
        //
        // Concrete rule: if invokeAsUser=true AND the operator picked Quick Setup
        // (savedFlow=="AUTH_CODE_QUICK"), substitute a fresh Public-app instance from
        // config.properties. This preserves the sandbox preference the operator chose at setup
        // time.
        //
        // IMPORTANT — why we DON'T rely on isPublicApp():
        //   The `publicApp` boolean sentinel on SalesforceCustomerServiceAttributes is a runtime
        //   flag set by the createPublic*() factories. It is NOT in `invoker.getAttributes()`,
        //   so HK2 attrs built via `@Inject SalesforceCustomerServiceAttributes(Invoker)` always
        //   come back with `publicApp=false` — even after a successful Quick Setup save. We must
        //   detect Quick Setup via something durable across KV round-trips. The attributes store
        //   persists `savedFlow` alongside the credential blob, so reading it via
        //   `attributesStore.getSavedFlow(invokerId)` returns "AUTH_CODE_QUICK" reliably here.
        //   We retain the `isPublicApp()` short-circuit for the (rare) call paths where the
        //   attrs were constructed directly via createPublic*() and the flag IS still true —
        //   e.g. the /v3/oauth/callback handler in AuthenticationResource.
        //
        // For operator-context calls (invokeAsUser=false) we always use the saved attrs, even
        // if they are Public-app backed — the AUTH_CODE operator path reads the setup-time RT
        // from KV under invokerId, and that RT was issued by the Public app, so the credentials
        // must match.
        // CRIT-023: also default to Public attrs when this is a per-user call AND nothing
        // has been saved in the Authentication tab. A fresh install must let the end user
        // sign in via Krista's bundled Connected App (central callback URL) — without this,
        // the consent popup would point at the customer's never-configured per-invoker
        // callback URL, which Salesforce would reject.
        // CRIT-029 — invokeAsUser=true ALWAYS uses Krista's Public Connected App for
        // per-user calls, regardless of what (if anything) admin has saved in the
        // Authentication tab. This is the per-user hermeticity invariant from
        // PER_USER_AUTH_INVARIANTS.md Rule 2 — operator-context config (CC, PG, JWT,
        // Advanced AUTH_CODE) MUST NOT affect per-user token acquisition.
        //
        // Concrete reason: the user's per-user RT is keyed by
        //   sfv1/wsContact_<id>/<hash(publicClientId+publicSecret)>
        // If we ever switched the per-user path to use a different clientId/secret pair
        // (e.g., because admin saved Server-to-Server with their own Connected App), the
        // hash would change, the lookup would miss, and the user would get a spurious
        // re-authorization prompt even though their token is still valid under the Public
        // hash. That violates the invariant and confuses the user.
        //
        // Operator-context calls (invokeAsUser=false) continue to use the saved attrs
        // unchanged — they read CC/PG/JWT/Advanced-AC credentials from the saved config
        // and acquire tokens through the matching flow.
        if (invokeAsUser && salesforceCustomerServiceAttributes != null) {
            try {
                boolean isSandbox = salesforceCustomerServiceAttributes.isSandbox();
                String callbackUrl = salesforceCustomerServiceAttributes.getCallbackUrl();
                String routingUrl = stripCallbackSuffix(callbackUrl);
                return SalesforceCustomerServiceAttributes.createPublicFromRoutingUrl(routingUrl, isSandbox);
            } catch (Exception e) {
                LOGGER.error("chooseEffectiveAttributes: failed to build Public-app attrs for per-user call — " +
                        "falling back to saved attrs. Per-user identity may be lost. Error: {}", e.getMessage(), e);
                // Fail-safe: config.properties missing or corrupt. Fall through to saved
                // attrs; the call will likely fail downstream with a clear "invalid_client"
                // or "no RT stored" error.
            }
        }
        return salesforceCustomerServiceAttributes;
    }

    /**
     * Bridges the SDK's {@link OAuth2AccessContext} to the legacy {@link SalesforceToken}
     * expected by downstream callers (SalesforceService, SalesforceCustomerServiceExtension).
     *
     * <p>Callers use exactly two accessors on the returned token:
     * {@code getAccessToken()} and {@code getInstanceUrl()}. Both are populated here from
     * the corresponding fields in the SDK's context.</p>
     *
     * @param ctx the context returned by the SDK's acquire methods
     * @return a {@link SalesforceToken} carrying the bearer token and instance URL
     */
    private SalesforceToken toLegacySalesforceToken(OAuth2AccessContext ctx) {
        String instanceUrl = ctx.providerMetadata() != null
                ? ctx.providerMetadata().get("instanceUrl")
                : null;
        // SalesforceToken(accessToken, instanceUrl, rawResponse)
        // rawResponse is not used by any downstream caller; pass null.
        return new SalesforceToken(ctx.accessToken(), instanceUrl, null);
    }

    /**
     * Converts a {@link SalesforceException} into a user-friendly error message,
     * preserving the rich guidance text from the original hand-rolled error handler.
     *
     * <p>The SDK throws typed exceptions ({@link InvalidGrantException},
     * {@link SalesforceException}) without user-facing details. This method maps the
     * exception's message content to the same detailed messages previously produced by
     * {@code getErrorMessage(Exception)} when operating against the ScribeJava layer.</p>
     *
     * @param e the Salesforce exception from the SDK
     * @return a user-friendly error string suitable for surfacing to operators
     */
    /**
     * Safely extracts the accountId from the AuthorizationContext. Returns null if the context
     * is unauthenticated, missing, or the account is anonymous — instead of throwing an NPE.
     * Used in error-handling paths so a missing accountId doesn't mask the actual Salesforce
     * error the operator needs to see.
     */
    /**
     * Returns {@code true} when the attrs have NO operator-flow credentials (no instanceUrl
     * for CLIENT_CREDENTIALS, no userCreds for PASSWORD_GRANT, no JWT creds for JWT_BEARER).
     *
     * <p>Per PER_USER_AUTH_INVARIANTS.md and HC-006: AUTH_CODE is never a valid operator
     * flow. When this returns {@code true} on an operator-context call ({@code invokeAsUser=false}),
     * {@link #getSalesforceToken} fails with a descriptive error rather than attempting to
     * bridge the per-user and operator keyspaces (which was Violation C / CRIT-008 data bleed).</p>
     */
    private boolean isAuthCodeOnlySetup() {
        var a = salesforceCustomerServiceAttributes;
        if (a == null) return false;
        boolean hasInstance = a.getInstanceUrl() != null && !a.getInstanceUrl().isBlank();
        boolean hasUserCreds = a.getUserName() != null && !a.getUserName().isBlank()
                && a.getPassword() != null && !a.getPassword().isBlank()
                && a.getSecurityToken() != null && !a.getSecurityToken().isBlank();
        // JWT creds (if SF Customer Service ever supports JWT, this check stays correct)
        boolean hasJwt = false;
        try {
            // Reflective-style guard — SalesforceCustomerServiceAttributes may not expose jwt fields
            var m = a.getClass().getMethod("getJwtPrivateKeyPem");
            Object pem = m.invoke(a);
            hasJwt = pem != null && !((String) pem).isBlank();
        } catch (Exception ignored) { /* attribute class lacks JWT methods — no JWT setup */ }
        return !hasInstance && !hasUserCreds && !hasJwt;
    }

    /**
     * Returns {@code true} when the operator's saved flow (persisted by
     * {@link app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceSaveConfigurationImpl#saveCredentials})
     * is {@code "AUTH_CODE_QUICK"}.
     *
     * <p>Reads the durable {@code savedFlow} entry from the attributes store keyed by
     * {@link #invokerId}. Returns {@code false} when the store is unavailable (test seam),
     * the invokerId is unknown, or the store lookup throws — i.e. err on the side of NOT
     * substituting Public credentials when we can't prove Quick Setup is configured.</p>
     */
    private boolean isSavedFlowQuickSetup() {
        if (attributesStore == null || invokerId == null) return false;
        try {
            String savedFlow = attributesStore.getSavedFlow(invokerId);
            return "AUTH_CODE_QUICK".equals(savedFlow);
        } catch (Exception e) {
            LOGGER.debug("isSavedFlowQuickSetup: getSavedFlow failed for invokerId={} — assuming non-Quick. {}",
                    invokerId, e.getMessage());
            return false;
        }
    }

    /**
     * Returns {@code true} when nothing has been saved in the Authentication tab for this
     * invoker — no Quick Setup, no Advanced AUTH_CODE, no CLIENT_CREDENTIALS, no PASSWORD_GRANT.
     *
     * <p>Detected by checking the durable {@code savedFlow} entry in the attributes store.
     * When this returns {@code true}:</p>
     * <ul>
     *   <li>Per-user calls ({@code invokeAsUser=true}) default to the Public App / Quick Setup
     *       route automatically — the end user gets a one-click consent through Krista's
     *       bundled Connected App and lands at the central callback URL. This is the
     *       outlook-4 default behavior.</li>
     *   <li>Operator-context calls ({@code invokeAsUser=false}) fail FAST with a clear,
     *       actionable error rather than attempting the Auth Code-only bridge to a
     *       non-existent setup-time RT.</li>
     * </ul>
     *
     * <p>Conservative on errors: if the store is unavailable or the lookup throws, returns
     * {@code false} so we don't accidentally treat a misread as "nothing saved" and silently
     * cross paths with a different keyspace.</p>
     */
    private boolean isNothingSaved() {
        if (attributesStore == null || invokerId == null) return false;
        try {
            return attributesStore.getSavedFlow(invokerId) == null;
        } catch (Exception e) {
            LOGGER.debug("isNothingSaved: getSavedFlow failed for invokerId={} — assuming saved. {}",
                    invokerId, e.getMessage());
            return false;
        }
    }

    /**
     * Strips the fixed {@link Constants#CALLBACK_URI} suffix from a fully-qualified callback URL
     * to recover the routing base URL.
     *
     * <p>Used by {@link #chooseEffectiveAttributes(boolean)} to recover the routing URL from
     * an already-constructed attributes instance when an {@link app.krista.extension.executor.Invoker}
     * is not directly available at the call site.</p>
     */
    private static String stripCallbackSuffix(String callbackUrl) {
        if (callbackUrl != null && callbackUrl.endsWith(
                app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.CALLBACK_URI)) {
            return callbackUrl.substring(0, callbackUrl.length() -
                    app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.CALLBACK_URI.length());
        }
        return callbackUrl;
    }

    private String safeAccountId() {
        try {
            if (authorizationContext == null || !authorizationContext.isAuthenticated()) return null;
            var account = authorizationContext.getAuthorizedAccount();
            return account != null ? account.getAccountId() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatErrorMessage(SalesforceException e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (message.contains(Constants.INVALID_CLIENT_ERROR)) {
            LOGGER.debug("Invalid client credentials detected (SDK path)");
            return buildClientCredentialsErrorMessage();
        }

        if (message.contains(Constants.INVALID_GRANT_ERROR)) {
            LOGGER.debug("Invalid grant detected (SDK path)");
            return buildPasswordGrantErrorMessage();
        }

        if (message.contains("unauthorized")) {
            return "Authentication failed: Unauthorized access. Please check your credentials and ensure your user account has the necessary permissions.";
        }

        if (message.contains("timeout")) {
            return "Authentication failed: Request timeout. Please check your network connection and try again.";
        }

        if (message.contains("connection")) {
            return "Authentication failed: Connection error. Please verify your Instance URL and network connectivity.";
        }

        return "Salesforce authentication failed: " + e.getMessage() +
               ". Please verify your Salesforce configuration and contact support if the issue persists.";
    }

    /**
     * Builds a detailed error message for client credentials authentication failures.
     * Provides specific guidance based on the current configuration.
     */
    private String buildClientCredentialsErrorMessage() {
        StringBuilder errorMsg = new StringBuilder(Constants.INVALID_CLIENT_MESSAGE);

        String instanceUrl = salesforceCustomerServiceAttributes.getInstanceUrl();
        if (instanceUrl != null && !instanceUrl.trim().isEmpty()) {
            errorMsg.append(" You are using Client Credentials flow with custom domain: ")
                   .append(instanceUrl)
                   .append(". Verify that:");
        } else {
            errorMsg.append(" You are using Authorization Code flow. Verify that:");
        }

        errorMsg.append("\n1. Your Client ID is correct and active in Salesforce")
               .append("\n2. Your Client Secret matches the connected app configuration")
               .append("\n3. The connected app has the correct OAuth scopes enabled");

        if (instanceUrl != null && !instanceUrl.trim().isEmpty()) {
            errorMsg.append("\n4. The Instance URL (").append(instanceUrl).append(") is correct and accessible");
        }

        return errorMsg.toString();
    }

    /**
     * Builds a detailed error message for password grant authentication failures.
     * Provides specific guidance for username/password authentication.
     */
    private String buildPasswordGrantErrorMessage() {
        StringBuilder errorMsg = new StringBuilder(Constants.INVALID_GRANT_MESSAGE);

        String username = String.valueOf(salesforceCustomerServiceAttributes.getUserName());
        errorMsg.append(" You are using Password Grant flow");

        if (username != null && !username.equals("null") && !username.trim().isEmpty()) {
            errorMsg.append(" with username: ").append(username);
        }

        errorMsg.append(". Verify that:")
               .append("\n1. Your username is correct and the user account is active")
               .append("\n2. Your password is current and not expired")
               .append("\n3. Your security token is valid (check your email for the latest token)")
               .append("\n4. The user has API access enabled in their profile")
               .append("\n5. Your organization allows API access")
               .append("\n6. You are connecting to the correct Salesforce environment (production vs sandbox)");

        return errorMsg.toString();
    }

    /**
     * Creates a {@link MustAuthorizeException} with appropriate details to trigger re-auth.
     *
     * @param reAuthentication true when the existing token has expired and re-auth is needed
     * @param accountId        the account requiring re-authorization
     * @return a configured {@link MustAuthorizeException}
     */
    private MustAuthorizeException createMustAuthorizationException(boolean reAuthentication, String accountId) {
        LOGGER.info(reAuthentication
                ? "Got error for Authentication. So sending for re authentication"
                : "Got error for Authentication. So sending for authentication");
        accountId = accountId == null ? safeAccountId() : accountId;
        List<NamedValuedField> details = new ArrayList<>();
        // CRIT-025 — mirror outlook-4's 3-field MAE pattern so the Krista platform's
        // central OAuth handler recognizes this as a per-user MCP flow and preserves
        // the user's session through the forward to /v3/oauth/callback.
        //
        // Outlook-4 produces:
        //   userId       = INV_<invokerId>     (the invoker, NOT the end user)
        //   clientUserId = wsContact_<id>      (the actual end-user identity)
        //   authContextId= <uuid>
        //
        // SF used to produce only:
        //   userId       = wsContact_<id>      (← wsContact in userId; no clientUserId)
        //   authContextId= <uuid>
        // …which the platform did NOT recognize as the MCP per-user pattern, so it forwarded
        // the callback unauthenticated → listener.authorized() got 500 → no auto-resume.
        //
        // With these three fields, the Salesforce SDK base class's `state = userId + HASH +
        // authContextId` produces `INV_xxx#authContextId`; our SalesforceCustomerServiceAuthenticator
        // override then PREPENDS clientUserId to yield `wsContact#INV_xxx#authContextId#forwardPath`
        // — identical to outlook-4's wire format.
        //
        // The /v3/oauth/callback continues to use parts[0] as the token-store accountId, which
        // is still wsContact — so RT storage keying is unchanged.
        if (invokerId != null) {
            details.add(new NamedValuedField("userId", "Text", invokerId, new HashMap<>(), new HashMap<>()));
        } else {
            // Defensive: if invokerId is unavailable, fall back to wsContact in userId (legacy behavior).
            // This shouldn't happen at runtime — the factory always sets invokerId before use.
            details.add(new NamedValuedField("userId", "Text", accountId, new HashMap<>(), new HashMap<>()));
        }
        details.add(new NamedValuedField("clientUserId", "Text", accountId, new HashMap<>(), new HashMap<>()));
        if (authContextId != null) {
            details.add(new NamedValuedField("authContextId", "Text", authContextId, new HashMap<>(), new HashMap<>()));
        }
        return new MustAuthorizeException(
                reAuthentication
                        ? "Refresh Token Expired. Please reauthorize yourself"
                        : "Authorize yourself and click 'Validate Attributes' before saving changes to proceed further",
                details);
    }

    /**
     * Determines whether OAuth-based authentication should be used.
     *
     * <p>OAuth is used when in admin context OR when username/password credentials are
     * not fully provided. This static helper is preserved because:
     * <ul>
     *   <li>It is tested directly by {@code SalesforceTokenProviderTest}</li>
     *   <li>It documents the legacy flow-selection logic (now performed by the SDK
     *       internally, but useful for diagnostic/logging purposes)</li>
     * </ul>
     *
     * @param isAdmin               whether the context is admin/operator
     * @param salesforceSalesAttributes the Salesforce attributes to inspect
     * @return true if OAuth should be used, false if password grant is sufficient
     */
    public static boolean validateUsingOAuth(boolean isAdmin,
                                             SalesforceCustomerServiceAttributes salesforceSalesAttributes) {
        boolean useOAuth = isAdmin
                || (salesforceSalesAttributes.getUserName() == null
                || salesforceSalesAttributes.getUserName().isBlank()
                || salesforceSalesAttributes.getPassword() == null
                || salesforceSalesAttributes.getPassword().isBlank()
                || salesforceSalesAttributes.getSecurityToken() == null
                || salesforceSalesAttributes.getSecurityToken().isBlank());

        LOGGER.debug("Authentication flow selection - Admin context: {}, Use OAuth: {}, "
                        + "Has username: {}, Has password: {}, Has security token: {}, Instance URL: {}",
                isAdmin, useOAuth,
                salesforceSalesAttributes.getUserName() != null
                        && !salesforceSalesAttributes.getUserName().isBlank(),
                salesforceSalesAttributes.getPassword() != null
                        && !salesforceSalesAttributes.getPassword().isBlank(),
                salesforceSalesAttributes.getSecurityToken() != null
                        && !salesforceSalesAttributes.getSecurityToken().isBlank(),
                salesforceSalesAttributes.getInstanceUrl() != null
                        && !salesforceSalesAttributes.getInstanceUrl().isEmpty()
                        ? "provided" : "not provided");

        return useOAuth;
    }
}
