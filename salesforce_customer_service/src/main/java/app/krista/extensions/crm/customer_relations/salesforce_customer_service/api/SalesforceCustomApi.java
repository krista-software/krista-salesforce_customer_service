package app.krista.extensions.crm.customer_relations.salesforce_customer_service.api;

import com.github.scribejava.core.builder.api.DefaultApi20;

public class SalesforceCustomApi extends DefaultApi20 {

    private final String domain;

    public SalesforceCustomApi(String domain) {
        this.domain = domain;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return domain + "/services/oauth2/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return domain + "/services/oauth2/authorize";
    }
}
