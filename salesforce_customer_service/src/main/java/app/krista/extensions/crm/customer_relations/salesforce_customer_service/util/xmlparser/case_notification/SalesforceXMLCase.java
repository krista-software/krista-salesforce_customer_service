package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification;


import app.krista.model.base.FreeForm;

import javax.xml.bind.annotation.XmlElement;

import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.*;
import static app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants.LONG;

public final class SalesforceXMLCase {

    @XmlElement(name = "AccountId", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String AccountId;
    @XmlElement(name = "CaseNumber", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public Double CaseNumber;
    @XmlElement(name = "ContactEmail", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String ContactEmail;
    @XmlElement(name = "Id", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String Id;
    @XmlElement(name = "ContactId", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String ContactId;
    @XmlElement(name = "ContactPhone", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String ContactPhone;
    @XmlElement(name = "Description", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String Description;
    @XmlElement(name = "IsClosed", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String IsClosed;
    @XmlElement(name = "Origin", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String Origin;
    @XmlElement(name = "OwnerId", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String OwnerId;
    @XmlElement(name = "Priority", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String Priority;
    @XmlElement(name = "Reason", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String Reason;
    @XmlElement(name = "Status", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String Status;
    @XmlElement(name = "Subject", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String Subject;
    @XmlElement(name = "MobilePhone", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String MobilePhone;
    @XmlElement(name = "Email", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public String Email;
    @XmlElement(name = "CreatedDate", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public Long CreatedDate;

    @XmlElement(name = "LastModifiedDate", namespace = "urn:sobject.enterprise.soap.sforce.com")
    public Long LastModifiedDate;

    public FreeForm toLeadFreeForm() {
        FreeForm freeForm = new FreeForm();
        freeForm.put("Id", TEXT, Id);
        freeForm.put("AccountId", TEXT, AccountId);
        freeForm.put("ContactEmail", TEXT, ContactEmail);
        freeForm.put("ContactId", TEXT, ContactId);
        freeForm.put("ContactPhone", TEXT, ContactPhone);
        freeForm.put("Description", TEXT, Description);
        freeForm.put("IsClosed", TEXT, IsClosed);
        freeForm.put("Origin", TEXT, Origin);
        freeForm.put("OwnerId", TEXT, OwnerId);//
        freeForm.put("Priority", TEXT, Priority);//
        freeForm.put("Reason", TEXT, Reason);//
        freeForm.put("Status", TEXT, Status);//
        freeForm.put("Subject", TEXT, Subject);//
        freeForm.put("MobilePhone", TEXT, MobilePhone);//
        freeForm.put("Email", TEXT, Email);//
        freeForm.put("CaseNumber", DOUBLE, CaseNumber);//
        freeForm.put("CreatedDate", LONG, CreatedDate);//
        freeForm.put("LastModifiedDate", LONG, LastModifiedDate);//
        return freeForm;
    }
}