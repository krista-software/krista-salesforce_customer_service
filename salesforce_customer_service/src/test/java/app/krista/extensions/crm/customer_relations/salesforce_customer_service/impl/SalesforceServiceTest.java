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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceCustomerServiceAttributes;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProvider;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.connectors.SalesforceTokenProviderFactory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.SalesforceCustomerServiceAttributesStore;
import com.github.scribejava.apis.salesforce.SalesforceToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalesforceService}.
 *
 * <p>The service resolves its HTTP path through a chain of collaborators that are all mocked
 * here: the {@link SalesforceTokenProviderFactory} yields a {@link SalesforceTokenProvider}
 * whose {@code getSalesforceTokenForUser()} returns a mocked {@link SalesforceToken}; the
 * effective attributes expose a mocked ScribeJava {@link OAuth20Service} whose
 * {@code execute(...)} returns a mocked {@link Response}. Using the package/public test-seam
 * constructor with {@code attributesStore = null} makes {@code effectiveAttributes()} return
 * the mocked invoker attributes (the KV path is skipped). Dedicated tests pass a mocked store
 * to exercise the KV branch.</p>
 */
public class SalesforceServiceTest {

    private static final String INVOKER_ID = "inv-1";
    private static final String INSTANCE_URL = "https://x.my.salesforce.com";
    private static final String OK_BODY = "{\"ok\":true}";
    private static final String STORAGE_LIMIT_BODY = "{\"errorCode\":\"STORAGE_LIMIT_EXCEEDED\"}";

    @Mock
    private SalesforceTokenProviderFactory providerFactory;
    @Mock
    private SalesforceCustomerServiceAttributes invokerAttributes;
    @Mock
    private SalesforceTokenProvider tokenProvider;
    @Mock
    private SalesforceToken token;
    @Mock
    private OAuth20Service oauth;
    @Mock
    private Response response;

    private SalesforceService service;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(providerFactory.create(any())).thenReturn(tokenProvider);
        when(tokenProvider.getSalesforceTokenForUser()).thenReturn(token);
        when(token.getInstanceUrl()).thenReturn(INSTANCE_URL);
        when(token.getAccessToken()).thenReturn("tok");
        when(invokerAttributes.getOauthService()).thenReturn(oauth);

        when(oauth.execute(any(OAuthRequest.class))).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.getBody()).thenReturn(OK_BODY);

        // attributesStore = null => effectiveAttributes() returns invokerAttributes (KV path skipped)
        service = new SalesforceService(providerFactory, invokerAttributes, null, INVOKER_ID);
    }

    // ==================== getRecordById ====================

    @Test
    public void testGetRecordById_Success() throws Exception {
        String result = service.getRecordById("500ID", "Case");
        assertEquals("Body should be returned verbatim", OK_BODY, result);
        verify(oauth).execute(any(OAuthRequest.class));
    }

    @Test
    public void testGetRecordById_UnsuccessfulResponse_StillReturnsBody() throws Exception {
        when(response.isSuccessful()).thenReturn(false);
        when(response.getBody()).thenReturn("{\"error\":\"bad\"}");
        String result = service.getRecordById("500ID", "Case");
        assertEquals("{\"error\":\"bad\"}", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRecordById_NullId_ThrowsIllegalArgument() throws Exception {
        service.getRecordById(null, "Case");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRecordById_BlankId_ThrowsIllegalArgument() throws Exception {
        service.getRecordById("   ", "Case");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRecordById_NullSubject_ThrowsIllegalArgument() throws Exception {
        service.getRecordById("500ID", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRecordById_EmptySubject_ThrowsIllegalArgument() throws Exception {
        service.getRecordById("500ID", "");
    }

    // ==================== getRecordByQuery ====================

    @Test
    public void testGetRecordByQuery_Success() throws Exception {
        String result = service.getRecordByQuery("SELECT Id FROM Account LIMIT 10");
        assertEquals(OK_BODY, result);
        verify(oauth).execute(any(OAuthRequest.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRecordByQuery_NullQuery_ThrowsIllegalArgument() throws Exception {
        service.getRecordByQuery(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRecordByQuery_BlankQuery_ThrowsIllegalArgument() throws Exception {
        service.getRecordByQuery("   ");
    }

    // ==================== createRecord ====================

    @Test
    public void testCreateRecord_Success() throws Exception {
        when(response.getBody()).thenReturn("{\"id\":\"500NEW\",\"success\":true}");
        String result = service.createRecord("{\"Subject\":\"x\"}", "Case");
        assertEquals("{\"id\":\"500NEW\",\"success\":true}", result);
        verify(oauth).execute(any(OAuthRequest.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRecord_NullBody_ThrowsIllegalArgument() throws Exception {
        service.createRecord(null, "Case");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRecord_BlankBody_ThrowsIllegalArgument() throws Exception {
        service.createRecord("  ", "Case");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRecord_EmptySObject_ThrowsIllegalArgument() throws Exception {
        service.createRecord("{\"Subject\":\"x\"}", "");
    }

    // ==================== updateRecord ====================

    @Test
    public void testUpdateRecord_Success() throws Exception {
        service.updateRecord("500ID", "{\"Status\":\"Closed\"}", "Case");
        verify(oauth).execute(any(OAuthRequest.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRecord_NullId_ThrowsIllegalArgument() throws Exception {
        service.updateRecord(null, "{\"Status\":\"Closed\"}", "Case");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRecord_BlankBody_ThrowsIllegalArgument() throws Exception {
        service.updateRecord("500ID", "   ", "Case");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRecord_EmptySObject_ThrowsIllegalArgument() throws Exception {
        service.updateRecord("500ID", "{\"Status\":\"Closed\"}", "");
    }

    // ==================== deleteRecordById ====================

    @Test
    public void testDeleteRecordById_Success() throws Exception {
        service.deleteRecordById("500ID", "Case");
        verify(oauth).execute(any(OAuthRequest.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteRecordById_NullId_ThrowsIllegalArgument() throws Exception {
        service.deleteRecordById(null, "Case");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteRecordById_BlankSObject_ThrowsIllegalArgument() throws Exception {
        service.deleteRecordById("500ID", "   ");
    }

    // ==================== getFeedsByCaseId ====================

    @Test
    public void testGetFeedsByCaseId_Success() throws Exception {
        when(response.getBody()).thenReturn("{\"elements\":[]}");
        String result = service.getFeedsByCaseId("500CASE", 10.0);
        assertEquals("{\"elements\":[]}", result);
        verify(oauth).execute(any(OAuthRequest.class));
    }

    @Test
    public void testGetFeedsByCaseId_IOException_ReturnsEmptyString() throws Exception {
        when(oauth.execute(any(OAuthRequest.class))).thenThrow(new IOException("boom"));
        String result = service.getFeedsByCaseId("500CASE", 5.0);
        assertEquals("IOException path returns empty string", "", result);
    }

    // ==================== createFeedPost* ====================

    @Test
    public void testCreateFeedPost_Success() throws Exception {
        when(response.getBody()).thenReturn("{\"id\":\"0D5FEED\"}");
        String result = service.createFeedPost("{\"body\":{}}");
        assertEquals("{\"id\":\"0D5FEED\"}", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFeedPost_IOException_ThrowsIllegalArgument() throws Exception {
        when(oauth.execute(any(OAuthRequest.class))).thenThrow(new IOException("boom"));
        service.createFeedPost("{\"body\":{}}");
    }

    @Test
    public void testCreateFeedPostForFile_Success() throws Exception {
        when(response.getBody()).thenReturn("{\"id\":\"068FILE\"}");
        String result = service.createFeedPostForFile("{\"VersionData\":\"...\"}");
        assertEquals("{\"id\":\"068FILE\"}", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFeedPostForFile_IOException_ThrowsIllegalArgument() throws Exception {
        when(oauth.execute(any(OAuthRequest.class))).thenThrow(new IOException("boom"));
        service.createFeedPostForFile("{\"VersionData\":\"...\"}");
    }

    @Test
    public void testCreateFeedPostForFeedItem_Success() throws Exception {
        when(response.getBody()).thenReturn("{\"id\":\"0D7ITEM\"}");
        String result = service.createFeedPostForFeedItem("{\"ParentId\":\"500\"}");
        assertEquals("{\"id\":\"0D7ITEM\"}", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFeedPostForFeedItem_IOException_ThrowsIllegalArgument() throws Exception {
        when(oauth.execute(any(OAuthRequest.class))).thenThrow(new IOException("boom"));
        service.createFeedPostForFeedItem("{\"ParentId\":\"500\"}");
    }

    @Test
    public void testCreateFeedPostForFeedAttachment_Success() throws Exception {
        when(response.getBody()).thenReturn("{\"id\":\"0ITATT\"}");
        String result = service.createFeedPostForFeedAttachment("{\"FeedEntityId\":\"0D7\"}");
        assertEquals("{\"id\":\"0ITATT\"}", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFeedPostForFeedAttachment_IOException_ThrowsIllegalArgument() throws Exception {
        when(oauth.execute(any(OAuthRequest.class))).thenThrow(new IOException("boom"));
        service.createFeedPostForFeedAttachment("{\"FeedEntityId\":\"0D7\"}");
    }

    // ==================== getFileResponse (getResponseForFile) ====================

    @Test
    public void testGetFileResponse_Success_ReturnsResponse() {
        when(response.getCode()).thenReturn(200);
        Response result = service.getFileResponse("/services/data/v58.0/sobjects/ContentVersion/068/VersionData");
        assertSame("Should return the underlying response", response, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFileResponse_ErrorCode_ThrowsIllegalArgument() throws Exception {
        when(response.getCode()).thenReturn(500);
        when(response.getBody()).thenReturn("{\"error\":\"server\"}");
        service.getFileResponse("/bad/url");
    }

    @Test
    public void testGetFileResponse_IOException_ReturnsNull() throws Exception {
        when(oauth.execute(any(OAuthRequest.class))).thenThrow(new IOException("boom"));
        Response result = service.getFileResponse("/some/url");
        assertNull("IOException path returns null", result);
    }

    // ==================== checkFieldExistsInSalesforce ====================

    @Test
    public void testCheckFieldExistsInSalesforce_FieldExists_ReturnsTrue() throws Exception {
        when(response.getBody()).thenReturn("{\"fields\":[{\"name\":\"Foo__c\"}]}");
        assertTrue("Existing field should return true",
                service.checkFieldExistsInSalesforce("Account", "Foo__c"));
    }

    @Test
    public void testCheckFieldExistsInSalesforce_FieldMissing_ReturnsFalse() throws Exception {
        when(response.getBody()).thenReturn("{\"fields\":[{\"name\":\"Foo__c\"}]}");
        assertFalse("Missing field should return false",
                service.checkFieldExistsInSalesforce("Account", "Missing__c"));
    }

    @Test
    public void testCheckFieldExistsInSalesforce_Exception_ReturnsFalse() throws Exception {
        when(oauth.execute(any(OAuthRequest.class))).thenThrow(new IOException("boom"));
        assertFalse("Exception path should return false",
                service.checkFieldExistsInSalesforce("Account", "Foo__c"));
    }

    // ==================== STORAGE_LIMIT_EXCEEDED branch (getResponse) ====================

    @Test(expected = IllegalArgumentException.class)
    public void testGetResponse_StorageLimitExceeded_ThrowsIllegalArgument() throws Exception {
        when(response.getBody()).thenReturn(STORAGE_LIMIT_BODY);
        service.getRecordById("500ID", "Case");
    }

    // ==================== KV credential branch (effectiveAttributes) ====================

    @Test
    public void testEffectiveAttributes_KvWithCredentials_UsesKv() throws Exception {
        SalesforceCustomerServiceAttributesStore store = mock(SalesforceCustomerServiceAttributesStore.class);
        SalesforceCustomerServiceAttributes kvAttrs = mock(SalesforceCustomerServiceAttributes.class);
        when(kvAttrs.getClientId()).thenReturn("cid");
        when(kvAttrs.getClientSecret()).thenReturn("secret");
        when(store.loadByInvokerId(INVOKER_ID)).thenReturn(kvAttrs);

        // Give the KV attrs a DISTINCT OAuth service returning a distinct body, so the returned
        // value alone proves the KV credentials (not the invoker fallback) were used.
        OAuth20Service kvOauth = mock(OAuth20Service.class);
        Response kvResponse = mock(Response.class);
        when(kvAttrs.getOauthService()).thenReturn(kvOauth);
        when(kvOauth.execute(any(OAuthRequest.class))).thenReturn(kvResponse);
        when(kvResponse.isSuccessful()).thenReturn(true);
        when(kvResponse.getBody()).thenReturn("{\"kv\":true}");

        SalesforceService kvService =
                new SalesforceService(providerFactory, invokerAttributes, store, INVOKER_ID);

        String result = kvService.getRecordById("500ID", "Case");

        assertEquals("KV credential path must be used", "{\"kv\":true}", result);
        verify(kvOauth).execute(any(OAuthRequest.class));
    }

    @Test
    public void testEffectiveAttributes_KvBlankCredentials_FallsBackToInvoker() throws Exception {
        SalesforceCustomerServiceAttributesStore store = mock(SalesforceCustomerServiceAttributesStore.class);
        SalesforceCustomerServiceAttributes kvAttrs = mock(SalesforceCustomerServiceAttributes.class);
        when(kvAttrs.getClientId()).thenReturn("   "); // blank => guard fails, fall back
        when(kvAttrs.getClientSecret()).thenReturn("secret");
        when(store.loadByInvokerId(INVOKER_ID)).thenReturn(kvAttrs);

        SalesforceService kvService =
                new SalesforceService(providerFactory, invokerAttributes, store, INVOKER_ID);

        String result = kvService.getRecordById("500ID", "Case");

        assertEquals(OK_BODY, result);
        verify(invokerAttributes, atLeastOnce()).getOauthService();
    }

    @Test
    public void testEffectiveAttributes_KvNull_FallsBackToInvoker() throws Exception {
        SalesforceCustomerServiceAttributesStore store = mock(SalesforceCustomerServiceAttributesStore.class);
        when(store.loadByInvokerId(INVOKER_ID)).thenReturn(null);

        SalesforceService kvService =
                new SalesforceService(providerFactory, invokerAttributes, store, INVOKER_ID);

        String result = kvService.getRecordById("500ID", "Case");

        assertEquals(OK_BODY, result);
        verify(invokerAttributes, atLeastOnce()).getOauthService();
    }

    // ==================== getDifference ====================

    @Test
    public void testGetDifference_StringFieldChanged_Mapped() throws Exception {
        Sample oldObj = new Sample("A", 1.0, "d", "1");
        Sample newObj = new Sample("B", 1.0, "d", "1");

        Map<String, String> diff = SalesforceService.getDifference(oldObj, newObj);

        assertTrue("Changed string field should be mapped", diff.containsKey("name"));
        assertEquals("B", diff.get("name"));
        assertFalse(diff.containsKey("amount"));
    }

    @Test
    public void testGetDifference_ValueToNull_MapsEmptyString() throws Exception {
        Sample oldObj = new Sample("A", 1.0, "d", "1");
        Sample newObj = new Sample(null, 1.0, "d", "1");

        Map<String, String> diff = SalesforceService.getDifference(oldObj, newObj);

        assertTrue(diff.containsKey("name"));
        assertEquals("Old-value-to-null should map to empty string", "", diff.get("name"));
    }

    @Test
    public void testGetDifference_NullToValue_MapsValue() throws Exception {
        Sample oldObj = new Sample(null, 1.0, "d", "1");
        Sample newObj = new Sample("B", 1.0, "d", "1");

        Map<String, String> diff = SalesforceService.getDifference(oldObj, newObj);

        assertTrue(diff.containsKey("name"));
        assertEquals("Null-to-new-value should map to new value", "B", diff.get("name"));
    }

    @Test
    public void testGetDifference_DoubleChanged_MapsIntegerString() throws Exception {
        Sample oldObj = new Sample("A", 1.0, "d", "1");
        Sample newObj = new Sample("A", 5.0, "d", "1");

        Map<String, String> diff = SalesforceService.getDifference(oldObj, newObj);

        assertTrue(diff.containsKey("amount"));
        assertEquals("Double should be rendered as its integer string", "5", diff.get("amount"));
    }

    @Test
    public void testGetDifference_DateAndIdFieldsIgnored() throws Exception {
        // Only the date-named field and the id field change; both must be ignored.
        Sample oldObj = new Sample("A", 1.0, "old", "1");
        Sample newObj = new Sample("A", 1.0, "new", "2");

        Map<String, String> diff = SalesforceService.getDifference(oldObj, newObj);

        assertFalse("date-named field should be ignored", diff.containsKey("createdDate"));
        assertFalse("id field should be ignored", diff.containsKey("id"));
        assertFalse(diff.containsKey("name"));
        assertFalse(diff.containsKey("amount"));
    }

    @Test
    public void testGetDifference_NoChanges_ProducesNoTrackedKeys() throws Exception {
        Sample oldObj = new Sample("A", 1.0, "d", "1");
        Sample newObj = new Sample("A", 1.0, "d", "1");

        Map<String, String> diff = SalesforceService.getDifference(oldObj, newObj);

        assertFalse(diff.containsKey("name"));
        assertFalse(diff.containsKey("amount"));
        assertFalse(diff.containsKey("createdDate"));
        assertFalse(diff.containsKey("id"));
    }

    /**
     * Minimal POJO exercising {@link SalesforceService#getDifference(Object, Object)}:
     * a plain String field ({@code name}), a {@code Double} field ({@code amount}),
     * a date-named field ({@code createdDate}) and an {@code id} field — the last two must
     * be ignored by the diff. Only String/Double field types are used so {@code getStringValue}'s
     * cast never fails.
     */
    static class Sample {
        String name;
        Double amount;
        String createdDate;
        String id;

        Sample(String name, Double amount, String createdDate, String id) {
            this.name = name;
            this.amount = amount;
            this.createdDate = createdDate;
            this.id = id;
        }
    }
}
