package com.sketch2svg.core;

// How an SVG document is laid out as text
public enum OutputStyle {
    // One element per line, no indentation
    DEFAULT,
    // One element per line, nested elements indented by two spaces
    PRETTY,
    // No line breaks at all, for the smallest files
    MINIFIED
}
