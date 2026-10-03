package com.sketch2svg.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class NumTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "20.0, 20",
            "3.3471675, 3.347",
            "-60.587784, -60.588",
            "0.5, 0.5",
            "1.0005, 1.001", // half up
            "1200, 1200"
    })
    void testRoundsAndTrims(double value, String expected) {
        assertEquals(expected, Num.format(value));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-8.742278E-8, -0.0, 0.0004})
    void testFloatNoiseBecomesZero(double value) {
        assertEquals("0", Num.format(value));
    }

    @Test
    void testRejectsNonFinite() {
        assertThrows(IllegalArgumentException.class, () -> Num.format(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> Num.format(Double.POSITIVE_INFINITY));
    }
}
