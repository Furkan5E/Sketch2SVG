package com.sketch2svg.parser;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

// The words of one script line, read left to right. The single place a line is split up:
// "quoted text" and {braced expressions} stay whole, so key="two words" is one token.
final class Tokens {

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");

    private final List<String> tokens;
    private int pos = 0;

    Tokens(List<String> tokens) {
        this.tokens = tokens;
    }

    // Splits on whitespace, keeping "quoted text" and {braced expressions} as single tokens
    static List<String> split(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        int braces = 0;
        for (char c : text.toCharArray()) {
            if (c == '"' && braces == 0) {
                inQuote = !inQuote;
            } else if (!inQuote && c == '{') {
                braces++;
            } else if (!inQuote && c == '}' && braces > 0) {
                braces--;
            }
            if (Character.isWhitespace(c) && !inQuote && braces == 0) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    // The text between a token's opening quote and the next one, or null if it isn't "quoted"
    static String quoted(String tok) {
        int close = tok.startsWith("\"") ? tok.indexOf('"', 1) : -1;
        return close < 0 ? null : tok.substring(1, close);
    }

    boolean hasNext() {
        return pos < tokens.size();
    }

    // Whether the whole next token matches
    boolean hasNext(Pattern pattern) {
        return hasNext() && pattern.matcher(tokens.get(pos)).matches();
    }

    boolean hasNext(String regex) {
        return hasNext() && tokens.get(pos).matches(regex);
    }

    // The next token without consuming it, or null at the end of the line
    String peek() {
        return hasNext() ? tokens.get(pos) : null;
    }

    // Throws NoSuchElementException at the end of the line, i.e. when a required parameter is missing
    String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return tokens.get(pos++);
    }

    int nextInt() {
        String tok = next();
        if (!INTEGER.matcher(tok).matches()) {
            throw new InputMismatchException();
        }
        try {
            return Integer.parseInt(tok);
        } catch (NumberFormatException e) {
            throw new InputMismatchException();
        }
    }

    // Everything not yet read, joined by single spaces
    String rest() {
        String rest = String.join(" ", tokens.subList(pos, tokens.size()));
        pos = tokens.size();
        return rest;
    }
}
