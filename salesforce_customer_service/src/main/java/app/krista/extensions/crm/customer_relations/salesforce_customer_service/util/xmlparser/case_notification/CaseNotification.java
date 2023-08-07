package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification;

import javax.xml.bind.annotation.XmlElement;

public final class CaseNotification {
    @XmlElement(name = "sObject", namespace = "http://soap.sforce.com/2005/09/outbound")
    public SalesforceXMLCase salesforceXMLCase;

    @XmlElement(name = "Id", namespace = "http://soap.sforce.com/2005/09/outbound")
    public String Id;

}