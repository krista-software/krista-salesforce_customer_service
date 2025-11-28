package app.krista.extensions.crm.customer_relations.salesforce_customer_service.catalog.errorhandlers;

import app.krista.extensions.util.KeyValueStore;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ErrorHandlingStateManager}.
 */
public class ErrorHandlingStateManagerTest {

    private KeyValueStore mockKeyValueStore;
    private ErrorHandlingStateManager stateManager;

    @Before
    public void setUp() {
        mockKeyValueStore = mock(KeyValueStore.class);
        stateManager = new ErrorHandlingStateManager(mockKeyValueStore);
    }

    @Test
    public void testPut_WithValidKeyAndValue_CallsKeyValueStore() {
        String key = "testKey";
        Object value = "testValue";

        stateManager.put(key, value);

        verify(mockKeyValueStore, times(1)).put(key, value);
    }

    @Test
    public void testPut_WithMapValue_CallsKeyValueStore() {
        String key = "testKey";
        Map<String, Object> value = new HashMap<>();
        value.put("field1", "value1");

        stateManager.put(key, value);

        verify(mockKeyValueStore, times(1)).put(key, value);
    }

    @Test
    public void testConstructor_WithKeyValueStore_CreatesInstance() {
        ErrorHandlingStateManager manager = new ErrorHandlingStateManager(mockKeyValueStore);
        assertNotNull("Manager should not be null", manager);
    }
}

