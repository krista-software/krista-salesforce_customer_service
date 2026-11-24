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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CustomerDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Account;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.AccountDao;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.CaseDao;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Contact;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.ContactDao;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.IndividualDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test class for {@link ObjectIdValidator}.
 * Tests validation logic for Salesforce object IDs across the supported object types
 * (Account, Case, Contact, Individual, Customer).
 *
 * Test Coverage Areas:
 * - Valid object ID for each supported object type (DAO returns a non-null object)
 * - Invalid object ID (DAO returns null)
 * - Null / blank object ID and null object type in context
 * - DAO throwing exceptions (validation returns false)
 * - Unknown object type (throws IllegalArgumentException)
 * - Field metadata accessors (name, type, messages)
 */
public class ObjectIdValidatorTest {

    private static final String OBJECT_ID = "001xx000000ABCD";

    @Mock
    private AccountDao mockAccountDao;
    @Mock
    private CaseDao mockCaseDao;
    @Mock
    private ContactDao mockContactDao;
    @Mock
    private IndividualDao mockIndividualDao;
    @Mock
    private CustomerDaoImpl mockCustomerDao;

    @Mock
    private Account mockAccount;
    @Mock
    private Case mockCase;
    @Mock
    private Contact mockContact;
    @Mock
    private Individual mockIndividual;
    @Mock
    private Customer mockCustomer;

    private ObjectIdValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new ObjectIdValidator(
                mockAccountDao, mockCaseDao, mockContactDao, mockIndividualDao, mockCustomerDao);
        context = new HashMap<>();
    }

    // ========== Valid Input Tests (DAO returns non-null) ==========

    @Test
    public void testValidate_WithValidAccountId_ReturnsTrue() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Account");
        when(mockAccountDao.getAccountById(OBJECT_ID)).thenReturn(mockAccount);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertTrue("Should return true when Account exists", result);
        verify(mockAccountDao, times(1)).getAccountById(OBJECT_ID);
    }

    @Test
    public void testValidate_WithValidCaseId_ReturnsTrue() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Case");
        when(mockCaseDao.getCaseById(OBJECT_ID)).thenReturn(mockCase);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertTrue("Should return true when Case exists", result);
        verify(mockCaseDao, times(1)).getCaseById(OBJECT_ID);
    }

    @Test
    public void testValidate_WithValidContactId_ReturnsTrue() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Contact");
        when(mockContactDao.getContactById(OBJECT_ID)).thenReturn(mockContact);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertTrue("Should return true when Contact exists", result);
        verify(mockContactDao, times(1)).getContactById(OBJECT_ID);
    }

    @Test
    public void testValidate_WithValidIndividualId_ReturnsTrue() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Individual");
        when(mockIndividualDao.getIndividualById(OBJECT_ID)).thenReturn(mockIndividual);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertTrue("Should return true when Individual exists", result);
        verify(mockIndividualDao, times(1)).getIndividualById(OBJECT_ID);
    }

    @Test
    public void testValidate_WithValidCustomerId_ReturnsTrue() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Customer");
        when(mockCustomerDao.getCustomerById(OBJECT_ID)).thenReturn(mockCustomer);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertTrue("Should return true when Customer exists", result);
        verify(mockCustomerDao, times(1)).getCustomerById(OBJECT_ID);
    }

    // ========== Invalid Input Tests (DAO returns null) ==========

    @Test
    public void testValidate_WithAccountIdNotFound_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Account");
        when(mockAccountDao.getAccountById(OBJECT_ID)).thenReturn(null);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Account does not exist", result);
    }

    @Test
    public void testValidate_WithCaseIdNotFound_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Case");
        when(mockCaseDao.getCaseById(OBJECT_ID)).thenReturn(null);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Case does not exist", result);
    }

    @Test
    public void testValidate_WithContactIdNotFound_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Contact");
        when(mockContactDao.getContactById(OBJECT_ID)).thenReturn(null);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Contact does not exist", result);
    }

    @Test
    public void testValidate_WithIndividualIdNotFound_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Individual");
        when(mockIndividualDao.getIndividualById(OBJECT_ID)).thenReturn(null);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Individual does not exist", result);
    }

    @Test
    public void testValidate_WithCustomerIdNotFound_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Customer");
        when(mockCustomerDao.getCustomerById(OBJECT_ID)).thenReturn(null);

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Customer does not exist", result);
    }

    // ========== Null / Blank Guard Tests ==========

    @Test
    public void testValidate_WithNullObjectId_ReturnsFalse() {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Account");

        Boolean result = validator.validate(null, context);

        assertFalse("Should return false for null object ID", result);
        verifyNoInteractions(mockAccountDao);
    }

    @Test
    public void testValidate_WithBlankObjectId_ReturnsFalse() {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Account");

        Boolean result = validator.validate("   ", context);

        assertFalse("Should return false for blank object ID", result);
        verifyNoInteractions(mockAccountDao);
    }

    @Test
    public void testValidate_WithNullObjectType_ReturnsFalse() {
        // OBJECT_TYPE intentionally not put into context
        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when object type is missing from context", result);
        verifyNoInteractions(mockAccountDao, mockCaseDao, mockContactDao, mockIndividualDao, mockCustomerDao);
    }

    // ========== DAO Exception Tests ==========

    @Test
    public void testValidate_WhenAccountDaoThrows_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Account");
        when(mockAccountDao.getAccountById(OBJECT_ID))
                .thenThrow(new RuntimeException("Salesforce error"));

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Account DAO throws", result);
    }

    @Test
    public void testValidate_WhenCaseDaoThrows_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Case");
        when(mockCaseDao.getCaseById(OBJECT_ID))
                .thenThrow(new RuntimeException("Salesforce error"));

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Case DAO throws", result);
    }

    @Test
    public void testValidate_WhenContactDaoThrows_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Contact");
        when(mockContactDao.getContactById(OBJECT_ID))
                .thenThrow(new RuntimeException("Salesforce error"));

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Contact DAO throws", result);
    }

    @Test
    public void testValidate_WhenIndividualDaoThrows_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Individual");
        when(mockIndividualDao.getIndividualById(OBJECT_ID))
                .thenThrow(new RuntimeException("Salesforce error"));

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Individual DAO throws", result);
    }

    @Test
    public void testValidate_WhenCustomerDaoThrows_ReturnsFalse() throws Exception {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Customer");
        when(mockCustomerDao.getCustomerById(OBJECT_ID))
                .thenThrow(new RuntimeException("Salesforce error"));

        Boolean result = validator.validate(OBJECT_ID, context);

        assertFalse("Should return false when Customer DAO throws", result);
    }

    // ========== Unknown Object Type Test ==========

    @Test(expected = IllegalArgumentException.class)
    public void testValidate_WithUnknownObjectType_ThrowsIllegalArgumentException() {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Widget");

        validator.validate(OBJECT_ID, context);
    }

    @Test
    public void testValidate_WithUnknownObjectType_ThrowsWithDescriptiveMessage() {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Widget");

        try {
            validator.validate(OBJECT_ID, context);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention the unknown object type",
                    e.getMessage().contains("Widget"));
        }
    }

    // ========== Field Metadata Tests ==========

    @Test
    public void testGetFetchFieldName_ReturnsObjectId() {
        assertEquals(SalesforceErrorHandlingResource.OBJECT_ID, validator.getFetchFieldName());
    }

    @Test
    public void testGetFieldType_ReturnsTextFieldType() {
        assertEquals(FieldTypes.TEXT_FIELD, validator.getFieldType());
    }

    @Test
    public void testGetFetchStepMessage_ReturnsNonEmptyMessage() {
        String message = validator.getFetchStepMessage();
        assertNotNull("Fetch step message should not be null", message);
        assertFalse("Fetch step message should not be empty", message.trim().isEmpty());
        assertTrue("Message should mention object ID", message.toLowerCase().contains("object id"));
    }

    @Test
    public void testGetConfirmationStepMessage_ContainsObjectTypeAndId() {
        context.put(Validator.ValidationResource.OBJECT_TYPE, "Account");

        String message = validator.getConfirmationStepMessage(OBJECT_ID, context);

        assertNotNull("Confirmation message should not be null", message);
        assertTrue("Message should contain the object ID", message.contains(OBJECT_ID));
        assertTrue("Message should contain the object type", message.contains("Account"));
    }

    @Test
    public void testGetErrMessage_ContainsObjectId() {
        String message = validator.getErrMessage(OBJECT_ID);

        assertNotNull("Error message should not be null", message);
        assertTrue("Message should contain the object ID", message.contains(OBJECT_ID));
    }
}
