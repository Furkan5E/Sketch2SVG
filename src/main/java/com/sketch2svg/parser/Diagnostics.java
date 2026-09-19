package com.sketch2svg.parser;

import java.util.NoSuchElementException;

// Prints script problems to stderr and counts them, so callers (e.g. --check) can tell whether a script is clean
final class Diagnostics {

    private int errors;
    private int warnings;

    void reset() {
        errors = 0;
        warnings = 0;
    }

    int errors() {
        return errors;
    }

    int warnings() {
        return warnings;
    }

    // Something was ignored, but the line still did its job
    void warning(String where, String message) {
        warnings++;
        System.err.printf("[Warning] %s: %s%n", where, message);
    }

    // The line (or block) was skipped
    void error(String where, String message) {
        errors++;
        System.err.printf("[Error] %s: %s%n", where, message);
    }

    void syntaxError(SourceLine line, NoSuchElementException e) {
        errors++;
        String reason = e.getMessage() != null ? " (" + e.getMessage() + ")" : "";
        System.err.printf("[Syntax Error] %s: Invalid or missing parameters in '%s'%s%n", line.where(), line.text(), reason);
    }
}
