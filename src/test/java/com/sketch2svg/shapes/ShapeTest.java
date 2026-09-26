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
    void testSixDigitHexIsOpaque() {
        Circle circle = new Circle();
        circle.fill("#FF0000");
        circle.stroke("00ff00");

        assertEquals((int) 0xFF0000FFL, circle.getFill());
        assertEquals((int) 0x00FF00FFL, circle.getStroke());
        assertThrows(IllegalArgumentException.class, () -> circle.fill("F00"));
        assertThrows(IllegalArgumentException.class, () -> circle.fill("GG0000"));
    }

    @Test
    void testStarRadiiMatchConstructorOrder() {
        Star viaConstructor = new Star(5, 20.0f, 10.0f, 0.0f, 0.0f);
        Star viaSetter = new Star().radii(20.0f, 10.0f);
        assertEquals(viaConstructor.toString(), viaSetter.toString());
        // Tips are at the outer radius: the first vertex points straight up
        assertEquals(20.0f, viaSetter.getVertex(0, 1), 1e-5f);
    }

    @Test
    void testColorsAreRgbPlusOpacity() {
        Circle opaque = new Circle(5.0f, 0.0f, 0.0f);
        opaque.fill("ffdc7a").stroke("000000ff");
        assertTrue(opaque.toString().contains("style=\"fill:#FFDC7A;stroke-width:1;stroke:#000000\""), opaque.toString());

        // Translucent colours keep their alpha as a separate opacity (0x80 = 128/255)
        Circle translucent = new Circle(5.0f, 0.0f, 0.0f);
        translucent.fill("00ff0080").stroke("00000066");
        assertTrue(translucent.toString().contains(
                "style=\"fill:#00FF00;fill-opacity:0.502;stroke-width:1;stroke:#000000;stroke-opacity:0.4\""),
                translucent.toString());
    }

    @Test
    void testRoundRectCorners() {
        // 40x20 with radius 5: straight bottom edge from x=-15 to 15, then a quarter circle up to (20,-5)
        String svg = new RoundRect(40.0f, 20.0f, 5.0f, 0.0f, 0.0f).toString();
        assertTrue(svg.startsWith("<path"), svg);
        assertTrue(svg.contains("d=\"M -15 10 L 15 10 C 17.761 10 20 7.761 20 5 L 20 -5"), svg);
        assertTrue(svg.endsWith("Z\"/>"), svg);

        // The radius is capped at half the shorter side, so a huge radius makes a stadium, not a mess
        String capped = new RoundRect(40.0f, 20.0f, 100.0f, 0.0f, 0.0f).toString();
        assertTrue(capped.contains("d=\"M -10 10 L 10 10 C 15.523 10 20 5.523 20 0 L 20 0"), capped);
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

    @Test
    void testScaleMultipliesSize() {
        assertTrue(new Circle(10.0f, 0.0f, 0.0f).scale(2.0f).toString().contains("r=\"20\""));

        // Rect 100x50 scaled by 2 spans x in [-100, 100] and y in [-50, 50]
        float[] rect = pointExtents(new Rect(100.0f, 50.0f, 0.0f, 0.0f).scale(2.0f).toString());
        assertArrayEquals(new float[]{100.0f, 50.0f}, rect, 1e-4f);

        float[] square = pointExtents(new Square(10.0f, 0.0f, 0.0f).scale(3.0f).toString());
        assertArrayEquals(new float[]{15.0f, 15.0f}, square, 1e-4f);

        // Vertex-up triangle: top vertex at radius * scale
        float[] tri = pointExtents(new RegPolygon(3, 10.0f, 0.0f, 0.0f).scale(2.0f).toString());
        assertEquals(20.0f, tri[1], 1e-4f);

        assertTrue(new Arc(10.0f, 90.0f, 180.0f, 0.0f, 0.0f).scale(2.0f).toString().contains("A 20 20"));
    }

    @Test
    void testUnevenScaleMakesEllipse() {
        Circle even = new Circle(5.0f, 0.0f, 0.0f);
        even.scale(-2.0f, 2.0f);
        assertEquals("circle", even.getTag());
        assertTrue(even.toString().contains("r=\"10\""));
        assertFalse(even.toString().contains("rx="));

        Circle ellipse = new Circle(5.0f, 1.0f, 2.0f);
        ellipse.scale(2.0f, 1.0f).rotate(30.0f);
        String svg = ellipse.toString();
        assertTrue(svg.startsWith("<ellipse"), svg);
        assertTrue(svg.contains("rx=\"10\" ry=\"5\""), svg);
        assertTrue(svg.contains("transform=\"rotate(-30 1 -2)\""), svg);
        assertFalse(svg.contains(" r="), svg);
    }

    @Test
    void testArcFollowsUnevenAndMirroredScale() {
        Arc stretched = new Arc(10.0f, 90.0f, 180.0f, 0.0f, 0.0f);
        stretched.scale(2.0f, 1.0f);
        assertTrue(stretched.toString().contains("A 20 10 0 0 0"), stretched.toString());

        Arc rotated = new Arc(10.0f, 90.0f, 180.0f, 0.0f, 0.0f);
        rotated.scale(2.0f, 1.0f).rotate(30.0f);
        assertTrue(rotated.toString().contains("A 20 10 -30"), rotated.toString());

        // Mirroring across the y axis moves the 45deg arc's midpoint to 135deg
        Arc mirrored = new Arc(10.0f, 45.0f, 90.0f, 0.0f, 0.0f);
        mirrored.scale(-1.0f, 1.0f);
        double[] mid = arcMidpoint(mirrored.toString());
        assertEquals(10 * Math.cos(Math.toRadians(135)), mid[0], 1e-3);
        assertEquals(10 * Math.sin(Math.toRadians(135)), mid[1], 1e-3);
    }

    @Test
    void testTextScalesAroundAnchor() {
        Text text = new Text("hi", 5.0f, 10.0f, 12.0f);
        text.scale(2.0f, 1.0f);
        assertTrue(text.toString().contains(
                "transform=\"translate(5 -10) scale(2 1) translate(-5 10)\""), text.toString());
    }

    // Largest |x| and |y| among a polygon's points
    private static float[] pointExtents(String svg) {
        Matcher m = Pattern.compile("points=\"([^\"]*)\"").matcher(svg);
        assertTrue(m.find(), svg);
        float mx = 0, my = 0;
        for (String pair : m.group(1).trim().split(" ")) {
            String[] xy = pair.split(",");
            mx = Math.max(mx, Math.abs(Float.parseFloat(xy[0])));
            my = Math.max(my, Math.abs(Float.parseFloat(xy[1])));
        }
        return new float[]{mx, my};
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