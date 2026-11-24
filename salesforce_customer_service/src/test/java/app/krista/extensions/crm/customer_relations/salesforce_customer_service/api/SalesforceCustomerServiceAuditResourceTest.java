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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAuditService;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceSdkFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import app.krista.oauth.core.audit.AuditData;
import app.krista.oauth.core.audit.AuditRecord;
import app.krista.oauth.core.audit.AuditSummary;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesforceCustomerServiceAuditResource}.
 *
 * <p>This JAX-RS resource extends the SDK's {@code AbstractOAuth2AuditResource} and
 * overrides the hooks that the base class invokes from its public REST endpoints. The
 * resource is a thin delegation layer onto {@link SalesforceCustomerServiceAuditService},
 * EXCEPT for {@code buildHealthResponse}, which adds live credential-health probing on
 * top of the service.</p>
 *
 * <h2>Coverage</h2>
 * <ul>
 *   <li>Each delegation hook ({@code getAuditData}, {@code clearAuditRecords},
 *       {@code getArchiveIndex}, {@code getArchivedRecords}, {@code deleteArchiveEntry},
 *       {@code getGlobalCounter}, {@code archiveAllRecords}) forwards to the audit service.</li>
 *   <li>{@code buildHealthResponse}: failure case (SDK acquireForAdmin throws) must produce
 *       a {@code status=error} response, never propagate the exception, and always include
 *       the static credential metadata fields.</li>
 *   <li>{@code buildHealthResponse}: client-id masking — the full client ID must never
 *       appear in the response.</li>
 * </ul>
 */
public class SalesforceCustomerServiceAuditResourceTest {

    private static final String INVOKER_ID = "salesforce-customer-service";
    private static final String FULL_CLIENT_ID = "3MVG9clientIdFullSensitive";
    private static final String INSTANCE_URL = "https://test.my.salesforce.com";
    private static final String ARCHIVE_KEY = "sfcs_customer_service_audit_archive_12345";

    @Mock
    private Invoker invoker;
    @Mock
    private SalesforceCustomerServiceAuditService auditService;
    @Mock
    private SalesforceCustomerServiceAttributes attributes;
    @Mock
    private SalesforceCustomerServiceAttributesStore attributesStore;
    @Mock
    private SalesforceCustomerServiceSdkFactory sdkFactory;

    private SalesforceCustomerServiceAuditResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(invoker.getInvokerId()).thenReturn(INVOKER_ID);
        resource = new SalesforceCustomerServiceAuditResource(
                invoker, auditService, attributes, attributesStore, sdkFactory);
    }

    // ── Delegation hooks ────────────────────────────────────────────────────────────

    @Test
    public void getAuditData_delegatesToAuditService() {
        AuditData expected = new AuditData(new AuditSummary(), Collections.emptyList());
        when(auditService.getData(INVOKER_ID, 100L, 200L, "type", "success"))
                .thenReturn(expected);

        AuditData actual = resource.getAuditData(INVOKER_ID, 100L, 200L, "type", "success");

        assertSame(expected, actual);
        verify(auditService, times(1)).getData(INVOKER_ID, 100L, 200L, "type", "success");
    }

    @Test
    public void clearAuditRecords_delegatesToAuditService() {
        resource.clearAuditRecords(INVOKER_ID, 100L, 200L);
        verify(auditService, times(1)).clearRecords(INVOKER_ID, 100L, 200L);
    }

    @Test
    public void getArchiveIndex_delegatesToAuditService() {
        List<Map<String, Object>> expected = Collections.singletonList(
                Map.of("key", ARCHIVE_KEY));
        when(auditService.getArchiveIndex()).thenReturn(expected);

        List<Map<String, Object>> actual = resource.getArchiveIndex();
        assertSame(expected, actual);
        verify(auditService, times(1)).getArchiveIndex();
    }

    @Test
    public void getArchivedRecords_delegatesToAuditService() {
        List<AuditRecord> expected = Collections.singletonList(new AuditRecord());
        when(auditService.getArchivedRecords(ARCHIVE_KEY)).thenReturn(expected);

        assertSame(expected, resource.getArchivedRecords(ARCHIVE_KEY));
        verify(auditService).getArchivedRecords(ARCHIVE_KEY);
    }

    @Test
    public void deleteArchiveEntry_delegatesToAuditService() {
        resource.deleteArchiveEntry(ARCHIVE_KEY);
        verify(auditService).deleteArchive(ARCHIVE_KEY);
    }

    @Test
    public void getGlobalCounter_delegatesToAuditService() {
        Map<String, Object> expected = Map.of("totalRequests", 42.0);
        when(auditService.getGlobalCounter()).thenReturn(expected);

        assertSame(expected, resource.getGlobalCounter());
        verify(auditService).getGlobalCounter();
    }

    @Test
    public void archiveAllRecords_delegatesToAuditService() {
        resource.archiveAllRecords();
        verify(auditService).archiveAll();
    }

    // ── buildHealthResponse ─────────────────────────────────────────────────────────

    @Test
    public void buildHealthResponse_sdkThrows_returnsErrorStatusAndNeverPropagates() {
        // The factory.newSdk path throws — covers the most common health-failure
        // (no creds yet, KV unreachable, attrs malformed).
        when(attributes.getInstanceUrl()).thenReturn(INSTANCE_URL);
        when(attributes.isSandbox()).thenReturn(false);
        when(attributes.getClientId()).thenReturn(FULL_CLIENT_ID);
        when(attributes.getUserName()).thenReturn(null);
        when(attributes.getPassword()).thenReturn(null);
        when(attributes.getSecurityToken()).thenReturn(null);

        when(sdkFactory.newSdk(attributes))
                .thenThrow(new RuntimeException("SDK build failed: no creds"));

        // Must not throw.
        Map<String, Object> health = resource.buildHealthResponse();

        assertNotNull(health);
        assertEquals("error", health.get("status"));
        assertTrue("message must surface the underlying cause",
                String.valueOf(health.get("message")).contains("SDK build failed"));

        // Static fields must still be populated even on failure.
        assertEquals(INSTANCE_URL, health.get("instanceUrl"));
        assertEquals(false, health.get("sandbox"));
        assertEquals(false, health.get("hasUserCredentials"));
    }

    @Test
    public void buildHealthResponse_masksClientIdNeverReturnsFullValue() {
        when(attributes.getInstanceUrl()).thenReturn(INSTANCE_URL);
        when(attributes.isSandbox()).thenReturn(true);
        when(attributes.getClientId()).thenReturn(FULL_CLIENT_ID);
        when(sdkFactory.newSdk(attributes)).thenThrow(new RuntimeException("forced-fail"));

        Map<String, Object> health = resource.buildHealthResponse();

        String mask = String.valueOf(health.get("clientIdMask"));
        assertNotEquals("Mask must not equal the full client ID", FULL_CLIENT_ID, mask);
        assertFalse("Mask must not contain the full client ID", mask.contains(FULL_CLIENT_ID));
    }

    @Test
    public void buildHealthResponse_nullInstanceUrl_serializesAsEmptyString() {
        when(attributes.getInstanceUrl()).thenReturn(null);
        when(attributes.isSandbox()).thenReturn(false);
        when(attributes.getClientId()).thenReturn(FULL_CLIENT_ID);
        when(sdkFactory.newSdk(attributes)).thenThrow(new RuntimeException("forced-fail"));

        Map<String, Object> health = resource.buildHealthResponse();
        // The resource's emptyIfNull helper guarantees the field is "" not null —
        // simplifies JSON serialization for the dashboard's JS consumer.
        assertEquals("", health.get("instanceUrl"));
    }

    @Test
    public void buildHealthResponse_hasUserCredentials_trueWhenAllThreeFieldsSet() {
        when(attributes.getInstanceUrl()).thenReturn(INSTANCE_URL);
        when(attributes.isSandbox()).thenReturn(false);
        when(attributes.getClientId()).thenReturn(FULL_CLIENT_ID);
        when(attributes.getUserName()).thenReturn("admin@example.com");
        when(attributes.getPassword()).thenReturn("secret");
        when(attributes.getSecurityToken()).thenReturn("tok");
        when(sdkFactory.newSdk(attributes)).thenThrow(new RuntimeException("forced-fail"));

        Map<String, Object> health = resource.buildHealthResponse();
        assertEquals(true, health.get("hasUserCredentials"));
    }

    @Test
    public void buildHealthResponse_hasUserCredentials_falseWhenAnyFieldBlank() {
        when(attributes.getInstanceUrl()).thenReturn(INSTANCE_URL);
        when(attributes.isSandbox()).thenReturn(false);
        when(attributes.getClientId()).thenReturn(FULL_CLIENT_ID);
        when(attributes.getUserName()).thenReturn("admin@example.com");
        when(attributes.getPassword()).thenReturn("");           // blank
        when(attributes.getSecurityToken()).thenReturn("tok");
        when(sdkFactory.newSdk(attributes)).thenThrow(new RuntimeException("forced-fail"));

        Map<String, Object> health = resource.buildHealthResponse();
        assertEquals(false, health.get("hasUserCredentials"));
    }
}
