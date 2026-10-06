package com.sketch2svg.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Script variables ("set name value") and their substitution into command lines
final class Variables {

    static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final Map<String, String> values = new HashMap<>();

    void set(String name, String value) {
        values.put(name, value);
    }

    String get(String name) {
        return values.get(name);
    }

    // Removes a variable, or restores its previous value (used to scope loop indices)
    void restore(String name, String previous) {
        if (previous == null) values.remove(name);
        else values.put(name, previous);
    }

    void clear() {
        values.clear();
    }

    // Expands a whole line's tokens; the first one (the command) is never substituted
    Tokens substituteAll(List<String> tokens) {
        List<String> out = new ArrayList<>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            out.add(i == 0 ? tokens.get(i) : substitute(tokens.get(i)));
        }
        return new Tokens(out);
    }

    // Expands one token:
    //   "quoted text"   left as is
    //   name            replaced by the variable's value when defined (otherwise e.g. a color name)
    //   key=name        the value part is replaced the same way
    //   {expression}    evaluated anywhere inside the token, e.g. {i*10},0 or rot={a/2}
    String substitute(String tok) {
        if (tok.startsWith("\"")) {
            return tok;
        }
        if (NAME.matcher(tok).matches() && values.containsKey(tok)) {
            return values.get(tok);
        }
        int eq = tok.indexOf('=');
        if (eq > 0 && tok.indexOf('{') < 0) {
            String value = tok.substring(eq + 1);
            if (NAME.matcher(value).matches() && values.containsKey(value)) {
                return tok.substring(0, eq + 1) + values.get(value);
            }
        }
        return expandBraces(tok);
    }

    private String expandBraces(String tok) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < tok.length()) {
            int open = tok.indexOf('{', i);
            if (open < 0) {
                out.append(tok, i, tok.length());
                break;
            }
            int close = tok.indexOf('}', open);
            if (close < 0) {
                throw new InputMismatchException("Missing '}' in " + tok);
            }
            out.append(tok, i, open);
            out.append(format(evaluate(tok.substring(open + 1, close))));
            i = close + 1;
        }
        if (out.indexOf("}") >= 0) {
            throw new InputMismatchException("Unexpected '}' in " + tok);
        }
        return out.toString();
    }

    double evaluate(String expression) {
        try {
            return Expr.evaluate(expression, this::numberValue);
        } catch (IllegalArgumentException e) {
            throw new InputMismatchException(e.getMessage());
        }
    }

    private Double numberValue(String name) {
        String value = values.get(name);
        if (value == null) {
            return null;
        }
        if (!StyleArgs.NUMBER.matcher(value).matches()) {
            throw new IllegalArgumentException("Variable '" + name + "' is not a number (" + value + ")");
        }
        return Double.parseDouble(value);
    }

    // Plain decimal without exponent or trailing zeros, so integers stay integers ("5", not "5.0")
    static String format(double value) {
        if (value == 0) {
            return "0"; // also avoids "-0"
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
