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