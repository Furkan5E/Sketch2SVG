package com.sketch2svg.parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

// Collects script problems so callers (e.g. the CLI) can report them and tell whether a script is clean.
// The same problem on the same line (a bad line inside a loop) is kept once, with a count.
final class Diagnostics {

    // Each distinct problem in the order it first came up, with the number of times it was seen
    private final Map<Diagnostic, Integer> found = new LinkedHashMap<>();

    void reset() {
        found.clear();
    }

    List<Diagnostic> all() {
        return found.entrySet().stream()
                .map(e -> new Diagnostic(e.getKey().kind(), e.getKey().where(), e.getKey().message(), e.getValue()))
                .toList();
    }

    // Distinct problems: a bad line in a loop counts once, however often the loop ran
    int errors() {
        return (int) found.keySet().stream().filter(Diagnostic::isError).count();
    }

    int warnings() {
        return found.size() - errors();
    }

    // Something was ignored, but the line still did its job
    void warning(String where, String message) {
        add(Diagnostic.Kind.WARNING, where, message);
    }

    // The line (or block) was skipped
    void error(String where, String message) {
        add(Diagnostic.Kind.ERROR, where, message);
    }

    void syntaxError(SourceLine line, NoSuchElementException e) {
        String reason = e.getMessage() != null ? " (" + e.getMessage() + ")" : "";
        add(Diagnostic.Kind.SYNTAX_ERROR, line.where(), "Invalid or missing parameters in '" + line.text() + "'" + reason);
    }

    private void add(Diagnostic.Kind kind, String where, String message) {
        found.merge(new Diagnostic(kind, where, message, 1), 1, Integer::sum);
    }
}
