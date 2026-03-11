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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.validators.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResourceUtil {

    private ValidationResourceUtil() {
    }

    /**
     * Generates a validation map for a case using the provided case ID and number of posts.
     * Adds entries to the map only if the corresponding values are not null:
     *
     * @param caseId        the ID of the case to validate
     * @param numberOfPosts the number of posts associated with the case
     * @return a map containing the non-null validation resources and their string values
     */
    public static Map<Validator.ValidationResource, String> generateCaseValidationMap(String caseId, Double numberOfPosts) {
        Map<Validator.ValidationResource, String> map = new HashMap<>();
        if (isNotNull(caseId)) {
            map.put(Validator.ValidationResource.CASE_ID, caseId);
        }
        if (isNotNull(String.valueOf(numberOfPosts))) {
            map.put(Validator.ValidationResource.NUMBER_OF_POSTS, String.valueOf(numberOfPosts));
        }
        return map;
    }

    private static boolean isNotNull(String input) {
        return input != null;
    }

    /**
     * Generates a validation map for content fields using the provided object ID, object type, and custom fields.
     * Adds entries to the map only if the corresponding values are not null or empty:
     *
     * @param objectId     the ID of the object to validate
     * @param objectType   the type of the object to validate
     * @param customFields a list of custom field names to validate
     * @return a map containing the non-null validation resources and their string values
     */
    public static Map<Validator.ValidationResource, String> generateContentFieldValidationMap(String objectId, String objectType, List<String> customFields) {
        Map<Validator.ValidationResource, String> map = new HashMap<>();
        if (isNotNull(objectId)) {
            map.put(Validator.ValidationResource.OBJECT_ID, objectId);
        }
        if (isNotNull(objectType)) {
            map.put(Validator.ValidationResource.OBJECT_TYPE, objectType);
        }
        if (customFields != null && !customFields.isEmpty()) {
            map.put(Validator.ValidationResource.CUSTOM_FIELDS, String.join(", ", customFields));
        }
        return map;
    }
}
