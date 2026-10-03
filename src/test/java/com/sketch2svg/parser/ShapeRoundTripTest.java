package com.sketch2svg.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

// Every drawing command, parsed from script text and read back from the SVG as XML
public class ShapeRoundTripTest {

    @TempDir
    Path tempDir;

    // script line -> expected element, and one attribute with its expected value (worked out by hand)
    static Stream<Arguments> cases() {
        return Stream.of(
                arguments("circle 5 10 20", "circle", "r", "5"),
                arguments("ellipse 8 4 0 0", "ellipse", "ry", "4"),
                arguments("arc 10 90 180 0 0", "path", "d", "M 10 0 A 10 10 0 0 0 -10 0"),
                arguments("line 0 0 10 5", "polyline", "points", "0,0 10,-5 "),
                arguments("rect 20 10 0 0", "polygon", "points", "-10,5 10,5 10,-5 -10,-5 "),
                arguments("roundrect 20 10 0 0 0", "path", "d", "M -10 5 L 10 5 C 10 5 10 5 10 5 L 10 -5 C 10 -5 10 -5 10 -5 L -10 -5 C -10 -5 -10 -5 -10 -5 L -10 5 C -10 5 -10 5 -10 5 Z"),
                arguments("square 4 1 1", "polygon", "points", "-1,1 3,1 3,-3 -1,-3 "),
                arguments("ngon 4 10 0 0", "polygon", "points", "0,-10 -10,0 0,10 10,0 "),
                arguments("trapezoid 10 20 4 0 0", "polygon", "points", "-5,-2 5,-2 10,2 -10,2 "),
                arguments("star 4 10 5 0 0", "polygon", "points", "0,-10 -3.536,-3.536 -10,0 -3.536,3.536 0,10 3.536,3.536 10,0 3.536,-3.536 "),
                arguments("arrow 20 4 0 0", "polygon", "points", "-10,2 3,2 3,4 10,0 3,-4 3,-2 -10,-2 "),
                arguments("text 1 2 10 \"Hi <there>\"", "text", "y", "-2"),
                arguments("polygon 0,0 10,0 10,10", "polygon", "points", "0,0 10,0 10,-10 "),
                arguments("polyline 0,0 5,5", "polyline", "points", "0,0 5,-5 "),
                arguments("path M 0,0 Q 5,10 10,0", "path", "d", "M 0 0 Q 5 -10 10 0")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void testCommandRoundTrips(String script, String tag, String attribute, String value) throws Exception {
        Path file = tempDir.resolve("case.txt");
        Files.writeString(file, script + "\n");
        Sketch sketch = new Sketch();
        assertTrue(sketch.fromFile(file.toString()));
        assertEquals(0, sketch.getErrorCount() + sketch.getWarningCount(), "reported problems");

        // Well-formed XML with exactly one shape inside <svg>
        Element svg = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(sketch.toSVGString()))).getDocumentElement();
        List<Element> shapes = new ArrayList<>();
        for (Node n = svg.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) shapes.add(e);
        }
        assertEquals(1, shapes.size());
        assertEquals(tag, shapes.get(0).getTagName());
        assertEquals(value, shapes.get(0).getAttribute(attribute));
    }

    @Test
    void testTextContentSurvivesEscaping() throws Exception {
        Path file = tempDir.resolve("text.txt");
        Files.writeString(file, "text 0 0 10 \"Tom & Jerry <3\"\n");
        Sketch sketch = new Sketch();
        sketch.fromFile(file.toString());
        Element svg = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(sketch.toSVGString()))).getDocumentElement();
        assertEquals("Tom & Jerry <3", svg.getElementsByTagName("text").item(0).getTextContent().strip());
    }
}
