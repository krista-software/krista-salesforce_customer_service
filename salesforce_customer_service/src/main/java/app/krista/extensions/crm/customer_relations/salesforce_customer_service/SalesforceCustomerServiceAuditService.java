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
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.oauth.core.audit.AuditData;
import app.krista.oauth.core.audit.AuditRecord;
import app.krista.oauth.core.audit.AuditRecorder;
import app.krista.oauth.core.audit.AuditSummary;
import app.krista.oauth.core.audit.RequestLogCapture;
import com.google.gson.Gson;
import com.kristasoft.common.holders.ThreadLocalProxy;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thin HK2-injectable audit recording service for the {@code salesforce_customer_service}
 * extension.
 *
 * <p>Implements {@link AuditRecorder} directly so instances of this class can be passed
 * wherever the SDK expects an {@link AuditRecorder} — including as the constructor argument
 * to {@link SalesforceCustomerServiceAuditResource} which extends
 * {@link app.krista.oauth.core.audit.AbstractOAuth2AuditResource} and accepts an
 * {@code invokerId} string.</p>
 *
 * <h2>Storage strategy</h2>
 * <p>Delegates all persistence to the Krista {@link KeyValueStore} — the same substrate
 * used by {@code MsAuthAuditService} for the Microsoft provider. Records are stored under
 * {@code sfcs_audit_*} keys to avoid collisions with the MS-side entries on shared KV stores.
 * The record ring-buffer is capped at 2 000 entries; oldest entries are auto-archived
 * to a GZIP-compressed, Base64-encoded KV entry when the cap is reached.</p>
 *
 * <h2>Audit must never break business logic</h2>
 * <p>Every public method wraps its body in a {@code try/catch(Exception)}. An audit
 * failure is logged at WARN level and then silently swallowed so the calling catalog
 * request is never interrupted. This matches the CTO invariant stated in
 * {@link AuditRecorder}.</p>
 *
 * <h2>Thread-safety</h2>
 * <p>Mutating operations ({@link #record}, {@link #clearRecords}, {@link #archiveAll})
 * are {@code synchronized} to prevent interleaved KV reads/writes under concurrent
 * catalog execution. {@link KeyValueStore} itself is not guaranteed to be thread-safe
 * at the multi-operation level, so coarse synchronisation on {@code this} is the
 * correct guard.</p>
 *
 * <h2>Gap note</h2>
 * <p>The SDK v1.1.0 ships {@code MsAuthAuditService} as the concrete MS-provider
 * implementation; there is no equivalent Salesforce-aware concrete service. This class
 * fills that gap for the extension while remaining completely independent of MS-specific
 * types. It will be superseded by a future SDK-provided {@code SfAuthAuditService}
 * when the SDK adds Salesforce audit annotation support (post-v1.1.0).</p>
 *
 * @since 2.0.18 (microsoft-auth-sdk 1.1.0 migration, Chunk M3)
 */
@Service
@Singleton
public class SalesforceCustomerServiceAuditService implements AuditRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceCustomerServiceAuditService.class);

    /** Maximum number of in-memory KV-backed records before auto-archiving. */
    private static final int MAX_RECORDS = 2_000;
    /** Threshold at which auto-archiving triggers during {@link #record}. */
    private static final int ARCHIVE_THRESHOLD = 2_000;
    /** Number of records kept after an auto-archive run. */
    private static final int ARCHIVE_KEEP = 1_000;
    /** Maximum log lines preserved per record to bound KV payload size. */
    private static final int MAX_LOGS_PER_RECORD = 50;

    private static final String KEY_PREFIX       = "sfcs_";
    private static final String KV_RECORDS_SUFFIX       = "_audit_records";
    private static final String KV_SUMMARY_SUFFIX       = "_audit_summary";
    private static final String KV_GLOBAL_COUNTER_SUFFIX = "_audit_global_counter";
    private static final String KV_ARCHIVE_INDEX_SUFFIX  = "_audit_archive_index";

    private static final Gson GSON = new Gson();

    /**
     * Per-thread request start timestamp, set by {@link #startRequest()} at the beginning of a
     * catalog request and consumed by {@link #record} to stamp {@code durationMs} on the terminal
     * SUCCESS/FAILURE record. Mirrors {@code MsAuthAuditService}'s duration-capture mechanism so
     * the dashboard's "Response Latency" card (which plots records with {@code durationMs > 0})
     * is populated.
     */
    private static final ThreadLocal<Long> REQUEST_START = new ThreadLocal<>();

    private final KeyValueStore kv;
    /** Unique identifier used as the KV-key namespace prefix (e.g. the invokerId). */
    private final String extensionId;

    private final String kvRecordsKey;
    private final String kvSummaryKey;
    private final String kvGlobalCounterKey;
    private final String kvArchiveIndexKey;

    /**
     * HK2 injection constructor.
     *
     * <p>The {@link KeyValueStore} is the Krista-managed per-invoker store;
     * no additional namespace is needed because HK2 scopes it to the current
     * invoker. The {@code extensionId} string is used as a KV-key prefix to
     * avoid collisions if multiple extensions ever share a KV store.</p>
     *
     * @param kv Krista KV store (HK2-provided)
     */
    @Inject
    public SalesforceCustomerServiceAuditService(KeyValueStore kv) {
        this(kv, KEY_PREFIX + "customer_service");
    }

    /**
     * Full constructor — also accepts an explicit {@code extensionId} so tests can
     * supply an isolated namespace without touching the real KV.
     *
     * @param kv          Krista KV store
     * @param extensionId key-namespace prefix (e.g. {@code "sfcs_customer_service"})
     */
    SalesforceCustomerServiceAuditService(KeyValueStore kv, String extensionId) {
        this.kv          = kv;
        this.extensionId = extensionId;
        this.kvRecordsKey       = extensionId + KV_RECORDS_SUFFIX;
        this.kvSummaryKey       = extensionId + KV_SUMMARY_SUFFIX;
        this.kvGlobalCounterKey = extensionId + KV_GLOBAL_COUNTER_SUFFIX;
        this.kvArchiveIndexKey  = extensionId + KV_ARCHIVE_INDEX_SUFFIX;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // AuditRecorder implementation
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Marks the start of a catalog request on the current thread so the terminal
     * SUCCESS/FAILURE record can be stamped with a real {@code durationMs} (drives the
     * dashboard's "Response Latency" card). Mirrors {@code MsAuthAuditService.startRequest()}.
     * Call once at the beginning of each catalog request (see the {@code audited(...)} wrapper).
     */
    public void startRequest() {
        try {
            // Stop any stale capture from a reused thread, then begin a fresh one so the
            // terminal record can carry per-request execution logs (drives the Records tab's
            // "Logs" button). Lazily installs the Log4j2 appender; no-ops if Log4j2 is absent.
            RequestLogCapture.stopCapture();
            RequestLogCapture.startCapture();
        } catch (Exception e) {
            LOGGER.debug("[sfcs-audit] startRequest log-capture skipped: {}", e.getMessage());
        }
        REQUEST_START.set(System.currentTimeMillis());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Appends a new {@link AuditRecord} to the KV-backed ring-buffer and recalculates
     * the rolling summary. If the buffer reaches {@value #ARCHIVE_THRESHOLD} entries,
     * the oldest {@code ARCHIVE_THRESHOLD - ARCHIVE_KEEP} records are GZIP-compressed
     * and archived automatically before the write.</p>
     *
     * <p>Never throws — all failures are swallowed at WARN level.</p>
     */
    @Override
    public synchronized void record(String invokerId,
                                    String eventType,
                                    String status,
                                    String catalogName,
                                    String reason,
                                    String exceptionClass,
                                    Map<String, String> metadata) {
        if (invokerId == null) {
            return;
        }
        // Match the outlook extension's dashboard behaviour: show only terminal catalog-request
        // outcomes (SUCCESS / FAILURE), never the transient "TRIGGERED" marker. The catalog
        // wrapper (executeCatalog) emits CATALOG_REQUEST_TRIGGERED before running the handler to
        // start log capture; outlook never surfaces it as a record (it uses startRequest()).
        // Dropping it here keeps the records list + timeline clean AND prevents it from inflating
        // the request counter (each request would otherwise count once for TRIGGERED and again
        // for its terminal SUCCESS/FAILURE).
        if ("CATALOG_REQUEST_TRIGGERED".equals(eventType)) {
            return;
        }
        try {
            List<AuditRecord> records = loadRecords();
            AuditSummary summary     = loadSummary();

            AuditRecord rec = buildRecord(eventType, status, catalogName, reason, exceptionClass, metadata);
            // Stamp request latency on the terminal record so the dashboard's "Response Latency"
            // card (records with durationMs > 0) is populated — mirrors MsAuthAuditService. The
            // start time is set by startRequest() at the top of the audited() catalog wrapper.
            if ("CATALOG_REQUEST_SUCCESS".equals(eventType) || "CATALOG_REQUEST_FAILURE".equals(eventType)) {
                Long startTime = REQUEST_START.get();
                REQUEST_START.remove();
                if (startTime != null) {
                    rec.durationMs = rec.timestamp - startTime;
                }
                // Attach the per-request execution logs captured since startRequest() so the
                // Records tab shows a "Logs" button (instead of "Details") — mirrors MsAuthAuditService.
                rec.executionLogs = trimLogs(RequestLogCapture.stopCapture());
                // Guaranteed fallback: the dashboard's Logs view must never be empty. When live
                // Log4j2 capture yields nothing (appender not receiving events in this runtime),
                // synthesize a request summary from the record's own fields.
                if (rec.executionLogs == null || rec.executionLogs.isEmpty()) {
                    List<String> synth = new ArrayList<>();
                    synth.add("[INFO] Catalog request '" + (catalogName != null ? catalogName : "—")
                            + "' finished with status: " + status);
                    if (rec.durationMs > 0) {
                        synth.add("[INFO] Duration: " + rec.durationMs + " ms");
                    }
                    if (rec.triggeredBy != null) {
                        synth.add("[INFO] Triggered by: " + rec.triggeredBy);
                    }
                    if (reason != null && !reason.isBlank()) {
                        synth.add("[ERROR] " + trimSafe(reason, 400));
                    }
                    if (exceptionClass != null && !exceptionClass.isBlank()) {
                        synth.add("[ERROR] Exception: " + exceptionClass);
                    }
                    synth.add("[WARN] Live execution-log capture returned no lines for this request "
                            + "(see 'RequestLogCapture' entries in the extension diagnostic log).");
                    rec.executionLogs = synth;
                }
            }
            records.add(rec);
            summary.updateWith(rec);
            incrementGlobalCounter(rec);

            if (records.size() >= ARCHIVE_THRESHOLD) {
                archiveOldestRecords(records);
                summary = new SalesforceAuditSummary();
                records.forEach(summary::updateWith);
            }

            trimRecords(records);
            saveRecords(records);
            saveSummary(summary);

        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] record() suppressed (non-critical): {}", e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Removes Salesforce auth-error event types from the ring-buffer and rebuilds the
     * summary, so that stale failure counts from a previous credential configuration are
     * cleared when credentials are saved successfully.</p>
     *
     * <p>Never throws — all failures are swallowed at WARN level.</p>
     */
    @Override
    public synchronized void resetAuthCounters() {
        try {
            List<AuditRecord> records = loadRecords();
            // Remove all Salesforce auth-error event types defined in OAuth2AuditEventType
            records.removeIf(r -> r.eventType != null && isSalesforceAuthErrorType(r.eventType));
            AuditSummary rebuilt = new SalesforceAuditSummary();
            records.forEach(rebuilt::updateWith);
            saveRecords(records);
            saveSummary(rebuilt);
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] resetAuthCounters() suppressed (non-critical): {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Audit query methods (called by SalesforceCustomerServiceAuditResource)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Returns filtered audit data for the dashboard.
     *
     * @param invokerId the invoker identifier (used for guard only; KV is already scoped)
     * @param fromTs    optional lower bound (epoch ms)
     * @param toTs      optional upper bound (epoch ms)
     * @param type      optional event-type filter (case-insensitive)
     * @param status    optional status filter ({@code "success"}, {@code "failure"})
     * @return the matching records wrapped in a summary envelope; never null
     */
    public AuditData getData(String invokerId, Long fromTs, Long toTs, String type, String status) {
        if (invokerId == null) {
            return emptyData();
        }
        try {
            List<AuditRecord> records = loadRecords();
            AuditSummary summary      = loadSummary();

            List<AuditRecord> filtered = new ArrayList<>(records);
            // Never surface the transient TRIGGERED marker in the dashboard (records + timeline),
            // matching the outlook extension. Also hides any TRIGGERED rows that were persisted
            // before the write-side skip in record() was added.
            filtered.removeIf(r -> "CATALOG_REQUEST_TRIGGERED".equals(r.eventType));
            if (fromTs  != null) filtered.removeIf(r -> r.timestamp < fromTs);
            if (toTs    != null) filtered.removeIf(r -> r.timestamp > toTs);
            if (type    != null && !type.isBlank())
                filtered.removeIf(r -> !type.equalsIgnoreCase(r.eventType));
            if (status  != null && !status.isBlank())
                filtered.removeIf(r -> !status.equalsIgnoreCase(r.status));

            filtered.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
            return new AuditData(summary, filtered);
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] getData() error: {}", e.getMessage());
            return emptyData();
        }
    }

    /**
     * Clears all records whose timestamp falls within {@code [fromTs, toTs]}.
     * Passing {@code null} for either bound means "no lower/upper bound".
     *
     * @param invokerId the invoker identifier (guard only)
     * @param fromTs    optional start of range (inclusive, epoch ms)
     * @param toTs      optional end of range (inclusive, epoch ms)
     */
    public synchronized void clearRecords(String invokerId, Long fromTs, Long toTs) {
        if (invokerId == null) {
            return;
        }
        try {
            List<AuditRecord> records = loadRecords();
            records.removeIf(r ->
                    (fromTs == null || r.timestamp >= fromTs) &&
                    (toTs   == null || r.timestamp <= toTs));
            AuditSummary rebuilt = new SalesforceAuditSummary();
            records.forEach(rebuilt::updateWith);
            saveRecords(records);
            saveSummary(rebuilt);
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] clearRecords() suppressed (non-critical): {}", e.getMessage());
        }
    }

    /**
     * Returns the global all-time request counter map (never resets).
     *
     * @return map with {@code totalRequests}, {@code totalSuccess}, {@code totalFailure}
     *         keys; values are doubles for Gson round-trip compatibility
     */
    public Map<String, Object> getGlobalCounter() {
        try {
            String json = readJsonFromKv(kvGlobalCounterKey);
            if (json != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> counter = GSON.fromJson(json, Map.class);
                if (counter != null) {
                    return counter;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] getGlobalCounter() error: {}", e.getMessage());
        }
        return Map.of("totalRequests", 0.0, "totalSuccess", 0.0, "totalFailure", 0.0);
    }

    /**
     * Returns the archive index — a list of maps each describing one compressed archive entry.
     *
     * @return mutable list of archive descriptor maps; empty if none exist
     */
    public List<Map<String, Object>> getArchiveIndex() {
        try {
            String json = readJsonFromKv(kvArchiveIndexKey);
            if (json != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> index = GSON.fromJson(json, List.class);
                if (index != null) {
                    return index;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] getArchiveIndex() error: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Returns the audit records stored in the named archive.
     *
     * @param archiveKey the opaque KV key returned by {@link #getArchiveIndex()}
     * @return the decompressed records; empty list if the key does not exist or is unreadable
     */
    public List<AuditRecord> getArchivedRecords(String archiveKey) {
        try {
            Object raw = kv.get(archiveKey, Object.class);
            if (raw == null) {
                return Collections.emptyList();
            }
            String compressed;
            if (raw instanceof String s) {
                compressed = s.trim();
                if (compressed.startsWith("\"") && compressed.endsWith("\"")) {
                    compressed = GSON.fromJson(compressed, String.class);
                }
            } else {
                compressed = GSON.toJson(raw);
            }
            if (compressed == null || compressed.isBlank()) {
                return Collections.emptyList();
            }
            String json = decompress(compressed);
            AuditRecord[] recs = GSON.fromJson(json, AuditRecord[].class);
            if (recs != null) {
                return Arrays.asList(recs);
            }
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] getArchivedRecords({}) error: {}", archiveKey, e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Deletes the named archive from the KV store and removes it from the index.
     *
     * @param archiveKey the opaque KV key returned by {@link #getArchiveIndex()}
     */
    public synchronized void deleteArchive(String archiveKey) {
        try {
            kv.remove(archiveKey);
            List<Map<String, Object>> index = getArchiveIndex();
            index.removeIf(entry -> archiveKey.equals(entry.get("key")));
            kv.put(kvArchiveIndexKey, GSON.toJson(index));
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] deleteArchive({}) suppressed: {}", archiveKey, e.getMessage());
        }
    }

    /**
     * Archives the current ring-buffer contents (oldest half) into a compressed KV entry.
     * Only runs when the buffer has reached {@value #MAX_RECORDS} records.
     */
    public synchronized void archiveAll() {
        try {
            List<AuditRecord> records = loadRecords();
            if (records.isEmpty() || records.size() < MAX_RECORDS) {
                LOGGER.info("[sfcs-audit] archiveAll skipped — only {} records (min {} required)",
                        records.size(), MAX_RECORDS);
                return;
            }
            records.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
            int archiveCount = records.size() / 2;
            List<AuditRecord> toArchive = new ArrayList<>(records.subList(0, archiveCount));
            List<AuditRecord> toKeep    = new ArrayList<>(records.subList(archiveCount, records.size()));

            String archiveKey = extensionId + "_audit_archive_" + System.currentTimeMillis();
            String compressed = compress(GSON.toJson(toArchive));
            kv.put(archiveKey, compressed);

            long fromTs = toArchive.get(0).timestamp;
            long toTs   = toArchive.get(toArchive.size() - 1).timestamp;
            List<Map<String, Object>> index = getArchiveIndex();
            Map<String, Object> entry = new HashMap<>();
            entry.put("key",       archiveKey);
            entry.put("from",      (double) fromTs);
            entry.put("to",        (double) toTs);
            entry.put("count",     (double) toArchive.size());
            entry.put("sizeBytes", (double) compressed.length());
            index.add(entry);
            kv.put(kvArchiveIndexKey, GSON.toJson(index));

            saveRecords(toKeep);
            AuditSummary rebuilt = new SalesforceAuditSummary();
            toKeep.forEach(rebuilt::updateWith);
            saveSummary(rebuilt);

            LOGGER.info("[sfcs-audit] Archived {} records (key={}), kept {}",
                    toArchive.size(), archiveKey, toKeep.size());
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] archiveAll() failed: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────────

    private void archiveOldestRecords(List<AuditRecord> records) {
        try {
            int archiveCount = records.size() - ARCHIVE_KEEP;
            if (archiveCount <= 0) {
                return;
            }
            List<AuditRecord> toArchive = new ArrayList<>(records.subList(0, archiveCount));
            long fromTs = toArchive.get(0).timestamp;
            long toTs   = toArchive.get(toArchive.size() - 1).timestamp;

            String archiveKey = extensionId + "_audit_archive_" + System.currentTimeMillis();
            String compressed = compress(GSON.toJson(toArchive));
            kv.put(archiveKey, compressed);

            List<Map<String, Object>> index = getArchiveIndex();
            Map<String, Object> entry = new HashMap<>();
            entry.put("key",       archiveKey);
            entry.put("from",      (double) fromTs);
            entry.put("to",        (double) toTs);
            entry.put("count",     (double) archiveCount);
            entry.put("sizeBytes", (double) compressed.length());
            index.add(entry);
            kv.put(kvArchiveIndexKey, GSON.toJson(index));

            records.subList(0, archiveCount).clear();
            LOGGER.info("[sfcs-audit] Auto-archived {} records (key={})", archiveCount, archiveKey);
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] archiveOldestRecords() failed: {}", e.getMessage());
        }
    }

    private void incrementGlobalCounter(AuditRecord rec) {
        try {
            Map<String, Object> counter = new HashMap<>(getGlobalCounter());
            double total   = ((Number) counter.getOrDefault("totalRequests", 0.0)).doubleValue();
            double success = ((Number) counter.getOrDefault("totalSuccess",  0.0)).doubleValue();
            double failure = ((Number) counter.getOrDefault("totalFailure",  0.0)).doubleValue();

            total++;
            if ("success".equals(rec.status)) {
                success++;
            } else if ("failure".equals(rec.status)) {
                failure++;
            }

            counter.put("totalRequests", total);
            counter.put("totalSuccess",  success);
            counter.put("totalFailure",  failure);
            if (!counter.containsKey("firstRecordedAt")) {
                counter.put("firstRecordedAt", (double) rec.timestamp);
            }
            counter.put("lastRecordedAt", (double) rec.timestamp);
            kv.put(kvGlobalCounterKey, GSON.toJson(counter));
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] incrementGlobalCounter() failed: {}", e.getMessage());
        }
    }

    private List<AuditRecord> loadRecords() {
        try {
            String json = readJsonFromKv(kvRecordsKey);
            if (json != null && !json.isBlank()) {
                AuditRecord[] loaded = GSON.fromJson(json, AuditRecord[].class);
                if (loaded != null) {
                    return new ArrayList<>(Arrays.asList(loaded));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] loadRecords() failed: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private AuditSummary loadSummary() {
        try {
            String json = readJsonFromKv(kvSummaryKey);
            if (json != null && !json.isBlank()) {
                AuditSummary loaded = GSON.fromJson(json, SalesforceAuditSummary.class);
                if (loaded != null) {
                    return loaded;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[sfcs-audit] loadSummary() failed: {}", e.getMessage());
        }
        return new SalesforceAuditSummary();
    }

    private void saveRecords(List<AuditRecord> records) {
        kv.put(kvRecordsKey, GSON.toJson(records));
    }

    private void saveSummary(AuditSummary summary) {
        kv.put(kvSummaryKey, GSON.toJson(summary));
    }

    private String readJsonFromKv(String key) {
        Object raw = kv.get(key, Object.class);
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            return s.isBlank() ? null : s;
        }
        String json = GSON.toJson(raw);
        return json.isBlank() ? null : json;
    }

    private static void trimRecords(List<AuditRecord> records) {
        while (records.size() > MAX_RECORDS) {
            records.remove(0);
        }
    }

    private static boolean isSalesforceAuthErrorType(String eventType) {
        return switch (eventType) {
            case "SF_TOKEN_EXPIRY_DETECTED",
                 "SF_INVALID_GRANT_DETECTED",
                 "SF_INVALID_CLIENT_DETECTED",
                 "SF_INACTIVE_USER_DETECTED",
                 "SF_RATE_LIMIT_HIT",
                 "SF_JWT_ASSERTION_INVALID",
                 "AUTH_CONFIG_FAILURE" -> true;
            default -> false;
        };
    }

    private static AuditRecord buildRecord(String eventType,
                                           String status,
                                           String catalogName,
                                           String reason,
                                           String exceptionClass,
                                           Map<String, String> metadata) {
        AuditRecord rec    = new AuditRecord();
        rec.id             = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        rec.timestamp      = System.currentTimeMillis();
        rec.eventType      = eventType;
        rec.status         = status;
        rec.catalogName    = catalogName;
        rec.reason         = trimSafe(reason, 500);
        rec.exceptionClass = exceptionClass;
        rec.metadata       = metadata;
        // Resolve the user who triggered the request from the thread-local AuthorizationContext
        // (populates the Records tab's "User" column) — mirrors MsAuthAuditService.
        rec.triggeredBy    = resolveTriggeredBy();
        // Fallback: if no auth context (e.g. webhook/async thread), use triggeredBy from metadata.
        if (rec.triggeredBy == null && metadata != null && metadata.containsKey("triggeredBy")) {
            rec.triggeredBy = metadata.get("triggeredBy");
        }
        return rec;
    }

    private static AuditData emptyData() {
        return new AuditData(new SalesforceAuditSummary(), Collections.emptyList());
    }

    /**
     * Resolves the user who triggered the current request from the thread-local
     * {@link AuthorizationContext} — primary email, then person name, then account id.
     * Returns {@code null} when there is no authenticated context (e.g. operator/admin
     * threads). Mirrors {@code MsAuthAuditService.resolveTriggeredBy()}.
     */
    private static String resolveTriggeredBy() {
        try {
            AuthorizationContext ctx = ThreadLocalProxy
                    .getThreadLocal(AuthorizationContext.class).get();
            if (ctx != null && ctx.isAuthenticated()) {
                app.krista.ksdk.accounts.Account account = ctx.getAuthorizedAccount();
                if (account != null) {
                    if (account.getPrimaryEmailAddress() != null
                            && !account.getPrimaryEmailAddress().isBlank()) {
                        return account.getPrimaryEmailAddress();
                    }
                    if (account.getPerson() != null
                            && account.getPerson().getPersonName() != null
                            && !account.getPerson().getPersonName().isBlank()) {
                        return account.getPerson().getPersonName();
                    }
                    if (account.getAccountId() != null) {
                        return account.getAccountId();
                    }
                }
            }
        } catch (Exception ignored) {
            // No auth context on this thread — leave triggeredBy null.
        }
        return null;
    }

    /**
     * Caps and truncates captured execution logs to bound the KV payload size.
     * Mirrors {@code MsAuthAuditService.trimLogs()}. Returns {@code null} for empty input.
     */
    private static List<String> trimLogs(List<String> logs) {
        if (logs == null || logs.isEmpty()) {
            return null;
        }
        List<String> trimmed = logs.size() > MAX_LOGS_PER_RECORD
                ? new ArrayList<>(logs.subList(0, MAX_LOGS_PER_RECORD))
                : new ArrayList<>(logs);
        trimmed.replaceAll(line -> trimSafe(line, 300));
        return trimmed;
    }

    private static String trimSafe(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }

    private static String compress(String data) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(baos)) {
                gzip.write(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return data; // fallback: store uncompressed
        }
    }

    private static String decompress(String base64) {
        try {
            byte[] compressed = Base64.getDecoder().decode(base64);
            try (java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(
                    new java.io.ByteArrayInputStream(compressed))) {
                return new String(gzip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return base64; // might already be plain JSON
        }
    }
}
