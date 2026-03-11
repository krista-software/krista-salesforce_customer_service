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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface AccountDao {

    /**
     * Returns {@link Account} object of salesforce from the account id provided.
     *
     * @param id id of account to be searched
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    Account getAccountById(String id) throws IOException, ExecutionException, InterruptedException;

    /**
     * Returns {@link List<Account>} list of object of salesforce.
     *
     * @param size           number of accounts to be fetched
     * @param page           page number to be fetched
     * @param orderByField   field name used for sorting the results
     * @param orderByOperand sorting direction, e.g., "ASC" or "DESC"
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    List<Account> getAccounts(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException;

    /**
     * Returns {@link List<Account>} list of object of salesforce.
     *
     * @param query query to be searched
     * @return empty list if it does not find any records
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    List<Account> getAccountByQuery(String query) throws IOException, ExecutionException, InterruptedException;

    /**
     * Add {@link Account} to salesforce from provided input fields
     *
     * @param entityDefinition fields which are added in the {@link Account} object of salesforce to create
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    String createAccount(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException;

    /**
     * Update {@link Account} to salesforce from provided input fields
     *
     * @param account {@link Account} object of salesforce to be updated
     * @throws IOException          if input error occurs
     * @throws ExecutionException   if error in execution flow
     * @throws InterruptedException if error in the thread
     */
    void updateAccount(Account account) throws IllegalAccessException, IOException, ExecutionException, InterruptedException;

    /**
     * This function is used to delete account from salesforce instance
     *
     * @param primaryKey key of account to be deleted
     */
    void deleteAccountById(String primaryKey) throws IOException, ExecutionException, InterruptedException;
}
