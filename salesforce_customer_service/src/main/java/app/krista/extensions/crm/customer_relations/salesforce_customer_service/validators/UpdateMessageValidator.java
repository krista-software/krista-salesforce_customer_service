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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UpdateMessageValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateMessageValidator.class);

    @Override
    public Boolean validate(String messageInput, Map<ValidationResource, String> messageContext) {
        String updateMessage = messageContext.get(ValidationResource.UPDATE_MESSAGE);
        if (updateMessage == null || updateMessage.isBlank()) {
            LOGGER.error("Update message is null or empty");
            return false;
        }
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return SalesforceErrorHandlingResource.UPDATE_MESSAGE;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please provide a valid update message to share with the case.";
    }

    @Override
    public String getConfirmationStepMessage(String messageInput, Map<ValidationResource, String> messageContext) {
        return String.format("Message validation failed because the update message is empty or invalid. " +
                "Please provide a meaningful message to share with the case.");
    }

    @Override
    public String getErrMessage(String messageInput) {
        return String.format("Case update failed because the message '%s' is invalid or empty. " +
                "A valid update message is required to share information with the case. " +
                "Please provide a meaningful message and try again.", messageInput);
    }
}
