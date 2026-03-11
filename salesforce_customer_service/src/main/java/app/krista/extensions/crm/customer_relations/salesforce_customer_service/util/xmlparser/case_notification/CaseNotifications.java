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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.COM_2005_09_OUTBOUND;


@XmlRootElement(name = "Case Notifications", namespace = COM_2005_09_OUTBOUND)
@XmlAccessorType(XmlAccessType.FIELD)
public final class CaseNotifications {
    @XmlElement(name = "ActionId", namespace = COM_2005_09_OUTBOUND)
    private String ActionId;
    private String PartnerUrl;
    private String EnterpriseUrl;
    private String OrganizationId;
    private String SessionId;

    @XmlElement(name = "Notification", namespace = COM_2005_09_OUTBOUND)
    private CaseNotification caseNotification;

    public CaseNotification getCaseNotification() {
        return caseNotification;
    }
}