package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.CaseNotification;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.CaseNotifications;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;



public final class XMlResponseParser {

    // region Internal
    private static <T> JAXBElement<T> parseXmlResponse(String response, Class<T> notificationClass) {
        try {
            XMLInputFactory xif = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(response));
            xsr.nextTag(); // Advance to Envelope tag
            xsr.nextTag(); // Advance to Body tag
            xsr.nextTag();

            JAXBContext jc = JAXBContext.newInstance(notificationClass);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            return unmarshaller.unmarshal(xsr, notificationClass);

        } catch (XMLStreamException | JAXBException cause) {
            cause.printStackTrace();
        }
        return null;
    }

    // endregion

    public static JAXBElement<CaseNotifications> parseCaseXmlResponse(String response) throws Exception {
        return parseXmlResponse(response, CaseNotifications.class);
    }
}
