package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Constants {

    public static final String CALLBACK_URI = "/rest/salesforcecustomerservice/callback";

    public static final String REFRESH_TOKEN = "refresh_token";
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
    public static final String CONTACT_ID_IS_REQUIRED_TO_ADD_CONTACT_INFORMATION = "Contact Id is required to add Contact Information!";
    public static final String ACCOUNT_ID_IS_REQUIRED_TO_ADD_ACCOUNT_NAME = "Account Id is required to add Account Name!";
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
    public static final String CREATED_BY_ID = "Created By Id";
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
    public static final String PRIMARY_KEY_IS_EMPTY = "Primary key is empty";
    public static final String ACCOUNT_ID_IS_REQUIRED_TO_UPDATE_ACCOUNT_NAME = "Account Id is required to update account name.";
    public static final String ENTITY_INPUT_IS_NULL_OR_EMPTY = "Entity input is null or empty.";
    public static final String ID_IS_NULL_OR_EMPTY = "Id is null or empty.";
    public static final String STRING = "String";
    public static final String EQUALS = "equals";
    public static final String CASE_NUMBER = "Case Number";
    public static final String MODIFIED_DATE = "Modified Date";
    public static final String LONG = "Long";
    public static final String GT = "gt";
    public static final String LT = "lt";
    public static final String QUERY_IS_NULL_OR_EMPTY = "Query is null or empty.";
    public static final String CONDITIONS_NOT_FOUND = "Conditions not found.";
    public static final String ENTITY_MAP_IS_NULL_OR_EMPTY = "Entity map is null or empty. ";
    public static final String FAILED_TO_SEARCH = "Failed to Search. ";
    public static final Gson GSON = new Gson();
    public static final String CREATED = "created";
    public static final String TEXT = "Text";
    public static final String DOUBLE = "Double";
    public static final String ENTERPRISE_SOAP_SFORCE_COM = "urn:sobject.enterprise.soap.sforce.com";
    public static final String COM_2005_09_OUTBOUND = "http://soap.sforce.com/2005/09/outbound";



    private Constants() {
    }

}
