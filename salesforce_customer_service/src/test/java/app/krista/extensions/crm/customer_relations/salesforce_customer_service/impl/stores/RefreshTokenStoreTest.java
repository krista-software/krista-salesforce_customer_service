package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores;

import app.krista.extensions.util.KeyValueStore;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshTokenStore}.
 */
public class RefreshTokenStoreTest {

    private KeyValueStore mockKeyValueStore;
    private RefreshTokenStore refreshTokenStore;

    @Before
    public void setUp() {
        mockKeyValueStore = mock(KeyValueStore.class);
        refreshTokenStore = new RefreshTokenStore(mockKeyValueStore);
    }

    @Test
    public void testPut_WithValidKeyAndToken_CallsKeyValueStore() {
        String key = "testKey";
        String token = "testToken";

        refreshTokenStore.put(key, token);

        verify(mockKeyValueStore, times(1)).put(key, token);
    }

    @Test
    public void testGet_WithValidKey_ReturnsToken() {
        String key = "testKey";
        String expectedToken = "testToken";
        when(mockKeyValueStore.get(key, String.class)).thenReturn(expectedToken);

        String result = refreshTokenStore.get(key);

        assertEquals("Should return the token", expectedToken, result);
        verify(mockKeyValueStore, times(1)).get(key, String.class);
    }

    @Test
    public void testGet_WithNonExistentKey_ReturnsNull() {
        String key = "nonExistentKey";
        when(mockKeyValueStore.get(key, String.class)).thenReturn(null);

        String result = refreshTokenStore.get(key);

        assertNull("Should return null for non-existent key", result);
        verify(mockKeyValueStore, times(1)).get(key, String.class);
    }

    @Test
    public void testConstructor_WithKeyValueStore_CreatesInstance() {
        RefreshTokenStore store = new RefreshTokenStore(mockKeyValueStore);
        assertNotNull("Store should not be null", store);
    }
}

