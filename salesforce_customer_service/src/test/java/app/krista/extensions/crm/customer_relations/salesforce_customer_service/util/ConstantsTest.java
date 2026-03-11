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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Constants} class.
 * Tests constant values and GSON instance.
 */
public class ConstantsTest {

    /**
     * Test OAuth 2.0 endpoint constants are not null or empty.
     */
    @Test
    public void testOAuthEndpoints_NotNullOrEmpty() {
        // Assert
        assertNotNull("CALLBACK_URI should not be null", Constants.CALLBACK_URI);
        assertNotNull("OAUTH2_TOKEN_ENDPOINT should not be null", Constants.OAUTH2_TOKEN_ENDPOINT);
        assertNotNull("OAUTH2_AUTHORIZE_ENDPOINT should not be null", Constants.OAUTH2_AUTHORIZE_ENDPOINT);
        
        assertFalse("CALLBACK_URI should not be empty", Constants.CALLBACK_URI.isEmpty());
        assertFalse("OAUTH2_TOKEN_ENDPOINT should not be empty", Constants.OAUTH2_TOKEN_ENDPOINT.isEmpty());
        assertFalse("OAUTH2_AUTHORIZE_ENDPOINT should not be empty", Constants.OAUTH2_AUTHORIZE_ENDPOINT.isEmpty());
    }

    /**
     * Test Salesforce API path constants are not null or empty.
     */
    @Test
    public void testSalesforceApiPaths_NotNullOrEmpty() {
        // Assert
        assertNotNull("SALESFORCE_SOBJECTS_PATH should not be null", Constants.SALESFORCE_SOBJECTS_PATH);
        assertNotNull("SALESFORCE_QUERY_PATH should not be null", Constants.SALESFORCE_QUERY_PATH);
        
        assertFalse("SALESFORCE_SOBJECTS_PATH should not be empty", Constants.SALESFORCE_SOBJECTS_PATH.isEmpty());
        assertFalse("SALESFORCE_QUERY_PATH should not be empty", Constants.SALESFORCE_QUERY_PATH.isEmpty());
    }

    /**
     * Test authentication flow message constants are not null or empty.
     */
    @Test
    public void testAuthenticationFlowMessages_NotNullOrEmpty() {
        // Assert
        assertNotNull("CLIENT_CREDENTIALS_FLOW_MESSAGE should not be null", Constants.CLIENT_CREDENTIALS_FLOW_MESSAGE);
        assertNotNull("AUTHORIZATION_CODE_FLOW_MESSAGE should not be null", Constants.AUTHORIZATION_CODE_FLOW_MESSAGE);
        assertNotNull("PASSWORD_GRANT_FLOW_MESSAGE should not be null", Constants.PASSWORD_GRANT_FLOW_MESSAGE);
        
        assertFalse("CLIENT_CREDENTIALS_FLOW_MESSAGE should not be empty", Constants.CLIENT_CREDENTIALS_FLOW_MESSAGE.isEmpty());
        assertFalse("AUTHORIZATION_CODE_FLOW_MESSAGE should not be empty", Constants.AUTHORIZATION_CODE_FLOW_MESSAGE.isEmpty());
        assertFalse("PASSWORD_GRANT_FLOW_MESSAGE should not be empty", Constants.PASSWORD_GRANT_FLOW_MESSAGE.isEmpty());
    }

    /**
     * Test authentication error constants are not null or empty.
     */
    @Test
    public void testAuthenticationErrors_NotNullOrEmpty() {
        // Assert
        assertNotNull("INVALID_CLIENT_ERROR should not be null", Constants.INVALID_CLIENT_ERROR);
        assertNotNull("INVALID_GRANT_ERROR should not be null", Constants.INVALID_GRANT_ERROR);
        assertNotNull("INVALID_CLIENT_MESSAGE should not be null", Constants.INVALID_CLIENT_MESSAGE);
        assertNotNull("INVALID_GRANT_MESSAGE should not be null", Constants.INVALID_GRANT_MESSAGE);
        
        assertEquals("INVALID_CLIENT_ERROR should be 'invalid_client'", "invalid_client", Constants.INVALID_CLIENT_ERROR);
        assertEquals("INVALID_GRANT_ERROR should be 'invalid_grant'", "invalid_grant", Constants.INVALID_GRANT_ERROR);
    }

    /**
     * Test query-related constants are not null or empty.
     */
    @Test
    public void testQueryConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("AND should not be null", Constants.AND);
        assertNotNull("WHERE should not be null", Constants.WHERE);
        assertNotNull("ORDER_BY should not be null", Constants.ORDER_BY);
        assertNotNull("LIMIT should not be null", Constants.LIMIT);
        assertNotNull("OFFSET should not be null", Constants.OFFSET);
        
        assertEquals("AND should be 'AND'", "AND", Constants.AND);
        assertEquals("WHERE should be 'WHERE'", "WHERE", Constants.WHERE);
    }

    /**
     * Test field name constants are not null or empty.
     */
    @Test
    public void testFieldNameConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("ID should not be null", Constants.ID);
        assertNotNull("NAME should not be null", Constants.NAME);
        assertNotNull("EMAIL should not be null", Constants.EMAIL);
        assertNotNull("PHONE should not be null", Constants.PHONE);
        assertNotNull("STATUS should not be null", Constants.STATUS);
        assertNotNull("SUBJECT should not be null", Constants.SUBJECT);
        assertNotNull("DESCRIPTION should not be null", Constants.DESCRIPTION);
        
        assertEquals("ID should be 'Id'", "Id", Constants.ID);
        assertEquals("NAME should be 'Name'", "Name", Constants.NAME);
        assertEquals("EMAIL should be 'Email'", "Email", Constants.EMAIL);
    }

    /**
     * Test data type constants are not null or empty.
     */
    @Test
    public void testDataTypeConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("STRING should not be null", Constants.STRING);
        assertNotNull("DOUBLE should not be null", Constants.DOUBLE);
        assertNotNull("LONG should not be null", Constants.LONG);
        assertNotNull("BOOLEAN should not be null", Constants.BOOLEAN);
        assertNotNull("TEXT should not be null", Constants.TEXT);
        
        assertEquals("STRING should be 'String'", "String", Constants.STRING);
        assertEquals("DOUBLE should be 'Double'", "Double", Constants.DOUBLE);
        assertEquals("LONG should be 'Long'", "Long", Constants.LONG);
        assertEquals("BOOLEAN should be 'Boolean'", "Boolean", Constants.BOOLEAN);
    }

    /**
     * Test operator constants are not null or empty.
     */
    @Test
    public void testOperatorConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("EQUALS should not be null", Constants.EQUALS);
        assertNotNull("GT should not be null", Constants.GT);
        assertNotNull("LT should not be null", Constants.LT);
        
        assertEquals("EQUALS should be 'equals'", "equals", Constants.EQUALS);
        assertEquals("GT should be 'gt'", "gt", Constants.GT);
        assertEquals("LT should be 'lt'", "lt", Constants.LT);
    }

    /**
     * Test error message constants contain helpful information.
     */
    @Test
    public void testErrorMessages_ContainHelpfulInformation() {
        // Assert
        assertTrue("CLIENT_ID_REQUIRED should mention Client ID", 
                Constants.CLIENT_ID_REQUIRED.contains("Client ID"));
        assertTrue("CLIENT_SECRET_REQUIRED should mention Client Secret", 
                Constants.CLIENT_SECRET_REQUIRED.contains("Client Secret"));
        assertTrue("DOMAIN_NULL_ERROR should mention domain", 
                Constants.DOMAIN_NULL_ERROR.toLowerCase().contains("domain"));
        assertTrue("INVALID_CASE_ID should mention Case ID", 
                Constants.INVALID_CASE_ID.contains("Case ID"));
    }

    /**
     * Test GSON instance is not null and functional.
     */
    @Test
    public void testGSON_NotNullAndFunctional() {
        // Assert
        assertNotNull("GSON should not be null", Constants.GSON);
        assertTrue("GSON should be instance of Gson", Constants.GSON instanceof Gson);
        
        // Test GSON functionality
        String json = Constants.GSON.toJson("test");
        assertNotNull("GSON should serialize strings", json);
        assertEquals("GSON should serialize correctly", "\"test\"", json);
    }

    /**
     * Test HTTP method constants.
     */
    @Test
    public void testHttpMethodConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("HTTP_METHOD_PATCH should not be null", Constants.HTTP_METHOD_PATCH);
        assertEquals("HTTP_METHOD_PATCH should be correct", "?_HttpMethod=PATCH", Constants.HTTP_METHOD_PATCH);
    }

    /**
     * Test JSON error key constant.
     */
    @Test
    public void testJsonErrorKey_NotNullOrEmpty() {
        // Assert
        assertNotNull("JSON_ERROR_KEY should not be null", Constants.JSON_ERROR_KEY);
        assertEquals("JSON_ERROR_KEY should be 'error'", "error", Constants.JSON_ERROR_KEY);
    }

    /**
     * Test contact-related field constants.
     */
    @Test
    public void testContactFieldConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("CONTACT_ID should not be null", Constants.CONTACT_ID);
        assertNotNull("CONTACT_NAME should not be null", Constants.CONTACT_NAME);
        assertNotNull("CONTACT_PHONE should not be null", Constants.CONTACT_PHONE);
        assertNotNull("CONTACT_EMAIL should not be null", Constants.CONTACT_EMAIL);
        
        assertEquals("CONTACT_ID should be 'Contact Id'", "Contact Id", Constants.CONTACT_ID);
        assertEquals("CONTACT_NAME should be 'Contact Name'", "Contact Name", Constants.CONTACT_NAME);
    }

    /**
     * Test account-related field constants.
     */
    @Test
    public void testAccountFieldConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("ACCOUNT_ID should not be null", Constants.ACCOUNT_ID);
        assertNotNull("ACCOUNT_NAME should not be null", Constants.ACCOUNT_NAME);
        assertNotNull("ACCOUNT_NUMBER should not be null", Constants.ACCOUNT_NUMBER);
        
        assertEquals("ACCOUNT_ID should be 'Account Id'", "Account Id", Constants.ACCOUNT_ID);
        assertEquals("ACCOUNT_NAME should be 'Account Name'", "Account Name", Constants.ACCOUNT_NAME);
    }

    /**
     * Test case-related field constants.
     */
    @Test
    public void testCaseFieldConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("CASE_NUMBER should not be null", Constants.CASE_NUMBER);
        assertNotNull("IS_CLOSED should not be null", Constants.IS_CLOSED);
        assertNotNull("PRIORITY should not be null", Constants.PRIORITY);
        assertNotNull("ORIGIN should not be null", Constants.ORIGIN);
        
        assertEquals("CASE_NUMBER should be 'Case Number'", "Case Number", Constants.CASE_NUMBER);
        assertEquals("IS_CLOSED should be 'Is Closed'", "Is Closed", Constants.IS_CLOSED);
    }

    /**
     * Test date-related field constants.
     */
    @Test
    public void testDateFieldConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("CREATED_DATE should not be null", Constants.CREATED_DATE);
        assertNotNull("LAST_MODIFIED_DATE should not be null", Constants.LAST_MODIFIED_DATE);
        assertNotNull("MODIFIED_DATE should not be null", Constants.MODIFIED_DATE);
        assertNotNull("LASTMODIFIEDDATE should not be null", Constants.LASTMODIFIEDDATE);
        
        assertEquals("CREATED_DATE should be 'Created Date'", "Created Date", Constants.CREATED_DATE);
        assertEquals("LASTMODIFIEDDATE should be 'LastModifiedDate'", "LastModifiedDate", Constants.LASTMODIFIEDDATE);
    }

    /**
     * Test address-related field constants.
     */
    @Test
    public void testAddressFieldConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("BILLING_STREET should not be null", Constants.BILLING_STREET);
        assertNotNull("BILLING_CITY should not be null", Constants.BILLING_CITY);
        assertNotNull("BILLING_STATE should not be null", Constants.BILLING_STATE);
        assertNotNull("SHIPPING_STREET should not be null", Constants.SHIPPING_STREET);
        assertNotNull("SHIPPING_CITY should not be null", Constants.SHIPPING_CITY);
        
        assertEquals("BILLING_STREET should be 'Billing Street'", "Billing Street", Constants.BILLING_STREET);
        assertEquals("SHIPPING_CITY should be 'Shipping City'", "Shipping City", Constants.SHIPPING_CITY);
    }

    /**
     * Test validation error message constants.
     */
    @Test
    public void testValidationErrorMessages_NotNullOrEmpty() {
        // Assert
        assertNotNull("PRIMARY_KEY_IS_EMPTY should not be null", Constants.PRIMARY_KEY_IS_EMPTY);
        assertNotNull("ENTITY_INPUT_IS_NULL_OR_EMPTY should not be null", Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY);
        assertNotNull("ID_IS_NULL_OR_EMPTY should not be null", Constants.ID_IS_NULL_OR_EMPTY);
        assertNotNull("CONDITIONS_NOT_FOUND should not be null", Constants.CONDITIONS_NOT_FOUND);
        
        assertTrue("PRIMARY_KEY_IS_EMPTY should mention primary key", 
                Constants.PRIMARY_KEY_IS_EMPTY.toLowerCase().contains("primary key"));
        assertTrue("ENTITY_INPUT_IS_NULL_OR_EMPTY should mention entity", 
                Constants.ENTITY_INPUT_IS_NULL_OR_EMPTY.toLowerCase().contains("entity"));
    }

    /**
     * Test namespace constants.
     */
    @Test
    public void testNamespaceConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("ENTERPRISE_SOAP_SFORCE_COM should not be null", Constants.ENTERPRISE_SOAP_SFORCE_COM);
        assertNotNull("COM_2005_09_OUTBOUND should not be null", Constants.COM_2005_09_OUTBOUND);
        
        assertTrue("ENTERPRISE_SOAP_SFORCE_COM should contain urn", 
                Constants.ENTERPRISE_SOAP_SFORCE_COM.contains("urn"));
        assertTrue("COM_2005_09_OUTBOUND should contain http", 
                Constants.COM_2005_09_OUTBOUND.contains("http"));
    }

    /**
     * Test miscellaneous constants.
     */
    @Test
    public void testMiscellaneousConstants_NotNullOrEmpty() {
        // Assert
        assertNotNull("DESC should not be null", Constants.DESC);
        assertNotNull("SPACE should not be null", Constants.SPACE);
        assertNotNull("SPACE_AND should not be null", Constants.SPACE_AND);
        assertNotNull("CREATED should not be null", Constants.CREATED);
        assertNotNull("INVALID should not be null", Constants.INVALID);
        
        assertEquals("DESC should be 'DESC'", "DESC", Constants.DESC);
        assertEquals("SPACE should be ' '", " ", Constants.SPACE);
        assertEquals("CREATED should be 'created'", "created", Constants.CREATED);
    }
}

