# Account Entity Operations

## Overview

The Account entity in Salesforce represents business account records. Accounts are organizations or companies that you do business with, and they serve as the central hub for managing customer relationships, contacts, cases, and opportunities.

The Krista Salesforce Customer Service Extension supports full CRUD (Create, Read, Update, Delete) operations for Account entities through Krista's standard entity request mechanisms.

## Supported Operations

- **Search Account** - Query for Account records based on criteria
- **Get Account** - Retrieve a specific Account record
- **Create Account** - Create a new Account record
- **Update Account** - Modify an existing Account record
- **Delete Account** - Remove an Account record

---

## Search Account

### Description

Returns a list of Account records matching the search criteria. Use this operation to find accounts by ID, name, account number, or phone.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| ID | Text | No | The unique Salesforce ID of the Account | "0065w00002AlDtDAAM" |
| Name | Text | No | The name of the Account | "Acme Inc" |
| Account Number | Text | No | The account number | "ACC-12345" |
| Phone | Text | No | The phone number | "+1-555-0100" |

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Account | List&lt;Account&gt; | List of Account records matching the search criteria |

### Usage Example

**Search by Name:**
```
Input:
  Name: "Acme Inc"

Output:
  Account: [
    {
      Id: "0015w00000CzXyBBB",
      Name: "Acme Inc",
      Account Number: "ACC-12345",
      Phone: "+1-555-0100",
      Industry: "Technology"
    }
  ]
```

---

## Get Account

### Description

Returns the selected Account record with all available fields.

### Input Parameters

None (Account is selected from search results)

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Account | Account | The selected Account record with all available fields |

---

## Create Account

### Description

Creates a new Account record in Salesforce with comprehensive business information including billing and shipping addresses.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Name | Text | **Yes** | The name of the Account | "Acme Inc" |
| Type | Text | No | The type of account (Customer, Partner, Prospect, etc.) | "Customer" |
| Fax | Text | No | Fax number | "(650) 222-1111" |
| Account Number | Text | No | Account number | "ACC-12345" |
| Website | Text | No | Website URL | "www.acme.com" |
| Sic | Text | No | Standard Industrial Classification code | "0311" |
| Owner ID | Text | No | ID of the account owner (user) | "0055w00000D9a8iAAC" |
| Created by ID | Text | No | ID of the user who created the account | "0015w00002inu5PAAP" |
| Clean Status | Text | No | Data quality status | "New" |
| Customer Priority | Text | No | Priority level (High, Medium, Low) | "High" |
| SLA Serial Number | Text | No | SLA serial number | "5582" |
| Created Date | Date | No | Date the account was created | "2025-01-15" |
| Phone | Text | No | Phone number | "+1-555-0100" |
| Number of Employees | Number | No | Number of employees | 500 |
| SLA Expiration Date | Date | No | SLA expiration date | "2025-12-31" |
| Ownership | Text | No | Ownership type (Public, Private, etc.) | "Public" |
| Industry | Text | No | Industry classification | "Technology" |

**Billing Address Fields:**

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Billing Street | Street | No | Billing street address | "123 Main St" |
| Billing City | City | No | Billing city | "San Francisco" |
| Billing State | State | No | Billing state/province | "California" |
| Billing Postal Code | Postal Code | No | Billing postal/ZIP code | "94105" |
| Billing Country | Country | No | Billing country | "USA" |

**Shipping Address Fields:**

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Shipping Street | Street | No | Shipping street address | "123 Main St" |
| Shipping City | City | No | Shipping city | "San Francisco" |
| Shipping State | State | No | Shipping state/province | "California" |
| Shipping Postal Code | Postal Code | No | Shipping postal/ZIP code | "94105" |
| Shipping Country | Country | No | Shipping country | "USA" |

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Account | Account | The newly created Account record with system-generated fields |

### Validation Rules

- **Name is required** - Cannot create an Account without a name
- **Date format** - Dates must be in ISO 8601 format (YYYY-MM-DD)
- **Number of Employees** - Must be a positive integer if provided

### Usage Example

**Create Account with basic information:**
```
Input:
  Name: "Acme Inc"
  Account Number: "ACC-12345"
  Phone: "+1-555-0100"
  Industry: "Technology"
  Website: "www.acme.com"

Output:
  Account: {
    Id: "0015w00000CzXyBBB",
    Name: "Acme Inc",
    Account Number: "ACC-12345",
    Phone: "+1-555-0100",
    Industry: "Technology",
    Website: "www.acme.com",
    Created Date: "2025-01-15T10:30:00Z"
  }
```

**Create Account with full address:**
```
Input:
  Name: "Beta Industries"
  Billing Street: "456 Oak Ave"
  Billing City: "New York"
  Billing State: "New York"
  Billing Postal Code: "10001"
  Billing Country: "USA"
  Shipping Street: "456 Oak Ave"
  Shipping City: "New York"
  Shipping State: "New York"
  Shipping Postal Code: "10001"
  Shipping Country: "USA"

Output:
  Account: {
    Id: "0015w00000CzXyCCC",
    Name: "Beta Industries",
    Billing Address: "456 Oak Ave, New York, NY 10001, USA",
    Shipping Address: "456 Oak Ave, New York, NY 10001, USA",
    Created Date: "2025-01-15T10:35:00Z"
  }
```

---

## Update Account

### Description

Updates an existing Account record with the provided details. Only the fields you specify will be updated; other fields remain unchanged.

### Input Parameters

Same as Create Account (all fields optional except Name)

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Account | Account | The updated Account record with all current field values |

### Usage Example

**Update Account phone and industry:**
```
Input:
  Id: "0015w00000CzXyBBB"
  Name: "Acme Inc"
  Phone: "+1-555-0200"
  Industry: "Software"

Output:
  Account: {
    Id: "0015w00000CzXyBBB",
    Name: "Acme Inc",
    Phone: "+1-555-0200",
    Industry: "Software",
    Last Modified Date: "2025-01-15T14:20:00Z"
  }
```

---

## Delete Account

### Description

Deletes the selected Account record from Salesforce. This operation permanently removes the Account record.

> **⚠️ Warning**: Deletion is permanent and cannot be undone through the API. Deleting an Account may also delete related records (Contacts, Cases, Opportunities) depending on your Salesforce configuration.

### Input Parameters

| Parameter Name | Type | Required | Description |
|----------------|------|----------|-------------|
| Account | Account | **Yes** | The Account entity to delete (selected from search/get results) |

### Output

None (operation returns success or error)

---

## Business Rules

### Field Limitations

> **⚠️ Important**: Only the fields explicitly listed in the input parameters are supported. Other fields that may be visible in the entity structure are for internal use only and cannot be set through these requests.

### Required vs Optional Fields

- **Name** is required for Create and Update operations
- All other fields are optional
- For Update operations, only provide the fields you want to change

### Entity Relationships

- **Account → Contact**: Contacts can be associated with Accounts
- **Account → Case**: Cases can be associated with Accounts
- **Account → Opportunity**: Opportunities are linked to Accounts
- Deleting an Account may cascade delete related records based on Salesforce configuration

---

## Error Handling

### Common Errors

**"Required field missing: Name"**
- **Cause**: Name was not provided in Create or Update operation
- **Solution**: Provide a valid Name value

**"Account not found"**
- **Cause**: The specified Account ID doesn't exist
- **Solution**: Verify the Account ID is correct and the record exists

**"Insufficient privileges"**
- **Cause**: User lacks CRUD permissions on Account object
- **Solution**: Verify user's profile has appropriate permissions in Salesforce

**"Cannot delete Account: related records exist"**
- **Cause**: Account has related Contacts, Cases, or Opportunities
- **Solution**: Delete or update related records first, or configure cascade delete rules

---

## Best Practices

### 1. Search Before Create
Always search for existing Account records before creating new ones to avoid duplicates.

### 2. Use Account Number
Assign unique account numbers for easier tracking and integration with external systems.

### 3. Complete Address Information
Provide complete billing and shipping addresses for accurate record keeping and reporting.

### 4. Set Appropriate Owner
Assign the correct Owner ID to ensure proper account management and visibility.

### 5. Maintain Data Quality
Use the Clean Status field to track data quality and completeness.

---

## Common Use Cases

### 1. New Customer Onboarding
```
Scenario: New business customer signs up
Action: Create Account with company information and addresses
Result: Complete customer account record for relationship management
```

### 2. Account Information Update
```
Scenario: Customer moves to new location
Action: Update Account with new billing and shipping addresses
Result: Current address information for invoicing and shipping
```

### 3. Account Search
```
Scenario: Support agent needs to find customer account
Action: Search Account by name or phone number
Result: Quick access to customer account information
```

### 4. Account Hierarchy Management
```
Scenario: Managing parent-child account relationships
Action: Create child accounts and link to parent account
Result: Organized account hierarchy for enterprise customers
```

---

## See Also

- [Contact Entity Operations](pages/ContactEntity.md) - Contact records associated with Accounts
- [Case Entity Operations](pages/CaseEntity.md) - Cases associated with Accounts
- [Customer Entity Operations](pages/CustomerEntity.md) - Customer records
- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Troubleshooting](pages/Troubleshooting.md)

