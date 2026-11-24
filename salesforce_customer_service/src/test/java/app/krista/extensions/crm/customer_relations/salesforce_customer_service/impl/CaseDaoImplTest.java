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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCaseHistory;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores.KristaMediaClient;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CaseDaoImpl}.
 *
 * <p>The DAO delegates all remote interaction to {@link SalesforceService}, which is mocked here.
 * Read operations are implemented as SOQL queries (even {@code getCaseById}), so the tests stub
 * realistic Salesforce list JSON payloads and assert on the resulting {@link Case} objects.
 * Feed / file operations and case-history parsing are exercised through their own stubbed
 * payloads, and write/delete methods are verified through their interaction with the service.</p>
 */
public class CaseDaoImplTest {

    private static final String CASE = "Case";
    private static final String CASE_ID = "500XXXXXXXXXXXXXXX";

    private static final String CASE_JSON =
            "{\"Id\":\"" + CASE_ID + "\",\"Subject\":\"Login issue\",\"Status\":\"New\"," +
                    "\"Priority\":\"High\",\"Origin\":\"Web\",\"Description\":\"Cannot log in\"," +
                    "\"ContactId\":\"003XXX\",\"AccountId\":\"001XXX\",\"OwnerId\":\"005XXX\"," +
                    "\"IsClosed\":false,\"CaseNumber\":1001.0," +
                    "\"Contact\":{\"Name\":\"John Doe\"},\"Account\":{\"Name\":\"Acme Corp\"}}";

    @Mock
    private SalesforceService salesforceService;

    @Mock
    private KristaMediaClient kristaMediaClient;

    private CaseDaoImpl caseDao;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        caseDao = new CaseDaoImpl(salesforceService, kristaMediaClient);
    }

    // ==================== getCaseById ====================

    @Test
    public void testGetCaseById_ReturnsCase() throws Exception {
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + CASE_JSON + "],\"totalSize\":1,\"done\":true}");

        Case aCase = caseDao.getCaseById(CASE_ID);

        assertNotNull("Case should not be null", aCase);
        assertEquals("Id should match", CASE_ID, aCase.getId());
        assertEquals("Subject should match", "Login issue", aCase.getSubject());
        assertEquals("Status should match", "New", aCase.getStatus());
        assertEquals("Priority should match", "High", aCase.getPriority());
        assertEquals("Origin should match", "Web", aCase.getOrigin());
        assertEquals("Contact name should match", "John Doe", aCase.getContactName());
        assertEquals("Account name should match", "Acme Corp", aCase.getAccountName());
        assertFalse("IsClosed should be false", aCase.getIsClosed());
    }

    @Test(expected = RuntimeException.class)
    public void testGetCaseById_NotFound_ThrowsRuntime() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[]}");
        caseDao.getCaseById("500NOTFOUND");
    }

    @Test(expected = RuntimeException.class)
    public void testGetCaseById_NullId_NotFound_ThrowsRuntime() throws Exception {
        // Null id enters the guard branch, then the empty result set triggers the not-found path.
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[]}");
        caseDao.getCaseById(null);
    }

    // ==================== getCasesByQuery ====================

    @Test
    public void testGetCasesByQuery_ReturnsCases() throws Exception {
        String second = "{\"Id\":\"500BBB\",\"Subject\":\"Billing\"}";
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + CASE_JSON + "," + second + "],\"totalSize\":2,\"done\":true}");

        List<Case> cases = caseDao.getCasesByQuery("SELECT Id FROM Case");

        assertNotNull("Cases list should not be null", cases);
        assertEquals("Should return two cases", 2, cases.size());
        assertEquals("First case id should match", CASE_ID, cases.get(0).getId());
        assertEquals("Second case subject should match", "Billing", cases.get(1).getSubject());
    }

    @Test
    public void testGetCasesByQuery_EmptyRecords_ReturnsEmptyList() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[],\"totalSize\":0,\"done\":true}");

        List<Case> cases = caseDao.getCasesByQuery("SELECT Id FROM Case");

        assertNotNull("Cases list should not be null", cases);
        assertTrue("Cases list should be empty", cases.isEmpty());
    }

    @Test
    public void testGetCasesByQuery_BlankQuery_HandlesGuard() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[]}");

        List<Case> cases = caseDao.getCasesByQuery("   ");

        assertNotNull("Cases list should not be null", cases);
        assertTrue("Cases list should be empty", cases.isEmpty());
    }

    // ==================== getCases (paginated) ====================

    @Test
    public void testGetCases_ReturnsCasesAndBuildsQuery() throws Exception {
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + CASE_JSON + "],\"totalSize\":1,\"done\":true}");

        List<Case> cases = caseDao.getCases(10, 2, "CreatedDate", "DESC");

        assertNotNull("Cases list should not be null", cases);
        assertEquals("Should return one case", 1, cases.size());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(salesforceService).getRecordByQuery(queryCaptor.capture());
        String query = queryCaptor.getValue();
        assertTrue("Query should sort", query.contains("ORDER BY CreatedDate DESC"));
        assertTrue("Query should page", query.contains("LIMIT 10"));
        assertTrue("Query should offset", query.contains("OFFSET 10"));
    }

    // ==================== createCase ====================

    @Test
    public void testCreateCase_ReturnsId() throws Exception {
        Map<String, Object> entity = new HashMap<>();
        entity.put("Subject", "New ticket");
        entity.put("Status", "New");
        when(salesforceService.createRecord(anyString(), eq(CASE)))
                .thenReturn("{\"id\":\"500NEWCASE\",\"success\":true}");

        String id = caseDao.createCase(entity);

        assertEquals("Created case id should match", "500NEWCASE", id);
        verify(salesforceService).createRecord(anyString(), eq(CASE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateCase_NullMap_ThrowsIllegalArgument() throws Exception {
        caseDao.createCase(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateCase_EmptyMap_ThrowsIllegalArgument() throws Exception {
        caseDao.createCase(new HashMap<>());
    }

    // ==================== updateCase ====================

    @Test
    public void testUpdateCase_InvokesUpdateRecord() throws Exception {
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[{\"Id\":\"" + CASE_ID + "\",\"Subject\":\"Old subject\"," +
                        "\"Status\":\"New\"}],\"totalSize\":1,\"done\":true}");

        Case aCase = mock(Case.class);
        when(aCase.getId()).thenReturn(CASE_ID);
        when(aCase.getSubject()).thenReturn("Updated subject");

        caseDao.updateCase(aCase);

        verify(salesforceService).updateRecord(eq(CASE_ID), anyString(), eq(CASE));
    }

    // ==================== deleteCaseById ====================

    @Test
    public void testDeleteCaseById_InvokesDelete() throws Exception {
        caseDao.deleteCaseById(CASE_ID);

        verify(salesforceService).deleteRecordById(CASE_ID, CASE);
    }

    // ==================== getFeedItemsForCaseId ====================

    @Test
    public void testGetFeedItemsForCaseId_NotFound_ReturnsEmptyList() throws Exception {
        when(salesforceService.getFeedsByCaseId(anyString(), anyDouble()))
                .thenReturn("{\"errorCode\":\"NOT_FOUND\"}");

        List<Map<String, Object>> feed = caseDao.getFeedItemsForCaseId(CASE_ID, 10.0);

        assertNotNull("Feed list should not be null", feed);
        assertTrue("Feed list should be empty", feed.isEmpty());
    }

    @Test
    public void testGetFeedItemsForCaseId_ParsesFeedItemAndSkipsOthers() throws Exception {
        String response = "{\"elements\":[" +
                "{\"feedElementType\":\"FeedItem\"," +
                "\"actor\":{\"name\":\"John Doe\"}," +
                "\"body\":{\"messageSegments\":[{\"type\":\"Text\",\"text\":\"Hello world\"}]}," +
                "\"capabilities\":{}}," +
                "{\"feedElementType\":\"TextPost\",\"capabilities\":{}}" +
                "]}";
        when(salesforceService.getFeedsByCaseId(anyString(), anyDouble())).thenReturn(response);

        List<Map<String, Object>> feed = caseDao.getFeedItemsForCaseId(CASE_ID, 5.0);

        assertEquals("Both elements are collected", 2, feed.size());
        Map<String, Object> post = feed.get(0);
        assertEquals("Actor name should match", "John Doe", post.get("Actor Name"));
        assertEquals("Post message should match", "Hello world", post.get("Post Message"));
        assertTrue("File list should be empty", ((List<?>) post.get("File")).isEmpty());
        assertTrue("Non-FeedItem element yields empty map", feed.get(1).isEmpty());
    }

    @Test
    public void testGetFeedItemsForCaseId_NullActorAndBody() throws Exception {
        String response = "{\"elements\":[" +
                "{\"feedElementType\":\"FeedItem\",\"capabilities\":{}}" +
                "]}";
        when(salesforceService.getFeedsByCaseId(anyString(), anyDouble())).thenReturn(response);

        List<Map<String, Object>> feed = caseDao.getFeedItemsForCaseId(CASE_ID, 1.0);

        assertEquals("One post is collected", 1, feed.size());
        Map<String, Object> post = feed.get(0);
        assertNull("Actor name is null when actor is absent", post.get("Actor Name"));
        assertNull("Post message is null when body is absent", post.get("Post Message"));
        assertTrue("File list should be empty", ((List<?>) post.get("File")).isEmpty());
    }

    // ==================== addFeedUpdateToCaseById (text only) ====================

    @Test
    public void testAddFeedUpdate_TextOnly_Success() throws Exception {
        when(salesforceService.createFeedPost(anyString())).thenReturn("{\"id\":\"0D5FEED\"}");

        Boolean result = caseDao.addFeedUpdateToCaseById(CASE_ID, "A comment", null, null);

        assertTrue("Text-only update should succeed", result);
        verify(salesforceService).createFeedPost(anyString());
    }

    @Test
    public void testAddFeedUpdate_TextOnly_InvalidIdField_ReturnsFalse() throws Exception {
        when(salesforceService.createFeedPost(anyString()))
                .thenReturn("[{\"errorCode\":\"INVALID_ID_FIELD\",\"message\":\"bad id\"}]");

        Boolean result = caseDao.addFeedUpdateToCaseById(CASE_ID, "A comment", null, null);

        assertFalse("Invalid id field should fail", result);
    }

    @Test
    public void testAddFeedUpdate_TextOnly_NoId_ReturnsFalse() throws Exception {
        when(salesforceService.createFeedPost(anyString())).thenReturn("{\"foo\":\"bar\"}");

        Boolean result = caseDao.addFeedUpdateToCaseById(CASE_ID, "A comment", null, null);

        assertFalse("Missing id should fail", result);
    }

    // ==================== addFeedUpdateToCaseById (with file) ====================

    @Test
    public void testAddFeedUpdate_WithFile_Success() throws Exception {
        when(salesforceService.createFeedPostForFeedItem(anyString())).thenReturn("{\"id\":\"0D7ITEM\"}");
        when(salesforceService.createFeedPostForFile(anyString())).thenReturn("{\"id\":\"068FILE\"}");
        when(salesforceService.createFeedPostForFeedAttachment(anyString())).thenReturn("{\"id\":\"0ITATT\"}");

        Boolean result = caseDao.addFeedUpdateToCaseById(CASE_ID, "msg", "YmFzZTY0", "report.txt");

        assertTrue("File update should succeed", result);
        verify(salesforceService).createFeedPostForFeedItem(anyString());
        verify(salesforceService).createFeedPostForFile(anyString());
        verify(salesforceService).createFeedPostForFeedAttachment(anyString());
    }

    @Test
    public void testAddFeedUpdate_WithFile_MalformedId_ReturnsFalse() throws Exception {
        when(salesforceService.createFeedPostForFeedItem(anyString()))
                .thenReturn("[{\"errorCode\":\"MALFORMED_ID\"}]");

        Boolean result = caseDao.addFeedUpdateToCaseById(CASE_ID, "msg", "YmFzZTY0", "report.txt");

        assertFalse("Malformed id should fail", result);
        verify(salesforceService, never()).createFeedPostForFile(anyString());
    }

    @Test
    public void testAddFeedUpdate_WithFile_NoAttachmentId_ReturnsFalse() throws Exception {
        when(salesforceService.createFeedPostForFeedItem(anyString())).thenReturn("{\"id\":\"0D7ITEM\"}");
        when(salesforceService.createFeedPostForFile(anyString())).thenReturn("{\"id\":\"068FILE\"}");
        when(salesforceService.createFeedPostForFeedAttachment(anyString())).thenReturn("{\"foo\":\"bar\"}");

        Boolean result = caseDao.addFeedUpdateToCaseById(CASE_ID, "msg", "YmFzZTY0", "report.txt");

        assertFalse("Missing attachment id should fail", result);
    }

    // ==================== getCaseUpdatedFields ====================

    @Test
    public void testGetCaseUpdatedFields_ReturnsLatestValues() throws Exception {
        // Build the payload with Gson so the Date round-trips through the same default format
        // the DAO uses to deserialize it.
        SFCaseHistory history = new SFCaseHistory();
        history.records = new ArrayList<>();
        SFCaseHistory.Record record = new SFCaseHistory.Record();
        record.Field = "Status";
        record.NewValue = "Closed";
        record.CreatedDate = new Date();
        history.records.add(record);
        when(salesforceService.getRecordByQuery(anyString())).thenReturn(new Gson().toJson(history));

        Map<String, Object> updated = caseDao.getCaseUpdatedFields("SELECT Field, NewValue FROM CaseHistory");

        assertNotNull("Updated fields should not be null", updated);
        assertEquals("Latest status should be captured", "Closed", updated.get("Status"));
    }

    // ==================== getBase64EncodedString ====================

    @Test
    public void testGetBase64EncodedString_EncodesFileContent() throws Exception {
        File temp = File.createTempFile("case-dao-test", ".txt");
        temp.deleteOnExit();
        byte[] content = "Hello Krista".getBytes();
        Files.write(temp.toPath(), content);

        String encoded = caseDao.getBase64EncodedString(temp);

        assertEquals("Encoded content should match",
                Base64.getEncoder().encodeToString(content), encoded);
    }
}
