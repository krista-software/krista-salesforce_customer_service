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

import app.krista.model.base.File;

import javax.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StateMapperUtil {

    private StateMapperUtil() {
    }

    /**
     * Builds a metadata map for the "Get Case Feed Items By Case Id" catalog request.
     *
     * @param caseId        The case identifier (required).
     * @param numberOfPosts The number of posts to fetch (optional, may be null).
     * @param stateId       The state identifier for tracking (optional, may be null).
     * @return A LinkedHashMap containing the metadata for the request.
     */
    @NotNull
    public static Map<String, Object> addGetCaseFeedItemsByCaseIdMetaToMap(String caseId, Double numberOfPosts, String stateId) {
        Map<String, Object> metaData = new LinkedHashMap<>();
        metaData.put(SalesforceErrorHandlingResource.STATE_ID, stateId);
        metaData.put(SalesforceErrorHandlingResource.CASE_ID, caseId);
        metaData.put(SalesforceErrorHandlingResource.NUMBER_OF_POSTS, numberOfPosts);
        return metaData;
    }

    /**
     * Builds a metadata map for the "Share Update to Case" catalog request.
     *
     * @param caseId        The case identifier (required).
     * @param updateMessage The update message to be shared (required).
     * @param file          The file to attach to the update (optional, may be null).
     * @param stateId       The state identifier for tracking (optional, may be null).
     * @return A LinkedHashMap containing the metadata for the request.
     */
    @NotNull
    public static Map<String, Object> addShareUpdateToCaseMetaToMap(String caseId, String updateMessage, File file, String stateId) {
        Map<String, Object> metaData = new LinkedHashMap<>();
        metaData.put(SalesforceErrorHandlingResource.STATE_ID, stateId);
        metaData.put(SalesforceErrorHandlingResource.CASE_ID, caseId);
        metaData.put(SalesforceErrorHandlingResource.UPDATE_MESSAGE, updateMessage);
        if (file != null) {
            metaData.put(SalesforceErrorHandlingResource.FILE, List.of(file));
        }
        return metaData;
    }

    /**
     * Creates a metadata map with object and custom field information.
     *
     * @param objectId     The ID of the Salesforce object.
     * @param objectType   The type of the Salesforce object (e.g., "Case", "Account").
     * @param customFields The list of custom field names.
     * @param stateId      The state ID for tracking (can be null).
     * @return A map containing the state ID, object ID, object type, and custom fields.
     */
    @NotNull
    public static Map<String, Object> addCustomFieldsMetaToMap(String objectId, String objectType, List<String> customFields, String stateId) {
        Map<String, Object> metaData = new LinkedHashMap<>();
        metaData.put(SalesforceErrorHandlingResource.STATE_ID, stateId);
        metaData.put(SalesforceErrorHandlingResource.OBJECT_ID, objectId);
        metaData.put(SalesforceErrorHandlingResource.OBJECT_TYPE, objectType);
        metaData.put(SalesforceErrorHandlingResource.CUSTOM_FIELDS, customFields);
        return metaData;
    }




}
