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

import app.krista.extensions.util.KeyValueStore;
import app.krista.oauth.core.audit.AuditData;
import app.krista.oauth.core.audit.AuditRecord;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesforceCustomerServiceAuditService}.
 *
 * <p>The service must (a) persist audit data through the Krista KV store, (b) NEVER
 * propagate exceptions to the caller — audit failures must not break business logic
 * (the {@link app.krista.oauth.core.audit.AuditRecorder} invariant), and (c) honor the
 * null-invokerId guard on every public mutating method.</p>
 *
 * <h2>Scope</h2>
 * <ul>
 *   <li>{@code record}: writes records + summary; null-invokerId → no-op; KV failure swallowed</li>
 *   <li>{@code resetAuthCounters}: removes Salesforce auth-error event types; KV failure swallowed</li>
 *   <li>{@code getData}: filters by time range, type, status; null-invokerId → empty;
 *       exception → empty (never null)</li>
 *   <li>{@code clearRecords}: deletes within range; null-invokerId → no-op</li>
 *   <li>{@code getGlobalCounter} / {@code getArchiveIndex}: defensive defaults when empty</li>
 *   <li>{@code deleteArchive}: removes both the archive entry AND the index reference</li>
 * </ul>
 */
public class SalesforceCustomerServiceAuditServiceTest {

    private static final String INVOKER_ID = "salesforce-customer-service";
    private static final String EXTENSION_ID = "sfcs_test";
    private static final Gson GSON = new Gson();

    @Mock
    private KeyValueStore kv;

    private SalesforceCustomerServiceAuditService auditService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use package-private constructor so we control the KV-key namespace.
        auditService = new SalesforceCustomerServiceAuditService(kv, EXTENSION_ID);
    }

    // ── record ──────────────────────────────────────────────────────────────────────

    @Test
    public void record_withValidInputs_writesRecordsAndSummaryToKv() {
        // KV is empty initially — load* methods return null
        when(kv.get(anyString(), eq(Object.class))).thenReturn(null);

        auditService.record(INVOKER_ID, "CATALOG_EXECUTED", "success",
                "Fetch Cases", null, null, Map.of("foo", "bar"));

        // Records + summary + global-counter writes must occur.
        verify(kv, atLeastOnce()).put(eq(EXTENSION_ID + "_audit_records"), anyString());
        verify(kv, atLeastOnce()).put(eq(EXTENSION_ID + "_audit_summary"), anyString());
        verify(kv, atLeastOnce()).put(eq(EXTENSION_ID + "_audit_global_counter"), anyString());
    }

    @Test
    public void record_nullInvokerId_returnsWithoutTouchingKv() {
        auditService.record(null, "CATALOG_EXECUTED", "success", "Fetch Cases", null, null, Map.of());

        // No reads, no writes — invokerId guard fires first.
        verifyNoInteractions(kv);
    }

    @Test
    public void record_kvThrows_swallowsExceptionAndDoesNotPropagate() {
        when(kv.get(anyString(), eq(Object.class)))
                .thenThrow(new RuntimeException("KV unreachable"));

        // Must not throw — audit failures cannot break business logic.
        auditService.record(INVOKER_ID, "CATALOG_EXECUTED", "success",
                "Fetch Cases", null, null, Map.of());

        // OK if no put() happens; the contract is "don't throw".
    }

    // ── resetAuthCounters ───────────────────────────────────────────────────────────

    @Test
    public void resetAuthCounters_removesSalesforceAuthErrorRecords() {
        AuditRecord authError = new AuditRecord();
        authError.eventType = "SF_TOKEN_EXPIRY_DETECTED";
        authError.status = "failure";
        authError.timestamp = 1_000L;

        AuditRecord normal = new AuditRecord();
        normal.eventType = "CATALOG_EXECUTED";
        normal.status = "success";
        normal.timestamp = 2_000L;

        when(kv.get(EXTENSION_ID + "_audit_records", Object.class))
                .thenReturn(GSON.toJson(new AuditRecord[]{authError, normal}));
        when(kv.get(EXTENSION_ID + "_audit_summary", Object.class)).thenReturn(null);

        auditService.resetAuthCounters();

        // Capture what was written back — auth-error record removed, normal kept.
        ArgumentCaptor<String> recordsCaptor = ArgumentCaptor.forClass(String.class);
        verify(kv).put(eq(EXTENSION_ID + "_audit_records"), recordsCaptor.capture());

        AuditRecord[] written = GSON.fromJson(recordsCaptor.getValue(), AuditRecord[].class);
        assertEquals("Auth-error type must be removed", 1, written.length);
        assertEquals("CATALOG_EXECUTED", written[0].eventType);
    }

    @Test
    public void resetAuthCounters_kvThrows_swallowsException() {
        when(kv.get(anyString(), eq(Object.class)))
                .thenThrow(new RuntimeException("KV down"));

        // Must not throw.
        auditService.resetAuthCounters();
    }

    // ── getData ─────────────────────────────────────────────────────────────────────

    @Test
    public void getData_emptyKv_returnsEmptyAuditData() {
        when(kv.get(anyString(), eq(Object.class))).thenReturn(null);

        AuditData data = auditService.getData(INVOKER_ID, null, null, null, null);

        assertNotNull("getData must never return null", data);
        assertNotNull(data.summary);
        assertTrue("Records list must be empty when KV is empty", data.records.isEmpty());
    }

    @Test
    public void getData_nullInvokerId_returnsEmptyData() {
        AuditData data = auditService.getData(null, null, null, null, null);

        assertNotNull(data);
        assertTrue(data.records.isEmpty());
        // KV must NOT be touched — null-invokerId guard fires first.
        verifyNoInteractions(kv);
    }

    @Test
    public void getData_filtersByTimestampRange() {
        AuditRecord oldRec = newRecord("CATALOG_EXECUTED", "success", 1_000L);
        AuditRecord midRec = newRecord("CATALOG_EXECUTED", "success", 5_000L);
        AuditRecord newRec = newRecord("CATALOG_EXECUTED", "success", 9_000L);

        when(kv.get(EXTENSION_ID + "_audit_records", Object.class))
                .thenReturn(GSON.toJson(new AuditRecord[]{oldRec, midRec, newRec}));
        when(kv.get(EXTENSION_ID + "_audit_summary", Object.class)).thenReturn(null);

        // Restrict to [4_000, 6_000] — only midRec survives.
        AuditData data = auditService.getData(INVOKER_ID, 4_000L, 6_000L, null, null);

        assertEquals(1, data.records.size());
        assertEquals(5_000L, data.records.get(0).timestamp);
    }

    @Test
    public void getData_filtersByType_caseInsensitive() {
        AuditRecord exec = newRecord("CATALOG_EXECUTED", "success", 1_000L);
        AuditRecord auth = newRecord("SF_TOKEN_EXPIRY_DETECTED", "failure", 2_000L);

        when(kv.get(EXTENSION_ID + "_audit_records", Object.class))
                .thenReturn(GSON.toJson(new AuditRecord[]{exec, auth}));
        when(kv.get(EXTENSION_ID + "_audit_summary", Object.class)).thenReturn(null);

        AuditData data = auditService.getData(INVOKER_ID, null, null, "catalog_executed", null);

        assertEquals(1, data.records.size());
        assertEquals("CATALOG_EXECUTED", data.records.get(0).eventType);
    }

    @Test
    public void getData_filtersByStatus() {
        AuditRecord ok  = newRecord("CATALOG_EXECUTED", "success", 1_000L);
        AuditRecord bad = newRecord("CATALOG_EXECUTED", "failure", 2_000L);

        when(kv.get(EXTENSION_ID + "_audit_records", Object.class))
                .thenReturn(GSON.toJson(new AuditRecord[]{ok, bad}));
        when(kv.get(EXTENSION_ID + "_audit_summary", Object.class)).thenReturn(null);

        AuditData data = auditService.getData(INVOKER_ID, null, null, null, "failure");

        assertEquals(1, data.records.size());
        assertEquals("failure", data.records.get(0).status);
    }

    @Test
    public void getData_sortsRecordsByTimestampDescending() {
        AuditRecord r1 = newRecord("CATALOG_EXECUTED", "success", 1_000L);
        AuditRecord r2 = newRecord("CATALOG_EXECUTED", "success", 5_000L);
        AuditRecord r3 = newRecord("CATALOG_EXECUTED", "success", 3_000L);

        when(kv.get(EXTENSION_ID + "_audit_records", Object.class))
                .thenReturn(GSON.toJson(new AuditRecord[]{r1, r2, r3}));
        when(kv.get(EXTENSION_ID + "_audit_summary", Object.class)).thenReturn(null);

        AuditData data = auditService.getData(INVOKER_ID, null, null, null, null);

        // Most recent first.
        assertEquals(5_000L, data.records.get(0).timestamp);
        assertEquals(3_000L, data.records.get(1).timestamp);
        assertEquals(1_000L, data.records.get(2).timestamp);
    }

    @Test
    public void getData_kvThrows_returnsEmptyAuditData() {
        when(kv.get(anyString(), eq(Object.class)))
                .thenThrow(new RuntimeException("KV down"));

        AuditData data = auditService.getData(INVOKER_ID, null, null, null, null);

        assertNotNull("Even on error, getData must return non-null", data);
        assertTrue(data.records.isEmpty());
    }

    // ── clearRecords ────────────────────────────────────────────────────────────────

    @Test
    public void clearRecords_removesRecordsInRange() {
        AuditRecord r1 = newRecord("CATALOG_EXECUTED", "success", 1_000L);
        AuditRecord r2 = newRecord("CATALOG_EXECUTED", "success", 5_000L);
        AuditRecord r3 = newRecord("CATALOG_EXECUTED", "success", 9_000L);

        when(kv.get(EXTENSION_ID + "_audit_records", Object.class))
                .thenReturn(GSON.toJson(new AuditRecord[]{r1, r2, r3}));
        when(kv.get(EXTENSION_ID + "_audit_summary", Object.class)).thenReturn(null);

        // Clear records with timestamp in [4_000, 9_000] — removes r2 and r3, keeps r1.
        auditService.clearRecords(INVOKER_ID, 4_000L, 9_000L);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(kv).put(eq(EXTENSION_ID + "_audit_records"), captor.capture());

        AuditRecord[] kept = GSON.fromJson(captor.getValue(), AuditRecord[].class);
        assertEquals(1, kept.length);
        assertEquals(1_000L, kept[0].timestamp);
    }

    @Test
    public void clearRecords_nullInvokerId_isNoOp() {
        auditService.clearRecords(null, 0L, Long.MAX_VALUE);
        verifyNoInteractions(kv);
    }

    // ── getGlobalCounter ────────────────────────────────────────────────────────────

    @Test
    public void getGlobalCounter_emptyKv_returnsZeroDefaults() {
        when(kv.get(EXTENSION_ID + "_audit_global_counter", Object.class)).thenReturn(null);

        Map<String, Object> counter = auditService.getGlobalCounter();

        assertEquals(0.0, ((Number) counter.get("totalRequests")).doubleValue(), 0.0);
        assertEquals(0.0, ((Number) counter.get("totalSuccess")).doubleValue(), 0.0);
        assertEquals(0.0, ((Number) counter.get("totalFailure")).doubleValue(), 0.0);
    }

    @Test
    public void getGlobalCounter_kvReturnsValidJson_parsesAndReturns() {
        Map<String, Object> stored = new HashMap<>();
        stored.put("totalRequests", 100.0);
        stored.put("totalSuccess", 90.0);
        stored.put("totalFailure", 10.0);

        when(kv.get(EXTENSION_ID + "_audit_global_counter", Object.class))
                .thenReturn(GSON.toJson(stored));

        Map<String, Object> counter = auditService.getGlobalCounter();
        assertEquals(100.0, ((Number) counter.get("totalRequests")).doubleValue(), 0.0);
        assertEquals(90.0,  ((Number) counter.get("totalSuccess")).doubleValue(), 0.0);
        assertEquals(10.0,  ((Number) counter.get("totalFailure")).doubleValue(), 0.0);
    }

    // ── getArchiveIndex / deleteArchive ────────────────────────────────────────────

    @Test
    public void getArchiveIndex_emptyKv_returnsEmptyList() {
        when(kv.get(EXTENSION_ID + "_audit_archive_index", Object.class)).thenReturn(null);

        List<Map<String, Object>> index = auditService.getArchiveIndex();

        assertNotNull(index);
        assertTrue(index.isEmpty());
    }

    @Test
    public void deleteArchive_removesKeyFromKvAndIndex() {
        String archiveKey = EXTENSION_ID + "_audit_archive_12345";
        Map<String, Object> entry = new HashMap<>();
        entry.put("key", archiveKey);
        entry.put("count", 100.0);
        when(kv.get(EXTENSION_ID + "_audit_archive_index", Object.class))
                .thenReturn(GSON.toJson(List.of(entry)));

        auditService.deleteArchive(archiveKey);

        verify(kv).remove(archiveKey);
        // Index re-written without the deleted entry.
        ArgumentCaptor<String> idxCaptor = ArgumentCaptor.forClass(String.class);
        verify(kv).put(eq(EXTENSION_ID + "_audit_archive_index"), idxCaptor.capture());
        List<?> writtenIdx = GSON.fromJson(idxCaptor.getValue(), List.class);
        assertTrue("Index must no longer contain the deleted archive key",
                writtenIdx.isEmpty());
    }

    // ── archiveAll ──────────────────────────────────────────────────────────────────

    @Test
    public void archiveAll_underMaxRecords_isNoOp() {
        // 5 records — well under MAX_RECORDS (2000). Should not archive.
        AuditRecord[] few = new AuditRecord[5];
        for (int i = 0; i < few.length; i++) {
            few[i] = newRecord("CATALOG_EXECUTED", "success", 1_000L * (i + 1));
        }
        when(kv.get(EXTENSION_ID + "_audit_records", Object.class))
                .thenReturn(GSON.toJson(few));

        auditService.archiveAll();

        // No archive key written, no records mutation.
        verify(kv, never()).put(argThat(k ->
                k != null && k.startsWith(EXTENSION_ID + "_audit_archive_") &&
                        !k.equals(EXTENSION_ID + "_audit_archive_index")), anyString());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────

    private AuditRecord newRecord(String type, String status, long ts) {
        AuditRecord r = new AuditRecord();
        r.id = "rec_" + ts;
        r.timestamp = ts;
        r.eventType = type;
        r.status = status;
        r.catalogName = "TestCatalog";
        return r;
    }
}
