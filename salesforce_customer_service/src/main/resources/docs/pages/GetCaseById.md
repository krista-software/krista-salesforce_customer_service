# Get Case By Id

## Overview

Fetches a single Salesforce Case by its record ID and returns it as the typed `Case` entity. Use this request when you already have a Case ID (for example, from a search or a case-change alert) and need the full set of case fields — subject, status, priority, contact, account, and more — in one call.

## Request Details

- **Area**: Case Management
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ❌ No - Invalid input is returned as an immediate error rather than a guided re-entry prompt
- **Ask Agent (MCP)**: ✅ Enabled - Exposed as a tool for Krista's Ask Agent / MCP integration

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Case ID | Text | Yes | The unique Salesforce ID of the case to retrieve | "5005w00000AlDtDAAV" |

### Parameter Details

**Case ID**:
- Must be a valid 15- or 18-character Salesforce Case ID
- The case must exist in your Salesforce instance
- The authenticated user must have read access to the case
- Format: Alphanumeric string starting with "500" (standard Case ID prefix)

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Case | Entity(Case) | The Case record identified by the supplied Case ID |

### Case Structure

The returned `Case` entity contains the following fields. See [Case Entity Operations](pages/CaseEntity.md) for the full entity reference.

| Field Name | Type | Description |
|------------|------|-------------|
| Id | Text | The unique Salesforce ID of the case |
| Contact Id | Text | ID of the contact associated with the case |
| Account Id | Text | ID of the account associated with the case |
| Contact Name | Text | Name of the associated contact |
| Subject | Text | Subject/title of the case |
| Is Closed | Switch | Whether the case is closed |
| Owner Id | Text | ID of the case owner (user) |
| Account Name | Text | Name of the associated account |
| Created Date | Date | Date the case was created |
| Last Modified Date | Date | Date the case was last modified |
| Case Number | Number | Auto-generated case number |
| Contact Email | Email | Email of the associated contact |
| Contact Phone | Text | Phone number of the associated contact |
| Status | Text | Current status of the case |
| Reason | Text | Reason the case was opened |
| Origin | Text | Channel the case originated from |
| Description | Paragraph | Detailed description of the case |
| Priority | Text | Priority of the case |

**Example Output**:
```json
{
  "Case": {
    "Id": "5005w00000AlDtDAAV",
    "Case Number": 12345,
    "Subject": "Login issue on customer portal",
    "Status": "New",
    "Priority": "High",
    "Contact Name": "John Smith",
    "Account Name": "Acme Inc",
    "Is Closed": false,
    "Origin": "Web",
    "Description": "Customer cannot sign in after password reset."
  }
}
```

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Case ID is null or blank | "Case ID is required." | Provide a non-empty Case ID |
| Case ID does not resolve in Salesforce | "An unexpected error occurred while fetching the case: [details]" | Verify the Case ID exists and is accessible in your Salesforce instance |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: The Case ID was not supplied.

**Common Scenarios**:
- Case ID is empty, null, or whitespace only

**Error Message**:
- "Case ID is required."

**Resolution**:
1. Provide a valid Case ID
2. Confirm the value was mapped correctly from the previous step

### System Errors (SYSTEM_ERROR)

**Cause**: The request reached Salesforce but the lookup failed, or an unexpected error occurred during retrieval or conversion.

**Common Scenarios**:
- The Case ID does not exist or has been deleted
- Salesforce API is unavailable or times out
- Network connectivity issues
- Unexpected Salesforce API error

**Error Message**:
- "An unexpected error occurred while fetching the case: [details]"

**Resolution**:
1. Verify the Case ID is correct and the case exists
2. Check network connectivity and Salesforce service status
3. Retry the request after a few moments
4. Review Krista logs for the detailed error

### Authorization Errors

**Cause**: The user is not authenticated with Salesforce, the access token has expired, or the user lacks read access to the case. In this situation the request surfaces the Salesforce consent flow so the user can (re)authorize before the call is retried.

**Resolution**:
1. Complete the Salesforce sign-in / consent prompt when presented
2. Verify the user has "Read" permission on the Case object
3. Check case sharing rules and ownership

## Usage Examples

### Example 1: Retrieve a Case by ID

**Input**:
```
Case ID: "5005w00000AlDtDAAV"
```

**Output**:
```json
{
  "Case": {
    "Id": "5005w00000AlDtDAAV",
    "Case Number": 12345,
    "Subject": "Login issue on customer portal",
    "Status": "New",
    "Priority": "High"
  }
}
```

**Result**: The full Case record is returned as the `Case` entity.

### Example 2: Missing Case ID

**Input**:
```
Case ID: ""
```

**Output**:
```
Error (INPUT_ERROR): "Case ID is required."
```

**Result**: The request fails immediately with an input error; no Salesforce call is made.

## Business Rules

1. **Single Record**: The request returns exactly one Case entity for the supplied ID.
2. **Fail Fast on Missing Input**: A null or blank Case ID is rejected before any Salesforce call is made.
3. **Consent on Demand**: If the user is not authorized, the Salesforce consent flow is surfaced rather than returning a generic error.
4. **Access Control**: Only cases the authenticated user is permitted to view are returned.

## Limitations

1. **API Rate Limits**: Subject to Salesforce API rate limits; excessive requests may be throttled.
2. **Read Only**: This request retrieves a case; it does not modify it. Use "Share Update to Case" to post updates.
3. **Exact ID Required**: The request looks up a case by its exact record ID; it does not search by other fields. Use the Case entity's Search operation to find cases by subject, status, or other criteria.

## Best Practices

### 1. Validate IDs Upstream
Where possible, obtain the Case ID from a preceding Search or Case Change Alert so you know it resolves before calling.

### 2. Handle Authorization Prompts
Design the flow to accommodate the Salesforce consent step for first-time or expired sessions.

### 3. Use the Right Environment
Ensure the Case ID belongs to the same Salesforce environment (production vs sandbox) the extension is configured against.

## Common Use Cases

### 1. Case Detail Lookup
```
Scenario: An agent has a Case ID and needs the full case record to respond
Action: Call Get Case By Id with the Case ID
Result: Complete case details for review
```

### 2. Enrich a Case Change Alert
```
Scenario: A Case Change Alert delivers a Case Id and a few fields
Action: Call Get Case By Id to retrieve the full record
Result: The complete, up-to-date case for downstream processing
```

## Related Catalog Requests

- [Get Case Feed Items By Case Id](pages/GetCaseFeedItemsByCaseId.md) - Retrieve the feed (posts, comments) for a case
- [Share Update to Case](pages/ShareUpdateToCase.md) - Add an update to a case feed
- [Case Change Alert](pages/CaseChangeAlert.md) - Receive notifications when cases are created or updated

## Technical Implementation

### Helper Class
- **Class**: `CaseManagementArea`
- **Package**: `app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog`
- **Method**: `getCaseById(String caseID)`

### Service Delegation
- **Service**: `CaseDaoImpl.getCaseById(caseID)`
- **API Call**: `GET /services/data/v53.0/sobjects/Case/{caseID}`
- **Mapping**: The service-layer record is converted to the catalog `Case` entity via `CatalogTypes.fromSFCase(...)` and returned under the `"Case"` output key.

## Troubleshooting

### Issue: "Case ID is required."

**Cause**: No Case ID was supplied.

**Solution**:
1. Provide a non-empty Case ID
2. Verify the value is mapped from the prior step

### Issue: "An unexpected error occurred while fetching the case"

**Cause**: The Case ID does not resolve, or Salesforce returned an error.

**Solution**:
1. Confirm the Case ID exists (15 or 18 characters) and is not deleted
2. Confirm you are targeting the correct Salesforce environment
3. Check Salesforce service status and retry

### Issue: Redirected to a Salesforce sign-in / consent screen

**Cause**: The session is not authorized, or the token has expired.

**Solution**:
1. Complete the consent flow
2. Verify "Read" permission on the Case object and applicable sharing rules

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Case Entity Operations](pages/CaseEntity.md)
- [Get Case Feed Items By Case Id](pages/GetCaseFeedItemsByCaseId.md)
