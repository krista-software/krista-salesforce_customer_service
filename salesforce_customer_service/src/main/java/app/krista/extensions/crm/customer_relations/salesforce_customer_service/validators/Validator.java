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

import java.util.Map;

public interface Validator {

    enum ValidationResource {
        CASE_ID,
        NUMBER_OF_POSTS,
        UPDATE_MESSAGE,
        FILE,
        OBJECT_ID,
        OBJECT_TYPE,
        CUSTOM_FIELDS
    }

    Boolean validate(String resourceId, Map<ValidationResource, String> context);

    String getFetchFieldName();

    String getFieldType();

    String getFetchStepMessage();

    String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context);

    String getErrMessage(String resourceId);
}
