package com.sketch2svg.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ViewBoxTest {

    @Test
    void testFitAddsPadding() {
        ViewBox vb = new ViewBox();
        vb.fit(-10, -5, 30, 15, 2);
        assertEquals("-12 -7 44 24", vb.toString());
    }

    @Test
    void testFitRejectsEmptyBounds() {
        ViewBox vb = new ViewBox();
        // The bounds of "no shapes" are +/-infinity; they must not become a broken viewBox
        assertThrows(IllegalArgumentException.class,
                () -> vb.fit(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                        Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 0));
        assertEquals("-100 -100 200 200", vb.toString());
    }
}
