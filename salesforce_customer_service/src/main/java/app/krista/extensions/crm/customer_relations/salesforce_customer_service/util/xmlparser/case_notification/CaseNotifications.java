package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.CaseNotification;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;




@XmlRootElement(name = "Case Notifications", namespace = "http://soap.sforce.com/2005/09/outbound")
@XmlAccessorType(XmlAccessType.FIELD)
public final class CaseNotifications {
    @XmlElement(name = "ActionId", namespace = "http://soap.sforce.com/2005/09/outbound")
    public String ActionId;
    public String PartnerUrl;
    public String EnterpriseUrl;
    public String OrganizationId;
    public String SessionId;

    @XmlElement(name = "Notification", namespace = "http://soap.sforce.com/2005/09/outbound")
    public CaseNotification caseNotification;
}