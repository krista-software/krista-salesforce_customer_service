package app.krista.extensions.crm.customer_relations.salesforce_customer_service.api;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SalesforceCustomerRelationsApplication}.
 */
public class SalesforceCustomerRelationsApplicationTest {

    @Test
    public void testGetClasses_ReturnsAuthenticationResource() {
        SalesforceCustomerRelationsApplication app = new SalesforceCustomerRelationsApplication();
        
        Set<Class<?>> classes = app.getClasses();
        
        assertNotNull("Classes should not be null", classes);
        assertEquals("Should have one class", 1, classes.size());
        assertTrue("Should contain AuthenticationResource", 
            classes.contains(AuthenticationResource.class));
    }

    @Test
    public void testApplicationInstantiation() {
        SalesforceCustomerRelationsApplication app = new SalesforceCustomerRelationsApplication();
        assertNotNull("Application should not be null", app);
    }
}

