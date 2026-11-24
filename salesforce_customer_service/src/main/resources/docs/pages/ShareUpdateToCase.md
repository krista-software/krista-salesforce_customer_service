# Share Update to Case

## Overview

Adds an update message (and optionally a file attachment) to the feed of a Salesforce Case. This request allows you to post comments, status updates, and share files with case stakeholders.

## Request Details

- **Area**: Case Management
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes - If validation fails, the user is prompted to re-enter the Case ID and/or provide a valid file
- **Ask Agent (MCP)**: ✅ Enabled - callable by the Krista AI agent

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Case Id | Text | Yes | The unique Salesforce ID of the case to update | "5005w00000AlDtDAAV" |
| Update Message | Paragraph | Yes | The message text to post to the case feed | "Customer issue has been resolved. Closing case." |
| File | File | No | Optional file attachment to include with the update | diagnostic_report.pdf |

### Parameter Details

**Case Id**:
- Must be a valid 15 or 18-character Salesforce Case ID
- The case must exist in your Salesforce instance
- The authenticated user must have edit access to the case
- Format: Alphanumeric string starting with "500" (standard Case ID prefix)

**Update Message**:
- Required text content for the feed post
- Cannot be null, empty, or contain only whitespace
- Supports multi-line text (paragraph format)
- Special characters are automatically escaped (quotes, newlines, tabs, etc.)
- Maximum length subject to Salesforce FeedItem limits (typically 10,000 characters)

**File**:
- Optional file attachment
- Single file upload only (multiple files not supported)
- Must have a valid file extension
- File is converted to Base64 encoding for upload to Salesforce
- Subject to Salesforce file size limits (typically 25 MB for attachments)

**Supported File Types**:
- **Documents**: pdf, doc, docx, rtf, txt
- **Spreadsheets**: xls, xlsx, csv
- **Presentations**: ppt, pptx
- **Images**: png, jpg, jpeg, gif, bmp, tif, tiff
- **Archives**: zip, rar

> **⚠️ Note**: HTML, XML, and HTM files are restricted and not supported.

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Is Updated | Boolean | Indicates whether the update was successfully posted to the case feed |

**Output Values**:
- `true`: Update was successfully posted to the case feed
- `false`: Update failed (should not occur if validation passes; system errors would throw exceptions)

**Example Output**:
```json
{
  "Is Updated": true
}
```

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Case ID is null or blank | "Case ID is required." | Provide a valid Case ID |
| Case ID is invalid or case doesn't exist | "Invalid case Id: [caseId]" | Verify the Case ID exists in Salesforce and is accessible |
| Update Message is null or blank | "Invalid update message: [message]" | Provide a non-empty update message |
| File has unsupported extension | "The provided file [fileName] is not supported" | Use a supported file type (pdf, doc, docx, xls, xlsx, png, jpg, etc.) |
| File has no extension | "The provided file [fileName] is not supported" | Ensure the file has a valid extension |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid Case ID, empty update message, or unsupported file type

**Common Scenarios**:
- Case ID is empty, null, or doesn't exist in Salesforce
- Update message is blank or contains only whitespace
- File has an unsupported extension (e.g., .exe, .html, .xml)
- File name has no extension

**Error Messages**:
- "The provided Case ID: [caseId] is invalid."
- "Invalid update message: [message]"
- "The provided file [fileName] is not supported"
- "Please provide a valid update message."

**Resolution**:
1. Verify the Case ID is correct and exists in Salesforce
2. Ensure the update message contains actual text content
3. Use only supported file types
4. Add a proper file extension to the file
5. Re-enter the parameters when prompted

### Logic Errors (LOGIC_ERROR)

**Cause**: Business logic failure during feed update

**Common Scenarios**:
- Case is locked or read-only
- User lacks permission to post to the case feed
- Salesforce validation rules prevent the update

**Resolution**:
- Verify the case is not locked or closed (if your org prevents updates to closed cases)
- Check user permissions for creating FeedItems
- Review Salesforce validation rules on the Case object

### System Errors (SYSTEM_ERROR)

**Cause**: Unexpected system-level failures during API communication

**Common Scenarios**:
- Salesforce API is unavailable
- Network connectivity issues
- File encoding/upload failure
- Timeout during feed post
- File size exceeds Salesforce limits

**Error Messages**:
- "Error occurred while sharing update to case."
- "Error occurred while updating to the Feed of the Case"

**Resolution**:
1. Check network connectivity to Salesforce
2. Verify Salesforce service status
3. Ensure file size is within limits (typically 25 MB)
4. Retry the request after a few moments
5. Check Salesforce API limits and quotas
6. Review Krista logs for detailed error information

### Authorization Errors

**Cause**: Authentication or permission issues

**Common Scenarios**:
- User is not authenticated with Salesforce
- Access token has expired
- User lacks create permission on FeedItem object
- User lacks edit access to the specific case

**Resolution**:
1. Re-authenticate with Salesforce
2. Verify user has "Create" permission on FeedItem object
3. Verify user has "Edit" permission on Case object
4. Check case sharing rules and ownership
5. Ensure user profile has API access enabled

## Usage Examples

### Example 1: Post Simple Text Update

**Input**:
```
Case Id: "5005w00000AlDtDAAV"
Update Message: "Customer confirmed the issue is resolved. Closing case."
File: [not provided]
```

**Output**:
```json
{
  "Is Updated": true
}
```

**Result**: Text update successfully posted to the case feed without any file attachment.

### Example 2: Post Update with File Attachment

**Input**:
```
Case Id: "5005w00000AlDtDAAV"
Update Message: "Attached diagnostic report for review by engineering team."
File: diagnostic_report.pdf (2.3 MB)
```

**Output**:
```json
{
  "Is Updated": true
}
```

**Result**: Update message and PDF file successfully posted to the case feed. File is available for download by case stakeholders.

### Example 3: Multi-line Update Message

**Input**:
```
Case Id: "5005w00000AlDtDAAV"
Update Message: "Update from customer call:
- Issue reproduced in test environment
- Root cause identified as configuration error
- Fix will be deployed in next release
- Customer satisfied with timeline"
File: [not provided]
```

**Output**:
```json
{
  "Is Updated": true
}
```

**Result**: Multi-line update with bullet points successfully posted. Special characters (newlines) are automatically escaped.

### Example 4: Invalid File Type with Retry

**Input**:
```
Case Id: "5005w00000AlDtDAAV"
Update Message: "Attaching configuration file"
File: config.xml
```

**Validation Error**:
```
"The provided file config.xml is not supported"
```

**User Prompted**: "Do you want to continue?"

**User Response**: Yes

**Re-enter Prompt**: "Please provide valid file."

**Corrected Input**:
```
Case Id: "5005w00000AlDtDAAV"
Update Message: "Attaching configuration file (converted to PDF)"
File: config.pdf
```

**Result**: Update successfully posted with the corrected file type.

## Business Rules

1. **Required Message**: An update message is always required; you cannot post a file-only update.
2. **Single File Limit**: Only one file can be attached per update. For multiple files, create multiple updates.
3. **File Size Limits**: Files are subject to Salesforce's attachment size limits (typically 25 MB).
4. **Character Escaping**: Special characters in the message are automatically escaped for JSON compatibility.
5. **Feed Visibility**: Updates are visible to all users who have access to the case, based on sharing rules.
6. **Audit Trail**: All feed updates are tracked in Salesforce with timestamp and author information.

## Limitations

1. **File Type Restrictions**: Only specific file types are supported. Executable files, scripts, and some web formats (HTML, XML) are not allowed.
2. **Single File Upload**: Cannot attach multiple files in a single request.
3. **File Size**: Maximum file size is limited by Salesforce (typically 25 MB for attachments).
4. **API Rate Limits**: Subject to Salesforce API rate limits. Excessive updates may be throttled.
5. **No Edit/Delete**: Once posted, feed items cannot be edited or deleted through this request (must be done in Salesforce UI).
6. **Message Length**: Update messages are subject to Salesforce FeedItem character limits.

## Best Practices

### 1. Provide Meaningful Updates
Write clear, concise update messages that provide value to case stakeholders. Include relevant details about actions taken, next steps, or status changes.

### 2. Use Appropriate File Formats
Convert files to widely supported formats (PDF for documents, PNG/JPG for images) to ensure compatibility and accessibility.

### 3. Validate Files Before Upload
Check file size and type before attempting to upload to avoid unnecessary API calls and validation errors.

### 4. Include Context in Messages
When attaching files, explain what the file contains and why it's relevant to the case.

### 5. Avoid Excessive Updates
Consolidate related information into single updates rather than posting multiple small updates in quick succession.

## Common Use Cases

### 1. Status Update Notification
```
Scenario: Agent resolves a customer issue
Action: Post update with resolution details
Result: Case stakeholders are informed of the resolution
```

### 2. File Sharing with Team
```
Scenario: Engineer needs to share diagnostic logs with support team
Action: Post update with log file attachment
Result: Team members can download and review the logs
```

### 3. Customer Communication Log
```
Scenario: Agent completes customer call
Action: Post summary of call discussion and outcomes
Result: Complete audit trail of customer interactions
```

### 4. Escalation Documentation
```
Scenario: Case is escalated to senior support
Action: Post update explaining escalation reason with supporting documents
Result: Senior support has full context and documentation
```

## Related Catalog Requests

- [Get Case Feed Items By Case Id](pages/GetCaseFeedItemsByCaseId.md) - Retrieve existing feed items from a case
- [Case Change Alert](pages/CaseChangeAlert.md) - Receive notifications when cases are updated

## Technical Implementation

### Helper Class
- **Class**: `CaseManagementAreaSubCatalogRequest`
- **Package**: `app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog`
- **Method**: `handleReenterShareUpdateToCase`

### Validation Flow
1. **CaseIdValidator**: Validates that the Case ID exists and is accessible
2. **FileValidator**: Validates file extension against allowed types (if file is provided)
3. **ValidationOrchestrator**: Coordinates all validators and collects validation results

### Service Delegation
- **Service**: `CaseDaoImpl.addFeedUpdateToCaseById(caseId, message, base64File, fileName)`
- **File Processing**: `KristaMediaClient.toJavaFile()` converts Krista File to Java File
- **Encoding**: File is Base64-encoded before upload to Salesforce
- **API Call**: Salesforce REST API to create FeedItem with ContentVersion (for files)

### Message Processing
- **Escape Characters**: Special characters are escaped using `getEscapeCharacterMessage()` method
- **Supported Escapes**: `\"`, `\n`, `\\`, `\f`, `\t`, `\b`, `\r`

### Telemetry Metrics
- Request execution time
- Validation failures
- File upload success/failure rates
- API call success/failure rates

## Troubleshooting

### Issue: "Invalid Case ID" Error

**Cause**: The provided Case ID doesn't exist or is malformed

**Solution**:
1. Verify the Case ID is correct (15 or 18 characters)
2. Check that the case exists in your Salesforce instance
3. Ensure you're using the correct Salesforce environment (production vs sandbox)
4. Verify the case hasn't been deleted

### Issue: "File Not Supported" Error

**Cause**: File has an unsupported extension or no extension

**Solution**:
1. Check the file extension against the supported types list
2. Convert the file to a supported format (e.g., convert .html to .pdf)
3. Ensure the file name includes a valid extension
4. Avoid using restricted formats (HTML, XML, executable files)

### Issue: File Upload Fails

**Cause**: File is too large or encoding fails

**Solution**:
1. Check file size (should be under 25 MB)
2. Compress large files or split into smaller files
3. Ensure file is not corrupted
4. Try uploading a different file to isolate the issue

### Issue: "Update Message Invalid" Error

**Cause**: Message is empty or contains only whitespace

**Solution**:
1. Ensure the message contains actual text content
2. Remove leading/trailing whitespace if that's the only content
3. Provide a meaningful update message

### Issue: Authorization Error

**Cause**: User not authenticated or lacks permissions

**Solution**:
1. Re-authenticate with Salesforce
2. Verify "Create" permission on FeedItem object
3. Verify "Edit" permission on Case object
4. Check case sharing rules and ownership

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Get Case Feed Items By Case Id](pages/GetCaseFeedItemsByCaseId.md)
- [Case Change Alert](pages/CaseChangeAlert.md)

