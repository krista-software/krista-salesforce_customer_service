# Release Notes

## Version 2.0.16

### Release Information

- **Release Calendar Month**: October 2025
- **Krista Service APIs Java**: 1.0.117
- **Global Catalog Version**: GC-2025.10.1
- **Java Version**: 21
- **Gradle Version**: 8.5

### What's New

This patch release focuses on improving user experience through enhanced error messages, comprehensive testing, technical modernization, and updated documentation.

#### Key Improvements

**Enhanced Error Messages**
- Completely refactored all error messages to follow a new format that clearly identifies problems, explains requirements, and provides actionable resolution steps
- Updated error messages in Constants.java, validator classes, DAO implementations, and store classes
- Improved user guidance for troubleshooting authentication, validation, and data operation issues

**Comprehensive Test Suite**
- Added extensive test coverage for all utility classes (StringHelper, SFObjectHelper, Validators)
- Implemented 100% coverage of public static methods with real concrete implementations (no Mockito)
- Included comprehensive edge case testing: null values, empty inputs, boundary values, special characters, and Unicode support
- Created detailed test documentation (TEST_RESULTS.md) with execution guidelines

**Technical Modernization**
- Ensured full Java 21 compatibility with updated build configuration
- Added Gradle 8.5 wrapper for consistent build environment
- Fixed deprecated API usage (URL constructor replaced with URI.create().toURL())
- Verified all code compiles and tests pass with Java 21

**Documentation Updates**
- Enhanced Troubleshooting.md with comprehensive error scenarios based on new error message format
- Updated Authentication.md error handling section with detailed resolution steps
- Improved error message clarity and actionability throughout documentation
- Added specific guidance for OAuth configuration, domain validation, and data operation issues

### Technical Changes

#### Error Message Improvements
- **Authentication Errors**: Enhanced messages for invalid client credentials, user credentials, and OAuth service initialization failures
- **Configuration Errors**: Improved guidance for missing Client ID/Secret, domain validation, and instance URL format issues
- **Validation Errors**: Better error messages for case ID validation, number of posts validation, file format validation, and custom field validation
- **Data Operation Errors**: Enhanced messages for missing record IDs, entity data, search queries, and case change alert processing

#### Test Coverage Enhancements
- **StringHelper**: 45 test cases covering all 6 public static methods including edge cases for null inputs, special characters, and large data sets
- **Validators**: 25 test cases covering both public static methods with comprehensive type conversion and boundary value testing
- **SFObjectHelper**: 12 test cases covering the upsertCaseMap method with various data scenarios and error conditions

#### Build and Compatibility
- **Java 21**: Full compatibility ensured with updated source and target compatibility settings
- **Gradle 8.5**: Added gradle wrapper for consistent build environment across development teams
- **Deprecation Fixes**: Resolved URL constructor deprecation warning by using URI.create().toURL()

### Resolved Issues

#### Error Message Enhancement
- **Improved User Experience**: All error messages now follow a consistent format that helps users understand what went wrong, what is required, and how to fix the issue
- **Better Troubleshooting**: Enhanced error messages provide specific, actionable steps rather than generic error descriptions
- **Comprehensive Coverage**: Updated error messages across all extension components including validators, DAOs, and utility classes

#### Test Coverage and Quality
- **Production-Ready Testing**: Comprehensive test suite ensures reliability and robustness of utility classes
- **Real Implementation Testing**: Tests use concrete implementations rather than mocks, providing more accurate validation
- **Edge Case Coverage**: Extensive testing of boundary conditions, null values, and error scenarios

#### Technical Modernization
- **Future-Proof Compatibility**: Java 21 support ensures the extension works with modern Java environments
- **Consistent Build Environment**: Gradle 8.5 wrapper ensures consistent builds across different development environments
- **Clean Code**: Removed deprecated API usage and modernized code patterns

### Migration Notes

This is a patch release with no breaking changes. Existing configurations and integrations will continue to work without modification.

#### For Developers
- The extension now requires Java 21 for compilation and runtime
- Use `./gradlew` instead of system gradle for consistent builds
- New comprehensive test suite can be run with `./gradlew test --tests "*util*"`

#### For Users
- Error messages are now more helpful and provide specific resolution steps
- Troubleshooting documentation has been significantly enhanced
- No configuration changes are required

### Known Issues

None at this time.

### See Also

- [Extension Configuration](ExtensionConfiguration.md)
- [Authentication Guide](Authentication.md)
- [Troubleshooting Guide](Troubleshooting.md)
- [Test Results Documentation](../../TEST_RESULTS.md)

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

1. Check the [Troubleshooting Guide](Troubleshooting.md) for common solutions
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

- [Extension Configuration](ExtensionConfiguration.md)
- [Authentication](Authentication.md)
- [Troubleshooting](Troubleshooting.md)
- [Supported Requests](SupportedRequests.md)

