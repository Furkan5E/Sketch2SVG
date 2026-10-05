package com.sketch2svg.parser;

// One problem found while reading a script. `where` locates it, e.g. "Line 5" or "Line 2 (roof.txt)",
// and is null for problems with the file as a whole. `count` is how often the same problem came up
// (more than once when the line sits inside a repeat loop).
public record Diagnostic(Kind kind, String where, String message, int count) {

    public enum Kind {
        WARNING("Warning"),          // something was ignored, but the line still did its job
        ERROR("Error"),              // the line (or block, or file) was skipped
        SYNTAX_ERROR("Syntax Error");

        private final String label;

        Kind(String label) {
            this.label = label;
        }
    }

    public boolean isError() {
        return kind != Kind.WARNING;
    }

    // The line a command-line tool would print, e.g. "[Warning] Line 5: Ignored unrecognized argument 'x'"
    @Override
    public String toString() {
        return "[" + kind.label + "] " + (where != null ? where + ": " : "") + message
                + (count > 1 ? " (" + count + " times)" : "");
    }
}
