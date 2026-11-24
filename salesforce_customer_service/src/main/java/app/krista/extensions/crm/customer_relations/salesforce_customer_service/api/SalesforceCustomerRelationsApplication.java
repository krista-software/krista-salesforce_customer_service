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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAuthConfig;
import app.krista.oauth.core.extension.OAuth2ConfigResource;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.List;
import java.util.Set;


/**
 * JAX-RS application for the Salesforce Customer Service extension.
 *
 * <p>Registers the following resources:</p>
 * <ul>
 *   <li>{@link AuthenticationResource} — OAuth callback ({@code /callback}),
 *       themed auth page ({@code /authentication/}), and the {@code /casechanged}
 *       webhook.</li>
 *   <li>{@link SalesforceCustomerServiceAuditResource} — audit dashboard HTML
 *       ({@code /audit/}), audit data ({@code /audit/data}), health check
 *       ({@code /audit/health}), archive management, and global counter endpoints.
 *       Registered as an HK2-managed class so the Krista runtime injects its
 *       dependencies ({@code Invoker}, {@code AuditService}, {@code Attributes},
 *       {@code SdkFactory}).</li>
 *   <li>{@code OAuth2ConfigResource} — SDK-provided resource that serves
 *       {@code /oauth/config?providerName=salesforce-platform} (theme + extension
 *       metadata JSON) and {@code /oauth/themes/salesforce-platform/logo.svg}. It is
 *       constructed with {@link SalesforceCustomerServiceAuthConfig} so the scanner
 *       indexes the correct extension metadata for the config-fetch the auth tab's
 *       {@code app.js} performs at runtime.</li>
 * </ul>
 */
@Service
@ApplicationPath("/")
@ContractsProvided(Application.class)
public class SalesforceCustomerRelationsApplication extends Application {

    /**
     * The shared {@code OAuth2ConfigResource} instance, constructed once with the
     * extension's AuthConfig class for annotation scanning.
     *
     * <p>{@code OAuth2ConfigResource} is thread-safe (immutable after construction)
     * so a single application-scope instance is correct here — HK2 Application
     * resources are singletons within a deployment unit.</p>
     */
    private static final OAuth2ConfigResource OAUTH2_CONFIG_RESOURCE =
            new OAuth2ConfigResource(List.of(SalesforceCustomerServiceAuthConfig.class));

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
            AuthenticationResource.class,
            SalesforceCustomerServiceAuditResource.class
        );
    }

    @Override
    public Set<Object> getSingletons() {
        return Set.of(OAUTH2_CONFIG_RESOURCE);
    }
}

