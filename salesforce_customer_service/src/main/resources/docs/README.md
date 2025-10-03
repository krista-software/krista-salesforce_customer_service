# Krista Salesforce Customer Service Extension

## Overview

The Krista Salesforce Customer Service Extension enables seamless integration between Krista and Salesforce Customer Service, allowing you to automate case management, retrieve custom fields, and manage customer service operations directly from your Krista conversations.

This extension connects to Salesforce using OAuth 2.0 authentication and supports both service account and user account authentication modes, providing flexibility for automated workflows and user-specific actions.

## Key Features

- **Case Feed Management**: Retrieve and share updates to case feeds with optional file attachments
- **Real-time Case Alerts**: Receive notifications when cases are created or updated in Salesforce
- **Custom Field Retrieval**: Access custom fields from any Salesforce object (Account, Case, Contact, Customer, Individual)
- **Flexible Authentication**: Support for both service account (automated) and user account (interactive) authentication
- **Entity Management**: Full CRUD operations for Individual, Customer, Account, Case, and Contact entities
- **Sandbox Support**: Connect to both production and sandbox Salesforce environments
- **OAuth 2.0 Security**: Industry-standard authentication with automatic token refresh

## Quick Start Guide

### 1. Prerequisites
- Salesforce Developer or Enterprise account
- Krista Appliance version 3.5.1 or higher
- Admin access to Salesforce to create Connected Apps

### 2. Setup Steps
1. [Create a Salesforce Connected App](pages/CreatingSalesforceApp.md) to obtain OAuth credentials
2. [Configure the extension](pages/ExtensionConfiguration.md) in Krista with your Salesforce credentials
3. [Understand authentication modes](pages/Authentication.md) and choose the appropriate mode for your use case
4. Start using catalog requests in your conversations

### 3. First Conversation
Try retrieving case feed items:
- Use the "Get Case Feed Items By Case Id" catalog request
- Provide a valid Salesforce Case ID
- Optionally specify the number of posts to retrieve

## Documentation Structure

### Getting Started
- [Extension Configuration](pages/ExtensionConfiguration.md) - Complete setup guide with all configuration parameters
- [Authentication](pages/Authentication.md) - OAuth 2.0 flows, token management, and authentication modes
- [Creating Salesforce Connected App](pages/CreatingSalesforceApp.md) - Step-by-step guide to set up Salesforce

### Case Management
- [Get Case Feed Items By Case Id](pages/GetCaseFeedItemsByCaseId.md) - Retrieve posts and updates from a case feed
- [Share Update to Case](pages/ShareUpdateToCase.md) - Add updates and files to case feeds
- [Case Change Alert](pages/CaseChangeAlert.md) - Receive real-time notifications for case changes

### Content Operations
- [Retrieve Custom Fields For Given Object](pages/RetrieveCustomFieldsForGivenObject.md) - Access custom fields from Salesforce objects

### Entity Operations
The extension supports full CRUD operations for the following entities:
- **[Individual](pages/IndividualEntity.md)**: Person records for GDPR/privacy compliance
- **[Customer](pages/CustomerEntity.md)**: Customer account records with Individual references
- **[Account](pages/AccountEntity.md)**: Business account records for organizations
- **[Case](pages/CaseEntity.md)**: Customer service case/support ticket records
- **[Contact](pages/ContactEntity.md)**: Individual people associated with accounts

> **📝 Note**: Entity operations are performed through Krista's standard entity request mechanisms. Refer to the [Entity Operations Overview](pages/SupportedRequests.md) page for a summary, or click on individual entity links above for detailed documentation.

## Support & Resources

- **Extension Version**: 2.0.15
- **Krista Service APIs Java**: 1.0.115
- **Global Catalog Version**: GC-2025.09.3
- **Appliance Release Version**: 3.5.1 or higher
- **Salesforce API**: REST API v52.0+
- **OAuth Scopes**: `api`, `refresh_token`, `full` (depending on your requirements)

### Additional Resources
- [Salesforce REST API Documentation](https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/)
- [Salesforce OAuth 2.0 Documentation](https://help.salesforce.com/articleView?id=remoteaccess_oauth_web_server_flow.htm)
- [Troubleshooting Guide](pages/Troubleshooting.md)
- [Release Notes](pages/ReleaseNotes.md)

## Important Notes

> **⚠️ Security**: Always store Client ID and Client Secret securely. Never commit credentials to version control.

> **📝 Sandbox vs Production**: Use the Sandbox flag to connect to Salesforce sandbox environments for testing.

> **💡 Custom Fields**: All custom field names in Salesforce must end with `__c`. The extension validates this requirement.

---

**Need Help?** Check the [Troubleshooting Guide](pages/Troubleshooting.md) for common issues and solutions.

