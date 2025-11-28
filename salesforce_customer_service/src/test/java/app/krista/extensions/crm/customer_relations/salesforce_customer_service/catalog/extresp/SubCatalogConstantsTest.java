package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SubCatalogConstants} constants class.
 */
public class SubCatalogConstantsTest {

    @Test
    public void testConfirmReenterGetCaseFeedItemsByCaseIdConstant() {
        assertEquals("confirmGetCaseFeedItemsByCaseId", 
            SubCatalogConstants.CONFIRM_REENTER_GET_CASE_FEED_ITEMS_BY_CASE_ID);
    }

    @Test
    public void testConfirmReenterShareUpdateToCaseConstant() {
        assertEquals("confirmShareUpdateToCase", 
            SubCatalogConstants.CONFIRM_REENTER_SHARE_UPDATE_TO_CASE);
    }

    @Test
    public void testConfirmRetrieveCustomFieldsForGivenObjectConstant() {
        assertEquals("confirmRetrieveCustomFieldsForGivenObject", 
            SubCatalogConstants.CONFIRM_RETRIEVE_CUSTOM_FIELDS_FOR_GIVEN_OBJECT);
    }

    @Test
    public void testValidationResultsConstant() {
        assertEquals("validationResult", SubCatalogConstants.VALIDATION_RESULTS);
    }
}

