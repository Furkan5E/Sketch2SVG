package com.sketch2svg.parser;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExprTest {

    private static double eval(String src) {
        Map<String, Double> vars = Map.of("i", 3.0, "r", 10.0);
        return Expr.evaluate(src, vars::get);
    }

    @Test
    void testPrecedenceAndAssociativity() {
        assertEquals(-60 + 3 * 30, eval("-60 + i*30"));
        assertEquals(20, eval("(1 + 1) * r"));
        assertEquals(1, eval("10 % 3"));
        assertEquals(512, eval("2 ^ 3 ^ 2")); // right-associative: 2^(3^2)
        assertEquals(-9, eval("-i^2"));
        assertEquals(2.5, eval("r / 4"));
    }

    @Test
    void testFunctionsUseDegrees() {
        assertEquals(1, eval("sin(90)"), 1e-12);
        assertEquals(-10, eval("r * cos(180)"), 1e-12);
        assertEquals(Math.PI, eval("pi"));
        assertEquals(1, eval("min(i, 1, 2)"));
        assertEquals(10, eval("max(i, r)"));
        assertEquals(4, eval("round(3.5)"));
        assertEquals(3, eval("sqrt(9)"));
    }

    @Test
    void testErrorsAreReadable() {
        assertEquals("Unknown variable 'x'", assertThrows(IllegalArgumentException.class, () -> eval("x + 1")).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> eval("1 +")).getMessage().contains("Unexpected end"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> eval("(1 + 2")).getMessage().contains("Expected ')'"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> eval("1 / 0")).getMessage().contains("not a finite number"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> eval("foo(1)")).getMessage().contains("Unknown function"));
    }

    @Test
    void testFormattingKeepsIntegersIntegral() {
        assertEquals("5", Variables.format(5.0));
        assertEquals("-60", Variables.format(-60.0));
        assertEquals("2.5", Variables.format(2.5));
        assertEquals("0", Variables.format(-0.0));
        assertEquals("0.0001", Variables.format(1e-4));
    }
}
