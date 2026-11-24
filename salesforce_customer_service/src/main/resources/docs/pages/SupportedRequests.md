# Supported Requests - Entity Operations

## Overview

The Krista Salesforce Customer Service Extension supports full CRUD (Create, Read, Update, Delete) operations for five Salesforce entities. These entity operations are performed through Krista's standard entity request mechanisms, providing a consistent interface for managing Salesforce data.

## Supported Entities

The extension supports the following Salesforce entities. Click on each entity name to view detailed documentation:

### 1. [Individual Entity](pages/IndividualEntity.md)
Person records for GDPR and privacy compliance. Individual records store personal information and are commonly used in conjunction with Customer records.

**Supported Operations**: Search, Get, Create, Update, Delete

**Key Fields**: First Name, Last Name, Birth Date

**Use Cases**: GDPR compliance, customer onboarding, data subject access requests, right to be forgotten

---

### 2. [Customer Entity](pages/CustomerEntity.md)
Customer account records that represent business relationships. Customer records reference Individual records via Party Id for privacy-compliant data management.

**Supported Operations**: Search, Get, Create, Update, Delete

**Key Fields**: Name, Party Id, Customer Status Type, Total Life Time Value

**Use Cases**: Customer onboarding, status management, lifetime value tracking, customer search

---

### 3. [Account Entity](pages/AccountEntity.md)
Business account records representing organizations or companies. Accounts serve as the central hub for managing customer relationships, contacts, cases, and opportunities.

**Supported Operations**: Search, Get, Create, Update, Delete

**Key Fields**: Name, Account Number, Phone, Industry, Billing/Shipping Address

**Use Cases**: New customer onboarding, account information updates, account hierarchy management

---

### 4. [Case Entity](pages/CaseEntity.md)
Customer service case records for tracking issues, questions, and requests. Cases provide a complete history of customer interactions and problem resolution.

**Supported Operations**: Search, Get, Create, Update, Delete

**Key Fields**: Subject, Status, Priority, Contact ID, Account ID, Description

**Use Cases**: Support ticket creation, case status management, case search and retrieval, case closure

---

### 5. [Contact Entity](pages/ContactEntity.md)
Individual people associated with accounts. Contacts are typically customers, partners, or stakeholders who interact with your organization.

**Supported Operations**: Search, Get, Create, Update, Delete

**Key Fields**: Name, Title, Email, Phone, Account ID, Department

**Use Cases**: New contact creation, contact information updates, account contact management

---

## Entity Operations Overview

Each entity supports the following standard operations:

### Search
Query for records based on search criteria. Returns a list of matching records.

### Get
Retrieve a specific record with all available fields. Typically used after a Search operation.

### Create
Create a new record with the provided field values. Required fields must be provided.

### Update
Modify an existing record. Only the fields you specify will be updated; other fields remain unchanged.

### Delete
Permanently remove a record from Salesforce. This operation cannot be undone through the API.

---

## General Guidelines

### Field Limitations

> **⚠️ Important**: For all entity operations, only the fields explicitly listed in the input parameters are supported. Other fields that may be visible in the entity structure are for internal use only and cannot be set through these requests.

### Required vs Optional Fields

- **Required fields** must be provided for the operation to succeed
- **Optional fields** can be omitted; Salesforce will use default values or leave them blank
- For Update operations, only provide the fields you want to change

### Entity Relationships

Understanding entity relationships is crucial for proper data management:

- **Customer → Individual**: Customer's Party Id references an Individual record
- **Case → Contact**: Case's Contact ID references a Contact record
- **Case → Account**: Case's Account ID references an Account record
- **Contact → Account**: Contact's Account ID references an Account record

### Best Practices

1. **Search Before Create**: Always search for existing records before creating new ones to avoid duplicates
2. **Validate IDs**: Ensure referenced IDs (Party Id, Account ID, Contact ID) exist before creating related records
3. **Handle Deletions Carefully**: Deleted records cannot be recovered through the API
4. **Use Appropriate Permissions**: Ensure users have appropriate CRUD permissions on each entity
5. **Respect Salesforce Validation Rules**: Entity operations are subject to Salesforce validation rules and triggers
6. **Update Only Changed Fields**: For Update operations, only provide the fields that need to be changed
7. **Maintain Data Quality**: Provide complete and accurate information for all required fields

---

## Quick Reference

| Entity | Primary Use | Key Required Fields | Common Search Fields |
|--------|-------------|---------------------|---------------------|
| [Individual](pages/IndividualEntity.md) | GDPR compliance, personal data | Last Name | Id, Last Name |
| [Customer](pages/CustomerEntity.md) | Business relationships | Name, Party Id | Id, Party Id, Name |
| [Account](pages/AccountEntity.md) | Organizations/companies | Name | ID, Name, Account Number, Phone |
| [Case](pages/CaseEntity.md) | Support tickets | None (all optional) | ID, Contact ID, Account ID, Status |
| [Contact](pages/ContactEntity.md) | People at accounts | Name | Name, Account ID, Email, Phone |

---

## See Also

### Entity Documentation
- [Individual Entity Operations](pages/IndividualEntity.md)
- [Customer Entity Operations](pages/CustomerEntity.md)
- [Account Entity Operations](pages/AccountEntity.md)
- [Case Entity Operations](pages/CaseEntity.md)
- [Contact Entity Operations](pages/ContactEntity.md)

### Catalog Requests
- [Get Case By Id](pages/GetCaseById.md) — Fetch a single Salesforce Case as the typed `Case` entity
- [Get Case Feed Items By Case Id](pages/GetCaseFeedItemsByCaseId.md)
- [Share Update to Case](pages/ShareUpdateToCase.md)
- [Case Change Alert](pages/CaseChangeAlert.md)
- [Retrieve Custom Fields For Given Object](pages/RetrieveCustomFieldsForGivenObject.md)

### Configuration and Support
- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Troubleshooting](pages/Troubleshooting.md)
- [Release Notes](pages/ReleaseNotes.md)

