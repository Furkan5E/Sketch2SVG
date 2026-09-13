package com.sketch2svg.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

// Recursive-descent evaluator for arithmetic inside {braces}:
//   + - * / % ^ (right-associative), unary minus, parentheses, numbers, variables,
//   pi, and sin cos tan (degrees) sqrt abs floor ceil round min max
final class Expr {

    private final String src;
    private final Function<String, Double> variables;
    private int pos = 0;

    private Expr(String src, Function<String, Double> variables) {
        this.src = src;
        this.variables = variables;
    }

    // variables returns a variable's numeric value, or null if it is undefined.
    // Throws IllegalArgumentException with a readable message on any error.
    static double evaluate(String src, Function<String, Double> variables) {
        Expr e = new Expr(src, variables);
        double value = e.expression();
        e.skipSpaces();
        if (e.pos < src.length()) {
            throw e.error("Unexpected '" + src.charAt(e.pos) + "'");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Expression {" + src + "} is not a finite number");
        }
        return value;
    }

    private double expression() {
        double value = term();
        while (true) {
            if (eat('+')) value += term();
            else if (eat('-')) value -= term();
            else return value;
        }
    }

    private double term() {
        double value = unary();
        while (true) {
            if (eat('*')) value *= unary();
            else if (eat('/')) value /= unary();
            else if (eat('%')) value %= unary();
            else return value;
        }
    }

    private double unary() {
        if (eat('-')) return -unary();
        if (eat('+')) return unary();
        return power();
    }

    private double power() {
        double base = primary();
        return eat('^') ? Math.pow(base, unary()) : base;
    }

    private double primary() {
        skipSpaces();
        if (eat('(')) {
            double value = expression();
            expect(')');
            return value;
        }
        if (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            return number();
        }
        if (pos < src.length() && isIdentStart(src.charAt(pos))) {
            String name = identifier();
            if (eat('(')) {
                return call(name, arguments());
            }
            if (name.equalsIgnoreCase("pi")) {
                return Math.PI;
            }
            Double value = variables.apply(name);
            if (value == null) {
                throw new IllegalArgumentException("Unknown variable '" + name + "'");
            }
            return value;
        }
        throw error(pos < src.length() ? "Unexpected '" + src.charAt(pos) + "'" : "Unexpected end of expression");
    }

    private List<Double> arguments() {
        List<Double> args = new ArrayList<>();
        if (eat(')')) {
            return args;
        }
        do {
            args.add(expression());
        } while (eat(','));
        expect(')');
        return args;
    }

    private static double call(String name, List<Double> args) {
        String fn = name.toLowerCase(Locale.ROOT);
        switch (fn) {
            case "min", "max" -> {
                if (args.isEmpty()) throw new IllegalArgumentException(fn + "() needs at least one argument");
                double result = args.get(0);
                for (double a : args) result = fn.equals("min") ? Math.min(result, a) : Math.max(result, a);
                return result;
            }
            default -> {
                if (args.size() != 1) throw new IllegalArgumentException(fn + "() takes one argument");
                double x = args.get(0);
                return switch (fn) {
                    case "sin" -> Math.sin(Math.toRadians(x));
                    case "cos" -> Math.cos(Math.toRadians(x));
                    case "tan" -> Math.tan(Math.toRadians(x));
                    case "sqrt" -> Math.sqrt(x);
                    case "abs" -> Math.abs(x);
                    case "floor" -> Math.floor(x);
                    case "ceil" -> Math.ceil(x);
                    case "round" -> Math.round(x);
                    default -> throw new IllegalArgumentException("Unknown function '" + name + "'");
                };
            }
        }
    }

    private double number() {
        int start = pos;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
        try {
            return Double.parseDouble(src.substring(start, pos));
        } catch (NumberFormatException e) {
            throw error("Invalid number '" + src.substring(start, pos) + "'");
        }
    }

    private String identifier() {
        int start = pos;
        while (pos < src.length() && (isIdentStart(src.charAt(pos)) || Character.isDigit(src.charAt(pos)))) pos++;
        return src.substring(start, pos);
    }

    static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean eat(char c) {
        skipSpaces();
        if (pos < src.length() && src.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect(char c) {
        if (!eat(c)) throw error("Expected '" + c + "'");
    }

    private void skipSpaces() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " in {" + src + "}");
    }
}
