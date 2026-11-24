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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Individual;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.IndividualDaoImpl;
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
 * Unit tests for {@link IndividualStore}.
 *
 * <p>{@code IndividualStore} delegates to a mocked {@link IndividualDaoImpl}. Guard clauses throw
 * {@link IllegalArgumentException}; wrapped DAO {@code IOException}/{@code ExecutionException}
 * surface as {@link RuntimeException}, and {@code InterruptedException} surfaces as
 * {@link InterruptedIOException}.</p>
 */
public class IndividualStoreTest {

    private IndividualDaoImpl dao;
    private IndividualStore store;

    @Before
    public void setUp() throws Exception {
        dao = mock(IndividualDaoImpl.class);
        Constructor<IndividualStore> ctor = IndividualStore.class.getDeclaredConstructor(IndividualDaoImpl.class);
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

    private app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual sf(String id) {
        // Real wrapper (only id set) — behaviourally identical to a mock stubbing only getId(),
        // but performs NO Mockito call during evaluation, so it is safe inside thenReturn(sf(...)).
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFIndividual i =
                new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFIndividual();
        i.id = id;
        return new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.IndividualImpl(i);
    }

    private Map<String, Object> nonEmptyMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("Last Name", "Smith");
        return m;
    }

    // ---- create ------------------------------------------------------------------

    @Test
    public void createReturnsMappedEntity() throws Exception {
        when(dao.createIndividual(anyMap())).thenReturn("I1");
        when(dao.getIndividualById(anyString())).thenReturn(sf("I1"));

        Individual result = store.create(nonEmptyMap());

        assertNotNull(result);
        assertEquals("I1", result.id);
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
        when(dao.createIndividual(anyMap())).thenThrow(new IOException("boom"));
        store.create(nonEmptyMap());
    }

    // ---- get ---------------------------------------------------------------------

    @Test
    public void getReturnsMappedEntity() throws Exception {
        when(dao.getIndividualById("I1")).thenReturn(sf("I1"));

        Individual result = store.get("I1");

        assertNotNull(result);
        assertEquals("I1", result.id);
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
        when(dao.getIndividualById(anyString())).thenThrow(new IOException("boom"));
        store.get("I1");
    }

    @Test(expected = InterruptedIOException.class)
    public void getWrapsInterruptedException() throws Exception {
        try {
            when(dao.getIndividualById(anyString())).thenThrow(new InterruptedException("boom"));
            store.get("I1");
        } finally {
            Thread.interrupted();
        }
    }

    // ---- update ------------------------------------------------------------------

    @Test
    public void updateReturnsRefreshedEntity() throws Exception {
        Individual input = new Individual();
        input.id = "I1";
        when(dao.getIndividualById("I1")).thenReturn(sf("I1"));

        Individual result = store.update(input);

        assertNotNull(result);
        assertEquals("I1", result.id);
        verify(dao).updateIndividual(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateWithNullThrows() throws Exception {
        store.update(null);
    }

    @Test(expected = RuntimeException.class)
    public void updateWrapsIOException() throws Exception {
        Individual input = new Individual();
        input.id = "I1";
        doThrow(new IOException("boom")).when(dao).updateIndividual(any());
        store.update(input);
    }

    // ---- delete ------------------------------------------------------------------

    @Test
    public void deleteDelegatesToDao() throws Exception {
        store.delete("I1");
        verify(dao).deleteIndividualById("I1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteWithNullKeyThrows() {
        store.delete(null);
    }

    @Test(expected = RuntimeException.class)
    public void deleteWrapsIOException() throws Exception {
        doThrow(new IOException("boom")).when(dao).deleteIndividualById(anyString());
        store.delete("I1");
    }

    @Test(expected = RuntimeException.class)
    public void deleteWrapsInterruptedException() throws Exception {
        try {
            doThrow(new InterruptedException("boom")).when(dao).deleteIndividualById(anyString());
            store.delete("I1");
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
    public void searchSingleConditionUsesGetIndividualsWithDefaultOrder() throws Exception {
        when(dao.getIndividuals(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("I1")));

        List<Individual> result = store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        assertEquals("I1", result.get(0).id);
        verify(dao).getIndividuals(anyLong(), anyLong(), eq("LastModifiedDate"), eq("DESC"));
    }

    @Test
    public void searchSingleConditionUsesQueryClauseOrder() throws Exception {
        when(dao.getIndividuals(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("I1")));

        List<Individual> result = store.search(
                query(List.of(cond("Id", "x")), List.of(clause("CreatedDate", "ASC"))), 0L, 10);

        assertEquals(1, result.size());
        verify(dao).getIndividuals(anyLong(), anyLong(), eq("CreatedDate"), eq("ASC"));
    }

    @Test
    public void searchMultipleConditionsBuildsQuery() throws Exception {
        when(dao.getIndividualsByQuery(anyString())).thenReturn(List.of(sf("I1")));

        List<Individual> result = store.search(
                query(List.of(cond("Id", "I1"), cond("Subject", "Hello")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(dao).getIndividualsByQuery(captor.capture());
        String q = captor.getValue();
        assertTrue(q.contains("Id = 'I1'"));
    }

    @Test(expected = RuntimeException.class)
    public void searchWrapsIOException() throws Exception {
        when(dao.getIndividuals(anyLong(), anyLong(), anyString(), anyString()))
                .thenThrow(new IOException("boom"));
        store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);
    }

    @Test(expected = InterruptedIOException.class)
    public void searchWrapsInterruptedException() throws Exception {
        try {
            when(dao.getIndividuals(anyLong(), anyLong(), anyString(), anyString()))
                    .thenThrow(new InterruptedException("boom"));
            store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);
        } finally {
            Thread.interrupted();
        }
    }
}
