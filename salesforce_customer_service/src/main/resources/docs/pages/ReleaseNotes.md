# Release Notes

## Version 2.0.18

### Release Information

- **Release Calendar Month**: June 2026
- **Krista Service APIs Java**: 1.0.125-sp1
- **OAuth Substrate**: Krista's centralized OAuth substrate (Salesforce provider, 1.1.0)
- **Java Version**: 21
- **Gradle Version**: 9.0

### What's New

This release migrates the extension to Krista's centralized OAuth substrate, ships a new **Quick Setup** flow that requires no Salesforce-side configuration, and adds a new **Get Case By Id** catalog request.

#### Key Improvements

**One-click Quick Setup**

- New **Quick Setup** authentication flow uses a Krista-managed Salesforce Connected App
- No Salesforce-side setup required — operator enters their admin email + sandbox toggle and clicks Authorize
- Refresh tokens are stored under a stable per-admin-email identity, so multiple admins can coexist without overwriting each other's tokens
- Best path for greenfield deployments and demos

**Multiple Authentication Flows in One Tab**

The Authentication tab now exposes a flow picker that supports:

- **Quick Setup** (Krista-managed Connected App) — new
- **User Sign-In** (Authorization Code) — for per-user auth
- **Server-to-Server** (Client Credentials) — for service-account automation
- **Username + Password** — legacy compatibility

The form is themed and SDK-bundled — no more legacy Invoker form. Stored secrets are surfaced as a masked placeholder (`••••••••`) and automatically resolved back to their stored values on save, so operators don't have to re-type them.

**New Catalog Request: Get Case By Id**

A new catalog request **Get Case By Id** fetches a single Salesforce Case by its unique Salesforce Id and returns it as the typed `Case` entity. The request is marked `tool = true`, making it callable by Krista AI agents. See [Get Case By Id](pages/GetCaseById.md) for full input/output details.

**AI-agent Tool Calls**

Several existing catalog requests are now marked `tool = true` (AI-agent callable):

- Get Case By Id (new)
- Get Case Feed Items By Case Id
- Share Update to Case

This unlocks agentic workflows where the agent can autonomously decide to call these requests with model-resolved input parameters.

**Authentication migrated to Krista's centralized OAuth substrate**

The OAuth code path is now powered by Krista's centralized OAuth substrate:

- Token acquisition, refresh, and code exchange delegated to `SalesforceOAuth2Sdk`
- Hand-rolled ScribeJava call sites removed
- Existing refresh tokens preserved — no customer re-authorization required on upgrade (a `LegacyKeyedSalesforceTokenStore` adapter translates between SDK and legacy KV key formats)
- PKCE remains intentionally off to preserve compatibility with classic Connected Apps; documented migration path is available in `docs/CHANGELOG.md` for when Salesforce mandates PKCE for new ISV apps

**New: Audit Dashboard tab**

A new **Audit Dashboard** custom tab gives operators visibility into the extension's authentication activity without leaving Krista. It surfaces:

- A table of OAuth **audit records** with a rolled-up **summary**
- A live **health check** of the configured credentials and Salesforce connectivity
- **Archive** management — browse archived record sets, trigger an archive on demand, and clear records within a time range
- An all-time **global counter** of audit events

The dashboard is powered by Krista's centralized OAuth substrate (`AbstractOAuth2AuditResource`) and its UI is bundled from the SDK. See [Audit Dashboard](pages/AuditDashboard.md) for details.

> **Note**: In SDK v1.1.0 the bundled dashboard uses the shared (Microsoft-themed) styling; a Salesforce-themed variant is planned for a later SDK release. Functionality is unaffected.

**Krista APIs Updated**

- `krista-apis` 1.0.118 → **1.0.125-sp1** (unlocks `@CatalogRequest(tool = true)` for AI-agent callability)
- Annotation processors bumped to match

**Expanded Test Coverage**

- 83 new Mockito unit tests added across 7 classes covering the SDK migration touchpoints
- Suite total: **561 tests / 0 failures**

### Migration Notes

#### For Existing Customers

This release is **transparent for existing customers**:

- Saved credentials continue to work without re-entry
- Refresh tokens are honored at their original KV keys
- No re-authorization prompts on upgrade
- The legacy Invoker form is replaced by the themed Auth tab, but the saved values round-trip correctly

#### For New Deployments

Choose **Quick Setup** for the fastest path to a working connection. No Salesforce-side configuration is required — Krista provides the Connected App.

If you need per-user audit trails or a customer-owned Connected App, choose **User Sign-In** (Authorization Code) and follow the [Creating Salesforce Connected App](pages/CreatingSalesforceApp.md) guide.

#### Breaking Changes

There are no breaking changes in this release. All existing configurations continue to work.

### Known Issues

- Quick Setup's two-hop callback (Salesforce → central Krista handler → extension `/v3/oauth/callback`) requires the central handler to forward Salesforce responses. This is a platform-side capability; if the central handler is not yet updated to route Salesforce responses, Quick Setup will halt after the consent screen and the operator must complete authorization via the User Sign-In flow instead.

### See Also

- [Authentication Guide](pages/Authentication.md) — full details on each flow
- [Extension Configuration](pages/ExtensionConfiguration.md) — flow-specific parameter reference
- [Get Case By Id](pages/GetCaseById.md) — new catalog request
- [Audit Dashboard](pages/AuditDashboard.md) — new authentication-audit tab
- [Changelog](../../docs/CHANGELOG.md) — engineering-level changelog with PKCE migration path
- [Troubleshooting Guide](pages/Troubleshooting.md)

---

## Version 2.0.17

### Release Information

- **Release Calendar Month**: October 2025
- **Krista Service APIs Java**: 1.0.118
- **Global Catalog Version**: GC-2025.11.5
- **Java Version**: 21
- **Gradle Version**: 8.10

### What's New

This patch release adds comprehensive test coverage and updates dependencies to the latest Krista platform version.

#### Key Improvements

**Comprehensive Test Suite**
- Added 32 new test classes with extensive coverage of all major components
- Implemented unit tests for:
  - Extension core (SalesforceCustomerServiceExtension, Authenticator)
  - Validators (CaseIdValidator, NumberOfPostsValidator, etc.)
  - Utilities (StringHelper, DatePlus, Constants)
  - Models (SFAccount, SFCase, SFContact, SFCustomer, etc.)
  - Catalog entities (Account, Case, Contact, Customer, Individual)
  - Error handlers and response factories
- Tests follow AAA (Arrange-Act-Assert) pattern with comprehensive edge case coverage
- Includes tests for null values, empty inputs, boundary values, and error scenarios
- All tests include clear JavaDoc documentation and meaningful assertion messages
- Uses Mockito for proper unit test isolation

**Dependency Updates**
- Updated Krista APIs from 1.0.117 to 1.0.118
- Updated extension-impl-anno-processors to 1.0.118
- Ensures compatibility with latest Krista platform features and improvements

**Documentation Enhancements**
- Added comprehensive Architecture.md with detailed system design documentation
- Added complete extension documentation in DOCX format
- Updated release notes with accurate change information

### Technical Changes

#### Test Coverage Details
- **Core Extension Tests**:
  - SalesforceCustomerServiceExtension (1,064 lines) - Tests constructors, authenticator, connection validation, custom tabs
  - SalesforceCustomerServiceAuthenticator (380 lines) - Tests OAuth2 authentication flow with comprehensive mocking

- **Validator Tests**:
  - CaseIdValidator (331 lines) - Tests case ID validation with valid/invalid cases
  - NumberOfPostsValidator - Tests post count validation
  - Other validator classes with edge case coverage

- **Utility Tests**:
  - StringHelper (274 lines) - Tests all string manipulation methods
  - DatePlus - Tests date handling utilities
  - Constants - Tests constant values and configurations

- **Model Tests**:
  - SFAccount, SFCase, SFContact, SFCustomer, SFIndividual - Tests all getter/setter methods
  - AccountImpl (204 lines) - Tests account implementation with 40+ test cases

- **Catalog Entity Tests**:
  - Account, Case, Contact, Customer, Individual - Tests entity operations

- **Error Handler Tests**:
  - ErrorHandlingStateManager - Tests error state management
  - RemediationActionFactory - Tests remediation action creation

#### Dependency Updates
- **Krista APIs**: Updated from 1.0.117 to 1.0.118 for latest platform features
- **Annotation Processors**: Updated to 1.0.118 for consistency

### Improvements

#### Test Coverage and Quality
- **Comprehensive Testing**: 32 new test classes with 6,200+ lines of test code
- **High-Quality Tests**: All tests follow AAA (Arrange-Act-Assert) pattern with clear documentation
- **Proper Isolation**: Tests use Mockito for proper unit test isolation and mocking
- **Edge Case Coverage**: Extensive testing of boundary conditions, null values, empty inputs, and error scenarios
- **Maintainability**: Well-documented tests with meaningful names and assertion messages

#### Platform Compatibility
- **Latest Krista APIs**: Updated to version 1.0.118 for latest features and bug fixes
- **Consistent Dependencies**: All Krista dependencies updated to matching versions
- **Java 21 Ready**: Extension continues to support Java 21 runtime environment

### Migration Notes

This is a patch release with no breaking changes. Existing configurations and integrations will continue to work without modification.

#### For Developers
- Extension continues to support Java 21 for compilation and runtime
- New comprehensive test suite can be run with `./gradlew test`
- Run specific test suites: `./gradlew test --tests "*validators*"` or `./gradlew test --tests "*util*"`
- All tests follow AAA pattern and include comprehensive documentation

#### For Users
- No configuration changes are required
- All existing functionality remains unchanged
- This release focuses on internal quality improvements through testing

### Known Issues

None at this time.

### See Also

- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication Guide](pages/Authentication.md)
- [Troubleshooting Guide](pages/Troubleshooting.md)
- [Architecture Documentation](../../docs/Architecture.md)

---

## Version 2.0.15

### Release Information

- **Release Date**: January 2025
- **Developers**: Vaibhav Choudhary
- **Krista Service APIs Java**: 1.0.117
- **Global Catalog Version**: GC-2025.10.1

### What's New

This major release brings comprehensive OAuth 2.0 support, enhanced error handling, and significant code quality improvements to the Salesforce Customer Service Extension.

#### Key Features

**OAuth 2.0 Authentication**
- Full support for OAuth 2.0 Authorization Code flow (user authentication)
- Client Credentials flow support for service account authentication
- Improved token management and automatic refresh handling
- Enhanced security with modern OAuth 2.0 standards

**Enhanced Error Handling**
- Comprehensive credential validation with clear error messages
- Improved error diagnostics across all catalog requests
- Better troubleshooting guidance for common issues
- Refactored error handling in all extension areas

**Code Quality Improvements**
- Modernized Java code patterns
- Comprehensive JavaDoc documentation
- Improved code organization and maintainability
- Enhanced logging for better debugging

**Documentation Enhancements**
- Complete documentation rewrite with visual guides
- Step-by-step OAuth 2.0 setup instructions
- Detailed troubleshooting guide
- Individual documentation pages for each catalog request and entity

### Resolved Issues

#### OAuth 2.0 and Authentication Enhancements

- **[KE-2428](https://antbrains.atlassian.net/browse/KE-2428)**: Added comprehensive OAuth 2.0 support with multiple authentication flows
  - **Impact**: Extension now supports modern OAuth 2.0 authentication patterns including Authorization Code flow and Client Credentials flow
  - **Resolution**:
    - Implemented OAuth 2.0 Authorization Code flow for user-based authentication
    - Added Client Credentials flow support for service account authentication
    - Refactored SalesforceTokenProvider with improved error handling and token management
    - Enhanced documentation with detailed OAuth flow diagrams and setup guides
  - **Benefit**:
    - Users can now authenticate using their own Salesforce credentials (Authorization Code flow)
    - Service accounts can authenticate automatically for background processes (Client Credentials flow)
    - Improved security with modern OAuth 2.0 standards
    - Better token refresh handling and session management

#### Error Handling and Validation

- **[KE-1949](https://antbrains.atlassian.net/browse/KE-1949)**: Improved error handling and Java version compatibility
  - **Impact**: Enhanced stability and compatibility with modern Java versions
  - **Resolution**:
    - Updated Java version compatibility
    - Improved logger statements for better debugging
    - Enhanced error messages for troubleshooting
  - **Benefit**: Better error diagnostics and improved extension stability

- **[KE-1894](https://antbrains.atlassian.net/browse/KE-1894)**: Fixed issue where entering invalid credentials caused the extension to not work as expected
  - **Impact**: Users entering incorrect Salesforce credentials would experience unexpected behavior
  - **Resolution**:
    - Improved credential validation with comprehensive error checking
    - Enhanced error messaging to provide clear feedback when credentials are invalid
    - Added validation for all authentication parameters
    - Refactored error handling in CaseManagementArea and ContentArea
  - **Benefit**:
    - Users now receive clear, actionable error messages when authentication fails
    - Better guidance for troubleshooting credential issues
    - Reduced confusion during initial setup

#### Code Quality and Refactoring

- **[KE-1118](https://antbrains.atlassian.net/browse/KE-1118)**: Code refactoring and modernization
  - **Impact**: Improved code maintainability and performance
  - **Resolution**:
    - Modernized Java code patterns
    - Refactored error handling across all areas
    - Added comprehensive JavaDocs
    - Improved code organization and structure
  - **Benefit**:
    - Easier maintenance and future enhancements
    - Better code readability
    - Improved developer experience

### Known Issues

There are no known issues in this release.

### Upgrade Notes

#### Upgrading from Previous Versions

1. **Backup Configuration**: Export your current extension configuration before upgrading
2. **Review Credentials**: Verify your Salesforce credentials are current and valid
3. **Test Authentication**: After upgrading, test authentication to ensure connectivity
4. **Verify Catalog Requests**: Test key catalog requests to ensure they function correctly
5. **Check Entity Operations**: Verify entity CRUD operations work as expected

#### Breaking Changes

There are no breaking changes in this release. All existing configurations and conversations will continue to work without modification.

### Compatibility

#### Supported Salesforce Editions

- Salesforce Enterprise Edition
- Salesforce Unlimited Edition
- Salesforce Performance Edition
- Salesforce Developer Edition

#### Supported Salesforce API Versions

- API Version 50.0 and above

#### Krista Platform Requirements

- Krista Appliance Release Version: 3.5.1 or higher
- Krista Service APIs Java: 1.0.115 or higher

### Deprecation Notices

There are no deprecations in this release.

### Security Updates

This release includes general security improvements:
- Enhanced credential validation
- Improved error handling to prevent information disclosure
- Updated OAuth token management

### Performance Improvements

- Optimized authentication flow for faster initial connection
- Improved error handling reduces unnecessary API calls
- Enhanced validation reduces failed requests

### Documentation Updates

This release includes a complete documentation overhaul with professional structure and visual enhancements:

#### New Documentation Structure
- **Organized Navigation**: Sidebar navigation with logical grouping of topics
- **Visual Guides**: 26 screenshots and diagrams added for step-by-step guidance
- **Separate Pages**: Individual documentation files for each catalog request and entity operation

#### Documentation Files Created
- **Getting Started** (3 files):
  - Extension Configuration guide with all parameters
  - OAuth 2.0 Authentication guide with flow diagrams
  - Creating Salesforce Connected App step-by-step guide

- **Catalog Requests** (4 files):
  - Get Case Feed Items By Case Id
  - Share Update to Case
  - Case Change Alert (with Salesforce workflow setup)
  - Retrieve Custom Fields For Given Object

- **Entity Operations** (6 files):
  - Individual Entity (GDPR compliance)
  - Customer Entity
  - Account Entity
  - Case Entity
  - Contact Entity
  - Entity Operations Overview

- **Additional Resources** (2 files):
  - Comprehensive Troubleshooting Guide (75+ error scenarios)
  - Release Notes (this document)

#### Documentation Enhancements
- **Visual Assets**: All images renamed with descriptive patterns (`<MD_File_Name>_<Image_Description>.png`)
- **Code Examples**: 40+ detailed usage examples across all documentation
- **Error Scenarios**: Comprehensive error handling documentation for each request
- **Best Practices**: Guidelines and recommendations for each feature
- **Cross-References**: Extensive linking between related documentation pages
- **Professional Formatting**: Consistent structure with tables, code blocks, and callouts

## Previous Versions

### Version 2.0.14 and Earlier

For information about previous versions, please contact Krista support or refer to the legacy documentation.

## Feedback and Support

### Reporting Issues

If you encounter any issues with this release:

1. Check the [Troubleshooting Guide](pages/Troubleshooting.md) for common solutions
2. Review Krista logs for detailed error information
3. Contact Krista support with:
   - Extension version number
   - Detailed description of the issue
   - Steps to reproduce
   - Error messages from logs
   - Salesforce edition and API version

### Feature Requests

To request new features or enhancements:

1. Contact your Krista account representative
2. Provide detailed use case and business justification
3. Include any specific Salesforce objects or features you'd like to see supported

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Troubleshooting](pages/Troubleshooting.md)
- [Supported Requests](pages/SupportedRequests.md)

