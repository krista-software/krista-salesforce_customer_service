# Case Entity Operations

## Overview

The Case entity in Salesforce represents customer service cases or support tickets. Cases are used to track customer issues, questions, and requests, providing a complete history of customer interactions and problem resolution.

The Krista Salesforce Customer Service Extension supports full CRUD (Create, Read, Update, Delete) operations for Case entities through Krista's standard entity request mechanisms.

## Supported Operations

- **Search Case** - Query for Case records based on criteria
- **Get Case** - Retrieve a specific Case record
- **Create Case** - Create a new Case record
- **Update Case** - Modify an existing Case record
- **Delete Case** - Remove a Case record

---

## Search Case

### Description

Returns a list of Case records matching the search criteria. Use this operation to find cases by ID, contact, account, subject, status, or other criteria.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| ID | Text | No | The unique Salesforce ID of the Case | "5005w00000AlDtDAAV" |
| Contact ID | Text | No | ID of the contact associated with the case | "0035w00000BxYzAAK" |
| Account ID | Text | No | ID of the account associated with the case | "0015w00000CzXyBBB" |
| Contact Name | Text | No | Name of the contact | "John Smith" |
| Subject | Text | No | Subject/title of the case | "Product Issue" |
| Owner ID | Text | No | ID of the case owner (user) | "0055w00000D9a8iAAC" |
| Account Name | Text | No | Name of the account | "Acme Inc" |
| Created Date | Date | No | Date the case was created | "2025-01-15" |
| Last Modified Date | Date | No | Date the case was last modified | "2025-01-16" |
| Case Number | Number | No | Auto-generated case number | 12345 |
| Contact Email | Email | No | Email of the contact | "john@example.com" |
| Contact Phone | Text | No | Phone number of the contact | "+1-555-0100" |
| Status | Text | No | Current status of the case | "New" |

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Case | List&lt;Case&gt; | List of Case records matching the search criteria |

### Usage Example

**Search by Subject:**
```
Input:
  Subject: "Product Issue"

Output:
  Case: [
    {
      Id: "5005w00000AlDtDAAV",
      Case Number: 12345,
      Subject: "Product Issue",
      Status: "New",
      Priority: "High",
      Contact Name: "John Smith"
    }
  ]
```

**Search by Status:**
```
Input:
  Status: "New"

Output:
  Case: [
    { Id: "5005w00000AlDtDAAV", Case Number: 12345, Subject: "Product Issue", Status: "New" },
    { Id: "5005w00000AlDtDBBN", Case Number: 12346, Subject: "Login Problem", Status: "New" }
  ]
```

---

## Get Case

### Description

Returns the selected Case record with all available fields.

### Input Parameters

None (Case is selected from search results)

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Case | Case | The selected Case record with all available fields |

---

## Create Case

### Description

Creates a new Case record in Salesforce to track customer issues, questions, or requests.

### Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Contact ID | Text | No | ID of the contact associated with the case | "0035w00000BxYzAAK" |
| Account ID | Text | No | ID of the account associated with the case | "0015w00000CzXyBBB" |
| Subject | Text | No | Subject/title of the case | "Product not working" |
| Is Closed | Switch | No | Whether the case is closed | false |
| Owner ID | Text | No | ID of the case owner (user) | "0055w00000D9a8iAAC" |
| Status | Text | No | Current status (New, In Progress, Closed, etc.) | "New" |
| Reason | Text | No | Reason for the case | "Performance" |
| Origin | Text | No | Origin of the case (Phone, Email, Web, etc.) | "Phone" |
| Description | Paragraph | No | Detailed description of the case | "Customer reports slow performance" |
| Priority | Text | No | Priority level (High, Medium, Low) | "High" |

> **📝 Note**: 
> - To add Contact Email, Contact Phone, or Contact Name, Contact ID is required
> - To add Account Name, Account ID is required

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Case | Case | The newly created Case record with system-generated fields (Case Number, Created Date, etc.) |

### Validation Rules

- **Contact ID must exist** - If provided, the Contact ID must reference an existing Contact
- **Account ID must exist** - If provided, the Account ID must reference an existing Account
- **Date format** - Dates must be in ISO 8601 format (YYYY-MM-DD)

### Usage Example

**Create Case with basic information:**
```
Input:
  Subject: "Product not working"
  Description: "Customer reports slow performance when using the application"
  Status: "New"
  Priority: "High"
  Origin: "Phone"

Output:
  Case: {
    Id: "5005w00000AlDtDAAV",
    Case Number: 12345,
    Subject: "Product not working",
    Description: "Customer reports slow performance when using the application",
    Status: "New",
    Priority: "High",
    Origin: "Phone",
    Is Closed: false,
    Created Date: "2025-01-15T10:30:00Z"
  }
```

**Create Case with Contact and Account:**
```
Input:
  Contact ID: "0035w00000BxYzAAK"
  Account ID: "0015w00000CzXyBBB"
  Subject: "Billing question"
  Description: "Customer has questions about recent invoice"
  Status: "New"
  Priority: "Medium"
  Origin: "Email"

Output:
  Case: {
    Id: "5005w00000AlDtDBBN",
    Case Number: 12346,
    Contact ID: "0035w00000BxYzAAK",
    Contact Name: "John Smith",
    Account ID: "0015w00000CzXyBBB",
    Account Name: "Acme Inc",
    Subject: "Billing question",
    Status: "New",
    Priority: "Medium",
    Created Date: "2025-01-15T10:35:00Z"
  }
```

---

## Update Case

### Description

Updates an existing Case record with the provided details. Only the fields you specify will be updated; other fields remain unchanged.

### Input Parameters

Same as Create Case (all fields optional)

> **📝 Note**: 
> - To update Contact Email, Contact Phone, or Contact Name, Contact ID is required
> - To update Account Name, Account ID is required

### Output

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Case | Case | The updated Case record with all current field values |

### Usage Example

**Update Case Status:**
```
Input:
  Id: "5005w00000AlDtDAAV"
  Status: "In Progress"

Output:
  Case: {
    Id: "5005w00000AlDtDAAV",
    Case Number: 12345,
    Subject: "Product not working",
    Status: "In Progress",
    Last Modified Date: "2025-01-15T14:20:00Z"
  }
```

**Close Case:**
```
Input:
  Id: "5005w00000AlDtDAAV"
  Status: "Closed"
  Is Closed: true

Output:
  Case: {
    Id: "5005w00000AlDtDAAV",
    Case Number: 12345,
    Status: "Closed",
    Is Closed: true,
    Closed Date: "2025-01-15T16:00:00Z"
  }
```

---

## Delete Case

### Description

Deletes the selected Case record from Salesforce. This operation permanently removes the Case record.

> **⚠️ Warning**: Deletion is permanent and cannot be undone through the API. Deleting a Case may also delete related records (feed items, attachments) depending on your Salesforce configuration.

### Input Parameters

| Parameter Name | Type | Required | Description |
|----------------|------|----------|-------------|
| Case | Case | **Yes** | The Case entity to delete (selected from search/get results) |

### Output

None (operation returns success or error)

---

## Business Rules

### Field Limitations

> **⚠️ Important**: Only the fields explicitly listed in the input parameters are supported. Other fields that may be visible in the entity structure are for internal use only and cannot be set through these requests.

### Required vs Optional Fields

- All fields are optional for Create and Update operations
- Salesforce may have validation rules that require certain fields
- For Update operations, only provide the fields you want to change

### Entity Relationships

- **Case → Contact**: Cases can be associated with Contacts
- **Case → Account**: Cases can be associated with Accounts
- **Case → Feed Items**: Cases have associated feed items (updates, comments)
- Deleting a Case may cascade delete related feed items and attachments

### Case Status Values

Common status values include:
- **New** - Case has been created but not yet worked on
- **In Progress** - Case is being actively worked on
- **Escalated** - Case has been escalated to higher support tier
- **On Hold** - Case is waiting for customer response or other dependency
- **Closed** - Case has been resolved

> **📝 Note**: Available status values may vary based on your Salesforce configuration.

---

## Error Handling

### Common Errors

**"Invalid Contact ID"**
- **Cause**: The specified Contact ID doesn't exist
- **Solution**: Verify the Contact record exists before creating/updating the Case

**"Invalid Account ID"**
- **Cause**: The specified Account ID doesn't exist
- **Solution**: Verify the Account record exists before creating/updating the Case

**"Case not found"**
- **Cause**: The specified Case ID doesn't exist
- **Solution**: Verify the Case ID is correct and the record exists

**"Insufficient privileges"**
- **Cause**: User lacks CRUD permissions on Case object
- **Solution**: Verify user's profile has appropriate permissions in Salesforce

---

## Best Practices

### 1. Associate with Contact and Account
Always associate cases with Contact and Account records for better tracking and reporting.

### 2. Use Descriptive Subjects
Provide clear, descriptive subjects that summarize the case issue.

### 3. Set Appropriate Priority
Set priority based on business impact to ensure proper case routing and SLA management.

### 4. Track Case Origin
Record the origin (Phone, Email, Web) for channel analytics and reporting.

### 5. Update Status Regularly
Keep case status current to reflect the actual state of the case.

---

## Common Use Cases

### 1. Customer Support Ticket Creation
```
Scenario: Customer calls with a product issue
Action: Create Case with customer's Contact ID, issue description, and priority
Result: Support ticket created for tracking and resolution
```

### 2. Case Status Management
```
Scenario: Agent starts working on a case
Action: Update Case status from "New" to "In Progress"
Result: Case status reflects current work state
```

### 3. Case Search and Retrieval
```
Scenario: Agent needs to find all open cases for a customer
Action: Search Case by Account ID and Status = "New" or "In Progress"
Result: List of active cases for the customer
```

### 4. Case Closure
```
Scenario: Issue is resolved
Action: Update Case with Status = "Closed" and Is Closed = true
Result: Case is marked as resolved and closed
```

---

## See Also

- [Get Case Feed Items By Case Id](pages/GetCaseFeedItemsByCaseId.md) - Retrieve feed items from cases
- [Share Update to Case](pages/ShareUpdateToCase.md) - Add updates to case feeds
- [Case Change Alert](pages/CaseChangeAlert.md) - Receive notifications for case changes
- [Contact Entity Operations](pages/ContactEntity.md) - Contact records associated with Cases
- [Account Entity Operations](pages/AccountEntity.md) - Account records associated with Cases
- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Troubleshooting](pages/Troubleshooting.md)

