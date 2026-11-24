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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Case;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseDaoImpl;
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
 * Unit tests for {@link CaseStore}.
 *
 * <p>{@code CaseStore} delegates to a mocked {@link CaseDaoImpl}. Guard clauses throw
 * {@link IllegalArgumentException}; wrapped DAO {@code IOException}/{@code ExecutionException}
 * surface as {@link RuntimeException}, and {@code InterruptedException} surfaces as
 * {@link InterruptedIOException}.</p>
 */
public class CaseStoreTest {

    private CaseDaoImpl dao;
    private CaseStore store;

    @Before
    public void setUp() throws Exception {
        dao = mock(CaseDaoImpl.class);
        Constructor<CaseStore> ctor = CaseStore.class.getDeclaredConstructor(CaseDaoImpl.class);
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
        when(c.getOperator()).thenReturn(SearchCondition.Operator.equals);
        return c;
    }

    private QueryClause clause(String field, Object operand) {
        QueryClause qc = mock(QueryClause.class);
        when(qc.getFieldName()).thenReturn(field);
        when(qc.getOperand()).thenReturn(operand);
        return qc;
    }

    private app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Case sf(String id) {
        // Real wrapper (only id set) — behaviourally identical to a mock stubbing only getId(),
        // but performs NO Mockito call during evaluation, so it is safe inside thenReturn(sf(...)).
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCase c =
                new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCase();
        c.id = id;
        return new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CaseImpl(c);
    }

    private Map<String, Object> nonEmptyMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("Subject", "Login issue");
        return m;
    }

    // ---- create ------------------------------------------------------------------

    @Test
    public void createReturnsMappedEntity() throws Exception {
        when(dao.createCase(anyMap())).thenReturn("500");
        when(dao.getCaseById(anyString())).thenReturn(sf("500"));

        Case result = store.create(nonEmptyMap());

        assertNotNull(result);
        assertEquals("500", result.id);
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
        when(dao.createCase(anyMap())).thenThrow(new IOException("boom"));
        store.create(nonEmptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createRethrowsIllegalArgument() throws Exception {
        when(dao.createCase(anyMap())).thenThrow(new IllegalArgumentException("bad"));
        store.create(nonEmptyMap());
    }

    // ---- get ---------------------------------------------------------------------

    @Test
    public void getReturnsMappedEntity() throws Exception {
        when(dao.getCaseById("500")).thenReturn(sf("500"));

        Case result = store.get("500");

        assertNotNull(result);
        assertEquals("500", result.id);
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
        when(dao.getCaseById(anyString())).thenThrow(new IOException("boom"));
        store.get("500");
    }

    @Test(expected = InterruptedIOException.class)
    public void getWrapsInterruptedException() throws Exception {
        try {
            when(dao.getCaseById(anyString())).thenThrow(new InterruptedException("boom"));
            store.get("500");
        } finally {
            Thread.interrupted(); // clear interrupt flag for subsequent tests
        }
    }

    // ---- update ------------------------------------------------------------------

    @Test
    public void updateReturnsRefreshedEntity() throws Exception {
        Case input = new Case();
        input.id = "500";
        when(dao.getCaseById("500")).thenReturn(sf("500"));

        Case result = store.update(input);

        assertNotNull(result);
        assertEquals("500", result.id);
        verify(dao).updateCase(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateWithNullThrows() throws Exception {
        store.update(null);
    }

    @Test(expected = RuntimeException.class)
    public void updateWrapsIOException() throws Exception {
        Case input = new Case();
        input.id = "500";
        doThrow(new IOException("boom")).when(dao).updateCase(any());
        store.update(input);
    }

    // ---- delete ------------------------------------------------------------------

    @Test
    public void deleteDelegatesToDao() throws Exception {
        store.delete("500");
        verify(dao).deleteCaseById("500");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteWithNullKeyThrows() {
        store.delete(null);
    }

    @Test(expected = RuntimeException.class)
    public void deleteWrapsIOException() throws Exception {
        doThrow(new IOException("boom")).when(dao).deleteCaseById(anyString());
        store.delete("500");
    }

    @Test(expected = RuntimeException.class)
    public void deleteWrapsInterruptedException() throws Exception {
        try {
            doThrow(new InterruptedException("boom")).when(dao).deleteCaseById(anyString());
            store.delete("500");
        } finally {
            Thread.interrupted();
        }
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
        assertTrue(store.search(List.of(cond("Id", "x")), 0L, 10).isEmpty());
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
    public void searchSingleConditionUsesGetCasesWithDefaultOrder() throws Exception {
        when(dao.getCases(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("C1")));

        List<Case> result = store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).id);
        verify(dao).getCases(anyLong(), anyLong(), eq("LastModifiedDate"), eq("DESC"));
    }

    @Test
    public void searchSingleConditionUsesQueryClauseOrder() throws Exception {
        when(dao.getCases(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("C1")));

        List<Case> result = store.search(
                query(List.of(cond("Id", "x")), List.of(clause("CreatedDate", "ASC"))), 0L, 10);

        assertEquals(1, result.size());
        verify(dao).getCases(anyLong(), anyLong(), eq("CreatedDate"), eq("ASC"));
    }

    @Test
    public void searchMultipleConditionsBuildsQuery() throws Exception {
        when(dao.getCasesByQuery(anyString())).thenReturn(List.of(sf("C1")));

        List<Case> result = store.search(
                query(List.of(cond("Id", "500"), cond("Subject", "Login")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(dao).getCasesByQuery(captor.capture());
        String q = captor.getValue();
        assertTrue(q.contains("from Case"));
        assertTrue(q.contains("Id = '500'"));
        assertTrue(q.contains("Subject LIKE '%Login%'"));
    }

    @Test(expected = RuntimeException.class)
    public void searchWrapsIOException() throws Exception {
        when(dao.getCases(anyLong(), anyLong(), anyString(), anyString()))
                .thenThrow(new IOException("boom"));
        store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);
    }

    @Test(expected = InterruptedIOException.class)
    public void searchWrapsInterruptedException() throws Exception {
        try {
            when(dao.getCases(anyLong(), anyLong(), anyString(), anyString()))
                    .thenThrow(new InterruptedException("boom"));
            store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);
        } finally {
            Thread.interrupted();
        }
    }
}
