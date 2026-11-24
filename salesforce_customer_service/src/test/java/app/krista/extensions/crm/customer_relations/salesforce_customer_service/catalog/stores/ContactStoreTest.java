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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.stores;

import app.krista.extension.executor.QueryClause;
import app.krista.extension.executor.SearchCondition;
import app.krista.extension.executor.SearchQuery;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Contact;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.ContactDaoImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContactStore}.
 *
 * <p>{@code ContactStore} delegates to a mocked {@link ContactDaoImpl}. Guard clauses throw
 * {@link IllegalArgumentException}; wrapped DAO {@code IOException}/{@code ExecutionException}
 * surface as {@link RuntimeException}, and {@code InterruptedException} surfaces as
 * {@link InterruptedIOException}.</p>
 */
public class ContactStoreTest {

    private ContactDaoImpl dao;
    private ContactStore store;

    @Before
    public void setUp() throws Exception {
        dao = mock(ContactDaoImpl.class);
        Constructor<ContactStore> ctor = ContactStore.class.getDeclaredConstructor(ContactDaoImpl.class);
        ctor.setAccessible(true);
        store = ctor.newInstance(dao);
    }

    // ---- helpers -----------------------------------------------------------------

    private SearchQuery query(List<SearchCondition> conds, List<QueryClause> clauses) {
        SearchQuery q = mock(SearchQuery.class);
        when(q.getSearchConditions()).thenReturn(conds);
        when(q.getQueryClauses()).thenReturn(clauses);
        return q;
    }

    private SearchCondition cond(String field, Object operand) {
        SearchCondition c = mock(SearchCondition.class);
        when(c.getFieldName()).thenReturn(field);
        when(c.getOperand()).thenReturn(operand);
        return c;
    }

    private QueryClause clause(String field, Object operand) {
        QueryClause qc = mock(QueryClause.class);
        when(qc.getFieldName()).thenReturn(field);
        when(qc.getOperand()).thenReturn(operand);
        return qc;
    }

    private app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Contact sf(String id) {
        // Real wrapper (only id set) — behaviourally identical to a mock stubbing only getId(),
        // but performs NO Mockito call during evaluation, so it is safe inside thenReturn(sf(...)).
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFContact c =
                new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFContact();
        c.id = id;
        return new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.ContactImpl(c);
    }

    private Map<String, Object> nonEmptyMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("Name", "John Doe");
        return m;
    }

    // ---- create ------------------------------------------------------------------

    @Test
    public void createReturnsMappedEntity() throws Exception {
        when(dao.createContact(anyMap())).thenReturn("C1");
        when(dao.getContactById(anyString())).thenReturn(sf("C1"));

        Contact result = store.create(nonEmptyMap());

        assertNotNull(result);
        assertEquals("C1", result.id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullMapThrows() throws Exception {
        store.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithEmptyMapThrows() throws Exception {
        store.create(new HashMap<>());
    }

    @Test(expected = RuntimeException.class)
    public void createWrapsIOException() throws Exception {
        when(dao.createContact(anyMap())).thenThrow(new IOException("boom"));
        store.create(nonEmptyMap());
    }

    // ---- get ---------------------------------------------------------------------

    @Test
    public void getReturnsMappedEntity() throws Exception {
        when(dao.getContactById("C1")).thenReturn(sf("C1"));

        Contact result = store.get("C1");

        assertNotNull(result);
        assertEquals("C1", result.id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getWithNullKeyThrows() throws Exception {
        store.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getWithBlankKeyThrows() throws Exception {
        store.get("  ");
    }

    @Test(expected = RuntimeException.class)
    public void getWrapsIOException() throws Exception {
        when(dao.getContactById(anyString())).thenThrow(new IOException("boom"));
        store.get("C1");
    }

    @Test(expected = InterruptedIOException.class)
    public void getWrapsInterruptedException() throws Exception {
        try {
            when(dao.getContactById(anyString())).thenThrow(new InterruptedException("boom"));
            store.get("C1");
        } finally {
            Thread.interrupted();
        }
    }

    // ---- update ------------------------------------------------------------------

    @Test
    public void updateReturnsRefreshedEntity() throws Exception {
        Contact input = new Contact();
        input.id = "C1";
        input.name = "John Doe";
        when(dao.getContactById("C1")).thenReturn(sf("C1"));

        Contact result = store.update(input);

        assertNotNull(result);
        assertEquals("C1", result.id);
        verify(dao).updateContact(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateWithNullThrows() throws Exception {
        store.update(null);
    }

    @Test(expected = RuntimeException.class)
    public void updateWrapsIOException() throws Exception {
        Contact input = new Contact();
        input.id = "C1";
        input.name = "John Doe";
        doThrow(new IOException("boom")).when(dao).updateContact(any());
        store.update(input);
    }

    // ---- delete ------------------------------------------------------------------

    @Test
    public void deleteDelegatesToDao() throws Exception {
        store.delete("C1");
        verify(dao).deleteContactById("C1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteWithNullKeyThrows() {
        store.delete(null);
    }

    @Test(expected = RuntimeException.class)
    public void deleteWrapsIOException() throws Exception {
        doThrow(new IOException("boom")).when(dao).deleteContactById(anyString());
        store.delete("C1");
    }

    // ---- misc overrides ----------------------------------------------------------

    @Test
    public void containsReturnsFalse() {
        assertFalse(store.contains("x"));
    }

    @Test
    public void countReturnsZero() {
        assertEquals(0L, store.count(List.of()));
    }

    @Test
    public void lookupReturnsEmpty() {
        assertTrue(store.lookup(new HashMap<>(), List.of()).isEmpty());
    }

    @Test
    public void deprecatedSearchReturnsEmpty() {
        assertTrue(store.search(List.of(cond("Name", "x")), 0L, 10).isEmpty());
    }

    // ---- search(SearchQuery) -----------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void searchWithNullConditionsThrows() throws Exception {
        store.search(query(null, List.of()), 0L, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchWithEmptyConditionsThrows() throws Exception {
        store.search(query(List.of(), List.of()), 0L, 10);
    }

    @Test
    public void searchSingleConditionUsesGetContactsWithDefaultOrder() throws Exception {
        when(dao.getContacts(anyInt(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("C1")));

        List<Contact> result = store.search(query(List.of(cond("Name", "x")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).id);
        verify(dao).getContacts(anyInt(), anyLong(), eq("LastModifiedDate"), eq("DESC"));
    }

    @Test
    public void searchSingleConditionUsesQueryClauseOrder() throws Exception {
        when(dao.getContacts(anyInt(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("C1")));

        List<Contact> result = store.search(
                query(List.of(cond("Name", "x")), List.of(clause("CreatedDate", "ASC"))), 0L, 10);

        assertEquals(1, result.size());
        verify(dao).getContacts(anyInt(), anyLong(), eq("CreatedDate"), eq("ASC"));
    }

    @Test
    public void searchMultipleConditionsBuildsQuery() throws Exception {
        when(dao.getContactsByQuery(anyString())).thenReturn(List.of(sf("C1")));

        List<Contact> result = store.search(
                query(List.of(cond("Name", "John"), cond("Email", "e@x.com")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(dao).getContactsByQuery(captor.capture());
        String q = captor.getValue();
        assertTrue(q.contains("from Contact"));
        assertTrue(q.contains("Name = 'John'"));
        assertTrue(q.contains("Email LIKE '%e@x.com%'"));
    }

    @Test(expected = RuntimeException.class)
    public void searchWrapsIOException() throws Exception {
        when(dao.getContacts(anyInt(), anyLong(), anyString(), anyString()))
                .thenThrow(new IOException("boom"));
        store.search(query(List.of(cond("Name", "x")), List.of()), 0L, 10);
    }

    @Test(expected = InterruptedIOException.class)
    public void searchWrapsInterruptedException() throws Exception {
        try {
            when(dao.getContacts(anyInt(), anyLong(), anyString(), anyString()))
                    .thenThrow(new InterruptedException("boom"));
            store.search(query(List.of(cond("Name", "x")), List.of()), 0L, 10);
        } finally {
            Thread.interrupted();
        }
    }
}
