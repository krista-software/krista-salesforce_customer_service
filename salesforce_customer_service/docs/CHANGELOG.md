# Salesforce Customer Service Extension — Changelog

## 2.0.18 — Migrated to Krista's centralized OAuth substrate + new Authentication tab

### Changed

- **Credential editor moved from the Setup-tab Invoker form to a new
  Authentication tab** (powered by Krista's bundled `oauth/index.html`). The
  Setup-tab fields remain visible in this release for one-version migration
  compatibility (see "Upgrade behavior" below) but are read-only/inert at
  runtime — the Authentication tab is the canonical credential editor.
- OAuth flow handling now delegates to Krista's centralized OAuth substrate
  (`microsoft-auth-sdk:1.1.0`, Salesforce provider package `app.krista.sdk.sfauth`).
- New **Audit Dashboard** tab — real-time monitoring of OAuth health, request
  rates, latency, and failure modes. Provider-aware (SF-specific chip set:
  Auth / Email / Instance / Org / Token).
- New **Quick Setup** flow (`AUTH_CODE_QUICK`) — uses Krista's bundled Public
  Connected App instead of requiring customers to create their own SF Connected
  App. Admin enters their Salesforce User Email; per-user OAuth runs against
  Krista's app.
- Internal classes refactored:
    - `SalesforceCustomerServiceAuthenticator` now extends the SDK's
      `AbstractSalesforceRequestAuthenticator`
    - `SalesforceTokenProvider` delegates to `SalesforceOAuth2Sdk` via a
      per-request factory (`SalesforceCustomerServiceSdkFactory`)
    - `AuthenticationResource.callback()` calls `SalesforceOAuth2Sdk.handleCallback`
- **Per-user authentication is now hermetic from operator-context configuration
  (CRIT-029).** When `invokeAsUser=true`, all per-user code paths use Krista's
  bundled Public Connected App regardless of what the admin saved in the
  Authentication tab. This means the per-user refresh-token key is stable across
  admin flow changes — no spurious re-auth prompts when the admin switches
  between Quick Setup / Server-to-Server / Advanced AUTH_CODE.

### Upgrade behavior — existing customers (CRIT-031)

Customers upgrading from v2.0.17 (Setup-tab Invoker form) are migrated
automatically on first invoker boot after deployment. No manual action required.

- **Credential config (clientId, clientSecret, Instance URL, username, password,
  securityToken, sandbox)**: copied from Krista's Invoker-attribute store into
  the new Authentication-tab KV blob with an inferred `savedFlow` tag
  (`PASSWORD_GRANT`, `CLIENT_CREDENTIALS`, or `AUTH_CODE` depending on which
  fields were populated). The Authentication tab opens pre-filled on the matching
  flow tab. Legacy Invoker-attribute values are preserved (not deleted) as a
  rollback safety net.
- **Refresh tokens**: continue to live at their original KV keys —
  `{accountId}&{clientId}{clientSecret}` format with no separator between
  clientId and clientSecret. The v2.0.18 `LegacyKeyedSalesforceTokenStore` was
  initially shipped with a buggy `+` separator that has been corrected; the
  adapter also dual-reads the buggy slot once and migrates entries forward, so
  anyone who authenticated against an early 2.0.18 build is covered.
- **One-time re-authorization required for users on Advanced AUTH_CODE flow only**:
  v2.0.17 stored per-user refresh tokens keyed by the admin's customer Connected
  App credentials. v2.0.18 routes per-user requests through Krista's Public
  Connected App (per CRIT-029 hermeticity invariant). Salesforce-issued refresh
  tokens are scoped to the issuing OAuth app and cannot be presented to a
  different app, so each user must re-authorize once after upgrade. Customers on
  `CLIENT_CREDENTIALS` or `PASSWORD_GRANT` are not affected (those flows do not
  issue per-user refresh tokens).

A trace of the form `[SF-MIGRATE] CRIT-031 DONE — invokerId=… inferredFlow=…
→ new-KV-blob written` confirms successful migration in the container logs.

### Removed

- `RefreshTokenStore` internal helper (replaced by the SDK's structured storage with
  legacy-key adapter `LegacyKeyedSalesforceTokenStore`)
- Hand-rolled ScribeJava `OAuth20Service.refreshAccessToken` / `getAccessToken` /
  `getAuthorizationUrl` call sites — all routed through the SDK now

### Migration path to PKCE (when ready)

When Salesforce mandates PKCE for this Connected App (expected 2026 ECA requirements
for new ISV apps):

1. Update the Salesforce Connected App configuration to a "Web Server App with PKCE"
   type.
2. Edit `SalesforceCustomerServiceAttributes.toSalesforceConfig()` and change
   `.usePkce(false)` to `.usePkce(true)`.
3. Redeploy this extension.

No other code changes are required — the SDK handles the PKCE code-challenge/verifier
lifecycle automatically once `usePkce(true)` is set.
