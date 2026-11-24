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

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.service.Individual;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IndividualDaoImpl}.
 *
 * <p>The DAO delegates all remote interaction to {@link SalesforceService}, which is mocked here.
 * {@code getIndividualById} parses a single-record JSON payload, list operations parse Salesforce
 * list JSON, and create/update/delete are verified through their interaction with the service.
 * Unlike the other DAOs, the query/create/update guards throw checked exceptions, which the tests
 * assert directly.</p>
 */
public class IndividualDaoImplTest {

    private static final String INDIVIDUAL = "Individual";
    private static final String INDIVIDUAL_ID = "0PKXXXXXXXXXXXXXXX";

    private static final String INDIVIDUAL_JSON =
            "{\"Id\":\"" + INDIVIDUAL_ID + "\",\"FirstName\":\"John\",\"LastName\":\"Doe\"," +
                    "\"BirthDate\":\"1990-05-15\",\"CreatedById\":\"005XXX\"}";

    @Mock
    private SalesforceService salesforceService;

    private IndividualDaoImpl individualDao;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        individualDao = new IndividualDaoImpl(salesforceService);
    }

    // ==================== getIndividualById ====================

    @Test
    public void testGetIndividualById_ReturnsIndividual() throws Exception {
        when(salesforceService.getRecordById(INDIVIDUAL_ID, INDIVIDUAL)).thenReturn(INDIVIDUAL_JSON);

        Individual individual = individualDao.getIndividualById(INDIVIDUAL_ID);

        assertNotNull("Individual should not be null", individual);
        assertEquals("Id should match", INDIVIDUAL_ID, individual.getId());
        assertEquals("First name should match", "John", individual.getFirstName());
        assertEquals("Last name should match", "Doe", individual.getLastName());
        assertEquals("Created by id should match", "005XXX", individual.getCreatedById());
        assertNotNull("Birth date should be parsed", individual.getBirthDate());
        verify(salesforceService).getRecordById(INDIVIDUAL_ID, INDIVIDUAL);
    }

    // ==================== getIndividuals (paginated) ====================

    @Test
    public void testGetIndividuals_ReturnsIndividualsAndBuildsQuery() throws Exception {
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + INDIVIDUAL_JSON + "],\"totalSize\":1,\"done\":true}");

        List<Individual> individuals = individualDao.getIndividuals(15, 2, "LastName", "DESC");

        assertNotNull("Individuals list should not be null", individuals);
        assertEquals("Should return one individual", 1, individuals.size());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(salesforceService).getRecordByQuery(queryCaptor.capture());
        String query = queryCaptor.getValue();
        assertTrue("Query should sort", query.contains("ORDER BY LastName DESC"));
        assertTrue("Query should page", query.contains("LIMIT 15"));
        assertTrue("Query should offset", query.contains("OFFSET 15"));
    }

    // ==================== getIndividualsByQuery ====================

    @Test
    public void testGetIndividualsByQuery_ReturnsIndividuals() throws Exception {
        String second = "{\"Id\":\"0PKBBB\",\"FirstName\":\"Jane\",\"LastName\":\"Smith\"}";
        when(salesforceService.getRecordByQuery(anyString()))
                .thenReturn("{\"records\":[" + INDIVIDUAL_JSON + "," + second + "],\"totalSize\":2,\"done\":true}");

        List<Individual> individuals = individualDao.getIndividualsByQuery("SELECT Id FROM Individual");

        assertNotNull("Individuals list should not be null", individuals);
        assertEquals("Should return two individuals", 2, individuals.size());
        assertEquals("First individual id should match", INDIVIDUAL_ID, individuals.get(0).getId());
        assertEquals("Second individual first name should match", "Jane", individuals.get(1).getFirstName());
    }

    @Test
    public void testGetIndividualsByQuery_EmptyRecords_ReturnsEmptyList() throws Exception {
        when(salesforceService.getRecordByQuery(anyString())).thenReturn("{\"records\":[],\"totalSize\":0,\"done\":true}");

        List<Individual> individuals = individualDao.getIndividualsByQuery("SELECT Id FROM Individual");

        assertNotNull("Individuals list should not be null", individuals);
        assertTrue("Individuals list should be empty", individuals.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetIndividualsByQuery_NullQuery_ThrowsIllegalArgument() throws Exception {
        individualDao.getIndividualsByQuery(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetIndividualsByQuery_BlankQuery_ThrowsIllegalArgument() throws Exception {
        individualDao.getIndividualsByQuery("   ");
    }

    // ==================== createIndividual ====================

    @Test
    public void testCreateIndividual_ReturnsId() throws Exception {
        Map<String, Object> entity = new HashMap<>();
        entity.put("First Name", "Jane");
        entity.put("Last Name", "Smith");
        when(salesforceService.createRecord(anyString(), eq(INDIVIDUAL)))
                .thenReturn("{\"id\":\"0PKNEWINDIVIDUAL\",\"success\":true}");

        String id = individualDao.createIndividual(entity);

        assertEquals("Created individual id should match", "0PKNEWINDIVIDUAL", id);
        verify(salesforceService).createRecord(anyString(), eq(INDIVIDUAL));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateIndividual_NullMap_ThrowsIllegalArgument() throws Exception {
        individualDao.createIndividual(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateIndividual_EmptyMap_ThrowsIllegalArgument() throws Exception {
        individualDao.createIndividual(new HashMap<>());
    }

    // ==================== updateIndividual ====================

    @Test
    public void testUpdateIndividual_InvokesUpdateRecord() throws Exception {
        when(salesforceService.getRecordById(INDIVIDUAL_ID, INDIVIDUAL)).thenReturn(INDIVIDUAL_JSON);

        Individual individual = mock(Individual.class);
        when(individual.getId()).thenReturn(INDIVIDUAL_ID);
        when(individual.getFirstName()).thenReturn("Johnny");
        when(individual.getLastName()).thenReturn("Doe");
        when(individual.getBirthDate()).thenReturn(631152000000L);
        when(individual.getCreatedById()).thenReturn("005XXX");

        individualDao.updateIndividual(individual);

        verify(salesforceService).updateRecord(eq(INDIVIDUAL_ID), anyString(), eq(INDIVIDUAL));
    }

    @Test(expected = IllegalAccessException.class)
    public void testUpdateIndividual_NullIndividual_ThrowsIllegalAccess() throws Exception {
        individualDao.updateIndividual(null);
    }

    @Test(expected = IllegalAccessException.class)
    public void testUpdateIndividual_BlankId_ThrowsIllegalAccess() throws Exception {
        Individual individual = mock(Individual.class);
        when(individual.getId()).thenReturn("   ");

        individualDao.updateIndividual(individual);
    }

    // ==================== deleteIndividualById ====================

    @Test
    public void testDeleteIndividualById_InvokesDelete() throws Exception {
        individualDao.deleteIndividualById(INDIVIDUAL_ID);

        verify(salesforceService).deleteRecordById(INDIVIDUAL_ID, INDIVIDUAL);
    }
}
