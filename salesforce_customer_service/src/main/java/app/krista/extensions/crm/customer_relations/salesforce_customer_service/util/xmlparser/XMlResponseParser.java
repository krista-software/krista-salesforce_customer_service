package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.CaseNotifications;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

/**
 * Utility class for parsing XML responses into Java objects using JAXB.
 * <p>
 * Provides methods to securely parse Salesforce case notification XML responses
 * into {@link CaseNotifications} objects, disabling DTD and external entity support
 * for security. Not intended to be instantiated.
 */
public final class XMlResponseParser {

    private XMlResponseParser() {
    }

    // region Internal
    private static <T> JAXBElement<T> parseXmlResponse(String response, @SuppressWarnings("SameParameterValue") Class<T> notificationClass) {
        try (StringReader stringReader = new StringReader(response)) {
            XMLInputFactory xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false); // Disable DTD support
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false); // Disable external entities
            XMLStreamReader xsr = xif.createXMLStreamReader(stringReader);
            try {
                xsr.nextTag(); // Advance to Envelope tag
                xsr.nextTag(); // Advance to Body tag
                xsr.nextTag();

                JAXBContext jc = JAXBContext.newInstance(notificationClass);
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                return unmarshaller.unmarshal(xsr, notificationClass);
            } finally {
                xsr.close();
            }
        } catch (XMLStreamException | JAXBException cause) {
            System.err.println("XML Parsing Failed for " + notificationClass.getSimpleName() + " with error " + cause.getMessage());
        }
        return null;
    }
    // endregion

    /**
     * Provides utility methods for securely parsing Salesforce case notification XML responses
     * into {@link CaseNotifications} objects using JAXB. Disables DTD and external entity support
     * for security. Not intended to be instantiated.
     */
    public static JAXBElement<CaseNotifications> parseCaseXmlResponse(String response) {
        return parseXmlResponse(response, CaseNotifications.class);
    }
}
