# Retrieve Custom Fields For Given Object

## Overview

Retrieves custom field values from a specific Salesforce object record. This request allows you to access custom fields that have been added to standard Salesforce objects like Account, Case, Contact, Customer, or Individual.

## Request Details

- **Area**: Content
- **Type**: QUERY_SYSTEM
- **Retry Support**: ✅ Yes - If validation fails, the user is prompted to re-enter the Object ID, Object Type, or Custom Fields

## Input Parameters

| Parameter Name | Type | Required | Description | Example |
|----------------|------|----------|-------------|---------|
| Object Id | Text | Yes | The unique Salesforce ID of the record from which to retrieve custom fields | "0015w00000CzXyBBB" |
| Object Type | Pick One | Yes | The type of Salesforce object (Account, Case, Contact, Customer, or Individual) | "Account" |
| Custom Fields | List&lt;Text&gt; | Yes | List of custom field API names to retrieve (each must end with `__c`) | ["Competition__c", "SLA_Tier__c"] |

### Parameter Details

**Object Id**:
- Must be a valid 15 or 18-character Salesforce record ID
- The record must exist in your Salesforce instance
- The authenticated user must have read access to the record
- Format: Alphanumeric string (prefix varies by object type)

**Object Type**:
- Must be one of the following values:
  - `Account` - Business account records
  - `Case` - Customer service case records
  - `Contact` - Contact person records
  - `Customer` - Customer records (if using Customer object)
  - `Individual` - Individual person records (GDPR/privacy object)
- This determines which Salesforce object to query

**Custom Fields**:
- List of custom field API names
- Each field name **must end with `__c`** (Salesforce custom field naming convention)
- Field names are case-sensitive
- All specified fields must exist on the selected object type
- The authenticated user must have field-level read access to all specified fields

> **📝 Note**: Standard Salesforce fields do not end with `__c`. This request is specifically for custom fields only.

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| Field Details | FreeForm | Object containing the requested custom field names and their values |

### Field Details Structure

The output is a FreeForm object (key-value map) containing:
- **Keys**: Custom field API names (as specified in the input)
- **Values**: The actual values of those fields from the Salesforce record

**Example Output**:
```json
{
  "Field Details": {
    "Competition__c": "Competitor A",
    "SLA_Tier__c": "Gold",
    "Custom_Priority__c": 5,
    "Last_Review_Date__c": "2025-01-10",
    "Special_Instructions__c": "Handle with care - VIP customer"
  }
}
```

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Object ID is null or blank | "The provided object id \"[objectId]\" is invalid or does not exist in Salesforce." | Provide a valid Object ID |
| Object ID doesn't exist for the specified type | "The provided object id: [objectId] is invalid." | Verify the Object ID exists and matches the Object Type |
| Custom Fields list is null or empty | "One or more custom fields are invalid: [fields]. Please ensure each field name ends with '__c'." | Provide at least one custom field name |
| Custom field doesn't end with `__c` | "One or more fields are not valid custom fields. All custom fields must end with '__c'." | Ensure all field names end with `__c` |
| Custom field doesn't exist on the object | "The provided custom field(s): [fields] do not exist." | Verify the field exists on the specified object type in Salesforce |
| Object Type is invalid | "Unknown object type: [objectType]" | Use one of the supported object types (Account, Case, Contact, Customer, Individual) |

## Error Handling

### Input Errors (INPUT_ERROR)

**Cause**: Invalid Object ID, Object Type, or Custom Fields

**Common Scenarios**:
- Object ID is empty, null, or doesn't exist
- Object ID exists but is for a different object type than specified
- Custom field name doesn't end with `__c`
- Custom field doesn't exist on the specified object
- Custom Fields list is empty

**Error Messages**:
- "The provided object id \"[objectId]\" is invalid or does not exist in Salesforce."
- "The provided object id: [objectId] is invalid."
- "One or more fields are not valid custom fields. All custom fields must end with '__c'."
- "The provided custom field(s): [fields] do not exist."
- "One or more custom fields are invalid: [fields]. Please ensure each field name ends with '__c'."

**Resolution**:
1. Verify the Object ID is correct and exists in Salesforce
2. Ensure the Object Type matches the Object ID (e.g., don't use a Case ID with Object Type = "Account")
3. Check that all custom field names end with `__c`
4. Verify the custom fields exist on the specified object in Salesforce
5. Ensure field names are spelled correctly (case-sensitive)
6. Re-enter the parameters when prompted

### Logic Errors (LOGIC_ERROR)

**Cause**: Business logic failure during field retrieval

**Common Scenarios**:
- User lacks field-level security access to one or more custom fields
- Object exists but has no data for the requested fields (returns null values)

**Resolution**:
- Verify field-level security settings for the user's profile
- Check that the user has read access to all requested custom fields
- Null values are valid and indicate the field has no data

### System Errors (SYSTEM_ERROR)

**Cause**: Unexpected system-level failures during API communication

**Common Scenarios**:
- Salesforce API is unavailable
- Network connectivity issues
- Timeout during query execution
- Unexpected Salesforce API errors

**Error Messages**:
- "An unexpected error occurred. Please try again later."
- "An unexpected error occurred while retrieving custom fields."

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
- User lacks read permission on the object
- User lacks field-level security access to custom fields

**Resolution**:
1. Re-authenticate with Salesforce
2. Verify user has "Read" permission on the specified object type
3. Check field-level security settings for all requested custom fields
4. Ensure user profile has API access enabled
5. Verify object sharing rules allow access to the specific record

## Usage Examples

### Example 1: Retrieve Account Custom Fields

**Input**:
```
Object Id: "0015w00000CzXyBBB"
Object Type: "Account"
Custom Fields: ["Industry_Segment__c", "Annual_Contract_Value__c", "Renewal_Date__c"]
```

**Output**:
```json
{
  "Field Details": {
    "Industry_Segment__c": "Healthcare",
    "Annual_Contract_Value__c": 250000,
    "Renewal_Date__c": "2025-12-31"
  }
}
```

**Result**: Successfully retrieved three custom fields from the Account record.

### Example 2: Retrieve Case Custom Fields

**Input**:
```
Object Id: "5005w00000AlDtDAAV"
Object Type: "Case"
Custom Fields: ["Escalation_Reason__c", "Customer_Sentiment__c"]
```

**Output**:
```json
{
  "Field Details": {
    "Escalation_Reason__c": "Technical complexity",
    "Customer_Sentiment__c": "Frustrated"
  }
}
```

**Result**: Retrieved custom fields that help understand case escalation and customer mood.

### Example 3: Invalid Custom Field Name with Retry

**Input**:
```
Object Id: "0035w00000BxYzAAK"
Object Type: "Contact"
Custom Fields: ["PreferredLanguage", "VIP_Status__c"]
```

**Validation Error**:
```
"One or more fields are not valid custom fields. All custom fields must end with '__c'."
```

**Issue**: "PreferredLanguage" doesn't end with `__c`

**User Prompted**: "Do you want to continue?"

**User Response**: Yes

**Re-enter Prompt**: "Please enter a list of Salesforce custom field names (each must end with '__c')."

**Corrected Input**:
```
Object Id: "0035w00000BxYzAAK"
Object Type: "Contact"
Custom Fields: ["Preferred_Language__c", "VIP_Status__c"]
```

**Result**: Custom fields successfully retrieved after correction.

### Example 4: Field Doesn't Exist on Object

**Input**:
```
Object Id: "0015w00000CzXyBBB"
Object Type: "Account"
Custom Fields: ["Nonexistent_Field__c"]
```

**Validation Error**:
```
"The provided custom field(s): Nonexistent_Field__c do not exist."
```

**Resolution**: Verify the field exists on the Account object in Salesforce, or correct the field name.

## Business Rules

1. **Custom Fields Only**: This request only retrieves custom fields (ending with `__c`). Use standard entity requests for standard fields.
2. **Field-Level Security**: Users can only retrieve fields they have read access to based on their Salesforce profile and field-level security settings.
3. **Null Values**: If a custom field exists but has no value for the record, it returns `null` in the output.
4. **Case-Sensitive**: Custom field API names are case-sensitive and must match exactly.
5. **Object Type Validation**: The Object ID must correspond to the specified Object Type, or validation will fail.

## Limitations

1. **Custom Fields Only**: Cannot retrieve standard Salesforce fields using this request.
2. **Single Record**: Retrieves fields from one record at a time (no bulk retrieval).
3. **Object Type Restriction**: Only supports Account, Case, Contact, Customer, and Individual objects.
4. **Field Existence Check**: All specified fields must exist on the object; partial retrieval is not supported.
5. **API Rate Limits**: Subject to Salesforce API rate limits. Excessive requests may be throttled.
6. **Field-Level Security**: Respects Salesforce field-level security; inaccessible fields will cause errors.

## Best Practices

### 1. Verify Field Names Before Calling
Check the custom field API names in Salesforce Setup before making the request to avoid validation errors.

### 2. Use Correct Object Type
Ensure the Object Type matches the Object ID. For example, don't use a Case ID with Object Type = "Account".

### 3. Request Only Needed Fields
Only request the custom fields you actually need to minimize data transfer and improve performance.

### 4. Handle Null Values
Design your conversations to handle null values gracefully, as custom fields may not have data for all records.

### 5. Check Field-Level Security
Ensure the authenticated user has read access to all requested custom fields before making the request.

## Common Use Cases

### 1. Enriched Case Context
```
Scenario: Agent needs additional case context from custom fields
Action: Retrieve custom fields like "Escalation_Reason__c", "Customer_Tier__c"
Result: Agent has full context to handle the case effectively
```

### 2. Account Intelligence
```
Scenario: Sales rep needs account-specific custom data
Action: Retrieve custom fields like "Industry_Segment__c", "Contract_Value__c"
Result: Rep has insights to personalize customer interactions
```

### 3. Compliance and Privacy
```
Scenario: Need to check GDPR consent fields on Individual records
Action: Retrieve custom fields like "Marketing_Consent__c", "Data_Retention_Date__c"
Result: Ensure compliance with privacy regulations
```

### 4. Custom Workflow Data
```
Scenario: Automated workflow needs custom field values to make decisions
Action: Retrieve custom fields that drive business logic
Result: Workflow can branch based on custom field values
```

## Related Catalog Requests

- [Get Case Feed Items By Case Id](GetCaseFeedItemsByCaseId.md) - Retrieve case feed items
- [Share Update to Case](ShareUpdateToCase.md) - Add updates to cases

## Technical Implementation

### Helper Class
- **Class**: `ContentAreaSubCatalogRequest`
- **Package**: `app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog`
- **Method**: `handleReenterRetrieveCustomFieldsForGivenObject`

### Validation Flow
1. **ObjectIdValidator**: Validates that the Object ID exists for the specified Object Type
2. **CustomFieldsValidator**: Validates that all custom fields end with `__c` and exist on the object
3. **ValidationOrchestrator**: Coordinates all validators and collects validation results

### Service Delegation
- **Service**: `ContentDaoImpl.parseJSONToMap()` - Parses Salesforce response
- **Field Validation**: `SalesforceService.checkFieldExistsInSalesforce()` - Verifies field existence
- **API Call**: Salesforce REST API SOQL query with dynamic field selection

### Query Construction
The extension builds a dynamic SOQL query:
```sql
SELECT [custom_field_1__c], [custom_field_2__c], ...
FROM [ObjectType]
WHERE Id = '[ObjectId]'
```

### Telemetry Metrics
- Request execution time
- Validation failures
- Field retrieval success/failure rates
- API call success/failure rates

## Troubleshooting

### Issue: "Field Must End with '__c'" Error

**Cause**: One or more field names don't follow Salesforce custom field naming convention

**Solution**:
1. Verify all field names end with `__c`
2. Check for typos in field names
3. Ensure you're not trying to retrieve standard fields (which don't end with `__c`)
4. Use the correct API name, not the field label

### Issue: "Custom Field Doesn't Exist" Error

**Cause**: The specified custom field doesn't exist on the object

**Solution**:
1. Go to Salesforce Setup → Object Manager → [Object Type] → Fields & Relationships
2. Verify the custom field exists
3. Check the exact API name (case-sensitive)
4. Ensure the field is on the correct object type

### Issue: "Invalid Object ID" Error

**Cause**: Object ID doesn't exist or doesn't match the Object Type

**Solution**:
1. Verify the Object ID is correct
2. Ensure the Object Type matches the Object ID (e.g., Account ID with Object Type = "Account")
3. Check that the record hasn't been deleted
4. Verify you're using the correct Salesforce environment (production vs sandbox)

### Issue: Field-Level Security Error

**Cause**: User lacks read access to one or more custom fields

**Solution**:
1. Check field-level security settings in Salesforce
2. Verify the user's profile has read access to all requested fields
3. Update field-level security or use a different user with appropriate permissions

### Issue: Null Values Returned

**Cause**: Custom fields exist but have no data for the record

**Solution**:
- This is normal behavior; not all records have values for all custom fields
- Handle null values in your conversation logic
- Verify the field actually has data in Salesforce UI

## See Also

- [Extension Configuration](ExtensionConfiguration.md)
- [Authentication](Authentication.md)
- [Salesforce Custom Fields Documentation](https://help.salesforce.com/articleView?id=adding_fields.htm)

