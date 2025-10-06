# Individual Entity Operations

## Overview

The Individual entity in Salesforce represents person records used for GDPR and privacy compliance. This entity stores personal information about individuals and is commonly used in conjunction with Customer records to maintain privacy-compliant data structures.

The Krista Salesforce Customer Service Extension supports full CRUD (Create, Read, Update, Delete) operations for Individual entities through Krista's standard entity request mechanisms.

## Supported Operations

- **Search Individual** - Query for Individual records based on criteria
- **Get Individual** - Retrieve a specific Individual record
- **Create Individual** - Create a new Individual record
- **Update Individual** - Modify an existing Individual record
- **Delete Individual** - Remove an Individual record

---

## Search Individual

### Description

Returns a list of Individual records matching the search criteria. Use this operation to find individuals by ID or last name.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Id | Text | No | The unique Salesforce ID of the Individual | "0065w00002AlDtDAAM" |
| Last Name | Text | No | The last name of the Individual | "Smith" |

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Individual | List&lt;Individual&gt; | List of Individual records matching the search criteria |

### Usage Example

**Search by Last Name:**
```
Input:
  Last Name: "Smith"

Output:
  Individual: [
    { Id: "0065w00002AlDtDAAM", First Name: "John", Last Name: "Smith", Birth Date: "1985-07-15" },
    { Id: "0065w00002AlDtDBBN", First Name: "Jane", Last Name: "Smith", Birth Date: "1990-03-22" }
  ]
```

**Search by ID:**
```
Input:
  Id: "0065w00002AlDtDAAM"

Output:
  Individual: [
    { Id: "0065w00002AlDtDAAM", First Name: "John", Last Name: "Smith", Birth Date: "1985-07-15" }
  ]
```

---

## Get Individual

### Description

Returns the selected Individual record. This operation is typically used after a Search operation to retrieve the full details of a specific Individual.

### Input Parameters

None (Individual is selected from search results)

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Individual | Individual | The selected Individual record with all available fields |

### Usage Example

```
Input:
  [Individual selected from search results]

Output:
  Individual: {
    Id: "0065w00002AlDtDAAM",
    First Name: "John",
    Last Name: "Smith",
    Birth Date: "1985-07-15"
  }
```

---

## Create Individual

### Description

Creates a new Individual record in Salesforce. Use this operation to add new person records for privacy compliance and customer relationship management.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| First Name | Text | No | The first name of the Individual | "John" |
| Last Name | Text | **Yes** | The last name of the Individual | "Smith" |
| Birth Date | Date | No | The birth date of the Individual | "1985-07-15" |

> **📝 Note**: Only the fields listed above are supported for input. Other fields visible in the entity are for internal use only and are managed by Salesforce.

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Individual | Individual | The newly created Individual record with system-generated fields (Id, Created Date, etc.) |

### Validation Rules

- **Last Name is required** - Cannot create an Individual without a last name
- **Birth Date format** - Must be in ISO 8601 format (YYYY-MM-DD)

### Usage Example

**Create Individual with all fields:**
```
Input:
  First Name: "John"
  Last Name: "Smith"
  Birth Date: "1985-07-15"

Output:
  Individual: {
    Id: "0065w00002AlDtDAAM",
    First Name: "John",
    Last Name: "Smith",
    Birth Date: "1985-07-15",
    Created Date: "2025-01-15T10:30:00Z"
  }
```

**Create Individual with required fields only:**
```
Input:
  Last Name: "Doe"

Output:
  Individual: {
    Id: "0065w00002AlDtDBBN",
    Last Name: "Doe",
    Created Date: "2025-01-15T10:35:00Z"
  }
```

---

## Update Individual

### Description

Updates an existing Individual record with the provided details. Only the fields you specify will be updated; other fields remain unchanged.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Id | Text | No | The unique Salesforce ID of the Individual to update | "0065w00002AlDtDAAM" |
| First Name | Text | No | The updated first name | "Jonathan" |
| Last Name | Text | **Yes** | The updated last name | "Smith" |
| Birth Date | Date | No | The updated birth date | "1985-07-15" |

> **📝 Note**: Only the fields listed above are supported for input. Other fields visible in the entity are for internal use only.

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Individual | Individual | The updated Individual record with all current field values |

### Validation Rules

- **Last Name is required** - Cannot update an Individual without providing a last name
- **Individual must exist** - The specified Individual ID must exist in Salesforce

### Usage Example

**Update First Name:**
```
Input:
  Id: "0065w00002AlDtDAAM"
  First Name: "Jonathan"
  Last Name: "Smith"

Output:
  Individual: {
    Id: "0065w00002AlDtDAAM",
    First Name: "Jonathan",
    Last Name: "Smith",
    Birth Date: "1985-07-15",
    Last Modified Date: "2025-01-15T14:20:00Z"
  }
```

**Update Birth Date:**
```
Input:
  Id: "0065w00002AlDtDAAM"
  Last Name: "Smith"
  Birth Date: "1985-08-20"

Output:
  Individual: {
    Id: "0065w00002AlDtDAAM",
    First Name: "Jonathan",
    Last Name: "Smith",
    Birth Date: "1985-08-20",
    Last Modified Date: "2025-01-15T14:25:00Z"
  }
```

---

## Delete Individual

### Description

Deletes the selected Individual record from Salesforce. This operation permanently removes the Individual record.

> **⚠️ Warning**: Deletion is permanent and cannot be undone through the API. Ensure you have proper authorization and business justification before deleting Individual records, especially for GDPR compliance.

### Input Parameters

| Parameter Name | Type | Required | Description |
|----------------|------|----------|-------------|
| Individual | Individual | **Yes** | The Individual entity to delete (selected from search/get results) |

### Output

None (operation returns success or error)

### Validation Rules

- **Individual must exist** - The Individual record must exist in Salesforce
- **No dependent records** - Deletion may fail if there are dependent Customer records or other related entities

### Usage Example

```
Input:
  Individual: { Id: "0065w00002AlDtDAAM", First Name: "John", Last Name: "Smith" }

Output:
  Success (no return value)
```

---

## Business Rules

### Field Limitations

> **⚠️ Important**: Only the fields explicitly listed in the input parameters are supported. Other fields that may be visible in the entity structure are for internal use only and cannot be set through these requests.

### Required vs Optional Fields

- **Last Name** is required for Create and Update operations
- **First Name** and **Birth Date** are optional
- For Update operations, only provide the fields you want to change

### GDPR and Privacy Compliance

- Individual records are designed for GDPR compliance
- Deletion of Individual records should follow your organization's data retention policies
- Consider data protection regulations before creating, updating, or deleting Individual records

### Relationships

- **Individual → Customer**: Customer records reference Individual records via the Party Id field
- Deleting an Individual may impact related Customer records

---

## Error Handling

### Common Errors

**"Required field missing: Last Name"**
- **Cause**: Last Name was not provided in Create or Update operation
- **Solution**: Provide a valid Last Name value

**"Individual not found"**
- **Cause**: The specified Individual ID doesn't exist
- **Solution**: Verify the Individual ID is correct and the record exists

**"Insufficient privileges"**
- **Cause**: User lacks CRUD permissions on Individual object
- **Solution**: Verify user's profile has appropriate permissions in Salesforce

**"Cannot delete Individual: related records exist"**
- **Cause**: Individual has related Customer records or other dependencies
- **Solution**: Delete or update related records first, or use Salesforce cascade delete rules

---

## Best Practices

### 1. Search Before Create
Always search for existing Individual records before creating new ones to avoid duplicates.

### 2. Validate Data
Ensure birth dates are in the correct format (YYYY-MM-DD) and are reasonable values.

### 3. Handle Privacy Carefully
Individual records contain personal information. Ensure proper authorization and audit logging for all operations.

### 4. Use Appropriate Permissions
Ensure users have appropriate CRUD permissions on the Individual object based on their role.

### 5. Consider Data Retention
Before deleting Individual records, ensure compliance with data retention policies and GDPR requirements.

---

## Common Use Cases

### 1. GDPR Compliance
```
Scenario: Store personal data separately for privacy compliance
Action: Create Individual record with personal information
Result: Personal data is isolated in Individual object for easier management
```

### 2. Customer Onboarding
```
Scenario: New customer signs up
Action: Create Individual record, then create Customer record referencing the Individual
Result: Proper data structure for privacy-compliant customer management
```

### 3. Data Subject Access Request
```
Scenario: Individual requests their personal data
Action: Search for Individual by name, retrieve full record
Result: Provide individual with their stored personal information
```

### 4. Right to be Forgotten
```
Scenario: Individual requests data deletion
Action: Delete Individual record (after removing dependencies)
Result: Personal data is removed from the system
```

---

## See Also

- [Customer Entity Operations](CustomerEntity.md) - Customer records that reference Individuals
- [Extension Configuration](ExtensionConfiguration.md)
- [Authentication](Authentication.md)
- [Troubleshooting](Troubleshooting.md)

