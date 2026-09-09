package com.sketch2svg.shapes;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class ShapeTest {

    @Test
    void testCircleTagAndDefaults() {
        Circle circle = new Circle(15.0f, 10.0f, 20.0f);
        assertEquals("circle", circle.getTag());
        assertEquals(10.0f, circle.getPos().x);
        assertEquals(20.0f, circle.getPos().y);
    }

    @Test
    void testRectTagAndDimensions() {
        Rect rect = new Rect(100.0f, 50.0f, 0.0f, 0.0f);
        assertEquals("polygon", rect.getTag());
        assertEquals(4, rect.getNumVertices());
    }

    @Test
    void testColorIntAndHexStyling() {
        Circle circle = new Circle();
        circle.fill("FF0000FF");
        circle.stroke("00FF00FF");
        circle.strokeWidth(2.5f);

        assertEquals((int) 0xFF0000FFL, circle.getFill());
        assertEquals((int) 0x00FF00FFL, circle.getStroke());
        assertEquals(2.5f, circle.getStrokeWidth());
    }

    @Test
    void testStarVertexCount() {
        Star star = new Star(5, 20.0f, 10.0f, 0.0f, 0.0f);
        // 5 points * 2 (inner + outer alternating) = 10 vertices
        assertEquals(10, star.getNumVertices());
    }

    @Test
    void testTextContentIsXmlEscaped() throws Exception {
        Text text = new Text("Tom & Jerry <3 \"quoted\"", 0.0f, 0.0f, 10.0f);
        String xml = text.toString();

        assertTrue(xml.contains("Tom &amp; Jerry &lt;3 &quot;quoted&quot;"));
        // Must parse as well-formed XML and round-trip the original text
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        assertEquals("Tom & Jerry <3 \"quoted\"", doc.getDocumentElement().getTextContent().strip());
    }

    @Test
    void testArcAnglesAreCounterClockwise() {
        // {angle, length, rotation}: the drawn arc's midpoint must sit at angle + rotation, measured CCW
        float[][] cases = {{45, 90, 0}, {90, 180, 0}, {0, 270, 0}, {0, -90, 0}, {90, 60, 30}};
        for (float[] c : cases) {
            Arc arc = new Arc(10.0f, c[0], c[1], 5.0f, 5.0f);
            arc.setRotation(c[2]);

            double[] mid = arcMidpoint(arc.toString());
            double a = Math.toRadians(c[0] + c[2]);
            String label = "angle=" + c[0] + " length=" + c[1] + " rot=" + c[2];
            assertEquals(5 + 10 * Math.cos(a), mid[0], 1e-3, label);
            assertEquals(5 + 10 * Math.sin(a), mid[1], 1e-3, label);
        }
    }

    // Decodes "M x1 y1 A r r 0 large sweep x2 y2" using the SVG spec's centre parameterisation
    // (SVG 1.1 F.6.5) and returns the arc's midpoint in +y-up coordinates
    private static double[] arcMidpoint(String svg) {
        Matcher m = Pattern.compile("d=\"M (\\S+) (\\S+) A (\\S+) \\S+ 0 (\\d) (\\d) (\\S+) (\\S+)\"").matcher(svg);
        assertTrue(m.find(), svg);
        double x1 = Double.parseDouble(m.group(1)), y1 = Double.parseDouble(m.group(2));
        double r = Double.parseDouble(m.group(3));
        boolean large = m.group(4).equals("1"), sweep = m.group(5).equals("1");
        double x2 = Double.parseDouble(m.group(6)), y2 = Double.parseDouble(m.group(7));

        double hx = (x1 - x2) / 2, hy = (y1 - y2) / 2;
        double k = Math.sqrt(Math.max(0, (r * r - hx * hx - hy * hy) / (hx * hx + hy * hy)));
        if (large == sweep) k = -k;
        double cx = k * hy + (x1 + x2) / 2, cy = -k * hx + (y1 + y2) / 2;

        double t1 = Math.atan2(y1 - cy, x1 - cx);
        double dt = Math.atan2(y2 - cy, x2 - cx) - t1;
        if (sweep && dt < 0) dt += 2 * Math.PI;
        if (!sweep && dt > 0) dt -= 2 * Math.PI;

        double tm = t1 + dt / 2;
        return new double[]{cx + r * Math.cos(tm), -(cy + r * Math.sin(tm))};
    }
}