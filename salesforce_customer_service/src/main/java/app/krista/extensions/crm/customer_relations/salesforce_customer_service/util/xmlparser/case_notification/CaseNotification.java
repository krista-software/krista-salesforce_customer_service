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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification;

import javax.xml.bind.annotation.XmlElement;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.COM_2005_09_OUTBOUND;

public final class CaseNotification {
    @XmlElement(name = "sObject", namespace = COM_2005_09_OUTBOUND)
    private SalesforceXMLCase SalesforceXMLCase;

    @XmlElement(name = "Id", namespace = COM_2005_09_OUTBOUND)
    private String Id;

    public app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser
            .case_notification.SalesforceXMLCase getSalesforceXMLCase() {
        return SalesforceXMLCase;
    }

}