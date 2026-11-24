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

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.oauth.core.token.ITokenStore;
import app.krista.oauth.providers.salesforce.SalesforceOAuth2Sdk;
import app.krista.oauth.providers.salesforce.extension.AbstractSalesforceRequestAuthenticator;
import app.krista.oauth.providers.salesforce.extension.SalesforceAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authenticator for handling Salesforce Customer Service OAuth 2.0 authentication in a
 * Krista extension.
 *
 * <p>As of salesforce_customer_service 2.0.18 (microsoft-auth-sdk 1.1.0 migration, T-M6),
 * this class extends {@link AbstractSalesforceRequestAuthenticator} instead of implementing
 * {@link app.krista.extension.authorization.RequestAuthenticator} directly. The base class
 * provides:</p>
 * <ul>
 *   <li>The full {@link #getMustAuthorizeResponse} implementation, which builds the OAuth
 *       authorize URL through {@link SalesforceOAuth2Sdk} rather than the legacy hand-rolled
 *       ScribeJava {@code OAuth20Service.getAuthorizationUrl} call.</li>
 *   <li>State grammar parsing with the 8-part DoS cap (CTO C5 / HC-005).</li>
 *   <li>The standard no-op implementations of {@link #getScheme()},
 *       {@link #getSupportedProtocols()}, {@link #setServiceAuthorization(String)},
 *       {@link #getAttributeFields()}, and the ProtoRequest-scoped methods.</li>
 * </ul>
 *
 * <p>This is the first chunk to route the OAuth authorize-URL production path through the
 * SDK. The token exchange (callback handling) is covered in Chunk 4 when
 * {@code AuthenticationResource} is refactored.</p>
 *
 * <h2>Multi-tenant support</h2>
 *
 * <p>When a {@code MustAuthorizeException} carries an {@code authContextId} detail field,
 * the base class calls {@link #loadAttributes(String)} with that ID, obtaining per-request
 * attributes (potentially different clientId / clientSecret than the operator defaults).
 * {@link #newSdk(SalesforceAttributes, ITokenStore)} is then overridden to build a fresh
 * {@link SalesforceOAuth2Sdk} via {@link SalesforceCustomerServiceSdkFactory}, which
 * creates a {@link LegacyKeyedSalesforceTokenStore} bound to those exact credentials —
 * preserving per-tenant token isolation.</p>
 *
 * <h2>PKCE</h2>
 *
 * <p>PKCE is disabled via the {@link SalesforceCustomerServiceAttributes#toSalesforceConfig()}
 * override (T-M5). The base class checks {@code config.isUsePkce()} before generating a
 * verifier, so {@link #persistPkceVerifier(String, String)} is implemented as a no-op with
 * a comment. If PKCE is enabled in a future migration step, the implementation here must
 * delegate to {@code attributesStore.updateWithVerifier(authContextId, verifier)} or an
 * equivalent.</p>
 *
 * <h2>Thread-safety</h2>
 *
 * <p>Instances are constructed per-request by HK2 and are NOT shared across threads. All
 * fields are effectively final after construction (assigned in constructor, never mutated).
 * The {@link SalesforceCustomerServiceSdkFactory} is stateless and safe for concurrent use.</p>
 *
 * @since salesforce_customer_service 2.0.18 (microsoft-auth-sdk 1.1.0 migration)
 */
public class SalesforceCustomerServiceAuthenticator extends AbstractSalesforceRequestAuthenticator {

    private final SalesforceCustomerServiceAttributes defaultAttributes;
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final SalesforceCustomerServiceSdkFactory sdkFactory;
    /** Cached so {@link #isSavedFlowQuickSetup()} can read the persisted savedFlow at call time. */
    private final String invokerIdForSavedFlowLookup;

    /**
     * Production constructor — all dependencies resolved by HK2 at injection time.
     *
     * <p>The {@code invokerId} and {@code routingUrl} are extracted from the {@link Invoker}
     * here and passed to the base class, which needs them to build the state parameter and
     * generate the callback URL on first-time / test-connection paths.</p>
     *
     * @param invoker           Krista Invoker (supplies invokerId and routingUrl at runtime)
     * @param defaultAttributes operator-default Salesforce attributes (injected as {@code @Service})
     * @param attributesStore   per-request attribute store (keyed by authContextId)
     * @param sdkFactory        factory that builds a per-request {@link SalesforceOAuth2Sdk}
     *                          backed by a {@link LegacyKeyedSalesforceTokenStore}
     */
    public SalesforceCustomerServiceAuthenticator(Invoker invoker,
                                                  SalesforceCustomerServiceAttributes defaultAttributes,
                                                  SalesforceCustomerServiceAttributesStore attributesStore,
                                                  SalesforceCustomerServiceSdkFactory sdkFactory) {
        super(invoker.getInvokerId(),
              invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE),
              Constants.CALLBACK_PATH_PREFIX);
        this.defaultAttributes = defaultAttributes;
        this.attributesStore = attributesStore;
        this.sdkFactory = sdkFactory;
        this.invokerIdForSavedFlowLookup = invoker.getInvokerId();
    }

    /**
     * Test / seam constructor — accepts pre-resolved invokerId and routingUrl.
     *
     * <p>Avoids the need to provide a live {@link Invoker} in unit tests. Also used by
     * {@link SalesforceCustomerServiceExtension}'s test-overloaded constructor.</p>
     *
     * @param invokerId         extension invoker ID
     * @param routingUrl        appliance routing URL
     * @param defaultAttributes operator-default Salesforce attributes
     * @param attributesStore   per-request attribute store
     * @param sdkFactory        per-request SDK factory
     */
    SalesforceCustomerServiceAuthenticator(String invokerId,
                                           String routingUrl,
                                           SalesforceCustomerServiceAttributes defaultAttributes,
                                           SalesforceCustomerServiceAttributesStore attributesStore,
                                           SalesforceCustomerServiceSdkFactory sdkFactory) {
        super(invokerId, routingUrl, Constants.CALLBACK_PATH_PREFIX);
        this.defaultAttributes = defaultAttributes;
        this.attributesStore = attributesStore;
        this.sdkFactory = sdkFactory;
        this.invokerIdForSavedFlowLookup = invokerId;
    }

    // ── AbstractSalesforceRequestAuthenticator hooks ─────────────────────────────

    /**
     * Load attributes by authContextId (or invokerId) from the attribute store.
     *
     * <h2>Quick Setup substitution (CRIT-019)</h2>
     *
     * <p>The base class calls {@code loadAttributes(invokerId)} for runtime per-user MAEs
     * BEFORE falling back to {@link #createDefaultAttributes(String)} — so the operator's
     * saved Quick Setup attrs (which exist in KV but carry {@code clientId=null}) are what
     * the base class picks up. Without substitution, the subsequent
     * {@code attributes.toSalesforceConfig()} explodes with {@code clientId is required}.</p>
     *
     * <p>Detection uses the durable {@code savedFlow="AUTH_CODE_QUICK"} signal — the
     * {@link SalesforceCustomerServiceAttributes#isPublicApp()} boolean sentinel does NOT
     * survive KV (de)serialization, so loaded attrs always come back with
     * {@code publicApp=false} even for Quick Setup. Reading {@code savedFlow} via
     * {@link SalesforceCustomerServiceAttributesStore#getSavedFlow(String)} returns the
     * persisted flow id reliably.</p>
     *
     * <p>When Quick Setup is detected, we replace the loaded attrs with a freshly-built
     * Public-app instance carrying Krista's bundled credentials (from
     * {@code config.properties}) plus the operator's saved sandbox preference. The routing
     * URL is recovered from the loaded attrs' callbackUrl (stripping the per-invoker
     * {@link Constants#CALLBACK_URI} suffix).</p>
     *
     * <p>For non-Quick saved flows (AUTH_CODE / CLIENT_CREDENTIALS / PASSWORD_GRANT) the
     * loaded attrs are returned unchanged.</p>
     *
     * @param key authContextId saved by {@code AuthenticationResource} before the OAuth
     *            redirect, or the invokerId for the operator-default slot
     * @return the loaded attributes (substituted with Public attrs for Quick Setup), or
     *         {@code null} if not stored
     */
    @Override
    protected SalesforceAttributes loadAttributes(String key) {
        try {
            SalesforceCustomerServiceAttributes loaded = attributesStore.load(key);
            // CRIT-023 — also substitute Public for the "nothing saved" case.
            //
            // When the catalog runs, SalesforceService creates a per-request token provider via
            // providerFactory.create(effectiveAttributes()) which saves the (Public-fallback)
            // attrs under a UUID authContextId. The resulting MAE carries that authContextId,
            // so the SDK base class hits the `if (authContextId.isPresent())` branch and calls
            // loadAttributes(authContextId) — bypassing createDefaultAttributes entirely.
            //
            // The KV round-trip drops the `publicApp` boolean sentinel (it's never serialized;
            // see SalesforceCustomerServiceAttributesStore.buildAttributesJson), so the loaded
            // attrs come back with publicApp=false even though clientId/secret are the Public
            // ones. Result: getCallbackEndPoint() returns the per-invoker URL → consent popup
            // hits a callback Salesforce was never told about → broken.
            //
            // Substituting Public attrs here forces publicApp=true on the freshly-constructed
            // instance, so getCallbackEndPoint() correctly returns the central URL.
            // CRIT-029 — loadAttributes is only invoked from getMustAuthorizeResponse,
            // which only fires for per-user MAEs (invokeAsUser=true). Per
            // PER_USER_AUTH_INVARIANTS.md Rule 2 the per-user path is hermetic from
            // operator-context config — ALWAYS build Public attrs here, regardless of
            // what flow the admin has saved. Keeps the per-user RT keyspace stable
            // (sfv1/wsContact_<id>/<hash(publicClientId+publicSecret)>) even when the
            // admin reconfigures the Authentication tab.
            if (loaded != null) {
                String routingUrl = loaded.getCallbackUrl().replaceAll(Constants.CALLBACK_URI + "$", "");
                boolean sandbox = loaded.isSandbox();
                return SalesforceCustomerServiceAttributes.createPublicFromRoutingUrl(routingUrl, sandbox);
            }
            return loaded;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Create default (operator-configured) attributes as a fallback for first-time /
     * test-connection paths where no per-request entry has been saved.
     *
     * <h2>Quick Setup substitution (CRIT-019)</h2>
     *
     * <p>When the operator has saved the Quick Setup flow ({@code savedFlow=AUTH_CODE_QUICK}),
     * the HK2-injected {@code defaultAttributes} carries {@code clientId=null} (Quick Setup
     * persists only the sandbox flag — the Krista-owned credentials live in
     * {@code config.properties}, not in KV). Returning those attrs unchanged would explode
     * inside {@link app.krista.oauth.providers.salesforce.config.SalesforceConfig.Builder#build}
     * with {@code clientId is required}. Instead, we detect Quick Setup via the durable
     * {@code savedFlow} signal and substitute a freshly-built Public-app attributes instance
     * carrying Krista's bundled credentials plus the operator's saved sandbox preference.</p>
     *
     * <p>The {@code routingUrl} parameter from the base class IS used in the Public path —
     * {@link SalesforceCustomerServiceAttributes#createPublicFromRoutingUrl(String, boolean)}
     * needs it to compute the central callback URL via
     * {@link SalesforceCustomerServiceAttributes#getCallbackEndPoint()}. For the non-Quick
     * (Advanced) path, the operator-default attrs already carry the correct routing URL from
     * their own injection-time construction, so the parameter is unused there.</p>
     *
     * @param routingUrl the appliance routing URL
     * @return Public-app attrs for Quick Setup, otherwise the operator-default attrs
     */
    @Override
    protected SalesforceAttributes createDefaultAttributes(String routingUrl) {
        // CRIT-023: also default to Public attrs when NOTHING has been saved in the
        // Authentication tab. The authenticator's createDefaultAttributes is only invoked
        // from getMustAuthorizeResponse — which only fires for invokeAsUser=true (the
        // per-user path threw a MAE). For a fresh install, this means the consent popup
        // would otherwise use the customer's never-configured per-invoker callback URL.
        // Defaulting to Public makes the popup go through Krista's bundled Connected App
        // at the central callback URL — the outlook-4 fresh-install behavior.
        // CRIT-029 — createDefaultAttributes (like loadAttributes) is only invoked from
        // getMustAuthorizeResponse, which only fires for per-user MAEs. Per the
        // per-user-hermeticity invariant, ALWAYS build Public attrs here regardless of
        // saved flow. Operator-context calls go through their own paths (Test Connection,
        // SalesforceTokenProvider.getSalesforceTokenForAdmin) and never reach this method.
        boolean sandbox = defaultAttributes != null && defaultAttributes.isSandbox();
        return SalesforceCustomerServiceAttributes.createPublicFromRoutingUrl(routingUrl, sandbox);
    }

    /**
     * Returns {@code true} when the operator's saved flow (persisted by
     * {@code SalesforceSaveConfigurationImpl#saveCredentials}) is {@code "AUTH_CODE_QUICK"}.
     *
     * <p>This mirrors {@code SalesforceTokenProvider.isSavedFlowQuickSetup()} — see that method
     * for the full rationale. In short: the {@code isPublicApp()} boolean sentinel on the
     * attrs object does NOT round-trip through KV, so we must consult the persisted
     * {@code savedFlow} string to detect Quick Setup reliably on HK2-injected attrs.</p>
     */
    private boolean isSavedFlowQuickSetup() {
        if (attributesStore == null || invokerIdForSavedFlowLookup == null) return false;
        try {
            return "AUTH_CODE_QUICK".equals(attributesStore.getSavedFlow(invokerIdForSavedFlowLookup));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} when no Authentication-tab configuration has been saved for
     * this invoker (CRIT-023). Used to decide that an unconfigured fresh-install per-user
     * MAE should consent through Krista's Public Connected App (central callback URL)
     * rather than through the customer's never-configured per-invoker callback.
     */
    private boolean isNothingSaved() {
        if (attributesStore == null || invokerIdForSavedFlowLookup == null) return false;
        try {
            return attributesStore.getSavedFlow(invokerIdForSavedFlowLookup) == null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Build the default token store for the operator-default attributes.
     *
     * <p>Called by the base class before {@link #newSdk(SalesforceAttributes, ITokenStore)}.
     * For multi-tenant flows the returned store is not actually used — {@link #newSdk} is
     * overridden to build a fresh store keyed to the per-request credentials. This method
     * therefore returns a token store bound to the default attributes' credentials, covering
     * the single-tenant (no authContextId) path.</p>
     *
     * @return a non-null {@link LegacyKeyedSalesforceTokenStore} for the default credentials
     */
    @Override
    protected ITokenStore getTokenStore() {
        // CRIT-029 — getTokenStore is only consulted by getMustAuthorizeResponse, which
        // only fires for per-user MAEs. Per the per-user-hermeticity invariant the token
        // store must be keyed by the Public clientId+secret so it matches what the
        // runtime catalog path (chooseEffectiveAttributes for invokeAsUser=true) uses.
        // Always build the store off Public attrs.
        String routingUrl = defaultAttributes != null
                ? defaultAttributes.getCallbackUrl().replaceAll(Constants.CALLBACK_URI + "$", "")
                : "";
        boolean sandbox = defaultAttributes != null && defaultAttributes.isSandbox();
        SalesforceCustomerServiceAttributes publicAttrs =
                SalesforceCustomerServiceAttributes.createPublicFromRoutingUrl(routingUrl, sandbox);
        return sdkFactory.buildTokenStore(publicAttrs);
    }

    /**
     * Build a {@link SalesforceOAuth2Sdk} wired to the resolved per-request attributes.
     *
     * <p>Overrides the base-class default to ensure the token store is bound to THIS
     * request's (clientId, clientSecret) pair. This is critical for multi-tenant flows
     * where different requests carry different OAuth app credentials via an
     * {@code authContextId}: a token store bound to the wrong credentials would look up
     * tokens at incorrect keys, causing spurious re-authorization prompts.</p>
     *
     * <p>The {@code tokenStore} argument passed by the base class (result of
     * {@link #getTokenStore()}) is deliberately ignored here — the factory creates its own
     * store from the resolved attributes, which is always correct.</p>
     *
     * @param attributes the resolved per-request or operator-default attributes
     * @param tokenStore the default token store from {@link #getTokenStore()} — ignored
     * @return a fully-wired {@link SalesforceOAuth2Sdk} for this request's credentials
     */
    @Override
    protected SalesforceOAuth2Sdk newSdk(SalesforceAttributes attributes, ITokenStore tokenStore) {
        return sdkFactory.newSdk((SalesforceCustomerServiceAttributes) attributes);
    }

    /**
     * Persist a PKCE verifier alongside the per-request attributes.
     *
     * <p><b>No-op implementation — PKCE is disabled for this extension.</b></p>
     *
     * <p>The {@link SalesforceCustomerServiceAttributes#toSalesforceConfig()} override sets
     * {@code usePkce(false)}, so the base class never calls this method (it only calls
     * {@code persistPkceVerifier} when {@code config.isUsePkce()} is {@code true}).</p>
     *
     * <p>If PKCE is enabled in a future migration step, this implementation MUST be updated
     * to persist the verifier so the callback can retrieve it for the token exchange. The
     * implementation would delegate to the attribute store saving the verifier alongside
     * the per-request attributes under the given {@code authContextId}.</p>
     *
     * @param authContextId the key to store the verifier under (unused while PKCE is off)
     * @param pkceVerifier  the RFC 7636 code verifier string (unused while PKCE is off)
     * @see SalesforceCustomerServiceAttributes#toSalesforceConfig()
     */
    @Override
    protected void persistPkceVerifier(String authContextId, String pkceVerifier) {
        // No-op: PKCE is disabled via SalesforceCustomerServiceAttributes.toSalesforceConfig()
        // (usePkce=false). The base class only calls this when config.isUsePkce() is true.
        // TODO (when PKCE enabled): attributesStore.updateWithVerifier(authContextId, pkceVerifier)
    }

    /**
     * Override of {@link AbstractSalesforceRequestAuthenticator#getMustAuthorizeResponse} to inject
     * the Krista central-callback forward path into the OAuth {@code state} parameter when
     * Quick Setup is configured (CRIT-020).
     *
     * <h2>Why this override exists</h2>
     *
     * <p>Quick Setup (Public App) flows go through the Krista platform's central callback
     * handler at {@link Constants#DEFAULT_CALLBACK_PATH} — Salesforce is registered with that
     * one URL across all tenants of Krista's Connected App. The central handler then forwards
     * the {@code code}+{@code state} to the per-extension v3 endpoint identified by the
     * forward URL embedded as the LAST segment of the {@code state} query parameter.</p>
     *
     * <p>The SDK base class builds {@code state} as {@code userId} or
     * {@code userId#authContextId} — perfectly correct for Private (per-invoker callback) flows
     * where the redirect_uri itself encodes the per-invoker routing. But for Quick Setup the
     * redirect_uri is the central URL, and without {@code #forwardPath} appended to state the
     * central handler returns 502 Bad Gateway (no way to route).</p>
     *
     * <p><b>Implementation:</b> call super to let the SDK build the URL with its standard
     * state encoding, then post-process the URL — parse out {@code state=...}, append
     * {@code #<urlencoded forward path>}, re-stitch the URL. State is opaque to Salesforce
     * (it just echoes it back), so the rewrite is safe.</p>
     *
     * <p>For non-Quick saved flows (Private AUTH_CODE / CC / PG) the super response is
     * returned unchanged — those use per-invoker callbacks and need no state rewrite.</p>
     *
     * @param cause the MustAuthorizeException raised by the runtime token-acquisition path
     * @return AuthorizationResponse with central-handler-routable state for Quick Setup,
     *         or the unmodified super response for non-Quick flows
     */
    @Override
    public RequestAuthenticator.AuthorizationResponse getMustAuthorizeResponse(MustAuthorizeException cause) {
        // Extract clientUserId from MAE details — needed by CRIT-025 state rewrite below.
        String clientUserIdFromMae = null;
        try {
            if (cause.getDetails() != null) {
                clientUserIdFromMae = cause.getDetails().stream()
                        .filter(f -> "clientUserId".equals(f.getName()))
                        .map(f -> (String) f.getValue())
                        .findFirst().orElse(null);
            }
        } catch (Exception ignored) { /* defensive */ }

        RequestAuthenticator.AuthorizationResponse base = super.getMustAuthorizeResponse(cause);
        // CRIT-029: per-user MAEs ALWAYS route through the central handler / Public
        // Connected App, regardless of what's saved in the Authentication tab. So state
        // ALWAYS needs the `#forwardPath` suffix and the `clientUserId#` prepend for the
        // platform to authenticate the forwarded callback (CRIT-027) and route to this
        // extension. No more conditional based on savedFlow — always rewrite.

        // Build the forward path the central handler must redirect to. This is the per-invoker
        // v3 callback endpoint inside THIS extension instance.
        String forwardPath = getRoutingUrl() + Constants.EXTENSION_FORWARD_PATH;

        // CRIT-025 — mirror outlook-4's 4-segment state format so the Krista platform's
        // central handler recognizes this as a per-user MCP flow and preserves the user's
        // session through the forward (proven by outlook-4's [OUTLOOK-DIAG] logs:
        // context.isAuthenticated()=true at callback entry, listener.authorized() succeeds).
        //
        // SDK base class builds state = userId + HASH + authContextId. Per CRIT-025 in
        // SalesforceTokenProvider.createMustAuthorizationException, our MAE now sets
        // userId=INV_xxx, so base state is "INV_xxx#authContextId". We PREPEND the
        // wsContact_<id> (from the MAE's `clientUserId` field) so the final state is
        // `wsContact#INV_xxx#authContextId#forwardPath` — identical to outlook-4's wire
        // format. The /v3/oauth/callback's parts[0] interpretation stays correct (still
        // the wsContact for token-store accountId).
        String rewritten = rewriteStateForPublicRoute(
                base.getLocation(), clientUserIdFromMae, forwardPath);
        return new RequestAuthenticator.AuthorizationResponse(rewritten, java.util.Collections.emptyList());
    }

    /**
     * Rewrites the {@code state} query parameter of the authorize URL to the 4-segment
     * MCP per-user format expected by the Krista platform's central callback handler:
     * {@code clientUserId#userId#authContextId#forwardPath}.
     *
     * <p>The SDK base class produces state of the form {@code userId#authContextId} (where
     * userId is now {@code INV_xxx} per CRIT-025). This method prepends the wsContact
     * value from the MAE and appends the forward path. State is opaque to Salesforce —
     * it just echoes it back — so the rewrite is safe.</p>
     */
    private static String rewriteStateForPublicRoute(String url, String clientUserId, String forwardPath) {
        if (url == null) return url;
        Pattern p = Pattern.compile("([?&]state=)([^&]*)");
        Matcher m = p.matcher(url);
        if (!m.find()) return url;
        String existingStateRaw = m.group(2);
        // existingStateRaw is URL-encoded. Decode to manipulate, then re-encode.
        String existingState;
        try {
            existingState = java.net.URLDecoder.decode(existingStateRaw, StandardCharsets.UTF_8);
        } catch (Exception e) {
            existingState = existingStateRaw;
        }

        StringBuilder newState = new StringBuilder();
        if (clientUserId != null && !clientUserId.isBlank()) {
            newState.append(clientUserId).append(Constants.HASH);
        }
        newState.append(existingState);
        if (forwardPath != null && !forwardPath.isBlank()) {
            newState.append(Constants.HASH).append(forwardPath);
        }

        String newStateEncoded = URLEncoder.encode(newState.toString(), StandardCharsets.UTF_8);
        return m.replaceFirst(m.group(1) + Matcher.quoteReplacement(newStateEncoded));
    }

    /**
     * Appends {@code #<urlencoded forward path>} to the {@code state} query parameter of
     * the given URL.
     *
     * <p>Uses a focused regex on {@code &state=...} or {@code ?state=...} rather than full
     * URI parsing to keep behaviour predictable. The {@code state} param is always present
     * (the SDK adds it unconditionally); failure to find it logs a warning and returns the
     * URL untouched so we never break a working URL.</p>
     */
    private static String appendForwardPathToState(String url, String forwardPath) {
        if (url == null || forwardPath == null || forwardPath.isBlank()) return url;
        Pattern p = Pattern.compile("([?&]state=)([^&]*)");
        Matcher m = p.matcher(url);
        if (!m.find()) return url;
        String existingState = m.group(2);
        String hashEncoded = URLEncoder.encode("#", StandardCharsets.UTF_8);
        String fpEncoded = URLEncoder.encode(forwardPath, StandardCharsets.UTF_8);
        String newState = existingState + hashEncoded + fpEncoded;
        return m.replaceFirst(m.group(1) + Matcher.quoteReplacement(newState));
    }
}
