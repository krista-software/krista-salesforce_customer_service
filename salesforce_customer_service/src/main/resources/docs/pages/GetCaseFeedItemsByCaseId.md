# Get Case Feed Items By Case Id

## Overview

Retrieves a list of feed items (posts, comments, and updates) from a Salesforce Case feed. This request allows you to fetch recent activity and conversations related to a specific case.

## Request Details

- **Area**: Case Management
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes - If validation fails, the user is prompted to re-enter the Case ID and/or Number of Posts
- **Ask Agent (MCP)**: ✅ Enabled - callable by the Krista AI agent

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Case Id | Text | Yes | The unique Salesforce ID of the case whose feed items you want to retrieve | "5005w00000AlDtDAAV" |
| Number Of Posts | Number | No | Maximum number of feed items to retrieve (1-100). If not specified, all available feed items are returned | 10 |

### Parameter Details

**Case Id**:
- Must be a valid 15 or 18-character Salesforce Case ID
- The case must exist in your Salesforce instance
- The authenticated user must have read access to the case
- Format: Alphanumeric string starting with "500" (standard Case ID prefix)

**Number Of Posts**:
- Optional parameter to limit the number of feed items returned
- Must be a positive integer between 1 and 100
- If omitted or null, all available feed items are returned
- Decimal values are not allowed (e.g., 10.5 will cause a validation error)
- Values greater than 100 or less than 1 will cause a validation error

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Feed Items | List&lt;MultiField&gt; | List of feed item objects containing actor name, post message, and optional file attachments |

### Feed Item Structure

Each feed item in the list contains:

| Field Name | Type | Description |
|------------|------|-------------|
| Actor Name | Text | Name of the user who created the feed item |
| Post Message | Text | The content/message of the feed item |
| File | File | Optional file attachment (if the feed item includes a file) |

**Example Output**:
```json
{
  "Feed Items": [
    {
      "Actor Name": "John Smith",
      "Post Message": "Customer issue has been resolved.",
      "File": null
    },
    {
      "Actor Name": "Jane Doe",
      "Post Message": "Attached diagnostic report for review.",
      "File": {
        "name": "diagnostic_report.pdf",
        "size": 245678
      }
    }
  ]
}
```

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Case ID is null or blank | "Case ID is required." | Provide a valid Case ID |
| Case ID is invalid or case doesn't exist | "Invalid case Id: [caseId]" | Verify the Case ID exists in Salesforce and is accessible |
| Number of Posts is not an integer | "Bad Request. Number of posts must be an integer." | Provide a whole number (e.g., 10, not 10.5) |
| Number of Posts is less than or equal to 0 | "Bad Request. Number of posts must be greater than 0." | Provide a positive number (minimum 1) |
| Number of Posts is greater than 100 | "Bad Request. Number of posts must be less than or equal to 100." | Reduce the number to 100 or less |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid Case ID or Number of Posts parameter

**Common Scenarios**:
- Case ID is empty, null, or contains invalid characters
- Case ID doesn't exist in Salesforce
- Number of Posts is not a valid integer
- Number of Posts is outside the allowed range (1-100)

**Error Messages**:
- "The provided Case ID: [caseId] is invalid."
- "The provided number of posts: [numberOfPosts] is invalid."
- "Invalid number of posts: [numberOfPosts]. Please enter an integer between 1 and 100."

**Resolution**:
1. Verify the Case ID is correct and exists in Salesforce
2. Ensure Number of Posts is a whole number between 1 and 100
3. Check that you have read access to the case
4. Re-enter the parameters when prompted

### Logic Errors (LOGIC_ERROR)

**Cause**: Business logic failure during feed item retrieval

**Common Scenarios**:
- Case exists but has no feed items
- Feed items exist but cannot be parsed

**Error Messages**:
- "The Feed Items is empty because the entries do not exist"

**Resolution**:
- This is informational; the case may not have any feed activity yet
- Users can add feed items using the "Share Update to Case" request

### System Errors (SYSTEM_ERROR)

**Cause**: Unexpected system-level failures during API communication

**Common Scenarios**:
- Salesforce API is unavailable
- Network connectivity issues
- Timeout during feed item retrieval
- Unexpected Salesforce API errors

**Error Messages**:
- "Error occurred while fetching post items from case."
- "Error Occurred while fetching the list of post items for the given case"

**Resolution**:
1. Check network connectivity to Salesforce
2. Verify Salesforce service status
3. Retry the request after a few moments
4. Check Salesforce API limits and quotas
5. Review Krista logs for detailed error information

### Authorization Errors

**Cause**: Authentication or permission issues

**Common Scenarios**:
- User is not authenticated with Salesforce
- Access token has expired
- User lacks read permission on the Case object
- User lacks access to specific case due to sharing rules

**Resolution**:
1. Re-authenticate with Salesforce
2. Verify user has "Read" permission on Case object
3. Check case sharing rules and ownership
4. Ensure user profile has API access enabled

## Usage Examples

### Example 1: Retrieve Last 5 Feed Items

**Input**:
```
Case Id: "5005w00000AlDtDAAV"
Number Of Posts: 5
```

**Output**:
```json
{
  "Feed Items": [
    {
      "Actor Name": "Sarah Johnson",
      "Post Message": "Case has been escalated to Tier 2 support.",
      "File": null
    },
    {
      "Actor Name": "Mike Chen",
      "Post Message": "Customer called for status update.",
      "File": null
    },
    {
      "Actor Name": "Sarah Johnson",
      "Post Message": "Initial investigation completed. See attached notes.",
      "File": {
        "name": "investigation_notes.docx"
      }
    }
  ]
}
```

**Result**: Successfully retrieved the 5 most recent feed items for the case.

### Example 2: Retrieve All Feed Items

**Input**:
```
Case Id: "5005w00000AlDtDAAV"
Number Of Posts: [not specified]
```

**Output**:
```json
{
  "Feed Items": [
    {
      "Actor Name": "System",
      "Post Message": "Case created by customer portal.",
      "File": null
    },
    {
      "Actor Name": "John Smith",
      "Post Message": "Case assigned to support team.",
      "File": null
    }
  ]
}
```

**Result**: All available feed items for the case are returned.

### Example 3: Invalid Case ID with Retry

**Input**:
```
Case Id: "INVALID123"
Number Of Posts: 10
```

**Validation Error**:
```
"The provided Case ID: INVALID123 is invalid."
```

**User Prompted**: "Do you want to continue?"

**User Response**: Yes

**Re-enter Prompt**: "Please enter a valid Case ID."

**Corrected Input**:
```
Case Id: "5005w00000AlDtDAAV"
Number Of Posts: 10
```

**Result**: Feed items successfully retrieved after correction.

## Business Rules

1. **Maximum Limit**: The maximum number of feed items that can be retrieved in a single request is 100.
2. **Empty Feed Handling**: If a case has no feed items, the request returns a success response with an informational message.
3. **Chronological Order**: Feed items are typically returned in reverse chronological order (newest first).
4. **File Attachments**: Files attached to feed items are included in the response with metadata (name, size).
5. **Access Control**: Users can only retrieve feed items from cases they have permission to view.

## Limitations

1. **API Rate Limits**: Subject to Salesforce API rate limits. Excessive requests may be throttled.
2. **Maximum Posts**: Cannot retrieve more than 100 feed items in a single request.
3. **Feed Item Types**: Only retrieves standard feed items; may not include all Chatter feed types.
4. **File Content**: File metadata is returned, but file content must be downloaded separately if needed.
5. **Historical Data**: Feed items are subject to Salesforce's data retention policies.

## Best Practices

### 1. Use Appropriate Limits
Specify a reasonable Number of Posts to improve performance and reduce data transfer. For most use cases, 10-25 recent posts are sufficient.

### 2. Handle Empty Feeds Gracefully
Check for empty feed responses and provide appropriate user feedback. Not all cases will have feed activity.

### 3. Validate Case IDs Before Calling
If possible, validate that the Case ID exists before making the request to avoid unnecessary API calls.

### 4. Monitor API Usage
Track your Salesforce API usage to avoid hitting rate limits, especially in high-volume scenarios.

### 5. Cache Results When Appropriate
If feed items don't change frequently, consider caching results to reduce API calls.

## Common Use Cases

### 1. Case Status Dashboard
```
Scenario: Display recent activity for all open cases
Action: Retrieve last 5 feed items for each case
Result: Quick overview of case progress and recent updates
```

### 2. Customer Service Agent View
```
Scenario: Agent needs full case history before responding to customer
Action: Retrieve all feed items for the case
Result: Complete conversation history and context
```

### 3. Automated Case Monitoring
```
Scenario: Monitor high-priority cases for new updates
Action: Periodically retrieve latest feed items
Result: Real-time awareness of case developments
```

## Related Catalog Requests

- [Share Update to Case](pages/ShareUpdateToCase.md) - Add new feed items to a case
- [Case Change Alert](pages/CaseChangeAlert.md) - Receive notifications when cases are updated

## Technical Implementation

### Helper Class
- **Class**: `CaseManagementAreaSubCatalogRequest`
- **Package**: `app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog`
- **Method**: `handleReenterGetCaseFeedItemsByCaseId`

### Validation Flow
1. **CaseIdValidator**: Validates that the Case ID exists and is accessible
2. **NumberOfPostsValidator**: Validates that Number of Posts is an integer between 1 and 100
3. **ValidationOrchestrator**: Coordinates all validators and collects validation results

### Service Delegation
- **Service**: `CaseDaoImpl.getFeedItemsForCaseId(caseId, numberOfPosts)`
- **API Call**: Salesforce REST API query to FeedItem object

### Telemetry Metrics
- Request execution time
- Validation failures
- API call success/failure rates
- Number of feed items retrieved

## Troubleshooting

### Issue: "Invalid Case ID" Error

**Cause**: The provided Case ID doesn't exist or is malformed

**Solution**:
1. Verify the Case ID is correct (15 or 18 characters)
2. Check that the case exists in your Salesforce instance
3. Ensure you're using the correct Salesforce environment (production vs sandbox)
4. Verify the case hasn't been deleted

### Issue: No Feed Items Returned

**Cause**: The case exists but has no feed activity

**Solution**:
- This is normal for newly created cases
- Use "Share Update to Case" to add feed items
- Verify you have permission to view feed items

### Issue: "Number of Posts Must Be an Integer" Error

**Cause**: Decimal value provided for Number of Posts

**Solution**:
- Use whole numbers only (e.g., 10, not 10.5)
- Remove any decimal points from the input

### Issue: Authorization Error

**Cause**: User not authenticated or lacks permissions

**Solution**:
1. Re-authenticate with Salesforce
2. Verify "Read" permission on Case object
3. Check case sharing rules
4. Ensure API access is enabled for the user

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Share Update to Case](pages/ShareUpdateToCase.md)
- [Case Change Alert](pages/CaseChangeAlert.md)

