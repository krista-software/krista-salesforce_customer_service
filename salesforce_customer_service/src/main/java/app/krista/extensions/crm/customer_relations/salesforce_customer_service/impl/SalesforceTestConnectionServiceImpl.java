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
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceSdkFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.oauth.core.OAuth2AccessContext;
import app.krista.oauth.providers.salesforce.SalesforceOAuth2Sdk;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * Tests whether a given set of Salesforce credentials can successfully obtain a token.
 *
 * <h2>Flow routing</h2>
 * <ul>
 *   <li>{@code CLIENT_CREDENTIALS} — calls {@code getSalesforceTokenForAdmin()} on the
 *       SDK-backed token provider (server-to-server operator token).</li>
 *   <li>{@code PASSWORD_GRANT} — calls {@code getSalesforceTokenForAdmin()} which routes to
 *       Password Grant when username/password/securityToken are present (HC-006 matrix).</li>
 *   <li>{@code AUTH_CODE} — implements the full Authorization Code test path:
 *       <ol>
 *         <li>Persists credentials to KV first (CRIT-001 deliberate exception — the {@code /callback}
 *             endpoint runs in a separate HTTP request and must be able to load these attrs).</li>
 *         <li>Attempts to refresh an existing token for the current user if one was stored
 *             from a prior authorization. On success, returns {@code isSuccess=true} — no
 *             popup needed.</li>
 *         <li>If no refresh token exists (or refresh fails), builds the Salesforce authorization
 *             URL and returns it in the response so the UI can open it in a popup window.</li>
 *       </ol>
 *   </li>
 *   <li>Any unknown flow — returns a failure response; JWT_BEARER is out of scope for
 *       this extension.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>Instances are per-request (HK2 default scope). All fields are effectively final
 * after construction.</p>
 *
 * @since salesforce_customer_service 2.0.18
 */
@Service
public class SalesforceTestConnectionServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceTestConnectionServiceImpl.class);

    private static final String FLOW_CLIENT_CREDENTIALS = "CLIENT_CREDENTIALS";
    private static final String FLOW_PASSWORD_GRANT = "PASSWORD_GRANT";
    private static final String FLOW_AUTH_CODE = "AUTH_CODE";
    private static final String FLOW_AUTH_CODE_QUICK = "AUTH_CODE_QUICK";

    private final SalesforceTokenProviderFactory providerFactory;
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final SalesforceCustomerServiceSdkFactory sdkFactory;
    private final AuthorizationContext authorizationContext;
    private final Invoker invoker;
    private final String invokerId;

    /**
     * HK2-injection constructor.
     *
     * @param providerFactory      factory for building token providers bound to specific credentials
     * @param attributesStore      store for persisting and loading credentials by invokerId / authContextId
     * @param sdkFactory           factory for building per-request {@link SalesforceOAuth2Sdk} instances
     * @param authorizationContext Krista authorization context — provides the current account ID
     * @param invoker              Krista Invoker — provides the invokerId for persistent credential storage
     */
    @Inject
    public SalesforceTestConnectionServiceImpl(
            SalesforceTokenProviderFactory providerFactory,
            SalesforceCustomerServiceAttributesStore attributesStore,
            SalesforceCustomerServiceSdkFactory sdkFactory,
            AuthorizationContext authorizationContext,
            app.krista.extension.executor.Invoker invoker) {
        this.providerFactory = providerFactory;
        this.attributesStore = attributesStore;
        this.sdkFactory = sdkFactory;
        this.authorizationContext = authorizationContext;
        this.invoker = invoker;
        this.invokerId = invoker.getInvokerId();
    }

    /**
     * Attempts to verify that the given credentials work for the specified flow.
     *
     * <p>For {@code CLIENT_CREDENTIALS} and {@code PASSWORD_GRANT} flows, a real token
     * acquisition is attempted. On success, the token is discarded — nothing is persisted.</p>
     *
     * <p>For {@code AUTH_CODE}, credentials are persisted first (see class Javadoc for
     * rationale), then an existing refresh token is probed. If found and valid, returns
     * {@code isSuccess=true}. Otherwise returns a {@code url} for the UI to open in a popup.</p>
     *
     * @param attrs    the credential attributes to test
     * @param flow     the flow identifier string (e.g. {@code "CLIENT_CREDENTIALS"})
     * @return JSON string — one of:
     *         {@code {"isSuccess": true}} on success,
     *         {@code {"isSuccess": false, "errorMessage": "..."}} on failure, or
     *         {@code {"isSuccess": false, "url": "<authorize-url>"}} when a popup is needed
     */
    public String testConnection(SalesforceCustomerServiceAttributes attrs, String flow) {
        LOGGER.debug("testConnection called for flow={}", flow);

        if (flow == null || flow.isBlank()) {
            return failureResponse("Flow identifier must not be blank.");
        }

        return switch (flow) {
            case FLOW_CLIENT_CREDENTIALS -> testAdminToken(attrs, "Client Credentials");
            case FLOW_PASSWORD_GRANT     -> testAdminToken(attrs, "Password Grant");
            case FLOW_AUTH_CODE          -> testAuthCode(attrs);
            case FLOW_AUTH_CODE_QUICK    -> testAuthCodeQuick(attrs);
            default -> {
                LOGGER.warn("Unsupported flow requested for testConnection: {}", flow);
                yield failureResponse("Flow '" + flow + "' is not supported by this extension.");
            }
        };
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Tests CLIENT_CREDENTIALS or PASSWORD_GRANT by requesting an admin token.
     * The SDK's HC-006 routing matrix selects the correct strategy automatically.
     *
     * <p>Rule 3 guard: MustAuthorizeException is explicitly rethrown before the broader
     * catch so the platform can handle consent prompts correctly. For operator-flow tests
     * (CC / PG), MAE should never be thrown — but if it is, the platform must see it.</p>
     */
    private String testAdminToken(SalesforceCustomerServiceAttributes attrs, String flowLabel) {
        try {
            providerFactory.create(attrs).getSalesforceTokenForAdmin();
            LOGGER.debug("{} test connection succeeded", flowLabel);
            return successResponse();
        } catch (app.krista.extension.authorization.MustAuthorizeException mae) {
            // Rule 3: MUST rethrow BEFORE any broader catch. Never wrap or swallow MAE.
            throw mae;
        } catch (Exception e) {
            LOGGER.error("{} test connection failed: {}", flowLabel, e.getMessage(), e);
            return failureResponse(e.getMessage());
        }
    }

    /**
     * Implements the AUTH_CODE test path.
     *
     * <h3>Step 0 — Secret resolution (CRIT-002 fix)</h3>
     * <p>The Authentication tab blanks out secret fields in its UI (clientSecret is returned as
     * "" by {@code GET /getCredentials}). On the re-test call (after popup closes), the payload
     * therefore contains {@code clientSecret=null}. Without this resolution step the SDK would
     * build a token-store with {@code clientSecret=null} — producing a KV lookup key like
     * {@code accountId&clientId+null} that can never match the key the callback wrote under
     * the real secret. The fix mirrors the previous version's {@code resolveSecret()} pattern:
     * if the incoming attrs have a null secret, load the previously-stored attrs and splice in
     * the real secret before doing anything else. This MUST run before step 1 (persistence) so
     * the KV entry written to in step 1 always has the real secret.</p>
     *
     * <h3>Step 1 — Persist first (CRIT-001 deliberate exception)</h3>
     * <p>Credentials are written to KV under {@code invokerId} before any redirect. The
     * {@code /callback} endpoint runs in a separate HTTP request and has no in-memory
     * access to the credentials from this request. Writing here is the only way to make
     * them available there without passing secrets through the OAuth {@code state} parameter
     * (which would leak them to logs and browser history).</p>
     *
     * <h3>Step 2 — Probe existing refresh token</h3>
     * <p>Calls {@link SalesforceOAuth2Sdk#acquireForUser(String)} with the current
     * user's account ID. If a valid refresh token exists in KV for this user + credential
     * pair, the SDK exchanges it for a new access token. Success means no popup is needed.</p>
     *
     * <h3>Step 3 — Return authorize URL for popup</h3>
     * <p>If no refresh token exists or the refresh fails, builds the Salesforce authorize
     * URL and returns it in the response body so the JS layer can open it in a popup.</p>
     */
    private String testAuthCode(SalesforceCustomerServiceAttributes attrs) {
        // Validate minimum required fields (check clientId first — always present)
        if (attrs.getClientId() == null || attrs.getClientId().isBlank()) {
            return failureResponse("Consumer Key is required for Authorization Code flow.");
        }

        // Option C (Consumer Email keying): User Sign-In now keys its setup-verification token
        // by the entered Consumer Email instead of the invokerId. Capture it BEFORE the
        // secret-resolution block below (mergeSecret rebuilds attrs without the email field).
        String consumerEmail = attrs.getEmail();
        if (consumerEmail == null || consumerEmail.isBlank()) {
            return failureResponse("Please enter the Consumer Email before testing User Sign-In.");
        }

        // ── Step 0: Secret resolution (CRIT-002) ─────────────────────────────────
        // The Authentication tab returns clientSecret="" from GET /getCredentials so the
        // re-test call arrives with clientSecret=null. If we build the SDK with a null
        // secret, the token-store key becomes "accountId&clientId+null" which NEVER matches
        // the key written by /callback (which used the real secret). The fix: when the
        // incoming secret is null/blank, load the previously-saved attrs from KV and
        // merge the real secret in before any further processing.
        //
        // This is IDENTICAL in principle to the previous version's resolveSecret() pattern
        // in AuthenticationResource.testAuthorizationCode:
        //   String resolvedClientSecret = resolveSecret(req.clientSecret,
        //                                     stored != null ? stored.getClientSecret() : null);
        // CRIT-002: also treat the masked placeholder "••••••••" as "use stored value" — the
        // UI sends this back verbatim when the operator hasn't typed a new secret (they just
        // want to re-verify the existing config). Mirrors the previous React version's contract.
        if (attrs.getClientSecret() == null || attrs.getClientSecret().isBlank()
                || app.krista.extensions.crm.customer_relations.salesforce_customer_service.api.AuthenticationResource.STORED_SECRET_MASK.equals(attrs.getClientSecret())) {
            SalesforceCustomerServiceAttributes stored = attributesStore.loadByInvokerId(invokerId);
            String resolvedSecret = stored != null ? stored.getClientSecret() : null;
            System.out.println("[SF-DIAG] testAuthCode: clientSecret was null/blank in payload, " +
                    "resolved from KV store: secretLen=" +
                    (resolvedSecret != null ? resolvedSecret.length() : -1) +
                    " invokerId=" + invokerId);
            if (resolvedSecret == null || resolvedSecret.isBlank()) {
                return failureResponse("Consumer Secret is required for Authorization Code flow.");
            }
            attrs = mergeSecret(attrs, resolvedSecret);
        }

        // Option C (Consumer Email keying): the OAuth state — which the SDK uses to derive the
        // setup-verification RT storage key — is the entered Consumer Email, NOT the invokerId.
        // This makes the User Sign-In setup token per-consumer (sfv1/{email}/{hash(clientId+secret)}),
        // mirroring Quick Setup, and makes the state start with the email like the Outlook reference.
        // It is verification-only: runtime per-user calls key by wsContact (acquireForUser) and the
        // operator path uses acquireForAdmin, so neither is affected by this change.
        // (Previously this was `invokerId` — stable/opaque but not per-consumer and not email-scoped.)
        String state = consumerEmail;
        String accountId = resolveAccountId();  // retained for diagnostic context only
        System.out.println("[SF-DIAG] testAuthCode: state=" + state +
                " (consumer-email-scoped; observed accountId=" + accountId + ") clientId=" + attrs.getClientId() +
                " secretLen=" + (attrs.getClientSecret() != null ? attrs.getClientSecret().length() : -1));

        // Step 1: Persist credentials so /callback can load them.
        // If clientId or clientSecret changed, clean up any stale refresh tokens first.
        try {
            evictStaleTokenIfCredentialRotated(attrs);
            attributesStore.save(attrs, invokerId, FLOW_AUTH_CODE);
            LOGGER.debug("AUTH_CODE: persisted credentials for invokerId={}", invokerId);
        } catch (Exception e) {
            LOGGER.error("AUTH_CODE: failed to persist credentials for invokerId={}: {}", invokerId, e.getMessage(), e);
            return failureResponse("Failed to prepare credentials for authorization: " + e.getMessage());
        }

        // Step 2: Resolve the state value used SYMMETRICALLY for both RT lookup and
        // the authorize URL. The RT is stored under a key derived from the state Salesforce
        // echoes back to /callback — so the test-side lookup MUST use the same value the
        // URL-build side will send. Using accountId only for lookup but falling back to
        // "admin" for the URL would mean the second testConnection (after popup completes)
        // never finds the RT and returns the popup URL forever.
        //
        // NOTE ON VIOLATION A / RULE 1:
        // The `state` used here is `invokerId` — NOT the admin's wsContact_<id>. This is the
        // SETUP-VERIFICATION RT (doc §Violation E, Option B degradation): when the admin's real
        // wsContact_id is not available from the iframe-served AuthorizationContext (CRIT-004
        // scenario), we use invokerId so the RT key is at least stable and invoker-scoped.
        // This RT is exclusively for verifying the Connected App configuration. Per-user
        // runtime requests use wsContact_<id> as the accountId via getSalesforceToken(true, ...)
        // and have their own per-user RT lifecycle. The two keyspaces are disjoint because:
        //   - Test Connection RT key: invokerId&clientId+secret   (operator setup verification)
        //   - Per-user runtime RT key: wsContact_X&clientId+secret (per-user runtime)
        // See also: SalesforceTokenProvider.getSalesforceToken() which enforces Rule 2 by
        // resolving from AuthorizationContext only when invokeAsUser=true, never from invokerId.
        System.out.println("[SF-DIAG] testAuthCode: calling sdk.acquireForUser(" + state + ") [setup-verify RT]");
        try {
            SalesforceOAuth2Sdk sdk = sdkFactory.newSdk(attrs);
            sdk.acquireForUser(state);
            LOGGER.info("AUTH_CODE test: existing refresh token is valid for invokerId={} state={}",
                    invokerId, state);
            System.out.println("[SF-DIAG] testAuthCode: acquireForUser succeeded → returning success");
            return successResponse();
        } catch (app.krista.extension.authorization.MustAuthorizeException mae) {
            // Rule 3: rethrow MAE before the broader catch, even in the test-connection probe.
            // MAE at this point means the SDK itself triggered a consent requirement (rare but
            // possible if the underlying strategy does so on first access). The UI must see it.
            throw mae;
        } catch (Exception e) {
            // No RT or refresh failed — fall through to return the authorize URL.
            // This is the expected path on first-time AUTH_CODE setup.
            System.out.println("[SF-DIAG] testAuthCode: acquireForUser threw: " +
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            LOGGER.debug("AUTH_CODE test: refresh attempt with state={} failed (expected on first auth or rotation): {}",
                    state, e.getMessage());
        }

        // Step 3: No valid refresh token — return the authorize URL for the popup.
        try {
            SalesforceOAuth2Sdk sdk = sdkFactory.newSdk(attrs);
            String authorizeUrl = withPromptLogin(sdk.buildAuthorizationUrl(state, null));
            LOGGER.info("AUTH_CODE test: returning authorize URL (prompt=login) for popup (invokerId={} state={})",
                    invokerId, state);
            System.out.println("[SF-DIAG] testAuthCode: returning authorizeUrl for popup, state=" + state);
            return urlNeededResponse(authorizeUrl);
        } catch (Exception e) {
            LOGGER.error("AUTH_CODE test: failed to build authorization URL for invokerId={}: {}",
                    invokerId, e.getMessage(), e);
            return failureResponse("Failed to build authorization URL: " + e.getMessage());
        }
    }

    /**
     * Implements the AUTH_CODE_QUICK (Quick Setup) test path.
     *
     * <p>Quick Setup uses Krista's own Salesforce Connected App credentials from
     * {@code config.properties}. The operator payload is not consulted for clientId/secret —
     * those come from the baked-in credentials. The only operator-supplied field is
     * {@code sandbox} (whether to use {@code test.salesforce.com}).</p>
     *
     * <p>The test path is identical to {@link #testAuthCode(SalesforceCustomerServiceAttributes)}
     * except:</p>
     * <ol>
     *   <li>Credentials are always from {@code config.properties} — never from the operator payload</li>
     *   <li>The callback URL used is the central platform callback
     *       ({@link Constants#DEFAULT_CALLBACK_PATH}) rather than the per-invoker one</li>
     *   <li>The state is still {@code invokerId} for consistency with the AUTH_CODE test path</li>
     *   <li>On save, {@code savedFlow=AUTH_CODE_QUICK} is persisted but clientId/secret are NOT
     *       stored in KV — they are always read from {@code config.properties} at runtime</li>
     * </ol>
     *
     * <p>{@code [SF-DIAG]} output is included for the Quick Setup path to aid diagnosis in the
     * production log stream.</p>
     *
     * @param attrs the attrs from the payload (only {@code sandbox} field is used)
     * @return JSON response — {@code isSuccess=true}, {@code url=...} for popup, or failure
     */
    private String testAuthCodeQuick(SalesforceCustomerServiceAttributes attrs) {
        // CRIT-022: Quick Setup now requires an admin email so the resulting RT can be
        // keyed by a stable per-admin identity instead of by invokerId (which would
        // silently overwrite on the next admin's Test Connection). Without an email
        // there is no way to attribute who authorized the connection.
        String adminEmail = attrs != null ? attrs.getEmail() : null;
        if (adminEmail == null || adminEmail.isBlank()) {
            LOGGER.warn("AUTH_CODE_QUICK: missing required `email` field for invokerId={}", invokerId);
            return failureResponse("Please enter the Salesforce User Email before testing the Quick Setup connection.");
        }
        boolean isSandbox = attrs != null && attrs.isSandbox();
        System.out.println("[SF-DIAG] testAuthCodeQuick: building Public-app attrs for Quick Setup " +
                "sandbox=" + isSandbox + " email=" + adminEmail + " invokerId=" + invokerId);

        // Build a Public-app attrs instance from config.properties + the admin email.
        // The operator payload is used only for the sandbox flag and email; clientId
        // and clientSecret come from the bundled config.properties.
        SalesforceCustomerServiceAttributes publicAttrs;
        try {
            publicAttrs = SalesforceCustomerServiceAttributes.createPublic(invoker, isSandbox, adminEmail);
        } catch (Exception e) {
            LOGGER.error("AUTH_CODE_QUICK: failed to load Public app credentials: {}", e.getMessage(), e);
            return failureResponse("Quick Setup credentials could not be loaded: " + e.getMessage());
        }

        // Persist the Public-app attrs under invokerId. The saved attrs carry the email
        // so that operator-context calls at runtime can re-derive the right token key
        // (`sfv1/{email}/{hash}`) without needing to re-prompt for the email.
        // NOTE: clientId/secret are stored in KV deliberately for the Quick Setup
        // test-then-save contract (the /callback endpoint needs to load attrs to do the
        // code exchange). These are Krista's own credentials, not the customer's.
        try {
            attributesStore.save(publicAttrs, invokerId, FLOW_AUTH_CODE_QUICK);
            LOGGER.debug("AUTH_CODE_QUICK: persisted Public-app attrs for invokerId={} email={}",
                    invokerId, adminEmail);
        } catch (Exception e) {
            LOGGER.error("AUTH_CODE_QUICK: failed to persist Public-app attrs: {}", e.getMessage(), e);
            return failureResponse("Failed to prepare Quick Setup credentials: " + e.getMessage());
        }

        // CRIT-022: state is the admin email, not invokerId. The RT will be stored under
        // `sfv1/{email}/{hash(publicClientId+publicSecret)}` — hermetic from the per-user
        // `wsContact_<id>` runtime keys (different prefix), and stable per admin (multiple
        // admins each get their own RT).
        String state = adminEmail;

        // Step 2: probe existing RT for this admin's email.
        System.out.println("[SF-DIAG] testAuthCodeQuick: probing existing RT via acquireForUser(state=" + state + ")");
        try {
            SalesforceOAuth2Sdk sdk = sdkFactory.newSdk(publicAttrs);
            OAuth2AccessContext ctx = sdk.acquireForUser(state);
            // A stored RT that refreshes proves ONLY that *some* prior authorization is still
            // valid. The email (`state`) is merely the KV lookup key — it is never sent to
            // Salesforce during the refresh and never validated against the identity that
            // actually owns the token. So a leftover RT minted for a DIFFERENT Salesforce user
            // (e.g. the browser was signed into another account when the popup completed, or a
            // typo'd email happens to have an old token) would otherwise pass Test Connection.
            // Verify the token's real identity matches the entered email before succeeding.
            IdentityCheck idc = verifyEnteredIdentity(ctx, adminEmail);
            return switch (idc.status()) {
                case MATCH -> {
                    LOGGER.info("AUTH_CODE_QUICK test: refresh token valid AND identity '{}' matches email={} invokerId={}",
                            idc.actual(), adminEmail, invokerId);
                    System.out.println("[SF-DIAG] testAuthCodeQuick: identity verified (" + idc.actual() + ") → isSuccess=true");
                    yield successResponse();
                }
                case MISMATCH -> {
                    LOGGER.warn("AUTH_CODE_QUICK test: stored token belongs to '{}' but entered email is '{}' — "
                            + "rejecting and clearing the stale token (invokerId={})", idc.actual(), adminEmail, invokerId);
                    System.out.println("[SF-DIAG] testAuthCodeQuick: identity MISMATCH stored=" + idc.actual()
                            + " entered=" + adminEmail + " → clearing RT, isSuccess=false");
                    try {
                        sdk.invalidate(state);
                    } catch (Exception ie) {
                        LOGGER.debug("AUTH_CODE_QUICK: failed to invalidate stale RT for state={}: {}", state, ie.getMessage());
                    }
                    yield failureResponse("This Salesforce authorization belongs to " + idc.actual()
                            + ", not " + adminEmail + ". The stale authorization has been cleared — "
                            + "click Test Connection again and sign in to Salesforce as " + adminEmail
                            + " when prompted.");
                }
                case UNVERIFIABLE -> {
                    // Refresh succeeded but the token's identity could not be read (e.g. userinfo
                    // needs the openid scope and Chatter is disabled). Preserve the working
                    // connection rather than introduce a false failure — but log loudly.
                    LOGGER.warn("AUTH_CODE_QUICK test: refresh succeeded but identity could NOT be verified "
                            + "for email={} invokerId={} ({}). Treating as success.", adminEmail, invokerId, idc.detail());
                    System.out.println("[SF-DIAG] testAuthCodeQuick: identity UNVERIFIABLE (" + idc.detail() + ") → soft-pass isSuccess=true");
                    yield successResponse();
                }
            };
        } catch (app.krista.extension.authorization.MustAuthorizeException mae) {
            throw mae;
        } catch (Exception e) {
            System.out.println("[SF-DIAG] testAuthCodeQuick: acquireForUser threw (expected on first auth): " +
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            LOGGER.debug("AUTH_CODE_QUICK: no existing RT for state={} — will return authorize URL", state);
        }

        // Step 3: return the authorize URL using the Public app credentials and central callback.
        // CRIT-016: For Public flows, the OAuth `state` MUST encode the per-extension forward
        // URL after a '#' separator. The Krista platform's central callback handler at
        // /extension/api/rest/v3/oauth/callback uses the segment of `state` after the last '#'
        // to know which extension instance to HTTP-forward the OAuth `code` to.
        //
        // Mirrors outlook-4's pattern in OutlookTestConnectionServiceImpl.createStateParameter()
        // (line 162):
        //     if (Constants.PUBLIC.equals(attrs.getAuthType())) {
        //         state += Constants.HASH + attrs.getForwardPath();
        //     }
        //
        // Without this, the central handler receives a state like "INV_xxx" with no '#' and
        // doesn't know where to forward → 502 Bad Gateway returned to the OAuth popup.
        String forwardPath = publicAttrs.getForwardPath();
        String publicState = state + Constants.HASH + forwardPath;
        try {
            SalesforceOAuth2Sdk sdk = sdkFactory.newSdk(publicAttrs);
            String authorizeUrl = withPromptLogin(sdk.buildAuthorizationUrl(publicState, null));
            LOGGER.info("AUTH_CODE_QUICK: returning authorize URL (prompt=login) for Quick Setup popup, state={}", publicState);
            System.out.println("[SF-DIAG] testAuthCodeQuick: returning authorizeUrl, state=" + publicState);
            return urlNeededResponse(authorizeUrl);
        } catch (Exception e) {
            LOGGER.error("AUTH_CODE_QUICK: failed to build authorize URL: {}", e.getMessage(), e);
            return failureResponse("Failed to build Quick Setup authorization URL: " + e.getMessage());
        }
    }

    /**
     * Produces a copy of {@code attrs} that has the same values as the original except
     * {@code clientSecret} is replaced by {@code resolvedSecret}.
     *
     * <p>This is the CRIT-002 secret-resolution helper — mirrors the previous version's
     * {@code resolveSecret()} contract: non-null secret in the SPA payload wins; null
     * falls back to the stored secret.</p>
     */
    private SalesforceCustomerServiceAttributes mergeSecret(
            SalesforceCustomerServiceAttributes attrs, String resolvedSecret) {
        // SalesforceCustomerServiceAttributes is mutable (has setters via update()), but
        // we cannot call update() here because it wipes all other fields from a map.
        // The cleanest approach is the all-args constructor which the store's fromMap()
        // also uses. Construct a fresh instance with the resolved secret.
        SalesforceCustomerServiceAttributes merged = new SalesforceCustomerServiceAttributes(
                attrs.getClientId(),
                resolvedSecret,
                attrs.getInstanceUrl(),
                attrs.getUserName(),
                attrs.getPassword(),
                attrs.getSecurityToken(),
                attrs.isSandbox(),
                // getCallbackUrl() = routingUrl + CALLBACK_URI; we need just routingUrl.
                // Derive it by stripping the CALLBACK_URI suffix from getCallbackUrl().
                stripCallbackSuffix(attrs.getCallbackUrl()));
        return merged;
    }

    /**
     * Strips the fixed CALLBACK_URI suffix from a fully-qualified callback URL to recover
     * the routing base URL needed by the {@link SalesforceCustomerServiceAttributes} constructor.
     */
    private static String stripCallbackSuffix(String callbackUrl) {
        if (callbackUrl != null && callbackUrl.endsWith(
                app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.CALLBACK_URI)) {
            return callbackUrl.substring(0, callbackUrl.length() -
                    app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.CALLBACK_URI.length());
        }
        return callbackUrl;
    }

    /**
     * Returns the current user's account ID from the {@link AuthorizationContext}, or
     * {@code null} if there is no authenticated session (e.g. admin-only context).
     */
    private String resolveAccountId() {
        try {
            if (authorizationContext.isAuthenticated()) {
                return authorizationContext.getAuthorizedAccount().getAccountId();
            }
        } catch (Exception e) {
            LOGGER.debug("AUTH_CODE: could not resolve account ID from AuthorizationContext: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Checks whether the incoming credentials differ from what was previously stored for
     * this {@code invokerId}. If the Connected App credentials changed (clientId or
     * clientSecret), the SDK's token store adapter translates keys using the old pair —
     * meaning existing tokens would silently be looked up under an unreachable key.
     *
     * <p>We do NOT attempt to delete the old KV entries here because the legacy key format
     * embeds the accountId which is user-specific and not available at this point. The
     * SDK's runtime {@code InvalidGrantException} handling triggers re-authorization
     * gracefully when a stale token is encountered. Logging at INFO gives observability.</p>
     */
    private void evictStaleTokenIfCredentialRotated(SalesforceCustomerServiceAttributes newAttrs) {
        SalesforceCustomerServiceAttributes old = attributesStore.loadByInvokerId(invokerId);
        if (old == null) {
            return; // first-time setup — nothing to evict
        }
        boolean clientIdChanged     = !equal(old.getClientId(),     newAttrs.getClientId());
        boolean clientSecretChanged = !equal(old.getClientSecret(), newAttrs.getClientSecret());
        if (clientIdChanged || clientSecretChanged) {
            LOGGER.info("AUTH_CODE testConnection: Connected App credentials changed for invokerId={} "
                    + "(clientId changed={}, secret changed={}). Existing per-user refresh tokens at "
                    + "the old credential key will be invalidated at next user request via "
                    + "InvalidGrantException → MustAuthorizeException.",
                    invokerId, clientIdChanged, clientSecretChanged);
        }
    }

    private static boolean equal(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // -------------------------------------------------------------------------
    // Identity verification (Quick Setup) — confirm the refreshed token actually
    // belongs to the entered email, not just that *some* stored RT still refreshes.
    // -------------------------------------------------------------------------

    private enum IdentityStatus { MATCH, MISMATCH, UNVERIFIABLE }

    /**
     * Outcome of comparing the entered email against the token's real Salesforce identity.
     *
     * @param status  MATCH / MISMATCH / UNVERIFIABLE
     * @param actual  the resolved identity (email or username) when known; null otherwise
     * @param detail  diagnostic reason when UNVERIFIABLE; null otherwise
     */
    private record IdentityCheck(IdentityStatus status, String actual, String detail) {
    }

    /**
     * Verifies that the Salesforce identity behind {@code ctx}'s access token matches the
     * {@code expectedEmail} the operator entered.
     *
     * <p>Resolves the real identity via the OIDC {@code /services/oauth2/userinfo} endpoint
     * (present when the {@code openid} scope was granted), falling back to the Chatter
     * {@code /services/data/vXX.0/chatter/users/me} resource (available with the {@code api}
     * scope this extension already requires). Never throws — on any error it returns
     * {@link IdentityStatus#UNVERIFIABLE} so the caller can decide the policy.</p>
     */
    private IdentityCheck verifyEnteredIdentity(OAuth2AccessContext ctx, String expectedEmail) {
        if (ctx == null || ctx.accessToken() == null || ctx.accessToken().isBlank()) {
            return new IdentityCheck(IdentityStatus.UNVERIFIABLE, null, "no access token in context");
        }
        String instanceUrl = ctx.providerMetadata() != null ? ctx.providerMetadata().get("instanceUrl") : null;
        if (instanceUrl == null || instanceUrl.isBlank()) {
            return new IdentityCheck(IdentityStatus.UNVERIFIABLE, null, "no instanceUrl in context");
        }
        instanceUrl = instanceUrl.replaceAll("/+$", "");

        String[] identity = fetchIdentity(instanceUrl, ctx.accessToken()); // {email, username} or null
        if (identity == null) {
            return new IdentityCheck(IdentityStatus.UNVERIFIABLE, null,
                    "userinfo and chatter/users/me both unavailable");
        }
        String email = identity[0];
        String username = identity[1];
        String actual = (email != null && !email.isBlank()) ? email : username;
        if (matchesEntered(expectedEmail, email, username)) {
            return new IdentityCheck(IdentityStatus.MATCH, actual, null);
        }
        return new IdentityCheck(IdentityStatus.MISMATCH, actual, null);
    }

    /**
     * Case-insensitive match of the entered email against the token's email/username, with
     * tolerance for the sandbox username suffix (prod {@code user@org.com} becomes
     * {@code user@org.com.sandboxName} in a sandbox).
     */
    private static boolean matchesEntered(String entered, String email, String username) {
        if (entered == null) return false;
        String e = entered.trim().toLowerCase(Locale.ROOT);
        if (e.isEmpty()) return false;
        for (String candidate : new String[]{email, username}) {
            if (candidate == null) continue;
            String c = candidate.trim().toLowerCase(Locale.ROOT);
            if (c.isEmpty()) continue;
            if (c.equals(e)) return true;
            if (c.startsWith(e + ".")) return true; // sandbox suffix on username
        }
        return false;
    }

    /**
     * Fetches the current user's {email, username} from Salesforce using the access token.
     * Tries the OIDC userinfo endpoint first, then Chatter {@code users/me}. Returns null if
     * neither yields an identity.
     */
    private static String[] fetchIdentity(String instanceUrl, String accessToken) {
        JsonObject userinfo = httpGetJson(instanceUrl + "/services/oauth2/userinfo", accessToken);
        if (userinfo != null) {
            String email = optString(userinfo, "email");
            String username = optString(userinfo, "preferred_username");
            if (notBlank(email) || notBlank(username)) {
                return new String[]{email, username};
            }
        }
        JsonObject me = httpGetJson(instanceUrl + "/services/data/v58.0/chatter/users/me", accessToken);
        if (me != null) {
            String email = optString(me, "email");
            String username = optString(me, "username");
            if (notBlank(email) || notBlank(username)) {
                return new String[]{email, username};
            }
        }
        return null;
    }

    /**
     * Performs an authenticated GET and parses a JSON object body, or returns null on any
     * non-2xx status or error. Never throws.
     */
    private static JsonObject httpGetJson(String url, String accessToken) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 == 2) {
                return JsonParser.parseString(resp.body()).getAsJsonObject();
            }
            System.out.println("[SF-DIAG] identity probe: GET " + url + " → HTTP " + resp.statusCode());
            LOGGER.debug("Identity probe GET {} returned HTTP {}", url, resp.statusCode());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Identity probe GET {} interrupted: {}", url, ie.getMessage());
        } catch (Exception e) {
            System.out.println("[SF-DIAG] identity probe: GET " + url + " threw "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            LOGGER.debug("Identity probe GET {} failed: {}", url, e.getMessage());
        }
        return null;
    }

    private static String optString(JsonObject o, String field) {
        try {
            return o.has(field) && !o.get(field).isJsonNull() ? o.get(field).getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Appends {@code prompt=login} to a Salesforce authorize URL so the OAuth popup always
     * forces fresh credential entry instead of silently reusing whatever Salesforce session
     * the browser already has. Without it, an operator who types one email but whose browser
     * is signed into Salesforce as a different user would authorize as that other user — which
     * the identity check then rejects, leaving no way to actually sign in as the entered email.
     *
     * <p>Idempotent: no-op if a {@code prompt=} parameter is already present.</p>
     */
    private static String withPromptLogin(String authorizeUrl) {
        if (authorizeUrl == null || authorizeUrl.isBlank()) return authorizeUrl;
        if (authorizeUrl.matches(".*[?&]prompt=.*")) return authorizeUrl;
        return authorizeUrl + (authorizeUrl.contains("?") ? "&" : "?") + "prompt=login";
    }

    // -------------------------------------------------------------------------
    // Response factories
    // -------------------------------------------------------------------------

    private static String successResponse() {
        return Constants.GSON.toJson(new TestConnectionResponse(true, null, null));
    }

    private static String failureResponse(String message) {
        return Constants.GSON.toJson(new TestConnectionResponse(false, message, null));
    }

    /**
     * Response indicating the user must authorize via a popup at the given URL.
     * The JS layer detects the presence of the {@code url} field (regardless of
     * {@code isSuccess}) and opens a popup.
     */
    private static String urlNeededResponse(String url) {
        return Constants.GSON.toJson(new TestConnectionResponse(false, null, url));
    }

    /**
     * Response shape expected by the Authentication tab's {@code doTest()} function.
     *
     * <p>Field semantics:</p>
     * <ul>
     *   <li>{@code isSuccess} — {@code true} when the connection is confirmed working.</li>
     *   <li>{@code errorMessage} — human-readable error text; omitted (null → Gson skips it)
     *       on success.</li>
     *   <li>{@code url} — Salesforce authorize URL; present only when a popup is needed.
     *       Null fields are omitted from Gson output (Gson default, no serializeNulls).</li>
     * </ul>
     */
    @SuppressWarnings("unused") // fields read by Gson serialization
    private record TestConnectionResponse(boolean isSuccess, String errorMessage, String url) {
    }
}
