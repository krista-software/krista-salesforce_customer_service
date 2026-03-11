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

