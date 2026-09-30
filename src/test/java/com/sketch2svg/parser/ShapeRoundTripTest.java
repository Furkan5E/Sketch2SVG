package com.sketch2svg.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Every drawing command, parsed from script text and read back from the SVG as XML
public class ShapeRoundTripTest {

    @TempDir
    Path tempDir;

    // script line -> expected element, and one attribute with its expected value (worked out by hand)
    private record Case(String script, String tag, String attribute, String value) {}

    private static final List<Case> CASES = List.of(
            new Case("circle 5 10 20", "circle", "r", "5"),
            new Case("ellipse 8 4 0 0", "ellipse", "ry", "4"),
            new Case("arc 10 90 180 0 0", "path", "d", "M 10 0 A 10 10 0 0 0 -10 0"),
            new Case("line 0 0 10 5", "polyline", "points", "0,0 10,-5 "),
            new Case("rect 20 10 0 0", "polygon", "points", "-10,5 10,5 10,-5 -10,-5 "),
            new Case("roundrect 20 10 0 0 0", "path", "d", "M -10 5 L 10 5 C 10 5 10 5 10 5 L 10 -5 C 10 -5 10 -5 10 -5 L -10 -5 C -10 -5 -10 -5 -10 -5 L -10 5 C -10 5 -10 5 -10 5 Z"),
            new Case("square 4 1 1", "polygon", "points", "-1,1 3,1 3,-3 -1,-3 "),
            new Case("ngon 4 10 0 0", "polygon", "points", "0,-10 -10,0 0,10 10,0 "),
            new Case("trapezoid 10 20 4 0 0", "polygon", "points", "-5,-2 5,-2 10,2 -10,2 "),
            new Case("star 4 10 5 0 0", "polygon", "points", "0,-10 -3.536,-3.536 -10,0 -3.536,3.536 0,10 3.536,3.536 10,0 3.536,-3.536 "),
            new Case("arrow 20 4 0 0", "polygon", "points", "-10,2 3,2 3,4 10,0 3,-4 3,-2 -10,-2 "),
            new Case("text 1 2 10 \"Hi <there>\"", "text", "y", "-2"),
            new Case("polygon 0,0 10,0 10,10", "polygon", "points", "0,0 10,0 10,-10 "),
            new Case("polyline 0,0 5,5", "polyline", "points", "0,0 5,-5 "),
            new Case("path M 0,0 Q 5,10 10,0", "path", "d", "M 0 0 Q 5 -10 10 0")
    );

    @Test
    void testEveryCommandRoundTrips() throws Exception {
        List<String> failures = new ArrayList<>();
        for (Case c : CASES) {
            Path file = tempDir.resolve("case.txt");
            Files.writeString(file, c.script() + "\n");
            Sketch sketch = new Sketch();
            assertTrue(sketch.fromFile(file.toString()));

            if (sketch.getErrorCount() + sketch.getWarningCount() != 0) {
                failures.add(c.script() + ": reported problems");
                continue;
            }
            // Well-formed XML with exactly one shape inside <svg>
            Element svg = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(sketch.toSVGString()))).getDocumentElement();
            List<Element> shapes = new ArrayList<>();
            for (Node n = svg.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element e) shapes.add(e);
            }
            if (shapes.size() != 1) {
                failures.add(c.script() + ": expected one element, got " + shapes.size());
                continue;
            }
            Element shape = shapes.get(0);
            if (!shape.getTagName().equals(c.tag())) {
                failures.add(c.script() + ": tag " + shape.getTagName() + " != " + c.tag());
            }
            if (!shape.getAttribute(c.attribute()).equals(c.value())) {
                failures.add(c.script() + ": " + c.attribute() + "=\"" + shape.getAttribute(c.attribute()) + "\" != \"" + c.value() + "\"");
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
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
