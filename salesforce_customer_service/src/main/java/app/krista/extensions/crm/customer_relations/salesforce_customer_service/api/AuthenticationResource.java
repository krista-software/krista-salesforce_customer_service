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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.RefreshTokenStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.XMlResponseParser;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.CaseNotifications;
import app.krista.extensions.util.EventHandler;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.model.base.FreeForm;
import com.github.scribejava.core.model.OAuth2AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.INVALID_STATE_PARAMETERS;
import static com.github.scribejava.core.model.OAuthConstants.CODE;
import static com.github.scribejava.core.model.OAuthConstants.STATE;

@Path("/salesforcecustomerservice")
public final class AuthenticationResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResource.class);
    private final RefreshTokenStore refreshTokenStore;
    private final SalesforceCustomerServiceAttributesStore attributesStore;
    private final SalesforceTokenProviderFactory providerFactory;
    private final AuthorizationContext context;
    private final AuthorizationListener authorizationListener;
    private final EventHandler eventHandler;


    @Inject
    public AuthenticationResource(RefreshTokenStore refreshTokenStore, SalesforceCustomerServiceAttributesStore attributesStore,
                                  SalesforceTokenProviderFactory providerFactory, AuthorizationContext context,
                                  AuthorizationListener authorizationListener, EventHandler eventHandler) {
        this.refreshTokenStore = refreshTokenStore;
        this.attributesStore = attributesStore;
        this.providerFactory = providerFactory;
        this.authorizationListener = authorizationListener;
        this.context = context;
        this.eventHandler = eventHandler;
    }

    @GET
    @Path("/callback")
    public String getCallBack(@QueryParam(CODE) String code, @QueryParam(STATE) String state) {
        Objects.requireNonNull(code);
        String[] parts = state.split("#");
        if (parts[0].isBlank() || parts.length > 2) {
            throw new BadRequestException(INVALID_STATE_PARAMETERS);
        }
        String key = parts[0];
        String authContextId = parts.length == 2 ? parts[1] : null;
        try {
            Objects.requireNonNull(code);
            SalesforceTokenProvider tokenProvider = getSalesforceTokenProvider(authContextId);
            OAuth2AccessToken accessToken = tokenProvider.getSalesforceCustomerServiceAttributes().getOauthService().getAccessToken(code);
            refreshTokenStore.put(key + "&" + tokenProvider.getSalesforceCustomerServiceAttributes().getClientId() +
                    tokenProvider.getSalesforceCustomerServiceAttributes().getClientSecret(), accessToken.getRefreshToken());
            if (context.isAuthenticated()) {
                authorizationListener.authorized();
                return Constants.USER_AUTHENTICATED_SUCCESSFULLY;
            }
            return Constants.USER_AUTHENTICATED_SUCCESSFULLY + Constants.SAVE_THE_CHANGES;
        } catch (IOException | ExecutionException cause) {
            LOGGER.error("Exception occurred during authorization", cause);
            throw new IllegalStateException("Error occurred during authorization", cause.getCause());
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            LOGGER.error("Authorization interrupted", cause);
            throw new IllegalStateException("Error occurred during authorization", cause.getCause());
        } finally {
            if (authContextId != null) {
                attributesStore.remove(authContextId);
            }
        }
    }

    /**
     * Returns a {@link SalesforceTokenProvider} based on the given authentication context ID.
     * If {@code authContextId} is {@code null}, returns the default provider.
     * If {@code authContextId} is blank, throws a {@link BadRequestException}.
     * Otherwise, loads the attributes for the given ID and creates a provider with them.
     *
     * @param authContextId the authentication context ID, or {@code null} for the default provider
     * @return a configured {@link SalesforceTokenProvider}
     * @throws BadRequestException if {@code authContextId} is blank
     */
    private SalesforceTokenProvider getSalesforceTokenProvider(String authContextId) {
        SalesforceTokenProvider tokenProvider;
        if (authContextId == null) {
            tokenProvider = providerFactory.create();
        } else if (authContextId.isBlank()) {
            throw new BadRequestException(INVALID_STATE_PARAMETERS);
        } else {
            SalesforceCustomerServiceAttributes salesforceSalesAttributes = attributesStore.load(authContextId);
            tokenProvider = providerFactory.create(salesforceSalesAttributes);
        }
        return tokenProvider;
    }

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
