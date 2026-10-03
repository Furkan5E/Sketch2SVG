package com.sketch2svg.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExprTest {

    private static double eval(String src) {
        Map<String, Double> vars = Map.of("i", 3.0, "r", 10.0);
        return Expr.evaluate(src, vars::get);
    }

    // i = 3 and r = 10; ^ is right-associative, and sin/cos work in degrees
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '|', textBlock = """
            -60 + i*30    | 30
            (1 + 1) * r   | 20
            10 % 3        | 1
            2 ^ 3 ^ 2     | 512
            -i^2          | -9
            r / 4         | 2.5
            sin(90)       | 1
            r * cos(180)  | -10
            min(i, 1, 2)  | 1
            max(i, r)     | 10
            round(3.5)    | 4
            sqrt(9)       | 3
            """)
    void testEvaluates(String expression, double expected) {
        assertEquals(expected, eval(expression), 1e-12);
    }

    @Test
    void testPiIsAConstant() {
        assertEquals(Math.PI, eval("pi"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
            x + 1    | Unknown variable
            1 +      | Unexpected end
            (1 + 2   | Expected ')'
            1 / 0    | not a finite number
            foo(1)   | Unknown function
            """)
    void testErrorsAreReadable(String expression, String message) {
        String actual = assertThrows(IllegalArgumentException.class, () -> eval(expression)).getMessage();
        assertTrue(actual.contains(message), actual);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"5.0, 5", "-60.0, -60", "2.5, 2.5", "-0.0, 0", "1e-4, 0.0001"})
    void testFormattingKeepsIntegersIntegral(double value, String expected) {
        assertEquals(expected, Variables.format(value));
    }
}
