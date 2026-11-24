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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SalesforceAuditSummary}.
 *
 * <p>Covers the Salesforce {@code SF_*} auth-failure event mapping (via the public
 * {@code updateWith} dispatch), the {@code noteFailure} bookkeeping, the base-class
 * fallback for unrecognized events, and {@code resetAuthCounters}.</p>
 */
public class SalesforceAuditSummaryTest {

    private SalesforceAuditSummary summary;

    @Before
    public void setUp() {
        summary = new SalesforceAuditSummary();
    }

    private static AuditRecord rec(String eventType, long timestamp, String reason) {
        AuditRecord r = new AuditRecord();
        r.eventType = eventType;
        r.timestamp = timestamp;
        r.reason = reason;
        return r;
    }

    @Test
    public void testInitialCountersAreZero() {
        assertEquals(0L, summary.tokenExpiryCount);
        assertEquals(0L, summary.passwordChangeCount);
        assertEquals(0L, summary.invalidClientSecretCount);
        assertEquals(0L, summary.invalidTokenCount);
        assertEquals(0L, summary.authConfigFailureCount);
    }

    @Test
    public void testUpdateWith_TokenExpiry_IncrementsTokenExpiryCount() {
        summary.updateWith(rec("SF_TOKEN_EXPIRY_DETECTED", 1000L, "token expired"));

        assertEquals(1L, summary.tokenExpiryCount);
        assertEquals(1000L, summary.latestTimestamp);
        assertEquals("token expired", summary.latestFailureReason);
        assertEquals("Other SF counters untouched", 0L, summary.invalidTokenCount);
    }

    @Test
    public void testUpdateWith_InvalidGrant_IncrementsInvalidTokenCount() {
        summary.updateWith(rec("SF_INVALID_GRANT_DETECTED", 1000L, "invalid_grant"));

        assertEquals(1L, summary.invalidTokenCount);
        assertEquals("invalid_grant", summary.latestFailureReason);
    }

    @Test
    public void testUpdateWith_InvalidClient_IncrementsInvalidClientSecretCount() {
        summary.updateWith(rec("SF_INVALID_CLIENT_DETECTED", 1000L, "invalid_client"));

        assertEquals(1L, summary.invalidClientSecretCount);
        assertEquals("invalid_client", summary.latestFailureReason);
    }

    @Test
    public void testUpdateWith_InactiveUser_IncrementsAuthConfigFailureCount() {
        summary.updateWith(rec("SF_INACTIVE_USER_DETECTED", 1000L, "inactive user"));

        assertEquals(1L, summary.authConfigFailureCount);
        assertEquals("inactive user", summary.latestFailureReason);
    }

    @Test
    public void testUpdateWith_JwtAssertionInvalid_IncrementsAuthConfigFailureCount() {
        summary.updateWith(rec("SF_JWT_ASSERTION_INVALID", 1000L, "bad jwt"));

        assertEquals(1L, summary.authConfigFailureCount);
        assertEquals("bad jwt", summary.latestFailureReason);
    }

    @Test
    public void testUpdateWith_RateLimitHit_IsNotCountedAsFailure() {
        summary.updateWith(rec("SF_RATE_LIMIT_HIT", 1000L, "throttled"));

        assertEquals(0L, summary.tokenExpiryCount);
        assertEquals(0L, summary.invalidTokenCount);
        assertEquals(0L, summary.invalidClientSecretCount);
        assertEquals(0L, summary.authConfigFailureCount);
        assertEquals("Not a catalog request either", 0L, summary.failureCount);
        assertNull("No failure reason recorded", summary.latestFailureReason);
    }

    @Test
    public void testNoteFailure_BlankReasonIsNotStored() {
        summary.updateWith(rec("SF_TOKEN_EXPIRY_DETECTED", 2000L, "   "));

        assertEquals(1L, summary.tokenExpiryCount);
        assertEquals("Timestamp still updated", 2000L, summary.latestTimestamp);
        assertNull("Blank reason should not be stored", summary.latestFailureReason);
    }

    @Test
    public void testNoteFailure_OlderTimestampDoesNotOverwriteLatest() {
        summary.updateWith(rec("SF_TOKEN_EXPIRY_DETECTED", 5000L, "first"));
        summary.updateWith(rec("SF_INVALID_GRANT_DETECTED", 1000L, "second"));

        assertEquals("Latest timestamp keeps the newer value", 5000L, summary.latestTimestamp);
        assertEquals(1L, summary.tokenExpiryCount);
        assertEquals(1L, summary.invalidTokenCount);
    }

    @Test
    public void testUpdateExtensionCounters_NullRecord_DoesNotThrow() {
        // Direct call (same-package access) to cover the null-record guard.
        summary.updateExtensionCounters(null);

        assertEquals(0L, summary.tokenExpiryCount);
        assertEquals(0L, summary.authConfigFailureCount);
    }

    @Test
    public void testUpdateExtensionCounters_NullEventType_DoesNotThrow() {
        summary.updateExtensionCounters(rec(null, 1000L, "n/a"));

        assertEquals(0L, summary.tokenExpiryCount);
        assertEquals(0L, summary.invalidTokenCount);
        assertNull(summary.latestFailureReason);
    }

    @Test
    public void testUnknownErrorEvent_FallsBackToBaseFailureHandling() {
        summary.updateWith(rec("WEBHOOK_ERROR", 1234L, "boom"));

        assertEquals("Base handles *_ERROR as a failure", 1L, summary.failureCount);
        assertEquals("boom", summary.latestFailureReason);
        assertEquals(1234L, summary.latestTimestamp);
        assertEquals("SF-specific counter untouched", 0L, summary.tokenExpiryCount);
    }

    @Test
    public void testUnknownReceivedEvent_FallsBackToBaseTotalRequests() {
        summary.updateWith(rec("WEBHOOK_RECEIVED", 1000L, null));

        assertEquals(1L, summary.totalRequests);
        assertEquals(0L, summary.failureCount);
    }

    @Test
    public void testUnknownTriggeredEvent_FallsBackToBaseSuccessCount() {
        summary.updateWith(rec("WEBHOOK_TRIGGERED", 1000L, null));

        assertEquals(1L, summary.successCount);
    }

    @Test
    public void testResetAuthCounters_ClearsAllSalesforceCounters() {
        summary.updateWith(rec("SF_TOKEN_EXPIRY_DETECTED", 1000L, "a"));
        summary.updateWith(rec("SF_INVALID_GRANT_DETECTED", 1001L, "b"));
        summary.updateWith(rec("SF_INVALID_CLIENT_DETECTED", 1002L, "c"));
        summary.updateWith(rec("SF_INACTIVE_USER_DETECTED", 1003L, "d"));

        assertTrue(summary.tokenExpiryCount > 0);
        assertTrue(summary.invalidTokenCount > 0);
        assertTrue(summary.invalidClientSecretCount > 0);
        assertTrue(summary.authConfigFailureCount > 0);

        summary.resetAuthCounters();

        assertEquals(0L, summary.tokenExpiryCount);
        assertEquals(0L, summary.passwordChangeCount);
        assertEquals(0L, summary.invalidClientSecretCount);
        assertEquals(0L, summary.invalidTokenCount);
        assertEquals(0L, summary.authConfigFailureCount);
    }
}
