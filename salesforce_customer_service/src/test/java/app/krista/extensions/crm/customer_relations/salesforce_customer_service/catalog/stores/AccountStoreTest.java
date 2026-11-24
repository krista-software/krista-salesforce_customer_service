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
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Account;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.AccountDaoImpl;
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
 * Unit tests for {@link AccountStore}.
 *
 * <p>{@code AccountStore} delegates to a mocked {@link AccountDaoImpl} and maps the DAO's
 * {@code service.Account} results through {@code CatalogTypes.fromSFAccount}. The store's
 * guard clauses throw {@link IllegalArgumentException}, while DAO exceptions are caught and a
 * default value ({@code new Account()} / {@code List.of()}) is returned.</p>
 */
public class AccountStoreTest {

    private AccountDaoImpl dao;
    private AccountStore store;

    @Before
    public void setUp() throws Exception {
        dao = mock(AccountDaoImpl.class);
        Constructor<AccountStore> ctor = AccountStore.class.getDeclaredConstructor(AccountDaoImpl.class);
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

    private app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Account sf(String id) {
        // Return a real wrapper (only id set, rest null) instead of a stubbed mock. This is
        // behaviourally identical to a mock stubbing only getId(), but performs NO Mockito call
        // during evaluation — so it is safe to use inside when(dao...).thenReturn(sf(...)).
        app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFAccount a =
                new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFAccount();
        a.id = id;
        return new app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.AccountImpl(a);
    }

    private Map<String, Object> nonEmptyMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("Name", "Acme");
        return m;
    }

    // ---- create ------------------------------------------------------------------

    @Test
    public void createReturnsMappedEntity() throws Exception {
        when(dao.createAccount(anyMap())).thenReturn("001");
        when(dao.getAccountById(anyString())).thenReturn(sf("001"));

        Account result = store.create(nonEmptyMap());

        assertNotNull(result);
        assertEquals("001", result.id);
        verify(dao).createAccount(anyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithNullMapThrows() {
        store.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithEmptyMapThrows() {
        store.create(new HashMap<>());
    }

    @Test
    public void createSwallowsDaoExceptionAndReturnsEmptyAccount() throws Exception {
        when(dao.createAccount(anyMap())).thenThrow(new IOException("boom"));

        Account result = store.create(nonEmptyMap());

        assertNotNull(result);
        assertNull(result.id);
    }

    // ---- get ---------------------------------------------------------------------

    @Test
    public void getReturnsMappedEntity() throws Exception {
        when(dao.getAccountById("001")).thenReturn(sf("001"));

        Account result = store.get("001");

        assertNotNull(result);
        assertEquals("001", result.id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getWithNullKeyThrows() {
        store.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getWithBlankKeyThrows() {
        store.get("   ");
    }

    @Test
    public void getSwallowsDaoExceptionAndReturnsEmptyAccount() throws Exception {
        when(dao.getAccountById(anyString())).thenThrow(new IOException("boom"));

        Account result = store.get("001");

        assertNotNull(result);
        assertNull(result.id);
    }

    // ---- update ------------------------------------------------------------------

    @Test
    public void updateReturnsRefreshedEntity() throws Exception {
        Account input = new Account();
        input.id = "001";
        when(dao.getAccountById("001")).thenReturn(sf("001"));

        Account result = store.update(input);

        assertNotNull(result);
        assertEquals("001", result.id);
        verify(dao).updateAccount(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateWithNullThrows() {
        store.update(null);
    }

    @Test
    public void updateSwallowsDaoExceptionAndReturnsEmptyAccount() throws Exception {
        Account input = new Account();
        input.id = "001";
        doThrow(new IOException("boom")).when(dao).updateAccount(any());

        Account result = store.update(input);

        assertNotNull(result);
        assertNull(result.id);
    }

    // ---- delete ------------------------------------------------------------------

    @Test
    public void deleteDelegatesToDao() throws Exception {
        store.delete("001");
        verify(dao).deleteAccountById("001");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteWithNullKeyThrows() {
        store.delete(null);
    }

    @Test
    public void deleteSwallowsDaoException() throws Exception {
        doThrow(new IOException("boom")).when(dao).deleteAccountById(anyString());
        store.delete("001"); // must not propagate
        verify(dao).deleteAccountById("001");
    }

    // ---- misc overrides ----------------------------------------------------------

    @Test
    public void containsReturnsFalse() {
        assertFalse(store.contains("anything"));
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
    public void searchSingleConditionUsesGetAccountsWithDefaultOrder() throws Exception {
        when(dao.getAccounts(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("A1")));

        List<Account> result = store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        assertEquals("A1", result.get(0).id);
        verify(dao).getAccounts(anyLong(), anyLong(), eq("LastModifiedDate"), eq("DESC"));
    }

    @Test
    public void searchSingleConditionUsesQueryClauseOrder() throws Exception {
        when(dao.getAccounts(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(List.of(sf("A1")));

        List<Account> result = store.search(
                query(List.of(cond("Id", "x")), List.of(clause("CreatedDate", "ASC"))), 0L, 10);

        assertEquals(1, result.size());
        verify(dao).getAccounts(anyLong(), anyLong(), eq("CreatedDate"), eq("ASC"));
    }

    @Test
    public void searchMultipleConditionsBuildsQuery() throws Exception {
        when(dao.getAccountByQuery(anyString())).thenReturn(List.of(sf("A1")));

        List<Account> result = store.search(
                query(List.of(cond("Id", "001"), cond("Name", "Acme")), List.of()), 0L, 10);

        assertEquals(1, result.size());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(dao).getAccountByQuery(captor.capture());
        String q = captor.getValue();
        assertTrue(q.contains("from Account"));
        assertTrue(q.contains("Id = '001'"));
        assertTrue(q.contains("Name LIKE '%Acme%'"));
    }

    @Test
    public void searchSwallowsDaoExceptionAndReturnsEmptyList() throws Exception {
        when(dao.getAccounts(anyLong(), anyLong(), anyString(), anyString()))
                .thenThrow(new IOException("boom"));

        List<Account> result = store.search(query(List.of(cond("Id", "x")), List.of()), 0L, 10);

        assertTrue(result.isEmpty());
    }
}
