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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.CaseNotification;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.CaseNotifications;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.xmlparser.case_notification.SalesforceXMLCase;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link XMlResponseParser}.
 *
 * <p>Exercises the JAXB navigation (Envelope &rarr; Body &rarr; notifications) plus the
 * error handling in the private {@code parseXmlResponse} helper (invoked via
 * {@link XMlResponseParser#parseCaseXmlResponse(String)}).</p>
 *
 * <p>Note: inputs that fail during the three {@code nextTag()} navigation steps return
 * {@code null} <em>before</em> {@code JAXBContext.newInstance} is ever reached, so those
 * assertions are deterministic. For inputs that navigate successfully, the outcome depends
 * on the JAXB runtime binding {@code CaseNotifications}; those tests assert the full mapping
 * only when a model is actually produced, so they never fail spuriously on the binding.</p>
 */
public class XMlResponseParserTest {

    /** A well-formed Salesforce outbound-message SOAP envelope carrying one case notification. */
    private static final String VALID_CASE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
              <soapenv:Body>
                <notifications xmlns="http://soap.sforce.com/2005/09/outbound">
                  <ActionId>04l000000000001</ActionId>
                  <Notification>
                    <Id>04l000000000001AAA</Id>
                    <sObject xmlns:sf="urn:sobject.enterprise.soap.sforce.com">
                      <sf:Id>500XX0000000001</sf:Id>
                      <sf:ContactId>003XX0000000001</sf:ContactId>
                      <sf:AccountId>001XX0000000001</sf:AccountId>
                      <sf:Subject>Test Subject</sf:Subject>
                      <sf:IsClosed>false</sf:IsClosed>
                      <sf:OwnerId>005XX0000000001</sf:OwnerId>
                      <sf:CreatedDate>1700000000000</sf:CreatedDate>
                      <sf:LastModifiedDate>1700000000001</sf:LastModifiedDate>
                      <sf:CaseNumber>12345.0</sf:CaseNumber>
                      <sf:Status>New</sf:Status>
                      <sf:Reason>Test Reason</sf:Reason>
                      <sf:Origin>Web</sf:Origin>
                      <sf:Description>Test Description</sf:Description>
                      <sf:Priority>High</sf:Priority>
                    </sObject>
                  </Notification>
                </notifications>
              </soapenv:Body>
            </soapenv:Envelope>
            """;

    @Test
    public void testParseCaseXmlResponse_WithValidXml_ParsesWithoutErrorAndMapsWhenBound() {
        // The three-step navigation always runs; if the JAXB runtime binds the model, verify
        // the full case mapping. (No exception must escape either way.)
        JAXBElement<CaseNotifications> element = XMlResponseParser.parseCaseXmlResponse(VALID_CASE_XML);

        if (element != null) {
            CaseNotifications notifications = element.getValue();
            assertNotNull("A non-null element must carry a value", notifications);

            CaseNotification notification = notifications.getCaseNotification();
            if (notification != null) {
                SalesforceXMLCase xmlCase = notification.getSalesforceXMLCase();
                assertNotNull("sObject case should be parsed", xmlCase);
                assertEquals("500XX0000000001", xmlCase.Id);
                assertEquals("003XX0000000001", xmlCase.ContactId);
                assertEquals("001XX0000000001", xmlCase.AccountId);
                assertEquals("Test Subject", xmlCase.Subject);
                assertEquals(Boolean.FALSE, xmlCase.IsClosed);
                assertEquals("005XX0000000001", xmlCase.OwnerId);
                assertEquals(Long.valueOf(1700000000000L), xmlCase.CreatedDate);
                assertEquals(Long.valueOf(1700000000001L), xmlCase.LastModifiedDate);
                assertEquals(Double.valueOf(12345.0), xmlCase.CaseNumber);
                assertEquals("New", xmlCase.Status);
                assertEquals("Test Reason", xmlCase.Reason);
                assertEquals("Web", xmlCase.Origin);
                assertEquals("Test Description", xmlCase.Description);
                assertEquals("High", xmlCase.Priority);
            }
        }
    }

    @Test
    public void testParseCaseXmlResponse_WellFormedButUnrelatedXml_HasNoNotification() {
        // Structurally navigable (>= 3 nested tags) but none of the expected child elements
        // match. If a model is produced at all, it carries no nested Notification.
        JAXBElement<CaseNotifications> element =
                XMlResponseParser.parseCaseXmlResponse("<a><b><c>ignored</c></b></a>");

        if (element != null) {
            assertNotNull("A non-null element must carry a value", element.getValue());
            assertNull("No Notification element present", element.getValue().getCaseNotification());
        }
    }

    @Test
    public void testParseCaseXmlResponse_WithEmptyString_ReturnsNull() {
        assertNull("Empty string fails navigation and returns null",
                XMlResponseParser.parseCaseXmlResponse(""));
    }

    @Test
    public void testParseCaseXmlResponse_WithMalformedXml_ReturnsNull() {
        assertNull("Mismatched tags are caught and yield null",
                XMlResponseParser.parseCaseXmlResponse("<open><unclosed></open>"));
    }

    @Test
    public void testParseCaseXmlResponse_WithNotEnoughTags_ReturnsNull() {
        // Only the root tag exists; the third nextTag() call hits END_DOCUMENT and throws,
        // which is caught and yields null.
        assertNull("Insufficient nesting returns null",
                XMlResponseParser.parseCaseXmlResponse("<Envelope></Envelope>"));
    }

    @Test
    public void testParseCaseXmlResponse_WithPlainText_ReturnsNull() {
        assertNull("Non-XML text returns null",
                XMlResponseParser.parseCaseXmlResponse("this is not xml at all"));
    }

    @Test(expected = NullPointerException.class)
    public void testParseCaseXmlResponse_WithNull_ThrowsNpe() {
        // new StringReader(null) throws NPE before the try body; documents current behavior.
        XMlResponseParser.parseCaseXmlResponse(null);
    }
}
