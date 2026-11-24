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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Customer;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CustomerDaoImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CustomerStore}.
 *
 * <p>{@code CustomerStore} delegates to a mocked {@link CustomerDaoImpl}. Most CRUD guard clauses
 * only log and fall through (returning {@code new Customer()}); DAO exceptions are caught and a
 * default value is returned. Only {@code search(SearchQuery,...)} throws
 * {@link IllegalArgumentException} on missing conditions.</p>
 */
public class CustomerStoreTest {

    private CustomerDaoImpl dao;
    private CustomerStore store;

    @Before
    public void setUp() throws Exception {
        dao = mock(CustomerDaoImpl.class);
        Constructor<CustomerStore> ctor = CustomerStore.class.getDeclaredConstructor(CustomerDaoImpl.class);
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

    private app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Customer sf(String id) {
        // Real wrapper (only id set) — behaviourally identical to a mock stubbing only getId(),
        // but performs NO Mockito call during evaluation, so it is safe inside thenReturn(sf(...)).
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCustomer c =
                new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFCustomer();
        c.id = id;
        return new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.CustomerImpl(c);
    }

    private Map<String, Object> nonEmptyMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("Name", "Globex");
        return m;
    }

    // ---- create ------------------------------------------------------------------

    @Test
    public void createReturnsMappedEntity() throws Exception {
        when(dao.createCustomer(anyMap())).thenReturn("C1");
        when(dao.getCustomerById(anyString())).thenReturn(sf("C1"));

        Customer result = store.create(nonEmptyMap());

        assertNotNull(result);
        assertEquals("C1", result.id);
    }

    @Test
    public void createWithNullMapFallsThroughToEmptyCustomer() throws Exception {
        Customer result = store.create(null);
        assertNotNull(result);
        assertNull(result.id);
    }

    @Test
    public void createSwallowsIllegalArgumentAndReturnsEmptyCustomer() throws Exception {
        when(dao.createCustomer(anyMap())).thenThrow(new IllegalArgumentException("bad"));

        Customer result = store.create(nonEmptyMap());

        assertNotNull(result);
        assertNull(result.id);
    }

    // ---- get ---------------------------------------------------------------------

    @Test
    public void getReturnsMappedEntity() throws Exception {
        when(dao.getCustomerById("C1")).thenReturn(sf("C1"));

        Customer result = store.get("C1");

        assertNotNull(result);
        assertEquals("C1", result.id);
    }

    @Test
    public void getWithNullKeyFallsThroughToEmptyCustomer() throws Exception {
        Customer result = store.get(null);
        assertNotNull(result);
        assertNull(result.id);
    }

    @Test
    public void getSwallowsDaoExceptionAndReturnsEmptyCustomer() throws Exception {
        when(dao.getCustomerById(anyString())).thenThrow(new IOException("boom"));

        Customer result = store.get("C1");

        assertNotNull(result);
        assertNull(result.id);
    }

    // ---- update ------------------------------------------------------------------

    @Test
    public void updateReturnsRefreshedEntity() throws Exception {
        Customer input = new Customer();
        input.id = "C1";
        when(dao.getCustomerById("C1")).thenReturn(sf("C1"));

        Customer result = store.update(input);

        assertNotNull(result);
        assertEquals("C1", result.id);
        verify(dao).updateCustomer(any());
    }

    @Test
    public void updateSwallowsDaoExceptionAndReturnsEmptyCustomer() throws Exception {
        Customer input = new Customer();
        input.id = "C1";
        doThrow(new IOException("boom")).when(dao).updateCustomer(any());

        Customer result = store.update(input);

        assertNotNull(result);
        assertNull(result.id);
    }

    // ---- delete ------------------------------------------------------------------

    @Test
    public void deleteDelegatesToDao() throws Exception {
        store.delete("C1");
        verify(dao).deleteCustomerById("C1");
    }

    @Test
    public void deleteWithNullKeyDoesNotThrow() throws Exception {
        store.delete(null); // guard only logs; must not throw
        verify(dao).deleteCustomerById(isNull());
    }

    @Test
    public void deleteSwallowsDaoException() throws Exception {
        doThrow(new IOException("boom")).when(dao).deleteCustomerById(anyString());
        store.delete("C1"); // must not propagate
        verify(dao).deleteCustomerById("C1");
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
    public void searchWithNullConditionsThrows() {
        store.search(query(null, List.of()), 0L, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchWithEmptyConditionsThrows() {
        store.search(query(List.of(), List.of()), 0L, 10);
    }

    @Test
    public void searchSingleConditionUsesGetCustomersWithDefaultOrder() throws Exception {
        when(dao.getCustomers(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("C1")));

        List<Customer> result = store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).id);
        verify(dao).getCustomers(anyLong(), anyLong(), eq("LastModifiedDate"), eq("DESC"));
    }

    @Test
    public void searchSingleConditionUsesQueryClauseOrder() throws Exception {
        when(dao.getCustomers(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("C1")));

        List<Customer> result = store.search(
                query(List.of(cond("Id", "x")), List.of(clause("CreatedDate", "ASC"))), 0L, 10);

        assertEquals(1, result.size());
        verify(dao).getCustomers(anyLong(), anyLong(), eq("CreatedDate"), eq("ASC"));
    }

    @Test
    public void searchMultipleConditionsBuildsQuery() throws Exception {
        when(dao.getCustomerByQuery(anyString())).thenReturn(List.of(sf("C1")));

        List<Customer> result = store.search(
                query(List.of(cond("Id", "C1"), cond("Subject", "Hello")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(dao).getCustomerByQuery(captor.capture());
        String q = captor.getValue();
        assertTrue(q.contains("Id = 'C1'"));
    }

    @Test
    public void searchSwallowsDaoExceptionAndReturnsEmptyList() throws Exception {
        when(dao.getCustomers(anyLong(), anyLong(), anyString(), anyString()))
                .thenThrow(new IOException("boom"));

        List<Customer> result = store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);

        assertTrue(result.isEmpty());
    }
}
