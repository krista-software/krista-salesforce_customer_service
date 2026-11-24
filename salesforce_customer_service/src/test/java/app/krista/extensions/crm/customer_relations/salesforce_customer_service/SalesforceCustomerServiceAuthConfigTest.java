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
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SalesforceCustomerServiceAuthConfig}.
 *
 * <p>This class is annotation-only — the test verifies that the metadata the SDK's
 * {@code OAuth2ExtensionScanner} reads at startup is correct. A wrong value here
 * silently breaks Auth UI rendering, scope requests, or auth-flow selection.</p>
 *
 * <h2>What's verified</h2>
 * <ul>
 *   <li>Class is {@code final}, has a single private no-arg ctor — annotation-only marker.</li>
 *   <li>{@link SalesforceExtension} annotation is present with the expected wire values
 *       (extensionType, callbackPathPrefix, supportedFlows, usePkce off, allowLegacyPasswordGrant,
 *       headers, audit-dashboard flag).</li>
 *   <li>The four expected {@link AuthScope} declarations exist: {@code api}, {@code refresh_token},
 *       {@code offline_access}, {@code content}.</li>
 *   <li>The two expected {@link CustomProperty} declarations exist: {@code sandbox},
 *       {@code Instance URL}.</li>
 * </ul>
 */
public class SalesforceCustomerServiceAuthConfigTest {

    private static final Class<?> TARGET = SalesforceCustomerServiceAuthConfig.class;

    // ── Class shape ─────────────────────────────────────────────────────────────────

    @Test
    public void class_isFinal() {
        assertTrue("Annotation-only marker class must be final",
                Modifier.isFinal(TARGET.getModifiers()));
    }

    @Test
    public void class_hasOnlyPrivateNoArgConstructor() {
        Constructor<?>[] ctors = TARGET.getDeclaredConstructors();
        assertEquals("Annotation-only marker class must have exactly one constructor",
                1, ctors.length);
        Constructor<?> ctor = ctors[0];
        assertEquals("Constructor must be no-arg", 0, ctor.getParameterCount());
        assertTrue("Constructor must be private — class is annotation-only",
                Modifier.isPrivate(ctor.getModifiers()));
    }

    // ── @SalesforceExtension ────────────────────────────────────────────────────────

    @Test
    public void salesforceExtensionAnnotation_isPresent() {
        assertNotNull("@SalesforceExtension must be present so the SDK scanner discovers this class",
                TARGET.getAnnotation(SalesforceExtension.class));
    }

    @Test
    public void salesforceExtensionAnnotation_hasExpectedWireValues() {
        SalesforceExtension ann = TARGET.getAnnotation(SalesforceExtension.class);
        assertNotNull(ann);

        assertEquals("salesforce-customer-service", ann.extensionType());
        assertEquals("/salesforcecustomerservice", ann.callbackPathPrefix());
        assertFalse("PKCE must remain off — legacy Connected Apps don't speak PKCE",
                ann.usePkce());
        assertTrue("allowLegacyPasswordGrant must be true — this extension opts in",
                ann.allowLegacyPasswordGrant());
        assertTrue("supportsOwnCredentials must be true so the Auth UI exposes the " +
                        "customer Connected-App fields",
                ann.supportsOwnCredentials());
        assertTrue("enableAuditDashboard was flipped on in Chunk M3 — keep it on",
                ann.enableAuditDashboard());
        assertEquals("Salesforce Customer Service", ann.headerTitle());
        assertEquals("Connect Krista to Salesforce", ann.headerSubtitle());
    }

    @Test
    public void salesforceExtensionAnnotation_supportedFlows_includesAllFourFlows() {
        SalesforceExtension ann = TARGET.getAnnotation(SalesforceExtension.class);
        Set<SalesforceAuthFlow> flows = new HashSet<>(Arrays.asList(ann.supportedFlows()));

        assertTrue("Quick Setup flow must be supported", flows.contains(SalesforceAuthFlow.AUTH_CODE_QUICK));
        assertTrue("User Sign-In (Authorization Code) must be supported",
                flows.contains(SalesforceAuthFlow.AUTH_CODE));
        assertTrue("Server-to-Server (Client Credentials) must be supported",
                flows.contains(SalesforceAuthFlow.CLIENT_CREDENTIALS));
        assertTrue("Password Grant (legacy) must be supported",
                flows.contains(SalesforceAuthFlow.PASSWORD_GRANT));
        assertEquals("Exactly four flows are supported — no more, no less", 4, flows.size());
    }

    // ── @AuthScope (repeatable) ─────────────────────────────────────────────────────

    @Test
    public void authScopes_includeAllExpectedValues() {
        AuthScope[] scopes = TARGET.getAnnotationsByType(AuthScope.class);
        Set<String> scopeValues = Arrays.stream(scopes)
                .map(AuthScope::value)
                .collect(Collectors.toSet());

        assertTrue("Must declare 'api' scope",            scopeValues.contains("api"));
        assertTrue("Must declare 'refresh_token' scope",  scopeValues.contains("refresh_token"));
        assertTrue("Must declare 'offline_access' scope", scopeValues.contains("offline_access"));
        assertTrue("Must declare 'content' scope (used for file attachments)",
                scopeValues.contains("content"));
        assertEquals("Exactly four scopes are declared", 4, scopes.length);
    }

    @Test
    public void authScope_api_isRequired() {
        AuthScope api = findScope("api");
        assertTrue("'api' is the workhorse scope and must be required", api.required());
    }

    @Test
    public void authScope_content_isOptional() {
        AuthScope content = findScope("content");
        assertFalse("'content' is only used for file attachments — must remain optional",
                content.required());
    }

    private AuthScope findScope(String value) {
        for (AuthScope s : TARGET.getAnnotationsByType(AuthScope.class)) {
            if (value.equals(s.value())) {
                return s;
            }
        }
        fail("Scope '" + value + "' not declared");
        return null; // unreachable
    }

    // ── @CustomProperty (repeatable) ────────────────────────────────────────────────

    @Test
    public void customProperties_includeSandboxAndInstanceUrl() {
        CustomProperty[] props = TARGET.getAnnotationsByType(CustomProperty.class);
        Set<String> keys = Arrays.stream(props)
                .map(CustomProperty::key)
                .collect(Collectors.toSet());

        assertTrue("Auth UI must expose the 'sandbox' toggle", keys.contains("sandbox"));
        assertTrue("Auth UI must expose the 'Instance URL' field for CC flow",
                keys.contains("Instance URL"));
    }

    @Test
    public void customProperty_sandbox_isBooleanWithDefaultFalse() {
        CustomProperty sandbox = findProperty("sandbox");
        assertEquals(PropertyType.BOOLEAN, sandbox.type());
        assertEquals("false", sandbox.defaultValue());
        assertFalse("sandbox is optional", sandbox.required());
    }

    @Test
    public void customProperty_instanceUrl_isUrlTypeAndOptional() {
        CustomProperty instanceUrl = findProperty("Instance URL");
        assertEquals(PropertyType.URL, instanceUrl.type());
        assertFalse("Instance URL is only used for CC flow — must remain optional",
                instanceUrl.required());
    }

    private CustomProperty findProperty(String key) {
        for (CustomProperty p : TARGET.getAnnotationsByType(CustomProperty.class)) {
            if (key.equals(p.key())) {
                return p;
            }
        }
        fail("CustomProperty '" + key + "' not declared");
        return null; // unreachable
    }
}
