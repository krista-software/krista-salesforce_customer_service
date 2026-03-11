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


import app.krista.model.base.FreeForm;

import javax.xml.bind.annotation.XmlElement;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.*;

public final class SalesforceXMLCase {
    @XmlElement(name = "Id", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String Id;
    @XmlElement(name = "ContactId", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String ContactId;
    @XmlElement(name = "AccountId", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String AccountId;
    @XmlElement(name = "Subject", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String Subject;
    @XmlElement(name = "IsClosed", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public Boolean IsClosed;
    @XmlElement(name = "OwnerId", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String OwnerId;
    @XmlElement(name = "CreatedDate", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public Long CreatedDate;
    @XmlElement(name = "LastModifiedDate", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public Long LastModifiedDate;
    @XmlElement(name = "CaseNumber", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public Double CaseNumber;
    @XmlElement(name = "Status", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String Status;
    @XmlElement(name = "Reason", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String Reason;
    @XmlElement(name = "Origin", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String Origin;
    @XmlElement(name = "Description", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String Description;
    @XmlElement(name = "Priority", namespace = ENTERPRISE_SOAP_SFORCE_COM)
    public String Priority;

    public FreeForm toCaseFreeForm() {
        FreeForm freeForm = new FreeForm();
        freeForm.put("Id", TEXT, Id);
        freeForm.put("ContactId", TEXT, ContactId);
        freeForm.put("AccountId", TEXT, AccountId);
        freeForm.put("Subject", TEXT, Subject);
        freeForm.put("IsClosed", BOOLEAN, IsClosed);
        freeForm.put("OwnerId", TEXT, OwnerId);
        freeForm.put("CreatedDate", LONG, CreatedDate);
        freeForm.put("LastModifiedDate", LONG, LastModifiedDate);
        freeForm.put("CaseNumber", DOUBLE, CaseNumber);
        freeForm.put("Status", TEXT, Status);
        freeForm.put("Reason", TEXT, Reason);
        freeForm.put("Origin", TEXT, Origin);
        freeForm.put("Description", TEXT, Description);
        freeForm.put("Priority", TEXT, Priority);
        return freeForm;
    }
}