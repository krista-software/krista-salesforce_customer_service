# Salesforce Customer Service Extension

Welcome to the Krista Salesforce Customer Service Extension documentation. This extension enables seamless integration between Krista and Salesforce Customer Service, providing powerful automation capabilities for case management, customer data operations, and workflow optimization.

## Overview

The Salesforce Customer Service Extension connects Krista to Salesforce, enabling automated case management, customer relationship operations, and real-time data synchronization. Built with OAuth 2.0 security and comprehensive error handling, this extension provides reliable access to Salesforce Customer Service functionality.

## Key Features

- **Case Management**: Fetch a case by id, retrieve case feed items, share updates, and monitor case changes
- **Custom Field Operations**: Access and manage custom fields across Salesforce objects
- **Entity Operations**: Full CRUD operations for Accounts, Cases, Contacts, Customers, and Individuals
- **Real-time Alerts**: Automated notifications for case changes and updates
- **Multiple Authentication Modes**:
  - **Quick Setup** — One-click connect using Krista's managed Salesforce Connected App (no Connected App configuration required)
  - **User Sign-In** (Authorization Code) — Each user authenticates with their own Salesforce account
  - **Server-to-Server** (Client Credentials) — Service account authentication via the org's My Domain
  - **Username + Password** (legacy) — For environments that still require it
- **AI-agent Tool Calls**: Key catalog requests are marked `tool = true`, making them callable from agentic workflows
- **Comprehensive Validation**: Input validation with user-friendly error handling
- **Retry Support**: Built-in retry mechanisms for reliable operations

## Quick Start Guide

### 1. Prerequisites
- Salesforce org with appropriate permissions
- Krista platform access
- System Administrator access to Salesforce

### 2. Setup Steps
1. **[Create Salesforce Connected App](pages/CreatingSalesforceApp.md)** - Configure OAuth application
2. **[Configure Extension](pages/ExtensionConfiguration.md)** - Set up connection parameters
3. **[Authenticate](pages/Authentication.md)** - Connect to your Salesforce instance
4. **Test Connection** - Verify setup with a simple catalog request

### 3. First Request
Try the "Get Case Feed Items By Case Id" request to verify your setup is working correctly.

## Documentation Structure

### 🚀 Getting Started
- [Extension Configuration](pages/ExtensionConfiguration.md) - Complete setup and configuration guide
- [Authentication](pages/Authentication.md) - OAuth 2.0 authentication and token management
- [Creating Salesforce App](pages/CreatingSalesforceApp.md) - Connected App creation and setup

### 📋 Case Management
- [Get Case By Id](pages/GetCaseById.md) - Fetch a single case by its Salesforce Id as a typed Case entity
- [Get Case Feed Items By Case Id](pages/GetCaseFeedItemsByCaseId.md) - Retrieve case feed posts and comments
- [Share Update to Case](pages/ShareUpdateToCase.md) - Add updates and files to case feeds
- [Case Change Alert](pages/CaseChangeAlert.md) - Monitor and respond to case changes

### 📊 Content Operations
- [Retrieve Custom Fields For Given Object](pages/RetrieveCustomFieldsForGivenObject.md) - Access custom field data

### 🏢 Entity Operations
- [Individual Entity](pages/IndividualEntity.md) - GDPR compliance and personal data management
- [Customer Entity](pages/CustomerEntity.md) - Business relationship management
- [Account Entity](pages/AccountEntity.md) - Organization and company management
- [Case Entity](pages/CaseEntity.md) - Support ticket and case operations
- [Contact Entity](pages/ContactEntity.md) - Contact and person management

### 📖 Reference & Support
- [Supported Requests](pages/SupportedRequests.md) - Complete catalog and entity request reference
- [Troubleshooting](pages/Troubleshooting.md) - Common issues, solutions, and best practices
- [Release Notes](pages/ReleaseNotes.md) - Version history, updates, and changes

## Support & Resources

- **Extension Version**: 2.0.18
- **OAuth Substrate**: Krista's centralized OAuth substrate (Salesforce provider, 1.1.0)
- **Salesforce API Versions Used**: v48.0 (Chatter), v53.0 (sObjects/SOQL), v58.0 (file attachments)
- **Authentication Flows**: Quick Setup, Authorization Code, Client Credentials, Password Grant
- **Java Version**: Java 21
- **Krista APIs**: 1.0.125-sp1
- **Supported Salesforce Objects**: Account, Case, Contact, Customer, Individual

## Need Help?

1. **Configuration Issues**: Check the [Extension Configuration](pages/ExtensionConfiguration.md) guide
2. **Authentication Problems**: Review the [Authentication](pages/Authentication.md) documentation
3. **Request Errors**: Consult the [Troubleshooting](pages/Troubleshooting.md) guide
4. **API Questions**: Reference the individual catalog request pages

---

*This documentation follows Krista Extension Documentation Guidelines to ensure consistency, accuracy, and usability across all extension documentation.*

