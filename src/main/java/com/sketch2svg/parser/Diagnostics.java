package com.sketch2svg.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

// Collects script problems so callers (e.g. the CLI) can report them and tell whether a script is clean
final class Diagnostics {

    private final List<Diagnostic> found = new ArrayList<>();

    void reset() {
        found.clear();
    }

    List<Diagnostic> all() {
        return List.copyOf(found);
    }

    int errors() {
        return (int) found.stream().filter(Diagnostic::isError).count();
    }

    int warnings() {
        return found.size() - errors();
    }

    // Something was ignored, but the line still did its job
    void warning(String where, String message) {
        found.add(new Diagnostic(Diagnostic.Kind.WARNING, where, message));
    }

    // The line (or block) was skipped
    void error(String where, String message) {
        found.add(new Diagnostic(Diagnostic.Kind.ERROR, where, message));
    }

    void syntaxError(SourceLine line, NoSuchElementException e) {
        String reason = e.getMessage() != null ? " (" + e.getMessage() + ")" : "";
        found.add(new Diagnostic(Diagnostic.Kind.SYNTAX_ERROR, line.where(),
                "Invalid or missing parameters in '" + line.text() + "'" + reason));
    }
}
