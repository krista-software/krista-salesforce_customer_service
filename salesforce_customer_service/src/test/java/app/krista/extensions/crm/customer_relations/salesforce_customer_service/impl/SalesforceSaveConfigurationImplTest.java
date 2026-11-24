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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesforceSaveConfigurationImpl}.
 *
 * <p>The class orchestrates the "Save Credentials" flow: parse the JSON payload into
 * {@link SalesforceCustomerServiceAttributes}, test the credentials via the injected
 * {@link SalesforceTestConnectionServiceImpl}, and — only on a passing test — persist
 * them via the {@link SalesforceCustomerServiceAttributesStore}.</p>
 *
 * <p>All three constructor dependencies are mocked. The {@code Invoker} supplies both the
 * {@code invokerId} (read in the constructor) and the routing URL used by
 * {@link SalesforceCustomerServiceAttributes#create} inside {@code attrsFromPayload}.</p>
 */
public class SalesforceSaveConfigurationImplTest {

    private static final String INVOKER_ID = "INV_test";
    private static final String ROUTING_URL = "https://test.krista.ai";

    private static final String SUCCESS_JSON = "{\"isSuccess\":true}";
    private static final String FAIL_WITH_MSG_JSON = "{\"isSuccess\":false,\"errorMessage\":\"bad creds\"}";
    private static final String FAIL_NO_MSG_JSON = "{\"isSuccess\":false}";

    @Mock
    private SalesforceCustomerServiceAttributesStore attributesStore;
    @Mock
    private Invoker invoker;
    @Mock
    private SalesforceTestConnectionServiceImpl testConnectionService;
    @Mock
    private RoutingInfo routingInfo;

    private SalesforceSaveConfigurationImpl saveConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(invoker.getInvokerId()).thenReturn(INVOKER_ID);
        // attrsFromPayload -> SalesforceCustomerServiceAttributes.create(invoker, map)
        // reads invoker.getRoutingInfo().getRoutingURL(...).
        when(invoker.getRoutingInfo()).thenReturn(routingInfo);
        when(routingInfo.getRoutingURL(any(), any())).thenReturn(ROUTING_URL);
        // Default: KV put succeeds. Overridden per test where a failure is exercised.
        when(attributesStore.save(any(SalesforceCustomerServiceAttributes.class), anyString(), any()))
                .thenReturn(true);
        // Constructor order is (attributesStore, invoker, testConnectionService).
        saveConfig = new SalesforceSaveConfigurationImpl(attributesStore, invoker, testConnectionService);
    }

    private static JsonObject payload(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonObject parse(String result) {
        return JsonParser.parseString(result).getAsJsonObject();
    }

    // ── constructor ──────────────────────────────────────────────────────────────────

    @Test
    public void constructor_readsInvokerIdFromInvoker() {
        verify(invoker, atLeastOnce()).getInvokerId();
        assertNotNull(saveConfig);
    }

    // ── success paths ──────────────────────────────────────────────────────────────────

    @Test
    public void saveCredentials_clientCredentialsSuccess_persistsAndReturnsSuccess() throws Exception {
        when(testConnectionService.testConnection(any(), eq("CLIENT_CREDENTIALS"))).thenReturn(SUCCESS_JSON);

        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"CLIENT_CREDENTIALS\",\"clientId\":\"cid\",\"clientSecret\":\"csec\",\"sandbox\":false}"));

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
        verify(attributesStore).save(any(SalesforceCustomerServiceAttributes.class), eq(INVOKER_ID), eq("CLIENT_CREDENTIALS"));
    }

    @Test
    public void saveCredentials_nullFlow_stillTestsAndSaves() throws Exception {
        // No "authFlow" key -> flow resolves to null -> not AUTH_CODE, no email gate, no evict.
        when(testConnectionService.testConnection(any(), any())).thenReturn(SUCCESS_JSON);

        String result = saveConfig.saveCredentials(payload(
                "{\"clientId\":\"cid\",\"clientSecret\":\"csec\"}"));

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
        verify(attributesStore).save(any(SalesforceCustomerServiceAttributes.class), eq(INVOKER_ID), isNull());
    }

    @Test
    public void saveCredentials_authCodeWithEmail_firstSave_success() throws Exception {
        when(testConnectionService.testConnection(any(), eq("AUTH_CODE"))).thenReturn(SUCCESS_JSON);
        // evictStaleRefreshTokenIfNeeded loads previous attrs; null == first-time save.
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(null);

        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"AUTH_CODE\",\"clientId\":\"cid\",\"clientSecret\":\"csec\",\"email\":\"admin@example.com\"}"));

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
        verify(attributesStore).loadByInvokerId(INVOKER_ID);
        verify(attributesStore).save(any(SalesforceCustomerServiceAttributes.class), eq(INVOKER_ID), eq("AUTH_CODE"));
    }

    @Test
    public void saveCredentials_authCodeCredentialsChanged_evictLogsAndStillSaves() throws Exception {
        when(testConnectionService.testConnection(any(), eq("AUTH_CODE"))).thenReturn(SUCCESS_JSON);
        // Previously-stored attrs with a DIFFERENT clientId -> exercises the rotation branch.
        SalesforceCustomerServiceAttributes old = new SalesforceCustomerServiceAttributes(
                "oldClientId", "oldSecret", null, null, null, null, false, ROUTING_URL);
        when(attributesStore.loadByInvokerId(INVOKER_ID)).thenReturn(old);

        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"AUTH_CODE\",\"clientId\":\"newClientId\",\"clientSecret\":\"csec\",\"email\":\"admin@example.com\"}"));

        assertTrue(parse(result).get("isSuccess").getAsBoolean());
        verify(attributesStore).save(any(SalesforceCustomerServiceAttributes.class), eq(INVOKER_ID), eq("AUTH_CODE"));
    }

    // ── test-connection failure paths ──────────────────────────────────────────────────

    @Test
    public void saveCredentials_testConnectionFailsWithMessage_returnsFailureWithoutSaving() throws Exception {
        when(testConnectionService.testConnection(any(), eq("CLIENT_CREDENTIALS"))).thenReturn(FAIL_WITH_MSG_JSON);

        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"CLIENT_CREDENTIALS\",\"clientId\":\"cid\",\"clientSecret\":\"csec\"}"));

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("bad creds", r.get("errorMessage").getAsString());
        verify(attributesStore, never()).save(any(SalesforceCustomerServiceAttributes.class), anyString(), any());
    }

    @Test
    public void saveCredentials_testConnectionFailsNoMessage_returnsDefaultReason() throws Exception {
        when(testConnectionService.testConnection(any(), eq("CLIENT_CREDENTIALS"))).thenReturn(FAIL_NO_MSG_JSON);

        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"CLIENT_CREDENTIALS\",\"clientId\":\"cid\",\"clientSecret\":\"csec\"}"));

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Authorization not yet completed. Please test the connection first.",
                r.get("errorMessage").getAsString());
        verify(attributesStore, never()).save(any(SalesforceCustomerServiceAttributes.class), anyString(), any());
    }

    // ── validation / early rejection ───────────────────────────────────────────────────

    @Test
    public void saveCredentials_authCodeMissingEmail_rejectedEarly() throws Exception {
        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"AUTH_CODE\",\"clientId\":\"cid\",\"clientSecret\":\"csec\"}"));

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Please enter the Consumer Email before saving User Sign-In credentials.",
                r.get("errorMessage").getAsString());
        // Rejected before test + save.
        verify(testConnectionService, never()).testConnection(any(), any());
        verify(attributesStore, never()).save(any(SalesforceCustomerServiceAttributes.class), anyString(), any());
    }

    @Test
    public void saveCredentials_authCodeBlankEmail_rejectedEarly() throws Exception {
        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"AUTH_CODE\",\"clientId\":\"cid\",\"clientSecret\":\"csec\",\"email\":\"   \"}"));

        assertFalse(parse(result).get("isSuccess").getAsBoolean());
        verify(testConnectionService, never()).testConnection(any(), any());
    }

    @Test
    public void saveCredentials_payloadParseFailure_returnsInvalidPayload() throws Exception {
        // Force attrsFromPayload to fail: routing resolution throws inside
        // SalesforceCustomerServiceAttributes.create(invoker, map).
        when(invoker.getRoutingInfo()).thenThrow(new RuntimeException("no routing"));

        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"CLIENT_CREDENTIALS\",\"clientId\":\"cid\",\"clientSecret\":\"csec\"}"));

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertTrue(r.get("errorMessage").getAsString().startsWith("Invalid credentials payload:"));
        verify(testConnectionService, never()).testConnection(any(), any());
    }

    // ── persistence failure paths ──────────────────────────────────────────────────────

    @Test
    public void saveCredentials_storeReturnsFalse_returnsStoreFailure() throws Exception {
        when(testConnectionService.testConnection(any(), eq("CLIENT_CREDENTIALS"))).thenReturn(SUCCESS_JSON);
        when(attributesStore.save(any(SalesforceCustomerServiceAttributes.class), anyString(), any()))
                .thenReturn(false);

        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"CLIENT_CREDENTIALS\",\"clientId\":\"cid\",\"clientSecret\":\"csec\"}"));

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Failed to save credentials to the configuration store.",
                r.get("errorMessage").getAsString());
    }

    @Test
    public void saveCredentials_storeThrows_returnsFailureWithMessage() throws Exception {
        when(testConnectionService.testConnection(any(), eq("CLIENT_CREDENTIALS"))).thenReturn(SUCCESS_JSON);
        doThrow(new RuntimeException("kv down"))
                .when(attributesStore).save(any(SalesforceCustomerServiceAttributes.class), anyString(), any());

        String result = saveConfig.saveCredentials(payload(
                "{\"authFlow\":\"CLIENT_CREDENTIALS\",\"clientId\":\"cid\",\"clientSecret\":\"csec\"}"));

        JsonObject r = parse(result);
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Failed to save credentials: kv down", r.get("errorMessage").getAsString());
    }
}
