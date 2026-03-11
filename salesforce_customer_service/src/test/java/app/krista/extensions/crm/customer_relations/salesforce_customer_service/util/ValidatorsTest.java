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

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for all public static methods in Validators utility class.
 * Tests include edge cases: null, empty, invalid inputs, and boundary values.
 * No Mockito used - only real concrete implementations.
 */
public class ValidatorsTest {

    // ==================== getDoubleOrZero() Tests ====================

    @Test
    public void testGetDoubleOrZero_WithValidDouble_ReturnsValue() {
        Double input = 5.5;
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return the double value", 5.5, result, 0.0);
    }

    @Test
    public void testGetDoubleOrZero_WithNull_ReturnsZero() {
        Double result = Validators.getDoubleOrZero(null);
        assertEquals("Should return 0 for null input", 0.0, result, 0.0);
    }

    @Test
    public void testGetDoubleOrZero_WithZero_ReturnsZero() {
        Double input = 0.0;
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return 0 for zero input", 0.0, result, 0.0);
    }

    @Test
    public void testGetDoubleOrZero_WithNegativeValue_ReturnsNegativeValue() {
        Double input = -10.5;
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return negative value", -10.5, result, 0.0);
    }

    @Test
    public void testGetDoubleOrZero_WithLargeValue_ReturnsLargeValue() {
        Double input = 999999.999999;
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return large value", 999999.999999, result, 0.000001);
    }

    @Test
    public void testGetDoubleOrZero_WithVerySmallValue_ReturnsSmallValue() {
        Double input = 0.000001;
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return very small value", 0.000001, result, 0.0000001);
    }

    @Test
    public void testGetDoubleOrZero_WithMaxValue_ReturnsMaxValue() {
        Double input = Double.MAX_VALUE;
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return max value", Double.MAX_VALUE, result, 0.0);
    }

    @Test
    public void testGetDoubleOrZero_WithMinValue_ReturnsMinValue() {
        Double input = Double.MIN_VALUE;
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return min value", Double.MIN_VALUE, result, 0.0);
    }

    @Test
    public void testGetDoubleOrZero_WithPositiveInfinity_ReturnsPositiveInfinity() {
        Double input = Double.POSITIVE_INFINITY;
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return positive infinity", Double.POSITIVE_INFINITY, result, 0.0);
    }

    @Test
    public void testGetDoubleOrZero_WithNegativeInfinity_ReturnsNegativeInfinity() {
        Double input = Double.NEGATIVE_INFINITY;
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return negative infinity", Double.NEGATIVE_INFINITY, result, 0.0);
    }

    @Test
    public void testGetDoubleOrZero_WithNaN_ReturnsNaN() {
        Double input = Double.NaN;
        double result = Validators.getDoubleOrZero(input);
        assertTrue("Should return NaN", Double.isNaN(result));
    }

    // ==================== getStringOrEmpty() Tests ====================

    @Test
    public void testGetStringOrEmpty_WithValidString_ReturnsString() {
        String input = "hello";
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return the string value", input, result);
    }

    @Test
    public void testGetStringOrEmpty_WithNull_ReturnsEmpty() {
        String result = Validators.getStringOrEmpty(null);
        assertEquals("Should return empty string for null input", "", result);
    }

    @Test
    public void testGetStringOrEmpty_WithEmptyString_ReturnsEmpty() {
        String input = "";
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return empty string for empty input", "", result);
    }

    @Test
    public void testGetStringOrEmpty_WithWhitespaceString_ReturnsWhitespace() {
        String input = "   ";
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return whitespace string as-is", "   ", result);
    }

    @Test
    public void testGetStringOrEmpty_WithSpecialCharacters_ReturnsSpecialCharacters() {
        String input = "!@#$%^&*()";
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return special characters as-is", "!@#$%^&*()", result);
    }

    @Test
    public void testGetStringOrEmpty_WithUnicodeCharacters_ReturnsUnicode() {
        String input = "Hello 世界 🌍";
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return unicode characters as-is", "Hello 世界 🌍", result);
    }

    @Test
    public void testGetStringOrEmpty_WithLongString_ReturnsLongString() {
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longString.append("A");
        }
        String input = longString.toString();
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return long string as-is", input, result);
    }

    @Test
    public void testGetStringOrEmpty_WithNewlineCharacters_ReturnsWithNewlines() {
        String input = "Line 1\nLine 2\rLine 3\r\nLine 4";
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string with newlines as-is", input, result);
    }

    @Test
    public void testGetStringOrEmpty_WithTabCharacters_ReturnsWithTabs() {
        String input = "Column1\tColumn2\tColumn3";
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string with tabs as-is", input, result);
    }

    // ==================== Type Conversion Tests ====================

    @Test
    public void testGetStringOrEmpty_WithIntegerObject_ReturnsStringRepresentation() {
        Integer input = 123;
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string representation of integer", "123", result);
    }

    @Test
    public void testGetStringOrEmpty_WithDoubleObject_ReturnsStringRepresentation() {
        Double input = 123.456;
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string representation of double", "123.456", result);
    }

    @Test
    public void testGetStringOrEmpty_WithBooleanObject_ReturnsStringRepresentation() {
        Boolean input = true;
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string representation of boolean", "true", result);
    }

    @Test
    public void testGetStringOrEmpty_WithObjectWithToString_ReturnsToStringResult() {
        Object input = new Object() {
            @Override
            public String toString() {
                return "Custom Object";
            }
        };
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return toString result", "Custom Object", result);
    }

    @Test
    public void testGetStringOrEmpty_WithZeroInteger_ReturnsZeroString() {
        Integer input = 0;
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string representation of zero", "0", result);
    }

    @Test
    public void testGetStringOrEmpty_WithNegativeInteger_ReturnsNegativeString() {
        Integer input = -42;
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string representation of negative integer", "-42", result);
    }

    @Test
    public void testGetStringOrEmpty_WithFalseBoolean_ReturnsFalseString() {
        Boolean input = false;
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string representation of false", "false", result);
    }

    @Test
    public void testGetStringOrEmpty_WithCharacterObject_ReturnsCharacterString() {
        Character input = 'A';
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string representation of character", "A", result);
    }

    @Test
    public void testGetStringOrEmpty_WithFloatObject_ReturnsFloatString() {
        Float input = 3.14f;
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string representation of float", "3.14", result);
    }

    @Test
    public void testGetStringOrEmpty_WithLongObject_ReturnsLongString() {
        Long input = 9876543210L;
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return string representation of long", "9876543210", result);
    }

    // ==================== Edge Case Tests ====================

    @Test
    public void testGetDoubleOrZero_WithClassCastException_ThrowsException() {
        try {
            // This should throw ClassCastException since we're passing a String where Double is expected
            Validators.getDoubleOrZero("not a double");
            fail("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // Expected behavior
            assertTrue("Should throw ClassCastException", true);
        }
    }

    @Test
    public void testGetStringOrEmpty_WithObjectThatThrowsInToString_HandlesGracefully() {
        Object input = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("toString failed");
            }
        };

        try {
            String result = Validators.getStringOrEmpty(input);
            fail("Should propagate the RuntimeException from toString");
        } catch (RuntimeException e) {
            assertEquals("Should propagate the exception message", "toString failed", e.getMessage());
        }
    }

    @Test
    public void testGetDoubleOrZero_WithDoubleZero_ReturnsZero() {
        Double input = Double.valueOf(0.0);
        double result = Validators.getDoubleOrZero(input);
        assertEquals("Should return 0 for Double.valueOf(0.0)", 0.0, result, 0.0);
    }

    @Test
    public void testGetStringOrEmpty_WithStringBuilder_ReturnsStringBuilderContent() {
        StringBuilder input = new StringBuilder("StringBuilder content");
        String result = Validators.getStringOrEmpty(input);
        assertEquals("Should return StringBuilder content", "StringBuilder content", result);
    }
}
