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

public interface IndividualDao {

    Individual getIndividualById(String id) throws IOException, ExecutionException, InterruptedException;

    List<Individual> getIndividuals(long size, long page, String orderByField, String orderByOperand) throws IOException, ExecutionException, InterruptedException;

    List<Individual> getIndividualsByQuery(String query) throws IOException, ExecutionException, InterruptedException;

    String createIndividual(Map<String, Object> entityDefinition) throws IOException, ExecutionException, InterruptedException;

    void updateIndividual(Individual individual) throws IOException, ExecutionException, InterruptedException, IllegalAccessException;

    void deleteIndividualById(String primaryKey) throws IOException, ExecutionException, InterruptedException;

}
