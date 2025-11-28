package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.model;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities.Individual;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SFIndividual} model class.
 */
public class SFIndividualTest {

    @Test
    public void testFrom_WithValidIndividual_ReturnsPopulatedSFIndividual() {
        Individual individual = new Individual();
        individual.id = "IND-001";
        individual.firstName = "John";
        individual.lastName = "Doe";

        SFIndividual result = SFIndividual.from(individual);

        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", individual.id, result.id);
        assertEquals("First name should match", individual.firstName, result.firstName);
        assertEquals("Last name should match", individual.lastName, result.lastName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullIndividual_ThrowsException() {
        SFIndividual.from((Individual) null);
    }

    @Test
    public void testFrom_WithValidMap_ReturnsPopulatedSFIndividual() {
        Map<String, Object> map = new HashMap<>();
        map.put("First Name", "John");
        map.put("Last Name", "Doe");

        SFIndividual result = SFIndividual.from(map);

        assertNotNull("Result should not be null", result);
        assertEquals("First name should match", "John", result.firstName);
        assertEquals("Last name should match", "Doe", result.lastName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithNullMap_ThrowsException() {
        SFIndividual.from((Map<String, Object>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrom_WithEmptyMap_ThrowsException() {
        SFIndividual.from(new HashMap<>());
    }
}

