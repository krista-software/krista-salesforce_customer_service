# Audit Dashboard

## Overview

The **Audit Dashboard** is a custom tab (introduced in version 2.0.18) that gives operators visibility into the extension's authentication activity directly inside Krista. It reports the OAuth events the extension records — token acquisitions, refreshes, and code exchanges — along with the current health of the configured credentials, so you can confirm the integration is working and diagnose authentication problems without leaving the platform.

The dashboard is powered by Krista's centralized OAuth substrate (the SDK's `AbstractOAuth2AuditResource`); its user interface is bundled from the SDK and served by the extension.

## Accessing the Dashboard

1. In Krista, open the Invoker configured for the Salesforce Customer Service extension.
2. Select the **Audit Dashboard** tab.

The tab is served from the extension's `rest/salesforcecustomerservice/audit/` endpoint.

## What the Dashboard Shows

| Section | Description |
|---------|-------------|
| Audit Records | A table of recorded authentication events for this invoker |
| Summary | A rolled-up view of the audit records (counts and status) |
| Health Check | A live check of the configured credentials and Salesforce connectivity. Reports `error` (rather than a stack trace) if a token cannot be acquired |
| Archives | Browse previously archived record sets, and open or delete an individual archive |
| Archive Now | Manually trigger archiving of the current records |
| Global Counter | An all-time count of audit events |

## Endpoints (Technical Reference)

The tab is backed by the following endpoints, provided by the SDK's audit resource. Most operators will not call these directly — they are listed for integration and troubleshooting purposes.

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/salesforcecustomerservice/audit/` | Audit dashboard HTML |
| GET | `/salesforcecustomerservice/audit/data` | JSON records + summary |
| GET | `/salesforcecustomerservice/audit/health` | Credential / connectivity status |
| DELETE | `/salesforcecustomerservice/audit/data` | Clear records in a time range |
| GET | `/salesforcecustomerservice/audit/archives` | Archive index |
| GET | `/salesforcecustomerservice/audit/archives/{key}` | Archived records for a key |
| DELETE | `/salesforcecustomerservice/audit/archives/{key}` | Delete an archive |
| POST | `/salesforcecustomerservice/audit/archiveNow` | Manually trigger an archive |
| GET | `/salesforcecustomerservice/audit/globalCounter` | All-time counter |

## Notes and Limitations

- **Theming**: With SDK v1.1.0 the bundled dashboard uses the shared (Microsoft-themed) styling. A Salesforce-themed variant is planned for a later SDK release. This is cosmetic only; functionality is unaffected.
- **Scope**: Audit records reflect authentication/OAuth activity for the invoker; the dashboard is a monitoring surface, not a configuration screen. Use the [Authentication](pages/Authentication.md) tab to configure credentials.
- **SDK-provided UI**: Because the dashboard page ships with the SDK, the exact on-screen layout and labels are governed by the SDK version in use and may evolve between releases.

## See Also

- [Authentication](pages/Authentication.md) — configure Salesforce credentials and choose an auth flow
- [Extension Configuration](pages/ExtensionConfiguration.md) — connection parameter reference
- [Troubleshooting](pages/Troubleshooting.md) — resolving authentication and connectivity issues
