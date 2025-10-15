# Customer Entity Operations

## Overview

The Customer entity in Salesforce represents customer account records. This entity is commonly used in conjunction with Individual records to maintain a privacy-compliant data structure where personal information is stored in the Individual object and business relationship data is stored in the Customer object.

The Krista Salesforce Customer Service Extension supports full CRUD (Create, Read, Update, Delete) operations for Customer entities through Krista's standard entity request mechanisms.

## Supported Operations

- **Search Customer** - Query for Customer records based on criteria
- **Get Customer** - Retrieve a specific Customer record
- **Create Customer** - Create a new Customer record
- **Update Customer** - Modify an existing Customer record
- **Delete Customer** - Remove a Customer record

---

## Search Customer

### Description

Returns a list of Customer records matching the search criteria. Use this operation to find customers by ID, Party ID (Individual reference), or name.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Id | Text | No | The unique Salesforce ID of the Customer | "0065w00002AlDtDAAM" |
| Party Id | Text | No | The ID of the Individual associated with this Customer | "0065w00002aeWETBBM" |
| Name | Text | No | The name of the Customer | "Acme Corporation" |

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Customer | List&lt;Customer&gt; | List of Customer records matching the search criteria |

### Usage Example

**Search by Name:**
```
Input:
  Name: "Acme Corporation"

Output:
  Customer: [
    {
      Id: "0065w00002AlDtDAAM",
      Name: "Acme Corporation",
      Party Id: "0065w00002aeWETBBM",
      Customer Status Type: "Active",
      Total Life Time Value: 500000
    }
  ]
```

**Search by Party Id:**
```
Input:
  Party Id: "0065w00002aeWETBBM"

Output:
  Customer: [
    {
      Id: "0065w00002AlDtDAAM",
      Name: "Acme Corporation",
      Party Id: "0065w00002aeWETBBM",
      Customer Status Type: "Active"
    }
  ]
```

---

## Get Customer

### Description

Returns the selected Customer record. This operation is typically used after a Search operation to retrieve the full details of a specific Customer.

### Input Parameters

None (Customer is selected from search results)

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Customer | Customer | The selected Customer record with all available fields |

### Usage Example

```
Input:
  [Customer selected from search results]

Output:
  Customer: {
    Id: "0065w00002AlDtDAAM",
    Name: "Acme Corporation",
    Party Id: "0065w00002aeWETBBM",
    Customer Status Type: "Active",
    Total Life Time Value: 500000
  }
```

---

## Create Customer

### Description

Creates a new Customer record in Salesforce. Use this operation to add new customer accounts and associate them with Individual records for privacy-compliant data management.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Name | Text | **Yes** | The name of the Customer | "Acme Corporation" |
| Party Id | Text | **Yes** | The ID of the Individual associated with this Customer | "0065w00002aeWEBM" |
| Customer Status Type | Pick One | No | The status of the customer (e.g., Active, Inactive) | "Active" |
| Total Life Time Value | Number | No | The total lifetime value of the customer | 500000 |

> **📝 Note**: 
> - **Party Id** is the ID of the Individual associated with this Customer. The Individual record must exist before creating the Customer.
> - Only the fields listed above are supported for input. Other fields visible in the entity are for internal use only.

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Customer | Customer | The newly created Customer record with system-generated fields |

### Validation Rules

- **Name is required** - Cannot create a Customer without a name
- **Party Id is required** - Must reference an existing Individual record
- **Party Id must exist** - The referenced Individual ID must exist in Salesforce
- **Total Life Time Value** - Must be a positive number if provided

### Usage Example

**Create Customer with all fields:**
```
Input:
  Name: "Acme Corporation"
  Party Id: "0065w00002aeWEBM"
  Customer Status Type: "Active"
  Total Life Time Value: 500000

Output:
  Customer: {
    Id: "0065w00002AlDtDAAM",
    Name: "Acme Corporation",
    Party Id: "0065w00002aeWEBM",
    Customer Status Type: "Active",
    Total Life Time Value: 500000,
    Created Date: "2025-01-15T10:30:00Z"
  }
```

**Create Customer with required fields only:**
```
Input:
  Name: "Beta Industries"
  Party Id: "0065w00002aeWECCC"

Output:
  Customer: {
    Id: "0065w00002AlDtDBBN",
    Name: "Beta Industries",
    Party Id: "0065w00002aeWECCC",
    Created Date: "2025-01-15T10:35:00Z"
  }
```

---

## Update Customer

### Description

Updates an existing Customer record with the provided details. Only the fields you specify will be updated; other fields remain unchanged.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Id | Text | No | The unique Salesforce ID of the Customer to update | "0065w00002AlDtDAAM" |
| Party Id | Text | No | The updated Individual ID associated with this Customer | "0065w00002aeWEBM" |
| Customer Status Type | Pick One | No | The updated status of the customer | "Inactive" |
| Total Life Time Value | Number | No | The updated total lifetime value | 750000 |

> **📝 Note**: 
> - **Party Id** is the ID of the Individual associated with this Customer
> - Only the fields listed above are supported for input

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Customer | Customer | The updated Customer record with all current field values |

### Validation Rules

- **Customer must exist** - The specified Customer ID must exist in Salesforce
- **Party Id must exist** - If updating Party Id, the new Individual ID must exist

### Usage Example

**Update Customer Status:**
```
Input:
  Id: "0065w00002AlDtDAAM"
  Customer Status Type: "Inactive"

Output:
  Customer: {
    Id: "0065w00002AlDtDAAM",
    Name: "Acme Corporation",
    Party Id: "0065w00002aeWEBM",
    Customer Status Type: "Inactive",
    Total Life Time Value: 500000,
    Last Modified Date: "2025-01-15T14:20:00Z"
  }
```

**Update Lifetime Value:**
```
Input:
  Id: "0065w00002AlDtDAAM"
  Total Life Time Value: 750000

Output:
  Customer: {
    Id: "0065w00002AlDtDAAM",
    Name: "Acme Corporation",
    Party Id: "0065w00002aeWEBM",
    Customer Status Type: "Inactive",
    Total Life Time Value: 750000,
    Last Modified Date: "2025-01-15T14:25:00Z"
  }
```

---

## Delete Customer

### Description

Deletes the selected Customer record from Salesforce. This operation permanently removes the Customer record.

> **⚠️ Warning**: Deletion is permanent and cannot be undone through the API. Ensure you have proper authorization and business justification before deleting Customer records.

### Input Parameters

| Parameter Name | Type | Required | Description |
|----------------|------|----------|-------------|
| Customer | Customer | **Yes** | The Customer entity to delete (selected from search/get results) |

### Output

None (operation returns success or error)

### Validation Rules

- **Customer must exist** - The Customer record must exist in Salesforce
- **No dependent records** - Deletion may fail if there are dependent records (Cases, Accounts, etc.)

### Usage Example

```
Input:
  Customer: { Id: "0065w00002AlDtDAAM", Name: "Acme Corporation" }

Output:
  Success (no return value)
```

---

## Business Rules

### Field Limitations

> **⚠️ Important**: Only the fields explicitly listed in the input parameters are supported. Other fields that may be visible in the entity structure are for internal use only and cannot be set through these requests.

### Required vs Optional Fields

- **Name** and **Party Id** are required for Create operations
- **Customer Status Type** and **Total Life Time Value** are optional
- For Update operations, only provide the fields you want to change

### Entity Relationships

- **Customer → Individual**: Customer's Party Id references an Individual record
- The Individual record must exist before creating a Customer
- Deleting an Individual may impact related Customer records

### Customer Status Types

Common status values include:
- **Active** - Customer is currently active
- **Inactive** - Customer is no longer active
- **Prospect** - Potential customer
- **Former** - Former customer

> **📝 Note**: Available status values may vary based on your Salesforce configuration.

---

## Error Handling

### Common Errors

**"Required field missing: Name"**
- **Cause**: Name was not provided in Create operation
- **Solution**: Provide a valid Name value

**"Required field missing: Party Id"**
- **Cause**: Party Id was not provided in Create operation
- **Solution**: Provide a valid Individual ID for Party Id

**"Invalid Party Id"**
- **Cause**: The specified Individual ID doesn't exist
- **Solution**: Verify the Individual record exists before creating the Customer

**"Customer not found"**
- **Cause**: The specified Customer ID doesn't exist
- **Solution**: Verify the Customer ID is correct and the record exists

**"Insufficient privileges"**
- **Cause**: User lacks CRUD permissions on Customer object
- **Solution**: Verify user's profile has appropriate permissions in Salesforce

**"Cannot delete Customer: related records exist"**
- **Cause**: Customer has related Case, Account, or other records
- **Solution**: Delete or update related records first, or use Salesforce cascade delete rules

---

## Best Practices

### 1. Create Individual First
Always create the Individual record before creating the Customer record, as Party Id is required.

### 2. Search Before Create
Search for existing Customer records before creating new ones to avoid duplicates.

### 3. Validate Party Id
Ensure the Individual (Party Id) exists before creating or updating Customer records.

### 4. Use Appropriate Status
Set Customer Status Type appropriately to reflect the current state of the customer relationship.

### 5. Track Lifetime Value
Regularly update Total Life Time Value to maintain accurate customer value metrics.

---

## Common Use Cases

### 1. Customer Onboarding
```
Scenario: New customer signs up
Action: Create Individual record, then create Customer record with Party Id
Result: Privacy-compliant customer data structure
```

### 2. Customer Status Management
```
Scenario: Customer becomes inactive
Action: Update Customer Status Type to "Inactive"
Result: Customer status reflects current relationship state
```

### 3. Customer Value Tracking
```
Scenario: Customer makes a purchase
Action: Update Total Life Time Value with new total
Result: Accurate customer value metrics for reporting
```

### 4. Customer Search
```
Scenario: Support agent needs customer information
Action: Search Customer by name or Party Id
Result: Quick access to customer records
```

---

## See Also

- [Individual Entity Operations](pages/IndividualEntity.md) - Individual records referenced by Customers
- [Account Entity Operations](pages/AccountEntity.md) - Business account records
- [Case Entity Operations](pages/CaseEntity.md) - Customer service cases
- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Troubleshooting](pages/Troubleshooting.md)

