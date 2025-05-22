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