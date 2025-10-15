# Contact Entity Operations

## Overview

The Contact entity in Salesforce represents individual people associated with accounts. Contacts are typically customers, partners, or other stakeholders who interact with your organization. They can be associated with Accounts and Cases for comprehensive relationship management.

The Krista Salesforce Customer Service Extension supports full CRUD (Create, Read, Update, Delete) operations for Contact entities through Krista's standard entity request mechanisms.

## Supported Operations

- **Search Contact** - Query for Contact records based on criteria
- **Get Contact** - Retrieve a specific Contact record
- **Create Contact** - Create a new Contact record
- **Update Contact** - Modify an existing Contact record
- **Delete Contact** - Remove a Contact record

---

## Search Contact

### Description

Returns a list of Contact records matching the search criteria. Use this operation to find contacts by name, account, phone, or email.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Name | Text | No | The name of the Contact | "John Smith" |
| Account ID | Text | No | ID of the account associated with the contact | "0015w00000CzXyBBB" |
| Account Name | Text | No | Name of the account | "Acme Inc" |
| Phone | Text | No | Phone number | "+1-555-0100" |
| Email | Email | No | Email address | "john@example.com" |

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Contact | List&lt;Contact&gt; | List of Contact records matching the search criteria |

### Usage Example

**Search by Name:**
```
Input:
  Name: "John Smith"

Output:
  Contact: [
    {
      Id: "0035w00000BxYzAAK",
      Name: "John Smith",
      Title: "VP of Sales",
      Email: "john@example.com",
      Phone: "+1-555-0100",
      Account Name: "Acme Inc"
    }
  ]
```

**Search by Email:**
```
Input:
  Email: "john@example.com"

Output:
  Contact: [
    {
      Id: "0035w00000BxYzAAK",
      Name: "John Smith",
      Email: "john@example.com",
      Account ID: "0015w00000CzXyBBB"
    }
  ]
```

---

## Get Contact

### Description

Returns the selected Contact record with all available fields.

### Input Parameters

None (Contact is selected from search results)

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Contact | Contact | The selected Contact record with all available fields |

---

## Create Contact

### Description

Creates a new Contact record in Salesforce. Use this operation to add new people to your customer relationship management system.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Name | Text | **Yes** | The name of the Contact | "John Smith" |
| Title | Text | No | Job title | "VP of Sales" |
| Reports to ID | Text | No | ID of the manager/supervisor contact | "0035w00000ManagerAAA" |
| Account ID | Text | No | ID of the account associated with the contact | "0015w00000CzXyBBB" |
| Created Date | Date | No | Date the contact was created | "2025-01-15" |
| Last Modified Date | Date | No | Date the contact was last modified | "2025-01-16" |
| Department | Text | No | Department name | "Sales" |
| Phone | Text | No | Phone number | "+1-555-0100" |
| Description | Paragraph | No | Additional notes or description | "Key decision maker" |
| Email | Email | No | Email address | "john@example.com" |

> **📝 Note**: To add Account Name, Account ID is required.

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Contact | Contact | The newly created Contact record with system-generated fields |

### Validation Rules

- **Name is required** - Cannot create a Contact without a name
- **Email format** - Must be a valid email address if provided
- **Account ID must exist** - If provided, the Account ID must reference an existing Account
- **Reports to ID must exist** - If provided, must reference an existing Contact
- **Date format** - Dates must be in ISO 8601 format (YYYY-MM-DD)

### Usage Example

**Create Contact with basic information:**
```
Input:
  Name: "John Smith"
  Title: "VP of Sales"
  Email: "john@example.com"
  Phone: "+1-555-0100"
  Department: "Sales"

Output:
  Contact: {
    Id: "0035w00000BxYzAAK",
    Name: "John Smith",
    Title: "VP of Sales",
    Email: "john@example.com",
    Phone: "+1-555-0100",
    Department: "Sales",
    Created Date: "2025-01-15T10:30:00Z"
  }
```

**Create Contact with Account association:**
```
Input:
  Name: "Jane Doe"
  Title: "CTO"
  Account ID: "0015w00000CzXyBBB"
  Email: "jane@acme.com"
  Phone: "+1-555-0200"
  Description: "Technical decision maker"

Output:
  Contact: {
    Id: "0035w00000BxYzBBL",
    Name: "Jane Doe",
    Title: "CTO",
    Account ID: "0015w00000CzXyBBB",
    Account Name: "Acme Inc",
    Email: "jane@acme.com",
    Phone: "+1-555-0200",
    Description: "Technical decision maker",
    Created Date: "2025-01-15T10:35:00Z"
  }
```

---

## Update Contact

### Description

Updates an existing Contact record with the provided details. Only the fields you specify will be updated; other fields remain unchanged.

### Input Parameters

Same as Create Contact (all fields optional except Name)

> **📝 Note**: To update Account Name, Account ID is required.

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Contact | Contact | The updated Contact record with all current field values |

### Usage Example

**Update Contact Title:**
```
Input:
  Id: "0035w00000BxYzAAK"
  Name: "John Smith"
  Title: "Senior VP of Sales"

Output:
  Contact: {
    Id: "0035w00000BxYzAAK",
    Name: "John Smith",
    Title: "Senior VP of Sales",
    Email: "john@example.com",
    Last Modified Date: "2025-01-15T14:20:00Z"
  }
```

**Update Contact Email and Phone:**
```
Input:
  Id: "0035w00000BxYzAAK"
  Name: "John Smith"
  Email: "john.smith@acme.com"
  Phone: "+1-555-0300"

Output:
  Contact: {
    Id: "0035w00000BxYzAAK",
    Name: "John Smith",
    Email: "john.smith@acme.com",
    Phone: "+1-555-0300",
    Last Modified Date: "2025-01-15T14:25:00Z"
  }
```

---

## Delete Contact

### Description

Deletes the selected Contact record from Salesforce. This operation permanently removes the Contact record.

> **⚠️ Warning**: Deletion is permanent and cannot be undone through the API. Deleting a Contact may impact related Cases and other records.

### Input Parameters

| Parameter Name | Type | Required | Description |
|----------------|------|----------|-------------|
| Contact | Contact | **Yes** | The Contact entity to delete (selected from search/get results) |

### Output

None (operation returns success or error)

### Validation Rules

- **Contact must exist** - The Contact record must exist in Salesforce
- **No dependent records** - Deletion may fail if there are dependent Cases or other records

---

## Business Rules

### Field Limitations

> **⚠️ Important**: Only the fields explicitly listed in the input parameters are supported. Other fields that may be visible in the entity structure are for internal use only and cannot be set through these requests.

### Required vs Optional Fields

- **Name** is required for Create and Update operations
- All other fields are optional
- For Update operations, only provide the fields you want to change

### Entity Relationships

- **Contact → Account**: Contacts can be associated with Accounts
- **Contact → Case**: Cases can reference Contacts
- **Contact → Contact**: Contacts can report to other Contacts (organizational hierarchy)
- Deleting a Contact may impact related Cases

---

## Error Handling

### Common Errors

**"Required field missing: Name"**
- **Cause**: Name was not provided in Create or Update operation
- **Solution**: Provide a valid Name value

**"Invalid email address"**
- **Cause**: Email format is invalid
- **Solution**: Provide a valid email address (e.g., user@domain.com)

**"Invalid Account ID"**
- **Cause**: The specified Account ID doesn't exist
- **Solution**: Verify the Account record exists before creating/updating the Contact

**"Contact not found"**
- **Cause**: The specified Contact ID doesn't exist
- **Solution**: Verify the Contact ID is correct and the record exists

**"Insufficient privileges"**
- **Cause**: User lacks CRUD permissions on Contact object
- **Solution**: Verify user's profile has appropriate permissions in Salesforce

**"Cannot delete Contact: related records exist"**
- **Cause**: Contact has related Cases or other dependencies
- **Solution**: Delete or update related records first, or use Salesforce cascade delete rules

---

## Best Practices

### 1. Associate with Account
Always associate contacts with Account records for better organization and reporting.

### 2. Maintain Accurate Contact Information
Keep email addresses and phone numbers current for effective communication.

### 3. Use Descriptive Titles
Provide job titles to understand the contact's role and decision-making authority.

### 4. Build Contact Hierarchy
Use the "Reports to ID" field to build organizational hierarchies within accounts.

### 5. Search Before Create
Search for existing Contact records before creating new ones to avoid duplicates.

---

## Common Use Cases

### 1. New Contact Creation
```
Scenario: New customer contact is identified
Action: Create Contact with name, email, phone, and account association
Result: Contact record created for relationship management
```

### 2. Contact Information Update
```
Scenario: Contact changes job title or email
Action: Update Contact with new title and email address
Result: Current contact information for communication
```

### 3. Contact Search
```
Scenario: Agent needs to find a contact by email
Action: Search Contact by email address
Result: Quick access to contact record
```

### 4. Account Contact Management
```
Scenario: View all contacts for a specific account
Action: Search Contact by Account ID
Result: List of all contacts associated with the account
```

---

## See Also

- [Account Entity Operations](pages/AccountEntity.md) - Account records associated with Contacts
- [Case Entity Operations](pages/CaseEntity.md) - Cases associated with Contacts
- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Troubleshooting](pages/Troubleshooting.md)

