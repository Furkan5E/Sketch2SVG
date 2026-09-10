package com.sketch2svg.parser;

import com.sketch2svg.core.Shape;
import com.sketch2svg.shapes.Circle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class SketchParserTest {

    @TempDir
    Path tempDir;

    @Test
    void testProgrammaticShapeAddition() {
        Sketch sketch = new Sketch();
        assertEquals(0, sketch.getShapes().size());

        sketch.add(new Circle(10, 0, 0));
        assertEquals(1, sketch.getShapes().size());
        assertEquals("circle", sketch.getShapes().get(0).getTag());
    }

    @Test
    void testGracefulHandlingOfNonExistentFile() {
        Sketch sketch = new Sketch();
        // Should log an error message without throwing an uncaught crash
        assertFalse(assertDoesNotThrow(() -> sketch.fromFile("non_existent_file.txt")));
        assertEquals(0, sketch.getShapes().size());
    }

    @Test
    void testParsingIsLocaleIndependent() throws IOException {
        Path input = tempDir.resolve("sketch.txt");
        Files.writeString(input, "CIRCLE 2.5 0 0\narc 10 90 180 0 0\n");

        Locale original = Locale.getDefault();
        try {
            // Turkish uses ',' for decimals and lowercases 'I' to a dotless 'ı'
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            Sketch sketch = new Sketch();
            sketch.fromFile(input.toString());

            assertEquals(2, sketch.getShapes().size());
            Circle circle = (Circle) sketch.getShapes().get(0);
            assertEquals(2.5f, circle.getRadius());
            assertFalse(sketch.getShapes().get(1).toString().contains(","));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void testRotationTokenAppliesToAnyShape() throws IOException {
        Sketch sketch = parse("""
                rect 10 20 0 0 rot=90 2 ff0000ff
                line 0 0 10 0 rot=90
                arrow 20 6 0 0 45 3
                arrow 20 6 0 0 2 ff0000ff rot=30
                arrow 20 6 0 0 2 ff0000ff
                """);

        assertEquals(5, sketch.getShapes().size());

        // 10x20 rect rotated 90deg becomes 20 wide, 10 tall; style tokens still apply around it
        Shape rect = sketch.getShapes().get(0);
        assertEquals(90f, rect.getRotation());
        assertEquals(2f, rect.getStrokeWidth());
        assertBounds(svgPoints(rect), -10, -5, 10, 5);

        // Line pivots around its midpoint (5,0), not the origin
        assertBounds(svgPoints(sketch.getShapes().get(1)), 5, -5, 5, 5);

        // Legacy "<rot> <strokeWidth>" arrow form still works
        assertEquals(45f, sketch.getShapes().get(2).getRotation());
        assertEquals(3f, sketch.getShapes().get(2).getStrokeWidth());
        assertEquals(30f, sketch.getShapes().get(3).getRotation());
        assertEquals(2f, sketch.getShapes().get(3).getStrokeWidth());
        // A lone number is the stroke width, not a rotation
        assertEquals(0f, sketch.getShapes().get(4).getRotation());
        assertEquals(2f, sketch.getShapes().get(4).getStrokeWidth());
    }

    @Test
    void testTextRotationEmitsTransform() throws IOException {
        Sketch sketch = parse("""
                text 5 10 12 "hi" rot=45
                text 0 0 12 "flat"
                """);

        assertTrue(sketch.getShapes().get(0).toString().contains("transform=\"rotate(-45.0 5.0 -10.0)\""));
        assertFalse(sketch.getShapes().get(1).toString().contains("transform"));
    }

    @Test
    void testInvalidRotationIsReportedAndSkipped() throws IOException {
        Sketch sketch = parse("rect 10 10 0 0 rot=abc\ncircle 5 0 0\n");

        assertEquals(1, sketch.getShapes().size());
        assertEquals("circle", sketch.getShapes().get(0).getTag());
    }

    @Test
    void testScriptColorForms() throws IOException {
        Sketch sketch = parse("""
                circle 5 0 0 2 ff0000 #00ff0080
                circle 5 0 0 2 #0000FF ffdc7aff
                """);

        assertEquals((int) 0xFF0000FFL, sketch.getShapes().get(0).getStroke());
        assertEquals((int) 0x00FF0080L, sketch.getShapes().get(0).getFill());
        assertEquals((int) 0x0000FFFFL, sketch.getShapes().get(1).getStroke());
        assertEquals((int) 0xFFDC7AFFL, sketch.getShapes().get(1).getFill());
    }

    @Test
    void testUnusedStyleArgumentsAreWarned() throws IOException {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Sketch sketch;
        try {
            System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
            sketch = parse("""
                    circle 5 0 0 2 ff00 000000ff
                    circle 5 0 0 2 3 NaN
                    circle 5 0 0 000000ff ff0000ff 00ff00ff
                    circle 5 0 0 4 000000ff # the moon
                    """);
        } finally {
            System.setErr(originalErr);
        }
        String log = err.toString(StandardCharsets.UTF_8);

        assertTrue(log.contains("Line 1: Ignored unrecognized argument 'ff00'"), log);
        assertTrue(log.contains("Line 2: Ignored unrecognized argument 'NaN'"), log);
        assertTrue(log.contains("Line 2: Ignored extra numbers [3.0]"), log);
        assertTrue(log.contains("Line 3: Ignored 1 extra color(s)"), log);
        assertFalse(log.contains("Line 4"), log);

        // Shapes are still drawn; the first stroke width wins
        assertEquals(4, sketch.getShapes().size());
        assertEquals(2f, sketch.getShapes().get(1).getStrokeWidth());
        assertEquals(4f, sketch.getShapes().get(3).getStrokeWidth());
    }

    @Test
    void testPolygonAndPolylineCommands() throws IOException {
        Sketch sketch = parse("""
                polygon 0,0 10,0 10,20 2 ff0000ff 00ff00ff
                polyline -5,0 5,0 rot=90
                polygon 0,0 10,0
                """);

        assertEquals(2, sketch.getShapes().size());

        Shape tri = sketch.getShapes().get(0);
        assertEquals("polygon", tri.getTag());
        assertEquals(2f, tri.getStrokeWidth());
        assertEquals((int) 0x00FF00FFL, tri.getFill());
        assertBounds(svgPoints(tri), 0, 0, 10, 20);

        // Open polyline rotates around its own centre
        Shape open = sketch.getShapes().get(1);
        assertEquals("polyline", open.getTag());
        assertBounds(svgPoints(open), 0, -5, 0, 5);
    }

    @Test
    void testNamedStyleArguments() throws IOException {
        Sketch sketch = parse("""
                circle 5 0 0 fill=ffdc7a stroke=000000ff stroke=3
                rect 10 20 0 0 1 ff0000ff 00ff00ff sw=4 fill=0000ffff
                square 10 0 0 at=5,-5 scale=2,1
                circle 5 0 0 fill=nope
                circle 5 0 0 colour=ff0000ff
                """);

        assertEquals(4, sketch.getShapes().size());

        Shape circle = sketch.getShapes().get(0);
        assertEquals((int) 0xFFDC7AFFL, circle.getFill());
        assertEquals((int) 0x000000FFL, circle.getStroke());
        assertEquals(3f, circle.getStrokeWidth());

        // Named arguments override positional ones
        Shape rect = sketch.getShapes().get(1);
        assertEquals(4f, rect.getStrokeWidth());
        assertEquals((int) 0xFF0000FFL, rect.getStroke());
        assertEquals((int) 0x0000FFFFL, rect.getFill());

        // at= moves the centre, scale= multiplies the size: 20x10 around (5,-5)
        assertBounds(svgPoints(sketch.getShapes().get(2)), -5, -10, 15, 0);
    }

    private Sketch parse(String source) throws IOException {
        Path input = tempDir.resolve("sketch.txt");
        Files.writeString(input, source);
        Sketch sketch = new Sketch();
        assertTrue(sketch.fromFile(input.toString()));
        return sketch;
    }

    // Reads the "points" attribute of a polygon/polyline, flipping y back to +y up
    private static float[][] svgPoints(Shape shape) {
        Matcher m = Pattern.compile("points=\"([^\"]*)\"").matcher(shape.toString());
        assertTrue(m.find());
        String[] pairs = m.group(1).trim().split(" ");
        float[][] pts = new float[pairs.length][];
        for (int i = 0; i < pairs.length; i++) {
            String[] xy = pairs[i].split(",");
            pts[i] = new float[]{Float.parseFloat(xy[0]), -Float.parseFloat(xy[1])};
        }
        return pts;
    }

    private static void assertBounds(float[][] pts, float minX, float minY, float maxX, float maxY) {
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (float[] p : pts) {
            x0 = Math.min(x0, p[0]);
            y0 = Math.min(y0, p[1]);
            x1 = Math.max(x1, p[0]);
            y1 = Math.max(y1, p[1]);
        }
        final float eps = 1e-4f;
        assertEquals(minX, x0, eps);
        assertEquals(minY, y0, eps);
        assertEquals(maxX, x1, eps);
        assertEquals(maxY, y1, eps);
    }
}
