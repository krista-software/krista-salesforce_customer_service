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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model.SFAccount;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Account;
import app.krista.extensions.crm.customer_relations.salesforce_customer_service.util.Constants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AccountDaoImpl}.
 *
 * <p>The DAO delegates all remote interaction to {@link SalesforceService}, which is mocked here.
 * Read methods parse the JSON strings returned by the mocked service into domain objects, so the
 * tests stub realistic Salesforce JSON payloads and assert on the resulting {@link Account}
 * objects. Write/delete methods are verified through their interaction with the service.</p>
 */
public class AccountDaoImplTest {

    private static final String ACCOUNT = "Account";
    private static final String ACCOUNT_ID = "001XXXXXXXXXXXXXXX";

    @Mock
    private SalesforceService salesforceService;

    private AccountDaoImpl accountDao;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        accountDao = new AccountDaoImpl(salesforceService);
    }

    // ==================== getAccountById ====================

    @Test
    public void testGetAccountById_ReturnsAccount() throws Exception {
        String json = "{\"Id\":\"" + ACCOUNT_ID + "\",\"Name\":\"Acme Corp\",\"Type\":\"Customer\"," +
                "\"Phone\":\"555-0100\",\"Website\":\"https://acme.example.com\"," +
                "\"AccountNumber\":\"ACC-100\",\"Industry\":\"Technology\"}";
        when(salesforceService.getRecordById(ACCOUNT_ID, ACCOUNT)).thenReturn(json);

        Account account = accountDao.getAccountById(ACCOUNT_ID);

        assertNotNull("Account should not be null", account);
        assertEquals("Id should match", ACCOUNT_ID, account.getId());
        assertEquals("Name should match", "Acme Corp", account.getName());
        assertEquals("Type should match", "Customer", account.getType());
        verify(salesforceService).getRecordById(ACCOUNT_ID, ACCOUNT);
    }

    @Test
    public void testGetAccountById_NullId_HandlesGuard() throws Exception {
        // Null id enters the guard branch; the DAO still delegates to the service.
        Account account = accountDao.getAccountById(null);
        assertNotNull("A wrapper account is always returned", account);
        verify(salesforceService).getRecordById(null, ACCOUNT);
    }

    @Test
    public void testGetAccountById_BlankId_HandlesGuard() throws Exception {
        Account account = accountDao.getAccountById("   ");
        assertNotNull("A wrapper account is always returned", account);
        verify(salesforceService).getRecordById("   ", ACCOUNT);
    }

    // ==================== getAccountByQuery ====================

    @Test
    public void testGetAccountByQuery_ReturnsAccounts() throws Exception {
        String json = "{\"records\":[" +
                "{\"Id\":\"001AAA\",\"Name\":\"Acme Corp\"}," +
                "{\"Id\":\"001BBB\",\"Name\":\"Globex\"}]," +
                "\"totalSize\":2,\"done\":true}";
        when(salesforceService.getRecordByQuery(anyString())).thenReturn(json);

        List<Account> accounts = accountDao.getAccountByQuery("SELECT Id, Name FROM Account");

        assertNotNull("Accounts list should not be null", accounts);
        assertEquals("Should return two accounts", 2, accounts.size());
        assertEquals("First account id should match", "001AAA", accounts.get(0).getId());
        assertEquals("Second account name should match", "Globex", accounts.get(1).getName());
    }

    @Test
    public void testGetAccountByQuery_EmptyRecords_ReturnsEmptyList() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[],\"totalSize\":0,\"done\":true}");

        List<Account> accounts = accountDao.getAccountByQuery("SELECT Id FROM Account");

        assertNotNull("Accounts list should not be null", accounts);
        assertTrue("Accounts list should be empty", accounts.isEmpty());
    }

    @Test
    public void testGetAccountByQuery_BlankQuery_HandlesGuard() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[]}");

        List<Account> accounts = accountDao.getAccountByQuery("   ");

        assertNotNull("Accounts list should not be null", accounts);
        assertTrue("Accounts list should be empty", accounts.isEmpty());
    }

    // ==================== getAccounts (paginated) ====================

    @Test
    public void testGetAccounts_ReturnsAccounts() throws Exception {
        String json = "{\"records\":[{\"Id\":\"001AAA\",\"Name\":\"Acme Corp\"}],\"totalSize\":1,\"done\":true}";
        when(salesforceService.getRecordByQuery(anyString())).thenReturn(json);

        List<Account> accounts = accountDao.getAccounts(10, 1, "Name", "ASC");

        assertNotNull("Accounts list should not be null", accounts);
        assertEquals("Should return one account", 1, accounts.size());
        assertEquals("Account id should match", "001AAA", accounts.get(0).getId());
    }

    // ==================== createAccount ====================

    @Test
    public void testCreateAccount_ReturnsId() throws Exception {
        Map<String, Object> entity = new HashMap<>();
        entity.put("Name", "Acme Corp");
        when(salesforceService.createRecord(anyString(), eq(ACCOUNT)))
                .thenReturn("{\"id\":\"001NEWACCOUNT\",\"success\":true}");

        String id = accountDao.createAccount(entity);

        assertEquals("Created account id should match", "001NEWACCOUNT", id);
        verify(salesforceService).createRecord(anyString(), eq(ACCOUNT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAccount_NullMap_ThrowsIllegalArgument() throws Exception {
        accountDao.createAccount(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAccount_EmptyMap_ThrowsIllegalArgument() throws Exception {
        accountDao.createAccount(new HashMap<>());
    }

    // ==================== updateAccount ====================

    @Test
    public void testUpdateAccount_InvokesUpdateRecord() throws Exception {
        String json = "{\"Id\":\"" + ACCOUNT_ID + "\",\"Name\":\"Acme Corp\",\"Type\":\"Customer\",\"Phone\":\"555-0100\"}";
        when(salesforceService.getRecordById(ACCOUNT_ID, ACCOUNT)).thenReturn(json);

        // Build the update input by wrapping the SAME record the service returns, so the computed
        // diff is empty and updateAccount's only observable effect is the delegation to updateRecord.
        // (A bare Mockito mock returns primitive-default values for the Long/Date getters, which
        // SalesforceService.getDifference cannot stringify — a mock artifact, not a real-flow bug.)
        Account account = new AccountImpl(Constants.GSON.fromJson(json, SFAccount.class));

        accountDao.updateAccount(account);

        verify(salesforceService).updateRecord(eq(ACCOUNT_ID), anyString(), eq(ACCOUNT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateAccount_NullAccount_ThrowsIllegalArgument() throws Exception {
        accountDao.updateAccount(null);
    }

    // ==================== deleteAccountById ====================

    @Test
    public void testDeleteAccountById_InvokesDelete() throws Exception {
        accountDao.deleteAccountById(ACCOUNT_ID);

        verify(salesforceService).deleteRecordById(ACCOUNT_ID, ACCOUNT);
    }
}
