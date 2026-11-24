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

import app.krista.oauth.core.audit.AuditRecord;
import app.krista.oauth.core.audit.AuditSummary;

/**
 * Salesforce-specific {@link AuditSummary} that counts the provider's {@code SF_*} auth-failure
 * events into the counter fields the audit dashboard's <b>"Auth Failures"</b> card reads.
 *
 * <h2>Why this exists</h2>
 * <p>The dashboard ({@code audit/app.js}) computes "Auth Failures" as:</p>
 * <pre>{@code tokenExpiryCount + passwordChangeCount + invalidClientSecretCount
 *        + invalidTokenCount + authConfigFailureCount}</pre>
 * <p>Those fields live only on the legacy Microsoft {@code AuditSummary}. The base substrate
 * {@link AuditSummary} has neither the fields nor any handling for Salesforce's {@code SF_*}
 * events — its generic {@code updateExtensionCounters} only matches
 * {@code *_RECEIVED / *_TRIGGERED / *_ERROR} suffixes, whereas Salesforce emits
 * {@code SF_*_DETECTED} / {@code _HIT} / {@code _INVALID}. So with the base summary the card is
 * <b>permanently 0</b> regardless of real auth failures.</p>
 *
 * <p>This subclass declares the <b>same field names</b> the UI already reads (so no dashboard
 * change is needed) and maps the Salesforce auth events onto them. Transient throttling
 * ({@code SF_RATE_LIMIT_HIT}) is intentionally NOT counted — it is not a credential failure.</p>
 *
 * <p>Gson serializes these subclass fields in the {@code /data} response because
 * {@code AuditData.summary} holds a runtime {@code SalesforceAuditSummary} instance
 * (reflective-adapter runtime-type serialization).</p>
 */
public class SalesforceAuditSummary extends AuditSummary {

    // Field names MUST match what audit/app.js reads for the "Auth Failures" card.
    public long tokenExpiryCount;
    public long passwordChangeCount;        // no Salesforce equivalent — kept at 0 for UI parity
    public long invalidClientSecretCount;
    public long invalidTokenCount;
    public long authConfigFailureCount;

    /**
     * Maps Salesforce auth-failure events onto the dashboard's auth-counter fields. Unknown
     * event types fall back to the base naming-convention handling.
     */
    @Override
    protected void updateExtensionCounters(AuditRecord record) {
        String type = record == null ? null : record.eventType;
        if (type == null) {
            return;
        }
        switch (type) {
            // Access/refresh token expired → re-authorization required.
            case "SF_TOKEN_EXPIRY_DETECTED" -> { tokenExpiryCount++; noteFailure(record); }
            // Refresh token rejected / revoked / invalid_grant → stored token no longer valid.
            case "SF_INVALID_GRANT_DETECTED" -> { invalidTokenCount++; noteFailure(record); }
            // Wrong or expired Consumer Key / Secret (invalid_client).
            case "SF_INVALID_CLIENT_DETECTED" -> { invalidClientSecretCount++; noteFailure(record); }
            // Inactive user / inactive org / JWT assertion problems → configuration failures.
            case "SF_INACTIVE_USER_DETECTED", "SF_JWT_ASSERTION_INVALID" -> {
                authConfigFailureCount++; noteFailure(record);
            }
            // SF_RATE_LIMIT_HIT is a transient throttle, NOT a credential failure — not counted.
            default -> super.updateExtensionCounters(record);
        }
    }

    /** Records the latest failure reason/timestamp for the dashboard's Auth-Failures sub-text. */
    private void noteFailure(AuditRecord record) {
        if (record.timestamp > latestTimestamp) {
            latestTimestamp = record.timestamp;
        }
        if (record.reason != null && !record.reason.isBlank()) {
            latestFailureReason = record.reason;
        }
    }

    /**
     * Clears the Salesforce auth counters (in addition to the base reset). Called when
     * credentials are saved successfully so stale failure counts from a previous configuration
     * do not linger on the dashboard.
     */
    @Override
    public synchronized void resetAuthCounters() {
        super.resetAuthCounters();
        tokenExpiryCount = 0;
        passwordChangeCount = 0;
        invalidClientSecretCount = 0;
        invalidTokenCount = 0;
        authConfigFailureCount = 0;
    }
}
