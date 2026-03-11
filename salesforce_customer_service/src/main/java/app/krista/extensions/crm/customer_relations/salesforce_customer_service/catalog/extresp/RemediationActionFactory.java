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

import app.krista.extension.executor.RemediationAction;
import app.krista.extension.executor.impl.AskAPersonAction;
import app.krista.extension.executor.impl.InformAPersonAction;
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;

import java.util.List;

/**
 * Factory class for creating RemediationAction objects.
 * <p>
 * This class provides utility methods to create various types of remediation actions
 * used in the Salesforce Customer Service extension. Remediation actions define
 * the next steps that should be taken when handling different scenarios, such as
 * asking for user input or informing users about issues.
 * <p>
 * The factory encapsulates the complexity of creating properly configured RemediationAction
 * objects and ensures consistency across the application.
 */
public class RemediationActionFactory {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private RemediationActionFactory(){}

    /**
     * Creates an AskAPersonAction with the specified message and fields directed to the active user.
     *
     * @param message The message to display to the user
     * @param fields The list of fields to request from the user
     * @return An AskAPersonAction configured with the specified parameters
     */
    public static RemediationAction createAskAction(String message, List<NamedField> fields) {
        return AskAPersonAction.create(message, RemediationAction.RecipientType.ACTIVE_USER, fields);
    }

    /**
     * Creates an InformAPersonAction with the specified message and fields directed to the active user.
     *
     * @param message The informational message to display to the user
     * @param fields The list of fields with values to display to the user
     * @return An InformAPersonAction configured with the specified parameters
     */
    public static RemediationAction createInformAction(String message,
                                                       List<NamedValuedField> fields) {
        return InformAPersonAction.create(message, RemediationAction.RecipientType.ACTIVE_USER, fields);
    }

    /**
     * Creates an InformAPersonAction with the specified message and fields directed to all participants.
     *
     * @param message The informational message to display to all participants
     * @param fields The list of fields with values to display to all participants
     * @return An InformAPersonAction configured with the specified parameters and directed to all participants
     */
    public static RemediationAction createInformActionALLParticipants(String message, List<NamedValuedField> fields) {
        return InformAPersonAction.create(message, RemediationAction.RecipientType.ALL_PARTICIPANTS, fields);
    }

}
