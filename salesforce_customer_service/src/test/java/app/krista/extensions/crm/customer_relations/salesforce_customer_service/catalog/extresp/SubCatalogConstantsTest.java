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

