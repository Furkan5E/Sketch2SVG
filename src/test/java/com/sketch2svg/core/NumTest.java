package com.sketch2svg.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NumTest {

    @Test
    void testRoundsAndTrims() {
        assertEquals("20", Num.format(20.0f));
        assertEquals("3.347", Num.format(3.3471675f));
        assertEquals("-60.588", Num.format(-60.587784f));
        assertEquals("0.5", Num.format(0.5));
        assertEquals("1.001", Num.format(1.0005)); // half up
        assertEquals("1200", Num.format(1200));
    }

    @Test
    void testFloatNoiseBecomesZero() {
        assertEquals("0", Num.format(-8.742278E-8f));
        assertEquals("0", Num.format(-0.0f));
        assertEquals("0", Num.format(0.0004));
    }

    @Test
    void testRejectsNonFinite() {
        assertThrows(IllegalArgumentException.class, () -> Num.format(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> Num.format(Double.POSITIVE_INFINITY));
    }
}
