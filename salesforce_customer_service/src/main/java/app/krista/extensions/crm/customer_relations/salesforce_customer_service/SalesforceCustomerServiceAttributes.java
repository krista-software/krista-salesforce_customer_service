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

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extension.util.InvokerAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.api.SalesforceCustomApi;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.oauth.providers.salesforce.config.SalesforceConfig;
import app.krista.oauth.providers.salesforce.extension.SalesforceAttributes;
import com.github.scribejava.apis.SalesforceApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

/**
 * Holds Salesforce configuration attributes used for OAuth and API integration.
 * Provides methods to initialize, access, and update Salesforce connection parameters.
 *
 * <h2>Public App / Quick Setup</h2>
 *
 * <p>When the operator chooses the "Quick Setup" flow ({@code AUTH_CODE_QUICK}), this extension
 * uses Krista's own Salesforce Connected App credentials baked into {@code config.properties}
 * rather than requiring the operator to supply their own. Use
 * {@link #createPublic(Invoker, boolean)} to obtain an instance backed by those credentials.</p>
 *
 * <p>The Public app uses a central Krista-platform callback URL
 * ({@link Constants#DEFAULT_CALLBACK_PATH}) that is NOT per-invoker.
 * The per-invoker callback ({@code routingUrl + CALLBACK_URI}) is used only for Advanced
 * AUTH_CODE flows where the operator supplies their own Connected App.</p>
 *
 * <h2>Thread-safety</h2>
 *
 * <p>Instances are per-request (HK2 default scope) and effectively immutable after construction,
 * except for the {@code update()} method which mutates non-final fields. The {@code oAuthService}
 * lazy-init is guarded by {@code synchronized}.</p>
 */
@Service
public final class SalesforceCustomerServiceAttributes implements SalesforceAttributes {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceCustomerServiceAttributes.class);

    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String INSTANCE_URL = "Instance URL";
    public static final String USER_NAME = "username";
    public static final String PASS_WORD = "password";
    public static final String SECURITY_TOKEN = "securityToken";
    public static final String SANDBOX = "sandbox";
    /**
     * CRIT-022: Quick Setup admin email. Used as the {@code accountId} for the operator
     * Test Connection in the Public-app flow, so the resulting refresh token is keyed by
     * a stable per-admin identity ({@code sfv1/{email}/{hash}}) instead of by
     * {@code invokerId} (which would silently overwrite on the next admin's Test Connection
     * and bury who actually authorized the connection).
     */
    public static final String EMAIL = "email";

    /** Sentinel that identifies a Public-app-backed instance (Quick Setup flow). */
    private final boolean publicApp;

    private final String routingUrl;
    private String clientId;
    private String clientSecret;
    private String instanceUrl;
    private String userName;
    private String password;
    private String securityToken;
    private Boolean isSandbox;
    /** Quick Setup operator email — see {@link #EMAIL}. {@code null} for Advanced flows. */
    private String email;
    private OAuth20Service oAuthService;

    /**
     * Constructs the attributes using an {@link Invoker}, extracting routing and attribute information.
     *
     * @param invoker the Krista extension {@link Invoker}
     */
    @Inject
    public SalesforceCustomerServiceAttributes(Invoker invoker) {
        this(invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE),
                invoker.getAttributes());
    }

    /**
     * Constructs the attributes using a routing URL and a map of attribute values.
     *
     * @param routingUrl the callback routing URL
     * @param attributes the map of attributes (clientId, clientSecret, username, etc.)
     */
    public SalesforceCustomerServiceAttributes(String routingUrl, Map<String, Object> attributes) {
        this((String) attributes.get(CLIENT_ID),
                (String) attributes.get(CLIENT_SECRET),
                (String) attributes.get(INSTANCE_URL),
                (String) attributes.get(USER_NAME),
                (String) attributes.get(PASS_WORD),
                (String) attributes.get(SECURITY_TOKEN),
                (Boolean) attributes.get(SANDBOX),
                routingUrl,
                false,
                (String) attributes.get(EMAIL));
    }

    /**
     * Constructs the attributes using individual configuration values.
     *
     * <p>This is the customer-supplied (Advanced / Private) constructor. The
     * {@code publicApp} sentinel is {@code false}; {@link #getCallbackEndPoint()} will
     * return the per-invoker callback URL.</p>
     *
     * @param clientId      Salesforce client ID
     * @param clientSecret  Salesforce client secret
     * @param instanceUrl   optional Salesforce custom domain (Client Credentials)
     * @param userName      Salesforce username (Password Grant)
     * @param password      Salesforce password (Password Grant)
     * @param securityToken Salesforce security token (Password Grant)
     * @param isSandbox     whether to use the sandbox environment
     * @param routingUrl    the callback routing URL
     */
    public SalesforceCustomerServiceAttributes(String clientId, String clientSecret, String instanceUrl, String userName, String password,
                                               String securityToken, Boolean isSandbox, String routingUrl) {
        this(clientId, clientSecret, instanceUrl, userName, password, securityToken, isSandbox, routingUrl, false, null);
    }

    /**
     * Overload that carries the Quick Setup admin email (CRIT-022).
     *
     * <p>Used by {@code SalesforceCustomerServiceAttributesStore.fromMap} when re-constructing
     * attrs from the persisted KV blob so the email round-trips correctly.</p>
     */
    public SalesforceCustomerServiceAttributes(String clientId, String clientSecret, String instanceUrl, String userName, String password,
                                               String securityToken, Boolean isSandbox, String routingUrl, String email) {
        this(clientId, clientSecret, instanceUrl, userName, password, securityToken, isSandbox, routingUrl, false, email);
    }

    /**
     * Full constructor including the {@code publicApp} sentinel and Quick Setup admin email.
     *
     * <p>Not intended for direct use by callers — use {@link #createPublic(Invoker, boolean)}
     * for Public-app instances and the other constructors for Advanced instances.</p>
     */
    private SalesforceCustomerServiceAttributes(String clientId, String clientSecret, String instanceUrl,
                                                String userName, String password, String securityToken,
                                                Boolean isSandbox, String routingUrl, boolean publicApp,
                                                String email) {
        // CRIT-021: outlook-4 pattern — when no operator credentials were supplied, fall back
        // to Krista's Public Connected App credentials from config.properties. This ensures
        // attrs constructed via HK2 injection after a Quick Setup save (where the saved KV blob
        // has clientId=null) still have working credentials for every downstream consumer
        // (toSalesforceConfig, getOauthService, getRecordByQuery, …) without each call site
        // needing its own Quick-Setup detection logic. Mirrors OutlookAttributes:
        //   loadPublicConfig();
        //   this.clientId = clientId == null ? publicClientId : clientId;
        //   this.clientSecret = clientSecret == null ? publicClientSecret : clientSecret;
        // Also auto-sets the publicApp sentinel when we ACTUALLY swapped in Public creds, so
        // getCallbackEndPoint() correctly chooses the central URL on round-trip-loaded attrs.
        // Mirror outlook-4 exactly: trigger only on null (NOT empty strings). KV-round-tripped
        // Quick Setup attrs have clientId=null because the save path never put a CLIENT_ID
        // entry in the map; explicit empty strings are user-form artifacts and stay preserved.
        boolean clientIdMissing = clientId == null;
        boolean clientSecretMissing = clientSecret == null;
        boolean usedPublicFallback = false;
        if (clientIdMissing && clientSecretMissing) {
            try {
                String[] publicCreds = loadPublicConfig();
                clientId = publicCreds[0];
                clientSecret = publicCreds[1];
                usedPublicFallback = true;
                System.out.println("[SF-DIAG] SalesforceCustomerServiceAttributes ctor: no operator creds → " +
                        "loaded Public Connected App creds from config.properties (publicApp auto-set)");
            } catch (RuntimeException e) {
                // Quick Setup is misconfigured / config.properties missing — leave null fields.
                // Downstream code will throw the existing "clientId is required" error with
                // operator-visible guidance.
                System.out.println("[SF-DIAG] SalesforceCustomerServiceAttributes ctor: no operator creds and " +
                        "Public fallback failed (" + e.getMessage() + ") — clientId/secret remain null");
            }
        }
        this.publicApp = publicApp || usedPublicFallback;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.instanceUrl = validateInstanceUrl(instanceUrl);
        this.userName = userName;
        this.password = password;
        this.securityToken = securityToken;
        this.isSandbox = isSandbox;
        this.routingUrl = routingUrl;
        this.email = email;
    }

    /**
     * Creates an instance using an {@link Invoker} and attributes map.
     *
     * @param invoker    the Krista extension {@link Invoker}
     * @param attributes the attributes map
     * @return a new {@link SalesforceCustomerServiceAttributes} instance
     */
    public static SalesforceCustomerServiceAttributes create(Invoker invoker, Map<String, Object> attributes) {
        return new SalesforceCustomerServiceAttributes(invoker.getRoutingInfo().getRoutingURL(
                HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE), attributes);
    }

    /**
     * Creates a Public-app-backed instance for the Quick Setup flow.
     *
     * <p>Loads {@code public.clientId} and {@code public.clientSecret} from
     * {@code config.properties} on the classpath. These are Krista's own Salesforce
     * Connected App credentials — they are NEVER stored in KV and NEVER returned to
     * the browser. The operator's saved credentials are irrelevant for this path.</p>
     *
     * <p>The sandbox flag is taken from the operator's Quick Setup configuration (stored
     * at save time). No other fields are loaded from the operator's saved attrs — the
     * Connected App itself determines scopes and policies.</p>
     *
     * <p>The {@link #getCallbackEndPoint()} of the returned instance returns the
     * platform-level central callback URL ({@link Constants#DEFAULT_CALLBACK_PATH}) rather
     * than the per-invoker callback. This matches the URL registered in Krista's Connected
     * App with Salesforce.</p>
     *
     * @param invoker   the current Krista Invoker (used to derive the routing base URL
     *                  for the platform-level callback path substitution)
     * @param isSandbox whether the operator chose the sandbox Salesforce org
     * @return a Public-app-backed attributes instance
     * @throws RuntimeException if {@code config.properties} cannot be loaded from the classpath
     */
    public static SalesforceCustomerServiceAttributes createPublic(Invoker invoker, boolean isSandbox) {
        return createPublic(invoker, isSandbox, null);
    }

    /**
     * Variant of {@link #createPublic(Invoker, boolean)} carrying the Quick Setup admin email
     * so the resulting attrs can use it as the operator {@code accountId} during Test Connection
     * and operator-context token acquisitions (CRIT-022).
     */
    public static SalesforceCustomerServiceAttributes createPublic(Invoker invoker, boolean isSandbox, String email) {
        String routingUrl = invoker.getRoutingInfo().getRoutingURL(
                HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        System.out.println("[SF-DIAG] createPublic: building Public-app attrs for Quick Setup, " +
                "sandbox=" + isSandbox + " email=" + email + " routingUrl=" + routingUrl);

        String[] publicCredentials = loadPublicConfig();
        String publicClientId     = publicCredentials[0];
        String publicClientSecret = publicCredentials[1];

        // Public-app instances: no instanceUrl, no username/password/securityToken.
        // The sandbox flag comes from the operator's Quick Setup choice.
        // The email is the Quick Setup admin identity (used as Test Connection accountId).
        return new SalesforceCustomerServiceAttributes(
                publicClientId,
                publicClientSecret,
                null,        // instanceUrl — not used for Auth Code
                null,        // userName — Quick Setup uses per-user browser auth
                null,        // password
                null,        // securityToken
                isSandbox,
                routingUrl,
                true,        // publicApp sentinel = true
                email
        );
    }

    /**
     * Loads the Public Connected App credentials from {@code config.properties} on the classpath.
     *
     * <p>Mirrors {@code OutlookAttributes.loadPublicConfig()} exactly. The properties file is
     * committed to the extension artifact and is never shipped to the browser or stored in KV.
     * Rotation is done by updating the file and redeploying the extension.</p>
     *
     * @return {@code [publicClientId, publicClientSecret]}
     * @throws RuntimeException if the file is absent or cannot be read
     */
    private static String[] loadPublicConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = SalesforceCustomerServiceAttributes.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            if (inputStream == null) {
                throw new RuntimeException(
                        "config.properties not found on classpath. The Salesforce Quick Setup " +
                        "feature requires Krista's Connected App credentials to be bundled in " +
                        "the extension artifact. Please ensure config.properties exists in " +
                        "src/main/resources/ and the extension was built correctly.");
            }
            properties.load(inputStream);
            String clientId = properties.getProperty("public.clientId");
            String clientSecret = properties.getProperty("public.clientSecret");
            if (clientId == null || clientId.isBlank()) {
                throw new RuntimeException("config.properties is missing 'public.clientId'.");
            }
            if (clientSecret == null || clientSecret.isBlank()) {
                throw new RuntimeException("config.properties is missing 'public.clientSecret'.");
            }
            return new String[]{clientId, clientSecret};
        } catch (IOException cause) {
            throw new RuntimeException(
                    "Failed to load config.properties required for Quick Setup (Public App) " +
                    "authentication. Please verify that the configuration file exists and is " +
                    "accessible, or contact your administrator for assistance.", cause);
        }
    }

    /**
     * Creates a Public-app-backed instance when the routing URL is already known (i.e. the
     * caller has already resolved it from an existing attributes instance rather than from
     * an Invoker injection point).
     *
     * <p>This factory variant is used by {@code SalesforceTokenProvider.chooseEffectiveAttributes()}
     * where HK2-injected {@code Invoker} is not available but the routing URL can be extracted
     * from the already-constructed {@code salesforceCustomerServiceAttributes} field.
     * It is functionally identical to {@link #createPublic(Invoker, boolean)} — the only
     * difference is the source of the routing URL.</p>
     *
     * @param routingUrl the routing base URL (the part before {@code /rest/...})
     * @param isSandbox  whether the operator chose the sandbox Salesforce org
     * @return a Public-app-backed attributes instance
     */
    public static SalesforceCustomerServiceAttributes createPublicFromRoutingUrl(
            String routingUrl, boolean isSandbox) {
        return createPublicFromRoutingUrl(routingUrl, isSandbox, null);
    }

    /**
     * Variant of {@link #createPublicFromRoutingUrl(String, boolean)} that also carries the
     * Quick Setup admin email (CRIT-022).
     */
    public static SalesforceCustomerServiceAttributes createPublicFromRoutingUrl(
            String routingUrl, boolean isSandbox, String email) {
        System.out.println("[SF-DIAG] createPublicFromRoutingUrl: building Public-app attrs, " +
                "sandbox=" + isSandbox + " email=" + email + " routingUrl=" + routingUrl);
        String[] creds = loadPublicConfig();
        return new SalesforceCustomerServiceAttributes(
                creds[0], creds[1],
                null, null, null, null,
                isSandbox, routingUrl,
                true,  // publicApp sentinel
                email
        );
    }

    /**
     * Returns whether this instance is backed by Krista's Public Connected App credentials
     * (Quick Setup flow) vs. the operator's own Connected App credentials (Advanced flows).
     *
     * @return {@code true} for Quick Setup / Public-app instances
     */
    public boolean isPublicApp() {
        return publicApp;
    }

    /**
     * @return the Quick Setup admin email — used as the {@code accountId} for the operator
     *         Test Connection RT in the Public-app flow (CRIT-022). {@code null} for Advanced flows.
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the Salesforce client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @return the Salesforce client secret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @return the Salesforce instance URL
     */
    public String getInstanceUrl() {
        return instanceUrl;
    }

    /**
     * @return the Salesforce username
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return the Salesforce password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the Salesforce security token
     */
    public String getSecurityToken() {
        return securityToken;
    }

    /**
     * Returns the per-invoker callback URL (Advanced flows only).
     *
     * <p>This is the URL registered in the customer's own Salesforce Connected App for
     * AUTH_CODE, CLIENT_CREDENTIALS, and PASSWORD_GRANT flows. It encodes the invoker's
     * specific routing URL so the Krista platform can route the callback to the correct
     * extension instance.</p>
     *
     * <p>For Quick Setup (Public App) flows, use {@link #getCallbackEndPoint()} instead,
     * which returns the Krista-platform central callback URL.</p>
     *
     * @return the per-invoker OAuth callback URL
     */
    public String getCallbackUrl() {
        return routingUrl + Constants.CALLBACK_URI;
    }

    /**
     * Implements {@link SalesforceAttributes#getCallbackEndPoint()} — the SDK contract for
     * the fully-qualified OAuth callback URL placed in the authorize and token requests.
     *
     * <h2>Public App (Quick Setup)</h2>
     *
     * <p>When this instance is backed by Krista's Public Connected App ({@link #isPublicApp()}
     * returns {@code true}), this returns the Krista-platform central callback URL
     * ({@link Constants#DEFAULT_CALLBACK_PATH}). The platform-level central callback is the URL
     * registered in Krista's own Salesforce Connected App and is shared across all Quick Setup
     * customers. The routingUrl regex strip mirrors {@code OutlookAttributes.getCallbackEndPoint()}:
     * {@code routingUrl.replaceAll("/extension/api/.*", DEFAULT_CALLBACK_PATH)}.</p>
     *
     * <h2>Private App (Advanced flows)</h2>
     *
     * <p>When this instance carries customer-supplied credentials ({@code publicApp=false}),
     * this delegates to {@link #getCallbackUrl()} — the per-invoker callback that the customer
     * registered in their own Connected App.</p>
     */
    @Override
    public String getCallbackEndPoint() {
        if (publicApp) {
            // Public: use the Krista-platform central callback — same URL across all tenants.
            // Regex mirrors OutlookAttributes.getCallbackEndPoint():
            //   routingUrl.replaceAll("/extension/api/.*", DEFAULT_CALLBACK_PATH)
            // This strips the per-invoker path segment (e.g. /extension/api/<invokerToken>/)
            // down to the platform base, then appends the central callback path.
            String central = routingUrl.replaceAll("/extension/api/.*", Constants.DEFAULT_CALLBACK_PATH);
            System.out.println("[SF-DIAG] getCallbackEndPoint [PUBLIC]: central=" + central);
            return central;
        }
        // Private / Advanced: per-invoker callback registered in the customer's Connected App.
        return getCallbackUrl();
    }

    /**
     * Returns the per-extension v3 forward path for Quick Setup callbacks.
     *
     * <p>The Krista-platform central callback handler (at {@link Constants#DEFAULT_CALLBACK_PATH})
     * receives the OAuth code from Salesforce and, using the OAuth state parameter, determines
     * which extension instance should handle the code exchange. It then forwards the
     * {@code code} + {@code state} to the per-extension v3 endpoint, which is this path
     * appended to the invoker's routing base URL.</p>
     *
     * <p>Returns an empty string for Private / Advanced instances — those use the legacy
     * per-invoker callback ({@link #getCallbackUrl()}) directly and have no forwarding step.</p>
     *
     * @return the fully-qualified v3 forward URL for Public App instances, or empty string
     */
    public String getForwardPath() {
        if (publicApp) {
            String forwardPath = routingUrl + Constants.EXTENSION_FORWARD_PATH;
            System.out.println("[SF-DIAG] getForwardPath [PUBLIC]: forwardPath=" + forwardPath);
            return forwardPath;
        }
        return "";
    }

    /**
     * Implements {@link SalesforceAttributes#getScopeString()} — space-separated OAuth 2.0
     * scope string sent on authorize and token requests.
     *
     * <p>The existing ScribeJava-based auth code in this extension did not pass an explicit
     * scope to {@code ServiceBuilder} (no {@code .scope(...)} call in {@link #getOauthService()}),
     * which means Salesforce applied the Connected App's default grant. The canonical Salesforce
     * default scope set — and the SDK's own default in {@code SalesforceConfig.Builder.scopes} —
     * is {@code "api refresh_token offline_access"}. Returning that string here preserves
     * the existing behaviour when the SDK strategies make authorize/token calls.</p>
     *
     * <p>NOTE (Chunk 3): If a specific Connected App was configured with narrower scopes (e.g.
     * {@code "api"} only), confirm with the operator that the full default set is acceptable,
     * or expose a configurable attribute for scopes.</p>
     */
    @Override
    public String getScopeString() {
        // Matches SalesforceConfig.Builder default and the pre-SDK ScribeJava implicit default.
        return "api refresh_token offline_access";
    }

    /**
     * Implements {@link SalesforceAttributes#getJwtPrivateKeyPem()}.
     *
     * <p>This extension does not use the Salesforce JWT Bearer flow. Returning {@code null}
     * tells the SDK routing matrix ({@code SalesforceStrategyFactory}) that no JWT credential
     * material is present, so the JWT Bearer strategy is never selected.</p>
     */
    @Override
    public String getJwtPrivateKeyPem() {
        return null;
    }

    /**
     * Implements {@link SalesforceAttributes#getJwtUser()}.
     *
     * <p>This extension does not use the Salesforce JWT Bearer flow. See
     * {@link #getJwtPrivateKeyPem()} for the rationale.</p>
     */
    @Override
    public String getJwtUser() {
        return null;
    }

    /**
     * Override of the default {@code toSalesforceConfig()} from {@link SalesforceAttributes}.
     *
     * <p><b>Why this override exists:</b> The default builder enables PKCE
     * ({@code usePkce=true}) per the SDK's C4 invariant. The existing
     * {@code salesforce_customer_service} extension was built BEFORE PKCE adoption,
     * so the customers' existing Salesforce Connected Apps are configured as
     * "Web Server Apps" without PKCE support. Letting PKCE default to TRUE would
     * cause Salesforce to reject the OAuth callback with a "PKCE required but not
     * provided" error.</p>
     *
     * <p><b>Why scopes is set to null:</b> The previous (pre-SDK) version of this
     * extension used ScribeJava's {@code ServiceBuilder} without calling {@code .defaultScope()}
     * and invoked {@code oAuthService.getAuthorizationUrl(state)} plain — meaning NO scope
     * parameter was ever added to the Salesforce authorize URL. The Salesforce Connected App
     * grants {@code refresh_token} by default based on its own configuration, without needing
     * an explicit {@code scope=} on the URL. Setting {@code scopes(null)} here preserves that
     * exact behavior: the SDK's {@code SalesforceAuthCodeStrategy.buildAuthorizationUrl()} skips
     * the manual {@code &scope=...} append when {@code config.getScopes()} is null, matching
     * the previous version identically. Do NOT pass a non-null scopes string unless the
     * Connected App has been confirmed to NOT grant {@code refresh_token} by default — doing so
     * risks {@code invalid_request: scope parameter not supported} errors on token exchange
     * if ScribeJava ever forwards the scope to the token endpoint.</p>
     *
     * <p><b>Migration path for customers wanting to enable PKCE:</b></p>
     * <ol>
     *   <li>Update the Salesforce Connected App configuration to a "Web Server App
     *       with PKCE" type (recommended; Salesforce 2026 ECA mandates this for
     *       new ISV apps).</li>
     *   <li>Change {@code .usePkce(false)} below to {@code .usePkce(true)}.</li>
     *   <li>Redeploy this extension.</li>
     * </ol>
     *
     * <p>All other config values come from the existing per-attribute getters.</p>
     *
     * @since salesforce_customer_service 2.0.18 (microsoft-auth-sdk 1.1.0 migration)
     */
    @Override
    public SalesforceConfig toSalesforceConfig() {
        return SalesforceConfig.builder()
                .clientId(getClientId())
                .clientSecret(getClientSecret())
                .callbackUrl(getCallbackEndPoint())
                // null = no scope on authorize URL — matches the previous version which
                // used ServiceBuilder with no .defaultScope() and called
                // getAuthorizationUrl(state) without any scope parameter. Salesforce's
                // Connected App grants refresh_token by default from its own settings.
                .scopes(null)
                .instanceUrl(getInstanceUrl())
                .sandbox(isSandbox())
                .userName(getUserName())
                .password(getPassword())
                .securityToken(getSecurityToken())
                .jwtPrivateKeyPem(getJwtPrivateKeyPem())
                .jwtUser(getJwtUser())
                .usePkce(false)   // OVERRIDE: preserve pre-SDK Connected App compatibility
                .build();
    }

    /**
     * @return {@code true} if using sandbox, {@code false} otherwise
     */
    public boolean isSandbox() {
        return isSandbox != null && isSandbox;
    }

    /**
     * Lazily initializes and returns the {@link OAuth20Service} for authentication.
     *
     * <p>This method creates an OAuth service for standard Salesforce authentication
     * using either production or sandbox endpoints based on the {@link #isSandbox()} setting.</p>
     *
     * @return the configured {@link OAuth20Service}
     * @throws IllegalStateException if the OAuth service cannot be created due to invalid configuration
     * @throws IllegalArgumentException if required configuration parameters are missing or invalid
     */
    public synchronized OAuth20Service getOauthService() {
        if (oAuthService == null) {
            try {
                validateOAuthConfiguration();

                oAuthService = new ServiceBuilder(getClientId())
                        .apiSecret(getClientSecret())
                        .callback(getCallbackUrl())
                        .build(isSandbox() ? SalesforceApi.sandbox() : SalesforceApi.instance());

            } catch (Exception e) {
                throw new IllegalStateException(Constants.OAUTH_SERVICE_CREATION_ERROR, e);
            }
        }
        return oAuthService;
    }

    /**
     * Lazily initializes and returns the {@link OAuth20Service} for authentication with a custom domain.
     *
     * <p>This method creates an OAuth service for Client Credentials flow authentication
     * using a custom Salesforce instance domain URL.</p>
     *
     * @param domainUrl the custom Salesforce instance domain URL
     * @return the configured {@link OAuth20Service}
     * @throws IllegalStateException if the OAuth service cannot be created due to invalid configuration
     * @throws IllegalArgumentException if the domain URL or other required parameters are invalid
     */
    public synchronized OAuth20Service getOauthService(String domainUrl) {
        if (oAuthService == null) {
            try {
                validateOAuthConfiguration();

                if (domainUrl == null || domainUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException(Constants.DOMAIN_URL_REQUIRED);
                }

                oAuthService = new ServiceBuilder(getClientId())
                        .apiSecret(getClientSecret())
                        .callback(getCallbackUrl())
                        .build(new SalesforceCustomApi(domainUrl));

            } catch (IllegalArgumentException e) {
                // Re-throw validation errors as-is
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(Constants.OAUTH_SERVICE_CUSTOM_DOMAIN_ERROR + domainUrl +
                        Constants.OAUTH_SERVICE_VERIFY_CONFIG, e);
            }
        }
        return oAuthService;
    }

    /**
     * Updates the current configuration with new attribute values.
     *
     * @param newAttributes the updated attributes map
     */
    public void update(Map<String, Object> newAttributes) {
        clientId = InvokerAttributes.getStringOrNull(newAttributes, CLIENT_ID);
        clientSecret = InvokerAttributes.getStringOrNull(newAttributes, CLIENT_SECRET);
        instanceUrl = validateInstanceUrl(InvokerAttributes.getStringOrNull(newAttributes, INSTANCE_URL));
        userName = InvokerAttributes.getStringOrNull(newAttributes, USER_NAME);
        password = InvokerAttributes.getStringOrNull(newAttributes, PASS_WORD);
        securityToken = InvokerAttributes.getStringOrNull(newAttributes, SECURITY_TOKEN);
        isSandbox = InvokerAttributes.getBooleanOrNull(newAttributes, SANDBOX);
        email = InvokerAttributes.getStringOrNull(newAttributes, EMAIL);
    }

    /**
     * Validates the Instance URL format.
     *
     * @param instanceUrl the Instance URL to validate
     * @return the validated Instance URL, or null if input is null/empty
     * @throws IllegalArgumentException if the Instance URL format is invalid
     */
    private String validateInstanceUrl(String instanceUrl) {
        if (instanceUrl == null || instanceUrl.trim().isEmpty()) {
            return instanceUrl; // Allow null or empty values
        }

        try {
            URL url = URI.create(instanceUrl.trim()).toURL();

            // Validate protocol
            String protocol = url.getProtocol();
            if (!"https".equals(protocol) && !"http".equals(protocol)) {
                throw new IllegalArgumentException(Constants.INSTANCE_URL_PROTOCOL_ERROR + protocol);
            }

            // Validate host
            String host = url.getHost();
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException(Constants.INSTANCE_URL_HOST_ERROR);
            }

            // Validate that it looks like a Salesforce domain (basic check)
            if (!host.contains("salesforce.com") && !host.contains("force.com") &&
                !host.contains("my.salesforce.com") && !host.contains("lightning.force.com")) {
                // Allow any domain for flexibility, but log a warning for non-Salesforce domains
                // This is a soft validation - we don't throw an exception
            }

            return instanceUrl.trim();

        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException(Constants.INSTANCE_URL_FORMAT_ERROR + instanceUrl + ". " + e.getMessage(), e);
        }
    }

    /**
     * Validates the OAuth configuration to ensure all required parameters are present and valid.
     *
     * @throws IllegalArgumentException if any required OAuth configuration is missing or invalid
     */
    private void validateOAuthConfiguration() {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException(Constants.CLIENT_ID_REQUIRED);
        }

        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalArgumentException(Constants.CLIENT_SECRET_REQUIRED);
        }

        // Validate routing URL first (required for callback URL generation)
        if (routingUrl == null || routingUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(Constants.ROUTING_URL_REQUIRED);
        }

        // Validate callback URL
        String callbackUrl = getCallbackUrl();
        if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(Constants.CALLBACK_URL_REQUIRED);
        }

        // Basic callback URL format validation
        if (!callbackUrl.startsWith("http://") && !callbackUrl.startsWith("https://")) {
            throw new IllegalArgumentException(Constants.CALLBACK_URL_FORMAT_ERROR + callbackUrl);
        }
    }

    /**
     * Validates the configuration for a specific authentication flow.
     *
     * @param flowType the authentication flow type to validate for
     * @return ValidationResult containing validation status and any error messages
     */
    public ValidationResult validateConfiguration(AuthenticationFlowType flowType) {
        ValidationResult result = new ValidationResult();

        try {
            switch (flowType) {
                case CLIENT_CREDENTIALS:
                    validateClientCredentialsFlow(result);
                    break;
                case AUTHORIZATION_CODE:
                    validateAuthorizationCodeFlow(result);
                    break;
                case PASSWORD_GRANT:
                    validatePasswordGrantFlow(result);
                    break;
                case AUTO_DETECT:
                    validateAutoDetectFlow(result);
                    break;
                default:
                    result.addError("Unknown authentication flow type: " + flowType);
            }
        } catch (Exception e) {
            result.addError("Validation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Validates configuration for OAuth 2.0 Client Credentials flow.
     * Requires: Client ID, Client Secret, Instance URL, Routing URL
     */
    private void validateClientCredentialsFlow(ValidationResult result) {
        // Basic OAuth requirements
        if (clientId == null || clientId.trim().isEmpty()) {
            result.addError(Constants.CLIENT_ID_REQUIRED);
        }

        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            result.addError(Constants.CLIENT_SECRET_REQUIRED);
        }

        if (routingUrl == null || routingUrl.trim().isEmpty()) {
            result.addError(Constants.ROUTING_URL_REQUIRED);
        }

        // Instance URL is required for Client Credentials flow
        if (instanceUrl == null || instanceUrl.trim().isEmpty()) {
            result.addError("Instance URL is required for Client Credentials flow");
        } else {
            try {
                validateInstanceUrl(instanceUrl);
            } catch (IllegalArgumentException e) {
                result.addError("Instance URL validation failed: " + e.getMessage());
            }
        }

        // Validate callback URL if routing URL is present
        if (routingUrl != null && !routingUrl.trim().isEmpty()) {
            try {
                String callbackUrl = getCallbackUrl();
                if (!callbackUrl.startsWith("http://") && !callbackUrl.startsWith("https://")) {
                    result.addError(Constants.CALLBACK_URL_FORMAT_ERROR + callbackUrl);
                }
            } catch (Exception e) {
                result.addError("Callback URL generation failed: " + e.getMessage());
            }
        }

        // Username/password should not be required for Client Credentials
        if ((userName != null && !userName.trim().isEmpty()) ||
            (password != null && !password.trim().isEmpty()) ||
            (securityToken != null && !securityToken.trim().isEmpty())) {
            result.addWarning("Username/password credentials are not needed for Client Credentials flow and will be ignored");
        }
    }

    /**
     * Validates configuration for OAuth 2.0 Authorization Code flow.
     * Requires: Client ID, Client Secret, Routing URL (no Instance URL, no username/password)
     */
    private void validateAuthorizationCodeFlow(ValidationResult result) {
        // Basic OAuth requirements
        if (clientId == null || clientId.trim().isEmpty()) {
            result.addError(Constants.CLIENT_ID_REQUIRED);
        }

        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            result.addError(Constants.CLIENT_SECRET_REQUIRED);
        }

        if (routingUrl == null || routingUrl.trim().isEmpty()) {
            result.addError(Constants.ROUTING_URL_REQUIRED);
        }

        // Instance URL should be empty for Authorization Code flow
        if (instanceUrl != null && !instanceUrl.trim().isEmpty()) {
            result.addWarning("Instance URL is not needed for Authorization Code flow and will be ignored");
        }

        // Username/password should not be provided for Authorization Code flow
        if ((userName != null && !userName.trim().isEmpty()) ||
            (password != null && !password.trim().isEmpty()) ||
            (securityToken != null && !securityToken.trim().isEmpty())) {
            result.addWarning("Username/password credentials are not needed for Authorization Code flow and will be ignored");
        }

        // Validate callback URL
        if (routingUrl != null && !routingUrl.trim().isEmpty()) {
            try {
                String callbackUrl = getCallbackUrl();
                if (!callbackUrl.startsWith("http://") && !callbackUrl.startsWith("https://")) {
                    result.addError(Constants.CALLBACK_URL_FORMAT_ERROR + callbackUrl);
                }
            } catch (Exception e) {
                result.addError("Callback URL generation failed: " + e.getMessage());
            }
        }
    }

    /**
     * Validates configuration for OAuth 2.0 Password Grant flow.
     * Requires: Client ID, Client Secret, Username, Password, Security Token, Routing URL
     */
    private void validatePasswordGrantFlow(ValidationResult result) {
        // Basic OAuth requirements
        if (clientId == null || clientId.trim().isEmpty()) {
            result.addError(Constants.CLIENT_ID_REQUIRED);
        }

        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            result.addError(Constants.CLIENT_SECRET_REQUIRED);
        }

        if (routingUrl == null || routingUrl.trim().isEmpty()) {
            result.addError(Constants.ROUTING_URL_REQUIRED);
        }

        // Username/password credentials are required for Password Grant flow
        if (userName == null || userName.trim().isEmpty()) {
            result.addError("Username is required for Password Grant flow");
        }

        if (password == null || password.trim().isEmpty()) {
            result.addError("Password is required for Password Grant flow");
        }

        if (securityToken == null || securityToken.trim().isEmpty()) {
            result.addError("Security Token is required for Password Grant flow");
        }

        // Instance URL should be empty for Password Grant flow (uses standard Salesforce endpoints)
        if (instanceUrl != null && !instanceUrl.trim().isEmpty()) {
            result.addWarning("Instance URL is not needed for Password Grant flow and will be ignored");
        }

        // Validate callback URL
        if (routingUrl != null && !routingUrl.trim().isEmpty()) {
            try {
                String callbackUrl = getCallbackUrl();
                if (!callbackUrl.startsWith("http://") && !callbackUrl.startsWith("https://")) {
                    result.addError(Constants.CALLBACK_URL_FORMAT_ERROR + callbackUrl);
                }
            } catch (Exception e) {
                result.addError("Callback URL generation failed: " + e.getMessage());
            }
        }
    }

    /**
     * Validates configuration for auto-detection of authentication flow.
     * Checks which flows are possible with the current configuration.
     */
    private void validateAutoDetectFlow(ValidationResult result) {
        boolean canUseClientCredentials = true;
        boolean canUseAuthorizationCode = true;
        boolean canUsePasswordGrant = true;

        // Check basic OAuth requirements (needed for all flows)
        if (clientId == null || clientId.trim().isEmpty()) {
            result.addError(Constants.CLIENT_ID_REQUIRED);
            canUseClientCredentials = canUseAuthorizationCode = canUsePasswordGrant = false;
        }

        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            result.addError(Constants.CLIENT_SECRET_REQUIRED);
            canUseClientCredentials = canUseAuthorizationCode = canUsePasswordGrant = false;
        }

        if (routingUrl == null || routingUrl.trim().isEmpty()) {
            result.addError(Constants.ROUTING_URL_REQUIRED);
            canUseClientCredentials = canUseAuthorizationCode = canUsePasswordGrant = false;
        }

        // Check Client Credentials flow requirements
        if (instanceUrl == null || instanceUrl.trim().isEmpty()) {
            canUseClientCredentials = false;
        } else {
            try {
                validateInstanceUrl(instanceUrl);
            } catch (IllegalArgumentException e) {
                canUseClientCredentials = false;
                result.addWarning("Client Credentials flow not available: " + e.getMessage());
            }
        }

        // Check Password Grant flow requirements
        if (userName == null || userName.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            securityToken == null || securityToken.trim().isEmpty()) {
            canUsePasswordGrant = false;
        }

        // Report available flows
        if (canUseClientCredentials) {
            result.addInfo("Client Credentials flow is available (Instance URL provided)");
        }
        if (canUseAuthorizationCode) {
            result.addInfo("Authorization Code flow is available");
        }
        if (canUsePasswordGrant) {
            result.addInfo("Password Grant flow is available (username/password provided)");
        }

        // Determine which flow will be selected
        if (canUseClientCredentials) {
            result.addInfo("Auto-detection will select: Client Credentials flow");
        } else if (canUseAuthorizationCode) {
            result.addInfo("Auto-detection will select: Authorization Code flow");
        } else if (canUsePasswordGrant) {
            result.addInfo("Auto-detection will select: Password Grant flow");
        } else {
            result.addError("No authentication flow is available with current configuration");
        }
    }

    /**
     * Enumeration of supported authentication flow types.
     */
    public enum AuthenticationFlowType {
        CLIENT_CREDENTIALS,
        AUTHORIZATION_CODE,
        PASSWORD_GRANT,
        AUTO_DETECT
    }

    /**
     * Result of configuration validation containing errors, warnings, and info messages.
     */
    public static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();
        private final java.util.List<String> info = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public void addInfo(String info) {
            this.info.add(info);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public java.util.List<String> getErrors() {
            return new java.util.ArrayList<>(errors);
        }

        public java.util.List<String> getWarnings() {
            return new java.util.ArrayList<>(warnings);
        }

        public java.util.List<String> getInfo() {
            return new java.util.ArrayList<>(info);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public boolean hasInfo() {
            return !info.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ValidationResult{");
            sb.append("valid=").append(isValid());
            if (!errors.isEmpty()) {
                sb.append(", errors=").append(errors);
            }
            if (!warnings.isEmpty()) {
                sb.append(", warnings=").append(warnings);
            }
            if (!info.isEmpty()) {
                sb.append(", info=").append(info);
            }
            sb.append('}');
            return sb.toString();
        }
    }
}
