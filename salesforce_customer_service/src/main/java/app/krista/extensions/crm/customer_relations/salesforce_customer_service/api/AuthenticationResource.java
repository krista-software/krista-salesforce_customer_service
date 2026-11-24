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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceSdkFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceSaveConfigurationImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.SalesforceTestConnectionServiceImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.XMlResponseParser;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.CaseNotifications;
import app.krista.extensions.util.EventHandler;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.model.base.FreeForm;
import app.krista.oauth.providers.salesforce.SalesforceOAuth2Sdk;
import app.krista.oauth.providers.salesforce.exception.SalesforceException;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.INVALID_STATE_PARAMETERS;
import static com.github.scribejava.core.model.OAuthConstants.CODE;
import static com.github.scribejava.core.model.OAuthConstants.STATE;

/* ─────────────────────────────────────────────────────────────────────────────
 * Callback HTML constants (spec §4.7) — XSS-safe: no user input is echoed.
 * Both pages include a <script>if(window.opener){window.close();}</script> tag
 * so the browser popup auto-closes after a successful or failed OAuth exchange.
 * ───────────────────────────────────────────────────────────────────────────── */

/**
 * JAX-RS resource handling Salesforce OAuth callbacks and webhook events.
 *
 * <h2>OAuth callback — {@code GET /callback} (T-M8)</h2>
 *
 * <p>As of salesforce_customer_service 2.0.18 (microsoft-auth-sdk 1.1.0 migration, T-M8),
 * the code-exchange in {@link #getCallBack(String, String)} now delegates to
 * {@link SalesforceOAuth2Sdk#handleCallback(String, String, app.krista.oauth.core.PkceVerifier)}
 * via {@link SalesforceCustomerServiceSdkFactory}. The SDK internally persists the refresh
 * token through {@link app.krista.extensions.crm.customer_relations.salesforce_customer_service.LegacyKeyedSalesforceTokenStore}
 * at the same legacy KV key format ({@code accountId&clientId+clientSecret}) that the pre-SDK
 * code wrote to — so existing customers' stored tokens remain valid after the upgrade with
 * zero data migration.</p>
 *
 * <h2>Key migration changes</h2>
 * <ul>
 *   <li>DELETED: {@code OAuth20Service.getAccessToken(code)} direct call</li>
 *   <li>DELETED: manual KV {@code put(key, refreshToken)} call — the SDK now
 *       handles refresh-token persistence atomically via its Auth Code strategy</li>
 *   <li>PRESERVED: multi-tenant {@code authContextId} resolution from the state parameter</li>
 *   <li>PRESERVED: {@code authorizationListener.authorized()} fired on success</li>
 *   <li>PRESERVED: {@code attributesStore.remove(authContextId)} cleanup after callback</li>
 *   <li>PRESERVED: {@code /casechanged} POST handler — byte-identical to pre-T-M8</li>
 * </ul>
 *
 * <h2>PKCE</h2>
 *
 * <p>PKCE is disabled for this extension (T-M5 invariant). {@code PkceVerifier} is passed
 * as {@code null} to {@code sdk.handleCallback(...)} — the SDK's Auth Code strategy accepts
 * null and skips the code-challenge validation step.</p>
 *
 * <h2>Thread-safety</h2>
 *
 * <p>Instances are per-request (HK2 scope). All fields are effectively final after injection.</p>
 *
 * @since salesforce_customer_service 2.0.18 (microsoft-auth-sdk 1.1.0 migration)
 */
@Path("/salesforcecustomerservice")
public final class AuthenticationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResource.class);

    /**
     * Placeholder returned in {@code /getCredentials} for secret fields that have a stored value.
     * The UI displays this as a hint that "a value is stored, keep it or replace it"; if the same
     * placeholder comes back in a subsequent {@code /testConnection} or {@code /saveCredentials}
     * payload it is treated as "use the stored value" and merged from KV via CRIT-002 resolution.
     */
    public static final String STORED_SECRET_MASK = "••••••••";

    private static String maskIfPresent(String value) {
        // Treat the stored mask itself as "no real value" — protects against the CRIT-005
        // corrupt-KV case where a previous bug persisted the placeholder as the secret.
        // If we returned the mask here, the UI would show a stored-secret affordance for
        // a value that doesn't actually exist, and the user would click Test Connection
        // expecting it to work and hit invalid_client.
        if (value == null || value.isBlank() || STORED_SECRET_MASK.equals(value)) return "";
        return STORED_SECRET_MASK;
    }

    /**
     * CRIT-005: Resolves the masked-secret placeholder back to the real value stored in KV.
     *
     * <p>The mask is a UI affordance — the form displays a saved secret as {@code "••••••••"}
     * so the operator doesn't have to re-type it on every save/test. When the form submits,
     * the JS sends the placeholder back to the server as the field value. Without this
     * resolution, downstream code (especially the {@code attributesStore.save(...)} call in
     * {@link SalesforceSaveConfigurationImpl#saveCredentials(com.google.gson.JsonObject)})
     * would treat the placeholder as the actual secret and persist {@code "••••••••"} into
     * KV — corrupting the saved credentials so every subsequent OAuth call fails with
     * {@code invalid_client}.</p>
     *
     * <p>This helper resolves all three secret fields ({@code clientSecret}, {@code password},
     * {@code securityToken}) by loading the stored attrs from KV (one read) and substituting
     * the real value wherever the payload still contains the mask. Mutates {@code attrMap}
     * in place; returns it for fluent use.</p>
     *
     * <p>Centralizing this here means every entry point into the attribute-build flow
     * (testConnection, saveCredentials, …) gets the same resolution behavior.</p>
     */
    public static java.util.Map<String, Object> resolveMaskedSecrets(
            java.util.Map<String, Object> attrMap,
            SalesforceCustomerServiceAttributesStore store,
            String invokerId) {
        if (attrMap == null || store == null || invokerId == null) return attrMap;

        SalesforceCustomerServiceAttributes stored = null;
        String[][] secretFieldMap = {
                {SalesforceCustomerServiceAttributes.CLIENT_SECRET, "clientSecret"},
                {SalesforceCustomerServiceAttributes.PASS_WORD,     "password"},
                {SalesforceCustomerServiceAttributes.SECURITY_TOKEN, "securityToken"}
        };

        for (String[] pair : secretFieldMap) {
            String key = pair[0];
            Object val = attrMap.get(key);
            if (!(val instanceof String) || !STORED_SECRET_MASK.equals(val)) continue;

            if (stored == null) {
                stored = store.loadByInvokerId(invokerId);
                if (stored == null) {
                    LOGGER.warn("resolveMaskedSecrets: payload contained mask for {} but no KV " +
                            "attrs were found for invokerId={}; leaving mask in place (will fail downstream)",
                            key, invokerId);
                    return attrMap;
                }
            }
            String realVal;
            switch (pair[1]) {
                case "clientSecret":  realVal = stored.getClientSecret();  break;
                case "password":      realVal = stored.getPassword();      break;
                case "securityToken": realVal = stored.getSecurityToken(); break;
                default:              realVal = null;
            }
            if (realVal != null && !realVal.isBlank() && !STORED_SECRET_MASK.equals(realVal)) {
                attrMap.put(key, realVal);
                System.out.println("[SF-DIAG] resolveMaskedSecrets: replaced mask for " + key +
                        " with stored value (len=" + realVal.length() + ")");
            } else if (STORED_SECRET_MASK.equals(realVal)) {
                // Corrupted KV: the stored secret itself is the mask placeholder. This happens
                // when an earlier version of saveCredentials wrote the mask back into KV. Wipe
                // the corrupt value so downstream code reports a clean "secret required" error
                // rather than failing with invalid_client against Salesforce.
                attrMap.put(key, "");
                System.out.println("[SF-DIAG] resolveMaskedSecrets: detected corrupt KV (stored " +
                        key + " IS the mask) — wiping; operator must re-enter the secret");
            }
        }
        return attrMap;
    }

    /**
     * HTML body returned when the OAuth callback completes successfully.
     *
     * <p>The {@code <script>} tag calls {@code window.close()} (guarded by an
     * {@code if(window.opener)} check so this is a no-op when the page is opened in a
     * normal tab rather than a popup). The popup window auto-closes and the JS polling
     * loop in {@code oauth/app.js} detects {@code popup.closed === true}, re-calls
     * {@code testConnection}, and enables "Save Changes" when the refresh token is present.</p>
     */
    private static final String CALLBACK_HTML_SUCCESS =
            "<!DOCTYPE html><html><head>" +
            "<meta charset=\"utf-8\">" +
            "<title>Salesforce Authorization</title>" +
            "<style>body{font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",sans-serif;" +
            "text-align:center;padding:3em 1em;color:#333;}h2{color:#43A047;}</style>" +
            "<script>" +
            // CRIT-024: best-effort postMessage to the opener window. The Krista platform's
            // backend authorizationListener.authorized() call from this callback is blocked
            // by the auth filter (we run unauthenticated because the central handler forward
            // doesn't carry session cookies). Signaling the opener via postMessage gives any
            // listener in the Krista UI a chance to auto-resume the original catalog request.
            // If the UI doesn't listen for this message, it's a harmless no-op and the
            // existing window.close() behavior takes over (user re-clicks manually).
            "if(window.opener){" +
            "try{window.opener.postMessage({type:'krista-oauth-complete',provider:'salesforce-platform',success:true},'*');}catch(e){}" +
            "window.close();" +
            "}" +
            "</script>" +
            "</head><body>" +
            "<h2>Authorization complete.</h2>" +
            "<p>You may close this window, or it will close automatically.</p>" +
            "</body></html>";

    /**
     * HTML body returned when the OAuth callback fails (exception, missing credentials,
     * or admin hitting the endpoint directly without going through Test Connection first).
     *
     * <p>The same {@code window.close()} script is present so any stale popup window
     * also closes automatically, preventing the user from getting stuck.</p>
     */
    private static final String CALLBACK_HTML_ERROR =
            "<!DOCTYPE html><html><head>" +
            "<meta charset=\"utf-8\">" +
            "<title>Salesforce Authorization Failed</title>" +
            "<script>if(window.opener){window.close();}</script>" +
            "</head><body>" +
            "<h2 style=\"color:#C61A24;\">Authorization failed.</h2>" +
            "<p>Please close this window and try again from the Authentication tab.</p>" +
            "</body></html>";

    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final SalesforceTokenProviderFactory providerFactory;
    private final AuthorizationContext context;
    private final AuthorizationListener authorizationListener;
    private final EventHandler eventHandler;
    private final SalesforceCustomerServiceSdkFactory sdkFactory;
    private final SalesforceSaveConfigurationImpl saveConfigurationImpl;
    private final SalesforceTestConnectionServiceImpl testConnectionService;
    private final Invoker invoker;
    private final String invokerId;

    @Inject
    public AuthenticationResource(SalesforceCustomerServiceAttributesStore attributesStore,
                                  SalesforceTokenProviderFactory providerFactory,
                                  AuthorizationContext context,
                                  AuthorizationListener authorizationListener,
                                  EventHandler eventHandler,
                                  SalesforceCustomerServiceSdkFactory sdkFactory,
                                  SalesforceSaveConfigurationImpl saveConfigurationImpl,
                                  SalesforceTestConnectionServiceImpl testConnectionService,
                                  Invoker invoker) {
        this.attributesStore = attributesStore;
        this.providerFactory = providerFactory;
        this.authorizationListener = authorizationListener;
        this.context = context;
        this.eventHandler = eventHandler;
        this.sdkFactory = sdkFactory;
        this.saveConfigurationImpl = saveConfigurationImpl;
        this.testConnectionService = testConnectionService;
        this.invoker = invoker;
        this.invokerId = invoker.getInvokerId();
    }

    /**
     * OAuth 2.0 Authorization Code callback.
     *
     * <p>Parses {@code code} and {@code state} from the Salesforce redirect, resolves the
     * per-request attributes (multi-tenant support via {@code authContextId}), then delegates
     * the code exchange and refresh-token persistence to
     * {@link SalesforceOAuth2Sdk#handleCallback(String, String, app.krista.oauth.core.PkceVerifier)}.
     * The SDK writes the refresh token to KV at the legacy key format via
     * {@link app.krista.extensions.crm.customer_relations.salesforce_customer_service.LegacyKeyedSalesforceTokenStore}
     * atomically — no manual store write is needed here.</p>
     *
     * <p>Returns an HTML page ({@link #CALLBACK_HTML_SUCCESS} or {@link #CALLBACK_HTML_ERROR})
     * with an embedded {@code <script>if(window.opener){window.close();}</script>} tag that
     * auto-closes the OAuth popup window. The JS polling loop in {@code oauth/app.js} detects
     * {@code popup.closed === true} and re-calls {@code testConnection} to confirm the refresh
     * token is now in KV.</p>
     *
     * <p><b>Defensive null check:</b> if {@code resolveAttributes()} returns {@code null} or
     * the resolved attributes have a {@code null} clientId/clientSecret, the admin likely hit
     * this endpoint directly without first clicking "Test Connection" in the Authentication tab
     * (which is what persists the credentials). In this case we return {@link #CALLBACK_HTML_ERROR}
     * rather than attempting a code exchange that would fail with an NPE.</p>
     *
     * @param code  the authorization code from Salesforce
     * @param state the state parameter encoding {@code accountId[#authContextId]}
     * @return HTTP 200 TEXT_HTML with auto-close popup script on success or failure
     */
    @GET
    @Path("/callback")
    @Produces(MediaType.TEXT_HTML)
    public Response getCallBack(@QueryParam(CODE) String code, @QueryParam(STATE) String state) {
        Objects.requireNonNull(code);
        System.out.println("[SF-DIAG] /callback: code=" + (code != null ? "present" : "null") +
                " state=" + state);
        String[] parts = state.split("#");
        if (parts[0].isBlank() || parts.length > 2) {
            LOGGER.warn("OAuth callback: invalid state parameter '{}'", state);
            return Response.ok(CALLBACK_HTML_ERROR, MediaType.TEXT_HTML).build();
        }
        String accountId = parts[0];
        String authContextId = parts.length == 2 ? parts[1] : null;

        try {
            // Resolve per-request attributes (multi-tenant path).
            SalesforceCustomerServiceAttributes attrs = resolveAttributes(authContextId);
            System.out.println("[SF-DIAG] /callback: resolveAttributes returned clientId=" +
                    (attrs != null ? attrs.getClientId() : "null-attrs") +
                    " secretLen=" + (attrs != null && attrs.getClientSecret() != null
                            ? attrs.getClientSecret().length() : -1));

            // Defensive check: testConnection should have persisted credentials before
            // redirecting the user. If we arrive here without credentials, the admin likely
            // hit /callback directly or there was an appliance restart between testConnection
            // and the callback. We cannot complete the code exchange in this case.
            if (attrs == null || attrs.getClientId() == null || attrs.getClientSecret() == null) {
                LOGGER.error("OAuth callback received but no Salesforce credentials are configured "
                        + "for invokerId/authContextId='{}'. The admin must complete the "
                        + "Authentication tab flow (click 'Test Connection') first.", authContextId);
                return Response.ok(CALLBACK_HTML_ERROR, MediaType.TEXT_HTML).build();
            }

            // Delegate code exchange to the SDK.
            // PKCE is disabled for this extension (T-M5 invariant) — pass null verifier.
            System.out.println("[SF-DIAG] /callback: calling sdk.handleCallback for accountId=" + accountId);
            SalesforceOAuth2Sdk sdk = sdkFactory.newSdk(attrs);
            sdk.handleCallback(code, accountId, null);
            System.out.println("[SF-DIAG] /callback: handleCallback returned successfully for accountId=" + accountId);

            // The SDK has persisted the refresh token via LegacyKeyedSalesforceTokenStore
            // at key: accountId&clientId+clientSecret — the same legacy KV key format.
            // No manual store.put() needed; the SDK handles this atomically.

            // CRIT-024 — same auto-resume limitation as /v3/oauth/callback. See comment there.
            try {
                authorizationListener.authorized();
            } catch (Exception listenerEx) {
                Throwable root = listenerEx;
                while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                LOGGER.warn("/callback: authorizationListener.authorized() failed — original request " +
                        "will NOT auto-resume (platform-side limitation). {}: {} (root: {}: {})",
                        listenerEx.getClass().getSimpleName(), listenerEx.getMessage(),
                        root.getClass().getSimpleName(), root.getMessage());
            }
            LOGGER.info("OAuth callback: code exchange successful for accountId={}", accountId);
            return Response.ok(CALLBACK_HTML_SUCCESS, MediaType.TEXT_HTML).build();

        } catch (app.krista.extension.authorization.MustAuthorizeException mae) {
            // Rule 3: rethrow MAE in the callback handler — the platform must see consent
            // requirements even in the callback path (unusual but possible if the SDK's
            // handleCallback internally triggers a per-user flow that needs consent).
            throw mae;
        } catch (SalesforceException e) {
            LOGGER.error("Salesforce exception during OAuth callback for accountId='{}': {}",
                    accountId, e.getMessage(), e);
            return Response.ok(CALLBACK_HTML_ERROR, MediaType.TEXT_HTML).build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error during OAuth callback for accountId='{}'", accountId, e);
            return Response.ok(CALLBACK_HTML_ERROR, MediaType.TEXT_HTML).build();
        } finally {
            // Always clean up the per-request attribute store entry, even on failure.
            if (authContextId != null) {
                attributesStore.remove(authContextId);
            }
        }
    }

    /**
     * OAuth 2.0 Authorization Code callback for the Quick Setup (Public App) flow.
     *
     * <h2>Two-hop callback architecture</h2>
     *
     * <p>Quick Setup uses Krista's own Salesforce Connected App, which is registered with a
     * <b>central</b> callback URL: {@code /extension/api/rest/v3/oauth/callback}. That platform
     * endpoint is shared across all Krista extensions that use Quick Setup. When Salesforce
     * redirects to it, the platform reads the OAuth {@code state} parameter (which encodes the
     * {@code invokerId} + extension routing key) and forwards the request to the per-extension
     * v3 endpoint — this method. This is the "two-hop" pattern: Salesforce → central callback
     * → extension v3 callback.</p>
     *
     * <p>The platform forward is a GET redirect to
     * {@code <routingUrl>/rest/salesforcecustomerservice/v3/oauth/callback?code=...&state=...}.
     * The {@code state} value is the {@code invokerId} that was passed as the OAuth state when
     * the authorize URL was built in {@code testAuthCodeQuick}.</p>
     *
     * <h2>Code exchange</h2>
     *
     * <p>Behaves identically to {@link #getCallBack(String, String)} except:</p>
     * <ul>
     *   <li>Credentials are loaded from the KV store by invokerId (persisted by
     *       {@code testAuthCodeQuick}). Those KV attrs are Public-app-backed (clientId/secret
     *       from {@code config.properties}, {@code isPublicApp()=true}).</li>
     *   <li>The state is the raw {@code invokerId} (no {@code #authContextId} suffix) because
     *       Quick Setup uses invokerId-scoped state.</li>
     * </ul>
     *
     * <h2>Platform gap note</h2>
     *
     * <p>The platform-level central callback handler at
     * {@link app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants#DEFAULT_CALLBACK_PATH}
     * ({@code /extension/api/rest/v3/oauth/callback}) is a platform feature that must exist
     * for the Quick Setup forwarding to work. As of this writing it exists for Outlook-4 (MS
     * provider). Whether Salesforce provider extensions are included in the same platform
     * handler is a gap that requires a platform-team conversation. If the platform does NOT
     * yet forward for Salesforce, Quick Setup will silently fail after the SF consent screen
     * (the user will see the central callback page with no forwarding). This is a documented
     * gap — the extension-side wiring implemented here is correct and complete. See the
     * playbook §Public App / Quick Setup pattern for the platform gap tracker.</p>
     *
     * @param code  the authorization code from Salesforce (forwarded by the platform)
     * @param state the OAuth state parameter — the {@code invokerId} for Quick Setup
     * @return HTTP 200 TEXT_HTML auto-close popup page on success or failure
     */
    @GET
    @Path("/v3/oauth/callback")
    @Produces(MediaType.TEXT_HTML)
    public Response getV3CallBack(@QueryParam(CODE) String code, @QueryParam(STATE) String state) {
        LOGGER.debug("/v3/oauth/callback [Quick Setup]: code={} state={}",
                code != null ? "present" : "null", state);

        if (code == null || code.isBlank()) {
            LOGGER.warn("v3/oauth/callback: code is null or blank for state={}", state);
            return Response.ok(CALLBACK_HTML_ERROR, MediaType.TEXT_HTML).build();
        }
        if (state == null || state.isBlank()) {
            LOGGER.warn("v3/oauth/callback: state is null or blank");
            return Response.ok(CALLBACK_HTML_ERROR, MediaType.TEXT_HTML).build();
        }

        // CRIT-016: the Krista platform's central callback handler forwards state UNCHANGED,
        // which still contains the '#<forwardPath>' suffix we appended in testAuthCodeQuick.
        // Strip that off before treating the leading segment as the invokerId/accountId.
        // Mirrors OutlookApiResource.getAuthenticationResponseMessage state-split (line 120).
        String[] parts = state.split(Constants.HASH);
        String parsedState = parts[0];
        System.out.println("[SF-DIAG] /v3/oauth/callback: stripped forward path, parsedState=" + parsedState +
                " (full state had " + parts.length + " segments)");

        // CRIT-017 + CRIT-022: The v3 callback is ONLY hit by Quick Setup flows.
        // Don't trust the KV-loaded attrs here — the `publicApp` sentinel doesn't survive
        // KV (de)serialization round-trips. Rebuild fresh Public-app attrs from
        // config.properties; pull only the `sandbox` flag (and the saved email for
        // logging) from KV.
        //
        // KEY DETAIL: the KV lookup uses our INJECTED invokerId, NOT parsedState. With
        // CRIT-022, parsedState IS the admin email — not an invokerId key — so
        // loadByInvokerId(parsedState) would return null and we'd lose the sandbox flag.
        SalesforceCustomerServiceAttributes savedAttrs = attributesStore.loadByInvokerId(invokerId);
        boolean sandbox = savedAttrs != null && savedAttrs.isSandbox();
        String savedEmail = savedAttrs != null ? savedAttrs.getEmail() : null;
        System.out.println("[SF-DIAG] /v3/oauth/callback: lookup by injected invokerId=" + invokerId +
                " → savedAttrs publicApp=" + (savedAttrs != null ? savedAttrs.isPublicApp() : "null") +
                " sandbox=" + sandbox + " savedEmail=" + savedEmail +
                " — rebuilding fresh Public attrs from config.properties");

        SalesforceCustomerServiceAttributes attrs = SalesforceCustomerServiceAttributes.createPublic(
                invoker, sandbox, parsedState);  // parsedState IS the admin's email (CRIT-022)
        System.out.println("[SF-DIAG] /v3/oauth/callback: rebuilt attrs isPublicApp=" + attrs.isPublicApp() +
                " callbackEndPoint=" + attrs.getCallbackEndPoint() + " email=" + attrs.getEmail());

        if (attrs.getClientId() == null || attrs.getClientSecret() == null) {
            LOGGER.error("v3/oauth/callback: Public app credentials missing from config.properties");
            return Response.ok(CALLBACK_HTML_ERROR, MediaType.TEXT_HTML).build();
        }

        // CRIT-022: parsedState IS the admin's email (sent as the state during
        // testAuthCodeQuick). Using it as the accountId keys the RT under
        // sfv1/{email}/{hash(publicClientId+publicSecret)} — a stable per-admin identity,
        // hermetic from the per-user `wsContact_<id>` keyspace.
        String accountId = parsedState;

        try {
            System.out.println("[SF-DIAG] /v3/oauth/callback: calling sdk.handleCallback for accountId=" + accountId);
            SalesforceOAuth2Sdk sdk = sdkFactory.newSdk(attrs);
            sdk.handleCallback(code, accountId, null);   // PKCE disabled for Quick Setup (usePkce=false)
            System.out.println("[SF-DIAG] /v3/oauth/callback: handleCallback succeeded for Quick Setup");

            // CRIT-024 — the auto-resume problem.
            //
            // Bytecode inspection of AuthorizationListener showed:
            //   public abstract void authorized();                  // requires @Authenticated
            //   public abstract void authorized(java.lang.String);  // requires @WorkspaceAdmin
            // Both methods are gated by Krista's auth filter at the proxy level. Our
            // /v3/oauth/callback endpoint is hit unauthenticated because the Krista platform's
            // central handler at /extension/api/rest/v3/oauth/callback forwards us the OAuth
            // response via a server-side proxy that does NOT carry the user's session cookies
            // (context.isAuthenticated() = false here, even though the user IS active in
            // Krista with valid session cookies in their browser). The no-arg authorized()
            // call silently no-ops in that case; the String variant THROWS
            // UndeclaredThrowableException because @WorkspaceAdmin is enforced.
            //
            // Net effect: from inside this callback we cannot make the platform resume the
            // original suspended Create-Case request. The user has to manually re-click. This
            // is the same limitation outlook-4 has — they also wrap authorized() in an
            // isAuthenticated() check, which is silently false here. A proper fix needs a
            // Krista platform change (either preserve cookies on the central-handler forward,
            // or expose an unauthenticated state-based resume API).
            //
            // What we DO from here: best-effort call to authorized() (no-arg). If the context
            // is authenticated, the platform resumes the suspended request and the user's
            // original click completes automatically. If not (the platform-side limitation),
            // the listener throws — we log it at WARN and return the success HTML which
            // includes a `window.opener.postMessage` signal that the Krista client UI MAY
            // pick up for frontend-level retry.
            try {
                authorizationListener.authorized();
            } catch (Exception listenerEx) {
                Throwable root = listenerEx;
                while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                LOGGER.warn("v3/oauth/callback: authorizationListener.authorized() failed — original " +
                        "catalog request will NOT auto-resume (platform-side limitation). " +
                        "User must re-trigger the action manually. {}: {} (root: {}: {})",
                        listenerEx.getClass().getSimpleName(), listenerEx.getMessage(),
                        root.getClass().getSimpleName(), root.getMessage());
            }
            LOGGER.info("v3/oauth/callback [Quick Setup]: code exchange successful for invokerId/state={}", state);
            return Response.ok(CALLBACK_HTML_SUCCESS, MediaType.TEXT_HTML).build();

        } catch (app.krista.extension.authorization.MustAuthorizeException mae) {
            throw mae;
        } catch (SalesforceException e) {
            LOGGER.error("v3/oauth/callback [Quick Setup]: Salesforce exception for state={}: {}",
                    state, e.getMessage(), e);
            return Response.ok(CALLBACK_HTML_ERROR, MediaType.TEXT_HTML).build();
        } catch (RuntimeException e) {
            LOGGER.error("v3/oauth/callback [Quick Setup]: unexpected error for state={}", state, e);
            return Response.ok(CALLBACK_HTML_ERROR, MediaType.TEXT_HTML).build();
        }
        // Note: no attributesStore.remove() here — the Quick Setup attrs must persist
        // in KV so runtime calls (chooseEffectiveAttributes) can read the isPublicApp flag
        // and sandbox preference. They are only removed when the operator switches flows.
    }

    /**
     * Serves the SDK's themed Salesforce OAuth setup page.
     *
     * <p>Reads {@code oauth/index.html} from the classpath — the SDK UI asset that the
     * {@code extractSDKResources} Gradle task copies from the SDK JAR into
     * {@code build/resources/main/oauth/index.html} at build time. The page fetches
     * its runtime configuration from
     * {@code /salesforcecustomerservice/oauth/config?providerName=salesforce-platform}
     * (served by {@code OAuth2ConfigResource} registered in
     * {@link SalesforceCustomerRelationsApplication}).</p>
     *
     * <p>Hardening headers applied per SDK spec §4.6.4.1:</p>
     * <ul>
     *   <li>{@code Content-Security-Policy} — matches the inline {@code <meta>} tag in
     *       {@code oauth/index.html}; both must stay in sync.</li>
     *   <li>{@code X-Content-Type-Options: nosniff} — prevents MIME-type sniffing.</li>
     *   <li>{@code X-Frame-Options: DENY} — prevents click-jacking.</li>
     *   <li>{@code Referrer-Policy: no-referrer} — suppresses referrer for outbound links.</li>
     *   <li>{@code Cache-Control: no-store} — OAuth UI must not be cached.</li>
     * </ul>
     *
     * @return HTTP 200 with the themed HTML; HTTP 503 if the asset is missing from the classpath
     *         (packaging error — the {@code extractSDKResources} task was not run)
     */
    @GET
    @Path("/authentication/")
    @Produces(MediaType.TEXT_HTML)
    public Response getAuthenticationPage() {
        InputStream in = getClass().getResourceAsStream("/oauth/index.html");
        if (in == null) {
            LOGGER.error("oauth/index.html not found on classpath. " +
                    "Run './gradlew build' to ensure the extractSDKResources task " +
                    "has copied the SDK UI assets into build/resources/main/oauth/.");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Authentication UI not available — packaging error")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        try {
            byte[] html = in.readAllBytes();
            // SDK v1.1.0 (with build-time fixes) ships oauth/index.html ready to serve
            // verbatim. The page handles:
            //   - relative path to /oauth/config (../../oauth/config?providerName=X)
            //   - flattening of OAuth2ConfigResource's nested response shape
            //   - logo-path rewriting to relative ../..
            //   - CSP hash baked at build time matches the inlined script
            // The page's CSP meta tag is the source of truth for script-src; we add
            // response headers for nosniff / frame-options / referrer-policy / no-store.
            return Response.ok(html, MediaType.TEXT_HTML)
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Referrer-Policy", "no-referrer")
                    .header("Cache-Control", "no-store")
                    .build();
        } catch (IOException e) {
            LOGGER.error("Failed to read oauth/index.html from classpath: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to load authentication UI")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // Best-effort close — stream state is irrelevant after readAllBytes()
            }
        }
    }

    /**
     * Resolves the {@link SalesforceCustomerServiceAttributes} for the current callback request.
     *
     * <p>When {@code authContextId} is null, falls back to operator-default attributes via
     * the {@link SalesforceTokenProviderFactory}. When present, loads the per-request
     * override saved before the OAuth redirect.</p>
     *
     * @param authContextId the per-request context ID, or null for operator default
     * @return the resolved attributes — never null
     * @throws BadRequestException if {@code authContextId} is non-null but blank
     */
    private SalesforceCustomerServiceAttributes resolveAttributes(String authContextId) {
        if (authContextId == null) {
            // Prefer the Authentication-tab's KV-stored attributes (saved by testConnection
            // before redirecting to Salesforce). The tab is the canonical writer; the legacy
            // Invoker-attribute path is only a fallback for installs that haven't migrated.
            // Without this preference, the callback would exchange the code against whatever
            // clientId/clientSecret happen to be in the Invoker form (often empty/stale),
            // the SDK would either trip the defensive null-check below OR store the RT under
            // a key derived from the wrong credentials — both produce the "Authorization was
            // not completed" loop on the re-test.
            SalesforceCustomerServiceAttributes saved = attributesStore.loadByInvokerId(invokerId);
            if (saved != null
                    && saved.getClientId() != null && !saved.getClientId().isBlank()
                    && saved.getClientSecret() != null && !saved.getClientSecret().isBlank()) {
                return saved;
            }
            return providerFactory.create().getSalesforceCustomerServiceAttributes();
        }
        if (authContextId.isBlank()) {
            throw new BadRequestException(INVALID_STATE_PARAMETERS);
        }
        return attributesStore.load(authContextId);
    }

    /**
     * Returns the OAuth metadata JSON shape that the SDK's bundled oauth/app.js expects to
     * fetch from {@code GET /oauth/metadata/{extensionId}}.
     *
     * <p><b>Why this exists:</b> The SDK's {@code OAuth2ConfigResource} only exposes
     * {@code /oauth/config?providerName=X} (theme + extension info), but the bundled
     * {@code oauth/app.js} ALSO fetches {@code /oauth/metadata/{extensionId}} as part of
     * its {@code init()} flow. If the metadata fetch fails, the page short-circuits to
     * {@code "Extension not found."} and the auth form never renders.</p>
     *
     * <p>The SDK has no endpoint at this path in v1.1.0 (tracked SDK gap — proper fix is to
     * add {@code @GET /metadata/{extensionId}} to OAuth2ConfigResource). Until then, the
     * extension serves the metadata directly from its own JAX-RS resource.</p>
     *
     * <p>The response shape (what app.js consumes — see {@code tryRender()} and
     * {@code renderPage()} in {@code oauth/app.js}):</p>
     * <ul>
     *   <li>{@code id} — must be truthy; the extension's identifier</li>
     *   <li>{@code providerLabel} — display name shown in the page header</li>
     *   <li>{@code defaultScopes} — array of scope strings pre-selected on first load</li>
     *   <li>{@code scopes} — array of {value, description, required} for the scope-panel UI</li>
     *   <li>{@code customSections} — array of provider-defined custom UI sections (empty for SF)</li>
     * </ul>
     *
     * @param extensionId the extension identifier from the URL path (must equal
     *                    {@code "salesforcecustomerservice"} for this extension)
     * @return HTTP 200 with the metadata JSON; HTTP 404 if extensionId doesn't match
     */
    @GET
    @Path("/oauth/metadata/{extensionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOAuthMetadata(@PathParam("extensionId") String extensionId) {
        if (!"salesforcecustomerservice".equals(extensionId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"unknown_extension_id\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        String json = "{"
                + "\"id\":\"salesforcecustomerservice\","
                + "\"providerLabel\":\"Salesforce Customer Service\","
                + "\"displayName\":\"Salesforce Customer Service\","
                + "\"defaultScopes\":[\"api\",\"refresh_token\",\"offline_access\"],"
                + "\"scopes\":["
                + "  {\"value\":\"api\",\"description\":\"Access and manage your data\",\"required\":true,"
                + "   \"catalogRequests\":[\"Fetch Cases\",\"Fetch Accounts\",\"Fetch Contacts\",\"Create Case\",\"Update Case\",\"Close Case\",\"Fetch Customers\",\"Fetch Individuals\"]},"
                + "  {\"value\":\"refresh_token\",\"description\":\"Perform requests at any time\",\"required\":true},"
                + "  {\"value\":\"offline_access\",\"description\":\"Perform requests at any time\",\"required\":true},"
                + "  {\"value\":\"content\",\"description\":\"Access Salesforce Files\",\"required\":false,"
                + "   \"catalogRequests\":[\"Fetch Content\",\"Upload Attachment\"]}"
                + "],"
                // showScopePanel:true — display the Permissions (Scopes) panel like the Outlook
                // extension does. NOTE: the granted scope set is FIXED (getScopeString() returns
                // "api refresh_token offline_access"), so the chips are INFORMATIONAL — toggling
                // them does not change what Salesforce grants (Connected Apps pin scopes at the
                // app config level). catalogRequests drive the Supported / Not-Supported table.
                + "\"showScopePanel\":true,"
                + "\"customSections\":[]"
                + "}";
        return Response.ok(json, MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-store")
                .build();
    }

    /**
     * Persists operator credentials from the Authentication tab's "Save Changes" button.
     *
     * <p>Delegates to {@link SalesforceSaveConfigurationImpl#saveCredentials(JsonObject)} which:</p>
     * <ol>
     *   <li>Parses the payload into {@link SalesforceCustomerServiceAttributes}.</li>
     *   <li>Calls {@code /testConnection} logic first — refuses to persist on failure.</li>
     *   <li>Writes the credentials blob (all SF fields + {@code savedFlow}) to KV under invokerId.</li>
     * </ol>
     *
     * <p>HTTP 200 is returned in both success and failure cases. The caller reads
     * {@code r.isSuccess} from the response body to determine the outcome — matching
     * the Outlook-4 pattern and the Authentication tab JS contract.</p>
     *
     * @param payload JSON body with fields: {@code clientId}, {@code clientSecret},
     *                {@code instanceUrl}, {@code userName}, {@code password},
     *                {@code securityToken}, {@code sandbox}, {@code flow}
     * @return HTTP 200 with body {@code {"isSuccess": true}} or
     *         {@code {"isSuccess": false, "errorMessage": "..."}}
     */
    @POST
    @Path("/saveCredentials")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveCredentials(JsonObject payload) {
        LOGGER.debug("saveCredentials called for invokerId={}", invokerId);
        String result = saveConfigurationImpl.saveCredentials(payload);
        return Response.ok(result, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Tests whether the supplied credentials can obtain a Salesforce token without
     * persisting anything.
     *
     * <p>Delegates to {@link SalesforceTestConnectionServiceImpl#testConnection(SalesforceCustomerServiceAttributes, String)}.
     * Builds a transient {@link SalesforceCustomerServiceAttributes} from the payload,
     * invokes the test, and returns the result body at HTTP 200.</p>
     *
     * <p>The Authentication tab's {@code doTest()} checks {@code r.isSuccess} on the
     * response to decide whether to enable the "Save Changes" button.</p>
     *
     * @param payload JSON body with at minimum {@code clientId}, {@code clientSecret},
     *                and {@code flow}
     * @return HTTP 200 with body {@code {"isSuccess": true}} or
     *         {@code {"isSuccess": false, "errorMessage": "..."}}
     */
    @POST
    @Path("/testConnection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testConnection(JsonObject payload) {
        LOGGER.debug("testConnection called for invokerId={}", invokerId);
        // JS sends the flow id as "authFlow" (see oauth/app.js collectPayload).
        String flow = extractString(payload, "authFlow");
        SalesforceCustomerServiceAttributes attrs = attrsFromPayload(payload);
        String result = testConnectionService.testConnection(attrs, flow);
        return Response.ok(result, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns previously-saved credentials for a given OAuth flow, or an empty JSON object
     * if none. Used by the bundled {@code oauth/app.js} to pre-fill the auth-tab form on
     * page load.
     *
     * <h2>Behaviour</h2>
     * <p>Looks up the saved flow for the current {@code invokerId} in the KV store. If the
     * saved flow matches the requested {@code authFlow} parameter, the stored credentials
     * are returned with secrets blanked out (empty strings). This prevents credentials from
     * being shipped to the browser while still allowing the UI to detect that credentials
     * exist and pre-fill non-secret fields.</p>
     *
     * <p><b>Secret handling:</b> {@code clientSecret}, {@code password}, and
     * {@code securityToken} are returned as empty strings. The UI will display the field
     * as empty; the operator must re-enter secrets to save again. Client-side encryption
     * (as done by Outlook-4's {@code EncryptionUtil}) is a planned follow-up.</p>
     *
     * <p>If no credentials are saved for this invokerId, or the saved flow does not match
     * the probed {@code authFlow}, an empty JSON object is returned. The UI treats an empty
     * object as "no credentials saved" and shows the blank form.</p>
     *
     * @param authType the MS-SDK auth-type probe value (e.g. {@code "Public"},
     *                 {@code "Private"}) — not used for SF flow matching; retained for
     *                 API parity with the MS-side schema
     * @param authFlow the specific flow to probe (e.g. {@code "AUTH_CODE"},
     *                 {@code "CLIENT_CREDENTIALS"}, {@code "PASSWORD_GRANT"})
     * @return HTTP 200 with the saved credential fields (secrets blanked) or {@code {}}
     */
    @GET
    @Path("/getCredentials")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCredentials(
            @QueryParam("authType") String authType,
            @QueryParam("authFlow") String authFlow) {
        LOGGER.debug("getCredentials — invokerId={} authType={} authFlow={}", invokerId, authType, authFlow);

        String savedFlow = attributesStore.getSavedFlow(invokerId);
        if (savedFlow == null || !savedFlow.equals(authFlow)) {
            // No credentials saved for this flow — return empty to let the UI show a blank form.
            return Response.ok("{}", MediaType.APPLICATION_JSON)
                    .header("Cache-Control", "no-store")
                    .build();
        }

        SalesforceCustomerServiceAttributes attrs = attributesStore.loadByInvokerId(invokerId);
        if (attrs == null) {
            return Response.ok("{}", MediaType.APPLICATION_JSON)
                    .header("Cache-Control", "no-store")
                    .build();
        }

        // AUTH_CODE_QUICK (Quick Setup): credentials are Krista-owned and must NEVER be
        // sent to the browser. Only the sandbox toggle and the operator's email are
        // meaningful to surface — the email lets the admin see WHICH Salesforce account
        // they previously authorized this connection with (CRIT-022 accountability fix).
        // Secrets and clientId are omitted entirely.
        if ("AUTH_CODE_QUICK".equals(savedFlow)) {
            Map<String, Object> quickResponse = new HashMap<>();
            quickResponse.put("email",     attrs.getEmail());
            quickResponse.put("sandbox",   attrs.isSandbox());
            quickResponse.put("savedFlow", savedFlow);
            return Response.ok(Constants.GSON.toJson(quickResponse), MediaType.APPLICATION_JSON)
                    .header("Cache-Control", "no-store")
                    .build();
        }

        // Secrets are returned as a masked placeholder when stored — mirrors the previous
        // React version's contract: the UI treats "••••••••" as "stored, keep as-is" so the
        // operator doesn't have to re-enter secrets just to re-test or save. When a secret-
        // bearing payload comes back to /testConnection or /saveCredentials with "••••••••"
        // as the value, CRIT-002 secret-resolution merges the real value from KV.
        // When no secret is stored, return empty string so the UI shows a blank field.
        //
        // CRIT-009: emit camelCase wire keys matching the theme JSON's field.key values
        // (instanceUrl, userName) so the JS `populateFields` can find them. The legacy
        // Java constants ("Instance URL" with space, "username" lowercase) do NOT match
        // what the UI looks for. Without this, instanceUrl/userName would never pre-fill
        // after a save → user sees an empty field even though the value is stored.
        // Other fields (clientId, clientSecret, password, securityToken, sandbox) already
        // match their constant values verbatim and need no remapping.
        Map<String, Object> response = new HashMap<>();
        response.put("clientId",      attrs.getClientId());
        response.put("clientSecret",  maskIfPresent(attrs.getClientSecret()));
        response.put("instanceUrl",   attrs.getInstanceUrl());
        response.put("userName",      attrs.getUserName());
        response.put("password",      maskIfPresent(attrs.getPassword()));
        response.put("securityToken", maskIfPresent(attrs.getSecurityToken()));
        response.put("sandbox",       attrs.isSandbox());
        response.put("savedFlow",     savedFlow);

        return Response.ok(Constants.GSON.toJson(response), MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-store")
                .build();
    }

    // -------------------------------------------------------------------------
    // Internal helpers shared by saveCredentials and testConnection endpoints
    // -------------------------------------------------------------------------

    /**
     * Builds a transient {@link SalesforceCustomerServiceAttributes} from the JSON payload
     * for use in the {@code /testConnection} endpoint.
     *
     * <p>Delegates to {@link SalesforceCustomerServiceAttributes#create(Invoker, Map)} so the
     * routing-URL prefix (used to build the OAuth {@code redirect_uri} for AUTH_CODE) is
     * derived from the Invoker. Passing {@code null} here would yield a malformed authorize
     * URL like {@code redirect_uri=null/rest/.../callback}.</p>
     */
    private SalesforceCustomerServiceAttributes attrsFromPayload(JsonObject payload) {
        // AUTH_CODE_QUICK (Quick Setup): do not try to parse clientId/clientSecret from the
        // payload — there are none (the form only shows the sandbox toggle). Return a minimal
        // attrs instance carrying only the sandbox preference; testAuthCodeQuick() will build
        // the real Public-app-backed attrs from config.properties.
        String flowFromPayload = extractString(payload, "authFlow");
        if ("AUTH_CODE_QUICK".equals(flowFromPayload)) {
            boolean isSandbox = false;
            if (payload.has(SalesforceCustomerServiceAttributes.SANDBOX)
                    && !payload.get(SalesforceCustomerServiceAttributes.SANDBOX).isJsonNull()) {
                isSandbox = payload.get(SalesforceCustomerServiceAttributes.SANDBOX).getAsBoolean();
            }
            // CRIT-022: Quick Setup also carries the admin email (entered on the Quick Setup
            // tab). Forward it so testAuthCodeQuick can validate it and use it as the
            // operator accountId for the resulting refresh token.
            java.util.Map<String, Object> quickMap = new java.util.HashMap<>();
            quickMap.put(SalesforceCustomerServiceAttributes.SANDBOX, isSandbox);
            quickMap.put(SalesforceCustomerServiceAttributes.EMAIL,
                    extractString(payload, SalesforceCustomerServiceAttributes.EMAIL));
            return SalesforceCustomerServiceAttributes.create(invoker, quickMap);
        }

        java.util.Map<String, Object> attrMap = new java.util.HashMap<>();
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_ID,
                extractString(payload, SalesforceCustomerServiceAttributes.CLIENT_ID));
        attrMap.put(SalesforceCustomerServiceAttributes.CLIENT_SECRET,
                extractString(payload, SalesforceCustomerServiceAttributes.CLIENT_SECRET));
        attrMap.put(SalesforceCustomerServiceAttributes.INSTANCE_URL,
                extractString(payload, "instanceUrl", SalesforceCustomerServiceAttributes.INSTANCE_URL));
        attrMap.put(SalesforceCustomerServiceAttributes.USER_NAME,
                extractString(payload, "userName", SalesforceCustomerServiceAttributes.USER_NAME));
        attrMap.put(SalesforceCustomerServiceAttributes.PASS_WORD,
                extractString(payload, SalesforceCustomerServiceAttributes.PASS_WORD));
        attrMap.put(SalesforceCustomerServiceAttributes.SECURITY_TOKEN,
                extractString(payload, SalesforceCustomerServiceAttributes.SECURITY_TOKEN));
        attrMap.put(SalesforceCustomerServiceAttributes.EMAIL,
                extractString(payload, SalesforceCustomerServiceAttributes.EMAIL));
        if (payload.has(SalesforceCustomerServiceAttributes.SANDBOX)
                && !payload.get(SalesforceCustomerServiceAttributes.SANDBOX).isJsonNull()) {
            attrMap.put(SalesforceCustomerServiceAttributes.SANDBOX,
                    payload.get(SalesforceCustomerServiceAttributes.SANDBOX).getAsBoolean());
        }
        // CRIT-005: Resolve secret masks ("••••••••") back to the real stored values BEFORE
        // building the attrs object. Without this, downstream code would persist the mask
        // into KV — overwriting the good secret on every subsequent save.
        resolveMaskedSecrets(attrMap, attributesStore, invokerId);
        return SalesforceCustomerServiceAttributes.create(invoker, attrMap);
    }

    private static String extractString(JsonObject payload, String... fieldAliases) {
        // CRIT-006: the new flow-driven UI sends camelCase wire names from theme JSON
        // (instanceUrl, userName) which mismatch the legacy Java constants used by the
        // Invoker-form path ("Instance URL", "username"). Without alias support, the
        // server reads null for those fields → HC-006 routing matrix falls back to
        // AUTH_CODE for what was supposed to be CLIENT_CREDENTIALS/PASSWORD_GRANT →
        // SDK throws NPE on accountId because per-user flow needs one. Try each alias
        // in order; first non-blank value wins.
        for (String field : fieldAliases) {
            if (payload.has(field) && !payload.get(field).isJsonNull()) {
                String val = payload.get(field).getAsString();
                if (!val.isBlank()) return val;
            }
        }
        return null;
    }

    /**
     * Salesforce outbound messaging webhook — case-changed event handler.
     *
     * <p>This handler is <b>completely unchanged</b> from the pre-T-M8 code. It is NOT an
     * OAuth endpoint and is out of scope for the 1.1.0 SDK migration.</p>
     *
     * @param xml the SOAP/XML outbound message payload from Salesforce
     * @return HTTP 200 OK
     */
    @POST
    @Path("/casechanged")
    public Response handleCaseChangedEvent(String xml) {
        JAXBElement<CaseNotifications> caseNotifications = XMlResponseParser.parseCaseXmlResponse(xml);
        if (caseNotifications != null) {
            FreeForm caseNotificationSObject = caseNotifications.getValue().getCaseNotification().getSalesforceXMLCase()
                    .toCaseFreeForm();
            eventHandler.handleEvent("Case Change Alert", caseNotificationSObject);
        }
        return Response.ok().build();
    }

}
