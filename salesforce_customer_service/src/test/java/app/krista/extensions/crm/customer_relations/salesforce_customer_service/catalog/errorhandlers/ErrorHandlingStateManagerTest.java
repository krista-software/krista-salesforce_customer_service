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

