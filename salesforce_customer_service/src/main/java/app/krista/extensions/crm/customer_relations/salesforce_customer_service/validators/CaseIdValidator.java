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

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.FieldTypes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.extresp.SalesforceErrorHandlingResource;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CaseIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseIdValidator.class);
    private final CaseDaoImpl caseDao;

    public CaseIdValidator(CaseDaoImpl caseDao) {
        this.caseDao = caseDao;
    }

    @Override
    public Boolean validate(String caseId, Map<ValidationResource, String> caseContext) {
        try {
            caseDao.getCaseById(caseId);
            return true;
        } catch (MustAuthorizeException cause) {
            LOGGER.error("Exception thrown while authorizing user : {} {}", cause.getMessage(), cause);

            throw cause;
        } catch (Exception cause) {
            LOGGER.error("Exception thrown while validating message ID: {}", cause.getMessage(), cause);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return SalesforceErrorHandlingResource.CASE_ID;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Salesforce Case ID to retrieve case feed items.";
    }

    @Override
    public String getConfirmationStepMessage(String caseId, Map<ValidationResource, String> caseContext) {
        return String.format("Case validation failed because the provided Case ID '%s' does not exist in Salesforce. " +
                "Please verify the Case ID is correct and accessible in your Salesforce system.", caseId);
    }

    @Override
    public String getErrMessage(String caseId) {
        return String.format("Case operation failed because the Case ID '%s' is invalid or does not exist. " +
                "A valid Salesforce Case ID is required to perform this operation. " +
                "Please verify the Case ID exists in your Salesforce system and you have access to it.", caseId);
    }

}
