package app.krista.extensions.crm.customer_relations.salesforce_customer_service;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extension.util.InvokerAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.api.SalesforceCustomApi;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import com.github.scribejava.apis.SalesforceApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;

/**
 * Holds Salesforce configuration attributes used for OAuth and API integration.
 * Provides methods to initialize, access, and update Salesforce connection parameters.
 */
@Service
public final class SalesforceCustomerServiceAttributes {

    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String INSTANCE_URL = "Instance URL";
    public static final String USER_NAME = "username";
    public static final String PASS_WORD = "password";
    public static final String SECURITY_TOKEN = "securityToken";
    public static final String SANDBOX = "sandbox";

    private final String routingUrl;
    private String clientId;
    private String clientSecret;
    private String instanceUrl;
    private String userName;
    private String password;
    private String securityToken;
    private Boolean isSandbox;
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
                routingUrl);
    }

    /**
     * Constructs the attributes using individual configuration values.
     *
     * @param clientId      Salesforce client ID
     * @param clientSecret  Salesforce client secret
     * @param userName      Salesforce username
     * @param password      Salesforce password
     * @param securityToken Salesforce security token
     * @param isSandbox     whether to use the sandbox environment
     * @param routingUrl    the callback routing URL
     */
    public SalesforceCustomerServiceAttributes(String clientId, String clientSecret, String instanceUrl, String userName, String password,
                                               String securityToken, Boolean isSandbox, String routingUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.instanceUrl = instanceUrl;
        this.userName = userName;
        this.password = password;
        this.securityToken = securityToken;
        this.isSandbox = isSandbox;
        this.routingUrl = routingUrl;
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
     * @return the computed OAuth callback URL
     */
    public String getCallbackUrl() {
        return routingUrl + Constants.CALLBACK_URI;
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
     * @return the configured {@link OAuth20Service}
     */
    public synchronized OAuth20Service getOauthService() {
        if (oAuthService == null) {
            oAuthService = new ServiceBuilder(getClientId())
                    .apiSecret(getClientSecret())
                    .callback(getCallbackUrl())
                    .build(isSandbox() ? SalesforceApi.sandbox() : SalesforceApi.instance());
        }
        return oAuthService;
    }

    public synchronized OAuth20Service getOauthService(String domainUrl) {
        if (oAuthService == null) {
            oAuthService = new ServiceBuilder(getClientId())
                    .apiSecret(getClientSecret())
                    .callback(getCallbackUrl())
                    .build(new SalesforceCustomApi(domainUrl));
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
        instanceUrl = InvokerAttributes.getStringOrNull(newAttributes, INSTANCE_URL);
        userName = InvokerAttributes.getStringOrNull(newAttributes, USER_NAME);
        password = InvokerAttributes.getStringOrNull(newAttributes, PASS_WORD);
        securityToken = InvokerAttributes.getStringOrNull(newAttributes, SECURITY_TOKEN);
        isSandbox = InvokerAttributes.getBooleanOrNull(newAttributes, SANDBOX);
    }
}
