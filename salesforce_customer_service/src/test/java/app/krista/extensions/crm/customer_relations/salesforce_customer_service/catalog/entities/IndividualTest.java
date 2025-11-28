package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.entities;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Individual} entity class.
 */
public class IndividualTest {

    @Test
    public void testIndividualInstantiation() {
        Individual individual = new Individual();
        assertNotNull("Individual should not be null", individual);
    }

    @Test
    public void testIndividualFieldAssignment() {
        Individual individual = new Individual();
        individual.id = "IND-001";
        individual.firstName = "John";
        individual.lastName = "Doe";

        assertEquals("ID should match", "IND-001", individual.id);
        assertEquals("First name should match", "John", individual.firstName);
        assertEquals("Last name should match", "Doe", individual.lastName);
    }

    @Test
    public void testIndividualWithNullValues() {
        Individual individual = new Individual();
        individual.id = null;
        individual.firstName = null;

        assertNull("ID should be null", individual.id);
        assertNull("First name should be null", individual.firstName);
    }
}

