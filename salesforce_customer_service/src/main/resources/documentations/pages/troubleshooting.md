# Salesforce Integration Troubleshooting Guide

## Common Issues and Solutions

### Authentication Failures

#### 1. Service Account Authentication Issues
**Symptoms:**
- Conversations fail with "Authentication Failed" message
- Unable to execute automated tasks
- Service account credentials rejected

**Possible Causes & Solutions:**
- **Invalid Credentials**: Verify service account credentials in extension setup
- **Expired Certificates**: Check if Salesforce certificates need renewal
- **IP Restrictions**: Ensure IP addresses are whitelisted in Salesforce
- **Permission Issues**: Verify service account has required Salesforce permissions

#### 2. User Account Authentication Problems
**Symptoms:**
- Users cannot authenticate with their accounts
- "Must Authenticate" exceptions
- Authentication popup loops

**Solutions:**
1. Verify "Have people authenticate with their account" checkbox is correctly set
2. Check user has valid Salesforce licenses
3. Ensure OAuth scopes are properly configured
4. Clear browser cache and cookies

### Token-Related Issues

#### 1. Token Expiration Problems
**Symptoms:**
- Intermittent authentication failures
- "Token expired" errors
- Sessions ending unexpectedly

**Solutions:**
1. Check token refresh mechanism is working
2. Verify refresh token hasn't expired
3. Ensure token storage is properly configured
4. Review token lifecycle logs

#### 2. Token Refresh Failures
**Symptoms:**
- Unable to get new access tokens
- Continuous authentication prompts
- "Invalid refresh token" errors

**Solutions:**
1. Verify refresh token is stored correctly
2. Check Salesforce Connected App settings
3. Ensure network connectivity to Salesforce
4. Review OAuth configuration

### Integration Configuration Issues

#### 1. Connected App Setup
**Symptoms:**
- OAuth initialization failures
- Invalid client credentials errors
- Callback URL problems

**Solutions:**
1. Verify Connected App credentials
2. Check callback URLs match exactly
3. Ensure all required OAuth scopes are configured
4. Review IP restrictions in Connected App

#### 2. Permission Sets and Profiles
**Symptoms:**
- Access denied errors
- Unable to perform specific operations
- Limited functionality

**Solutions:**
1. Review permission sets assigned to users
2. Check profile settings in Salesforce
3. Verify API access is enabled
4. Ensure proper object-level permissions

## Debugging Steps

### 1. Check Logs
- Review Krista extension logs
- Check Salesforce setup audit logs
- Examine browser console for frontend issues

### 2. Verify Configuration
```json
{
  "checklist": {
    "oauth": {
      "clientId": "Verify client ID is correct",
      "clientSecret": "Ensure client secret is valid",
      "redirectUri": "Check callback URL configuration"
    },
    "permissions": {
      "api": "Verify API access",
      "objects": "Check object permissions",
      "fields": "Verify field-level security"
    }
  }
}
```

### 3. Test Authentication Flow
1. Clear all existing tokens
2. Attempt new authentication
3. Monitor token refresh process
4. Check for error responses

## Common Error Messages

| Error Message | Possible Cause | Solution |
|--------------|----------------|----------|
| "INVALID_CLIENT_ID" | Incorrect Connected App configuration | Verify Client ID in setup |
| "INVALID_GRANT" | Invalid/expired refresh token | Re-authenticate user |
| "API_DISABLED" | API access not enabled | Enable API access in profile |
| "INVALID_SESSION_ID" | Expired/invalid access token | Force token refresh |

## Support Resources

### Internal Resources
- Extension documentation
- API reference guides
- Configuration templates

### External Resources
- [Salesforce OAuth Documentation](https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/intro_understanding_authentication.htm)
- [Salesforce Connected Apps](https://help.salesforce.com/articleView?id=connected_app_overview.htm)
- [OAuth 2.0 Specification](https://oauth.net/2/)

## Getting Additional Help
If issues persist after following this guide:
1. Gather relevant error messages and logs
2. Document reproduction steps
3. Contact support with collected information

---

**Note**: Keep this guide updated as new issues and solutions are discovered. Regular reviews of common problems help improve troubleshooting efficiency.