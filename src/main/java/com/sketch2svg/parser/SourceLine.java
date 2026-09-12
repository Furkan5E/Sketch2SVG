package com.sketch2svg.parser;

// A non-blank, non-comment script line and where it came from (file is null for the main sketch)
record SourceLine(String file, int number, String text) {

    // Location prefix for messages, e.g. "Line 4" or "Line 4 (parts/house.txt)"
    String where() {
        return file == null ? "Line " + number : "Line " + number + " (" + file + ")";
    }
}
