/*
 * Salesforce Customer Service Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import com.google.gson.Gson;

public class Constants {

    /**
     * Callback path prefix passed to {@link
     * app.krista.oauth.providers.salesforce.extension.AbstractSalesforceRequestAuthenticator}
     * constructor. The full internal callback path is this value + "/callback".
     *
     * <p>Note: {@link #CALLBACK_URI} carries the EXTERNAL (full) callback URL path
     * (prefixed with "/rest/" for the Krista routing layer), which is what Salesforce calls
     * back on. This constant is the Krista-internal routing path used for state-parsing.</p>
     */
    public static final String CALLBACK_PATH_PREFIX = "/salesforcecustomerservice";

    public static final String CALLBACK_URI = "/rest/salesforcecustomerservice/callback";

    // ── Public App / Quick Setup constants (mirroring outlook-4's pattern) ───────────────────
    //
    // DEFAULT_CALLBACK_PATH — the Krista-platform central OAuth callback endpoint.
    // This is the redirect_uri registered in the Krista-owned Salesforce Connected App.
    // It is NOT per-invoker; all Quick Setup flows across all customers land here first,
    // and the platform forwards to EXTENSION_FORWARD_PATH using the state parameter.
    //
    // EXTENSION_FORWARD_PATH — the per-extension v3 forwarding target. The platform's central
    // callback handler resolves the target extension from the OAuth state and POST-forwards
    // the code+state here for per-extension code exchange and RT persistence.
    //
    // PUBLIC / PRIVATE — flow-type sentinels used internally to distinguish Quick Setup
    // (Krista-managed Connected App) from Advanced / customer-supplied credentials.
    public static final String DEFAULT_CALLBACK_PATH  = "/extension/api/rest/v3/oauth/callback";
    public static final String EXTENSION_FORWARD_PATH = "/rest/salesforcecustomerservice/v3/oauth/callback";
    public static final String PUBLIC  = "PUBLIC";
    public static final String PRIVATE = "PRIVATE";

    /**
     * State-parameter separator (CRIT-016). Public flows MUST append the per-extension forward
     * URL after a '#' so Krista's central callback handler knows which extension to forward to.
     * Mirrors {@code OutlookConstants.HASH}.
     */
    public static final String HASH = "#";

    // OAuth 2.0 Endpoints
    public static final String OAUTH2_TOKEN_ENDPOINT = "/services/oauth2/token";
    public static final String OAUTH2_AUTHORIZE_ENDPOINT = "/services/oauth2/authorize";

    // Salesforce API Paths
    public static final String SALESFORCE_SOBJECTS_PATH = "/services/data/v53.0/sobjects/";
    public static final String SALESFORCE_QUERY_PATH = "/services/data/v53.0/query?q=";

    // Authentication Flow Messages
    public static final String CLIENT_CREDENTIALS_FLOW_MESSAGE = "Using OAuth 2.0 Client Credentials flow for authentication with custom domain: {}";
    public static final String AUTHORIZATION_CODE_FLOW_MESSAGE = "Using OAuth 2.0 Authorization Code flow for authentication (no custom domain provided)";
    public static final String PASSWORD_GRANT_FLOW_MESSAGE = "Using OAuth 2.0 Password Grant flow for authentication with username/password credentials";

    // Authentication Error Messages
    public static final String INVALID_CLIENT_ERROR = "invalid_client";
    public static final String INVALID_GRANT_ERROR = "invalid_grant";
    public static final String INVALID_CLIENT_MESSAGE = "Authentication failed due to invalid client credentials. " +
            "The Client ID or Client Secret provided is incorrect or expired. " +
            "Please verify your Salesforce Connected App credentials in the extension configuration and ensure they match your Salesforce setup.";
    public static final String INVALID_GRANT_MESSAGE = "Authentication failed due to invalid user credentials. " +
            "The username, password, or security token provided is incorrect or expired. " +
            "Please verify your Salesforce username, password, and security token in the extension configuration. " +
            "If using a security token, ensure it's current and appended to your password.";
    public static final String AUTHENTICATION_FAILED_PREFIX = "Authentication failed: ";
    public static final String EXPIRED_REFRESH_TOKEN = "expired access/refresh token";

    // OAuth Configuration Error Messages
    public static final String CLIENT_ID_REQUIRED = "OAuth configuration is incomplete due to missing Client ID. " +
            "A valid Salesforce Client ID is required for authentication. " +
            "Please obtain the Client ID from your Salesforce Connected App and enter it in the extension configuration.";
    public static final String CLIENT_SECRET_REQUIRED = "OAuth configuration is incomplete due to missing Client Secret. " +
            "A valid Salesforce Client Secret is required for authentication. " +
            "Please obtain the Client Secret from your Salesforce Connected App and enter it in the extension configuration.";
    public static final String ROUTING_URL_REQUIRED = "OAuth configuration is incomplete due to missing Routing URL. " +
            "A routing URL is required to generate the callback URL for OAuth authentication. " +
            "Please configure the routing URL in your Krista system settings or contact your administrator.";
    public static final String CALLBACK_URL_REQUIRED = "OAuth configuration is incomplete due to missing Callback URL. " +
            "A callback URL is required for OAuth authentication flow. " +
            "Please ensure the routing URL is properly configured in your Krista system settings.";
    public static final String CALLBACK_URL_FORMAT_ERROR = "OAuth configuration error due to invalid Callback URL format. " +
            "The callback URL must be a valid HTTP or HTTPS URL. Found: ";
    public static final String DOMAIN_URL_REQUIRED = "Custom OAuth service configuration is incomplete due to missing Domain URL. " +
            "A valid domain URL is required when using custom Salesforce domains. " +
            "Please provide your Salesforce instance URL (e.g., https://yourcompany.my.salesforce.com) in the extension configuration.";

    // Domain Validation Messages
    public static final String DOMAIN_NULL_ERROR = "Domain validation failed because no domain was provided. " +
            "A valid Salesforce domain URL is required for custom domain authentication. " +
            "Please provide your Salesforce instance URL (e.g., https://yourcompany.my.salesforce.com) in the extension configuration.";
    public static final String DOMAIN_EMPTY_ERROR = "Domain validation failed because the domain field is empty. " +
            "A valid Salesforce domain URL is required for custom domain authentication. " +
            "Please provide your Salesforce instance URL (e.g., https://yourcompany.my.salesforce.com) in the extension configuration.";
    public static final String DOMAIN_PROTOCOL_ERROR = "Domain validation failed due to invalid protocol. " +
            "The domain URL must start with 'http://' or 'https://' (HTTPS is recommended for security). " +
            "Please update your domain URL to include the proper protocol (e.g., https://yourcompany.my.salesforce.com).";
    public static final String DOMAIN_TOO_SHORT_ERROR = "Domain validation failed because the provided domain appears to be incomplete. " +
            "A valid Salesforce domain URL should include the full hostname. " +
            "Please provide the complete Salesforce instance URL (e.g., https://yourcompany.my.salesforce.com).";

    // Instance URL Validation Messages
    public static final String INSTANCE_URL_PROTOCOL_ERROR = "Instance URL validation failed due to invalid protocol. " +
            "The Instance URL must use HTTP or HTTPS protocol (HTTPS is recommended for security). Found: ";
    public static final String INSTANCE_URL_HOST_ERROR = "Instance URL validation failed due to missing or invalid hostname. " +
            "The Instance URL must include a valid Salesforce hostname. " +
            "Please provide a complete Salesforce instance URL (e.g., https://yourcompany.my.salesforce.com).";
    public static final String INSTANCE_URL_FORMAT_ERROR = "Instance URL validation failed due to invalid format. " +
            "The Instance URL must be a valid URL with proper protocol and hostname. Invalid format: ";

    // OAuth Service Creation Error Messages
    public static final String OAUTH_SERVICE_CREATION_ERROR = "OAuth service initialization failed for standard Salesforce authentication. " +
            "The system could not create the OAuth service due to configuration issues. " +
            "Please verify your Client ID and Client Secret are correct, ensure the callback URL is properly configured, " +
            "and check that your Salesforce Connected App settings allow the required OAuth flows.";
    public static final String OAUTH_SERVICE_CUSTOM_DOMAIN_ERROR = "OAuth service initialization failed for custom Salesforce domain. " +
            "The system could not create the OAuth service for the specified custom domain: ";
    public static final String OAUTH_SERVICE_VERIFY_CONFIG = ". Please verify your Client ID and Client Secret are correct, " +
            "ensure the callback URL is properly configured, check that your custom domain URL format is valid, " +
            "and confirm that your Salesforce Connected App settings allow the required OAuth flows.";

    // Authentication Success Messages
    public static final String USER_AUTHENTICATED_SUCCESSFULLY = "User authenticated successfully.";
    public static final String SAVE_THE_CHANGES = " Save the changes.";

    // HTTP Methods and Parameters
    public static final String HTTP_METHOD_PATCH = "?_HttpMethod=PATCH";

    // JSON Error Keys
    public static final String JSON_ERROR_KEY = "error";

    public static final String INVALID_STATE_PARAMETERS = "Invalid state parameters!";
    public static final String AND = "AND";
    public static final String WHERE = "WHERE";
    public static final String LASTMODIFIEDDATE = "LastModifiedDate";
    public static final String DESC = "DESC";
    public static final String FAILED_TO_GET_CUSTOMER = "Failed to get Customer. ";
    public static final String ORDER_BY = " ORDER BY ";
    public static final String LIMIT = " LIMIT ";
    public static final String OFFSET = " OFFSET ";
    public static final String SPACE_AND = " AND";
    public static final String SPACE = " ";
    public static final String CONTACT_ID = "Contact Id";
    public static final String CONTACT_NAME = "Contact Name";
    public static final String CONTACT_PHONE = "Contact Phone";
    public static final String CONTACT_EMAIL = "Contact Email";
    public static final String CONTACT_ID_IS_REQUIRED_TO_ADD_CONTACT_INFORMATION = "Contact information cannot be added because no Contact ID was provided. " +
            "A valid Salesforce Contact ID is required to associate contact information. " +
            "Please provide a valid Contact ID from your Salesforce system and try again.";
    public static final String ACCOUNT_ID_IS_REQUIRED_TO_ADD_ACCOUNT_NAME = "Account name cannot be added because no Account ID was provided. " +
            "A valid Salesforce Account ID is required to associate account information. " +
            "Please provide a valid Account ID from your Salesforce system and try again.";
    public static final String ACCOUNT_NAME = "Account Name";
    public static final String ACCOUNT_ID = "Account Id";
    public static final String STATUS = "Status";
    public static final String REASON = "Reason";
    public static final String ORIGIN = "Origin";
    public static final String DESCRIPTION = "Description";
    public static final String PRIORITY = "Priority";
    public static final String SUBJECT = "Subject";
    public static final String IS_CLOSED = "Is Closed";
    public static final String NAME = "Name";
    public static final String TITLE = "Title";
    public static final String REPORTS_TO_ID = "Reports To Id";
    public static final String ID = "Id";
    public static final String DEPARTMENT = "Department";
    public static final String PHONE = "Phone";
    public static final String EMAIL = "Email";
    public static final String CREATED_DATE = "Created Date";
    public static final String LAST_MODIFIED_DATE = "Last Modified Date";
    public static final String TYPE = "Type";
    public static final String FAX = "Fax";
    public static final String ACCOUNT_NUMBER = "Account Number";
    public static final String WEBSITE = "Website";
    public static final String SIC = "Sic";
    public static final String OWNER_ID = "Owner Id";
    public static final String CLEAN_STATUS = "Clean Status";
    public static final String CUSTOMER_PRIORITY = "CustomerPriority";
    public static final String SLA_SERIAL_NUMBER = "SLA Serial Number";
    public static final String BILLING_STREET = "Billing Street";
    public static final String BILLING_CITY = "Billing City";
    public static final String BILLING_STATE = "Billing State";
    public static final String BILLING_POSTAL_CODE = "Billing Postal Code";
    public static final String BILLING_COUNTRY = "Billing Country";
    public static final String SHIPPING_STREET = "Shipping Street";
    public static final String SHIPPING_CITY = "Shipping City";
    public static final String SHIPPING_STATE = "Shipping State";
    public static final String SHIPPING_POSTAL_CODE = "Shipping Postal Code";
    public static final String SHIPPING_COUNTRY = "Shipping Country";
    public static final String NUMBER_OF_EMPLOYEES = "Number Of Employees";
    public static final String SLA_EXPIRATION_DATE = "SLA Expiration Date";
    public static final String OWNERSHIP = "Ownership";
    public static final String INDUSTRY = "Industry";
    public static final String PRIMARY_KEY_IS_EMPTY = "Record operation failed because the primary key is missing. " +
            "A valid primary key is required to identify the record. " +
            "Please provide a valid primary key value and try again.";
    public static final String ACCOUNT_ID_IS_REQUIRED_TO_UPDATE_ACCOUNT_NAME = "Account name cannot be updated because no Account ID was provided. " +
            "A valid Salesforce Account ID is required to update account information. " +
            "Please provide a valid Account ID from your Salesforce system and try again.";
    public static final String ENTITY_INPUT_IS_NULL_OR_EMPTY = "Entity operation failed because no entity data was provided. " +
            "Valid entity information is required to perform this operation. " +
            "Please provide the required entity data and try again.";
    public static final String ID_IS_NULL_OR_EMPTY = "Record operation failed because no ID was provided. " +
            "A valid Salesforce record ID is required to perform this operation. " +
            "Please provide a valid record ID and try again.";
    public static final String STRING = "String";
    public static final String EQUALS = "equals";
    public static final String CASE_NUMBER = "Case Number";
    public static final String MODIFIED_DATE = "Modified Date";
    public static final String LONG = "Long";
    public static final String GT = "gt";
    public static final String LT = "lt";
    public static final String QUERY_IS_NULL_OR_EMPTY = "Search operation failed because no query was provided. " +
            "A valid search query is required to perform the search operation. " +
            "Please provide search criteria and try again.";
    public static final String CONDITIONS_NOT_FOUND = "Search operation failed because no search conditions were specified. " +
            "Valid search conditions are required to filter results. " +
            "Please specify search conditions and try again.";
    public static final String ENTITY_MAP_IS_NULL_OR_EMPTY = "Entity mapping operation failed because no entity data was provided. " +
            "A valid entity map with data is required to perform this operation. " +
            "Please provide the required entity mapping data and try again.";
    public static final String FAILED_TO_SEARCH = "Search operation failed due to an error during execution. " +
            "The system encountered an issue while performing the search. " +
            "Please check your search criteria and try again, or contact support if the issue persists.";
    public static final Gson GSON = new Gson();
    public static final String CREATED = "created";
    public static final String TEXT = "Text";
    public static final String DOUBLE = "Double";
    public static final String ENTERPRISE_SOAP_SFORCE_COM = "urn:sobject.enterprise.soap.sforce.com";
    public static final String COM_2005_09_OUTBOUND = "http://soap.sforce.com/2005/09/outbound";
    public static final String BOOLEAN = "Boolean";
    public static final String INVALID_CASE_ID = "Case operation failed because the provided Case ID is invalid. " +
            "A valid Salesforce Case ID is required to perform this operation. " +
            "Please verify the Case ID exists in your Salesforce system and try again.";
    public static final String INVALID_NUMBER_OF_POSTS = "Post retrieval failed because the number of posts specified is invalid. " +
            "The number of posts must be a positive integer between 1 and 100. " +
            "Please provide a valid number within the allowed range and try again.";
    public static final String INVALID = "Invalid";
    public static final String ERROR_CASE_CHANGE_ALERT = "Case change alert processing failed due to an error. " +
            "The system encountered an issue while processing the case change notification. " +
            "Please verify the case data and alert configuration, then try again.";

    private Constants() {
    }

}
