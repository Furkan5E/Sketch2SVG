package com.sketch2svg.parser;

import com.sketch2svg.core.Shape;
import com.sketch2svg.shapes.Circle;
import com.sketch2svg.shapes.Group;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    @Test
    void testShorthandAndNamedColors() throws IOException {
        Sketch sketch = parse("""
                circle 5 0 0 1 #f00 #0f08
                circle 5 0 0 fill=Gold stroke=none
                rect 10 10 0 0 2 black white
                circle 5 0 0 100 000
                """);

        assertEquals((int) 0xFF0000FFL, sketch.getShapes().get(0).getStroke());
        assertEquals((int) 0x00FF0088L, sketch.getShapes().get(0).getFill());
        assertEquals((int) 0xFFD700FFL, sketch.getShapes().get(1).getFill());
        assertEquals(0, sketch.getShapes().get(1).getStroke());
        assertFalse(sketch.getShapes().get(1).toString().contains("stroke:"));
        assertEquals((int) 0x000000FFL, sketch.getShapes().get(2).getStroke());
        assertEquals((int) 0xFFFFFFFFL, sketch.getShapes().get(2).getFill());
        // Without '#', short digit runs stay numbers
        assertEquals(100f, sketch.getShapes().get(3).getStrokeWidth());
    }

    @Test
    void testBackgroundCoversCanvasBehindShapes() throws IOException {
        Sketch sketch = parse("""
                circle 5 0 0
                background #0b1020
                """);
        assertEquals((int) 0x0B1020FFL, sketch.getBackground());
        assertEquals(1, sketch.getShapes().size());

        Path output = tempDir.resolve("out.svg");
        assertTrue(sketch.exportSVG(output.toString()));
        String svg = Files.readString(output);

        // Drawn first, covering the default -100..100 viewBox, even though it came after the circle
        int bg = svg.indexOf("fill:#0B1020FF");
        assertTrue(bg > 0 && bg < svg.indexOf("<circle"), svg);
        assertTrue(svg.contains("points=\"-100.0,100.0 100.0,100.0 100.0,-100.0 -100.0,-100.0 \""), svg);
    }

    @Test
    void testIncludeInlinesOtherFiles() throws IOException {
        Files.createDirectories(tempDir.resolve("parts"));
        // Paths resolve relative to the including file, so house.txt finds roof.txt next to it
        Files.writeString(tempDir.resolve("parts/house.txt"), "rect 10 10 0 0\ninclude roof.txt\n");
        Files.writeString(tempDir.resolve("parts/roof.txt"), "trapezoid 5 10 4 0 7\ncircle nope\n");
        Files.writeString(tempDir.resolve("loop.txt"), "include loop.txt\n");

        Sketch[] result = new Sketch[1];
        String log = stderrOf(() -> result[0] = parse("""
                circle 5 0 0
                include parts/house.txt   # the house
                include "missing.txt"
                include loop.txt
                square 2 0 0
                """));
        Sketch sketch = result[0];

        List<String> tags = sketch.getShapes().stream().map(Shape::getTag).toList();
        assertEquals(List.of("circle", "polygon", "polygon", "polygon"), tags);
        assertTrue(log.contains("Line 2 (roof.txt): Invalid or missing parameters in 'circle nope'"), log);
        assertTrue(log.contains("[Error] Line 3: Included file not found: missing.txt"), log);
        assertTrue(log.contains("[Error] Line 1 (loop.txt): Circular include of loop.txt"), log);
    }

    @Test
    void testVariablesAndExpressions() throws IOException {
        Sketch[] result = new Sketch[1];
        String log = stderrOf(() -> result[0] = parse("""
                set gold ffd700ff
                set r 5
                set d {r * 2}
                set label "Hello World"
                star {r} 2 1 {-60 + d*3} {d} 0 none gold
                circle r 0 0 stroke=gold rot={45/2}
                polygon 0,0 {d},0 {d},{d}
                text 0 0 12 label
                circle 5 0 0 {x + 1}
                set 9lives 1
                """));
        Sketch sketch = result[0];

        assertEquals(4, sketch.getShapes().size());

        // Integral results stay integers, so star's <points> still parses as an int
        Shape star = sketch.getShapes().get(0);
        assertEquals(-30f, star.getPos().x);
        assertEquals(10f, star.getPos().y);
        assertEquals((int) 0xFFD700FFL, star.getFill());

        Circle circle = (Circle) sketch.getShapes().get(1);
        assertEquals(5f, circle.getRadius());
        assertEquals((int) 0xFFD700FFL, circle.getStroke());
        assertEquals(22.5f, circle.getRotation());

        assertBounds(svgPoints(sketch.getShapes().get(2)), 0, 0, 10, 10);
        assertTrue(sketch.getShapes().get(3).toString().contains(">\nHello World\n<"));

        assertTrue(log.contains("Line 9: Invalid or missing parameters in 'circle 5 0 0 {x + 1}' (Unknown variable 'x')"), log);
        assertTrue(log.contains("Line 10: Invalid or missing parameters in 'set 9lives 1' (Invalid variable name '9lives')"), log);
    }

    @Test
    void testRepeatLoops() throws IOException {
        Sketch[] result = new Sketch[1];
        String log = stderrOf(() -> result[0] = parse("""
                set i outer
                set n 3
                repeat n i
                  repeat 2 j
                    circle 1 {i*10} {j*10}
                  end
                end
                circle 1 0 0 fill=i
                repeat 2
                  square 1 0 0
                end
                repeat 0 k
                  square 99 0 0
                end
                repeat -1
                  square 99 0 0
                end
                end
                """));
        Sketch sketch = result[0];

        // 3x2 grid of circles, then the index is restored to its old value
        List<String> positions = sketch.getShapes().subList(0, 6).stream()
                .map(s -> s.getPos().x + "," + s.getPos().y).toList();
        assertEquals(List.of("0.0,0.0", "0.0,10.0", "10.0,0.0", "10.0,10.0", "20.0,0.0", "20.0,10.0"), positions);
        assertTrue(log.contains("Line 8: Invalid or missing parameters in 'circle 1 0 0 fill=i' (Invalid color in fill=outer)"), log);

        // 6 circles + 2 squares: repeat 0 and the invalid count draw nothing
        assertEquals(8, sketch.getShapes().size());
        assertTrue(log.contains("Line 15: Invalid or missing parameters in 'repeat -1' (repeat count must be a whole number"), log);
        assertTrue(log.contains("[Error] Line 18: end without matching repeat"), log);
        assertFalse(log.contains("square 99"), log);
    }

    @Test
    void testRepeatWithoutEndRunsToEndOfFile() throws IOException {
        Sketch[] result = new Sketch[1];
        String log = stderrOf(() -> result[0] = parse("""
                repeat 3
                  circle 1 0 0
                """));
        assertEquals(3, result[0].getShapes().size());
        assertTrue(log.contains("[Error] Line 1: repeat without matching end"), log);
    }

    @Test
    void testGroupsNestAndShareStyle() throws IOException {
        Sketch sketch = parse("""
                group at=30,-40 rot=10 stroke=2 fill=gold
                  rect 42 28 0 0
                  square 8 12 0 fill=red
                  group scale=2 stroke=blue
                    repeat 2
                      circle 1 0 0
                    end
                  end
                end
                circle 1 0 0
                """);

        assertEquals(2, sketch.getShapes().size());
        Group outer = (Group) sketch.getShapes().get(0);
        String svg = outer.toString();
        assertTrue(svg.startsWith("<g transform=\"translate(30.0 40.0) rotate(-10.0)\">"), svg);
        assertFalse(svg.startsWith("<g style"), svg);
        assertEquals(3, outer.getChildren().size());

        // Group paint is a default; the shape's own arguments win
        Shape rect = outer.getChildren().get(0);
        assertEquals(2f, rect.getStrokeWidth());
        assertEquals((int) 0xFFD700FFL, rect.getFill());
        assertEquals((int) 0xFF0000FFL, outer.getChildren().get(1).getFill());

        // Nested group: inner stroke colour, outer fill and width
        Group inner = (Group) outer.getChildren().get(2);
        assertTrue(inner.toString().startsWith("<g transform=\"scale(2.0 2.0)\">"));
        assertEquals(2, inner.getChildren().size());
        Shape dot = inner.getChildren().get(0);
        assertEquals((int) 0x0000FFFFL, dot.getStroke());
        assertEquals((int) 0xFFD700FFL, dot.getFill());
        assertEquals(2f, dot.getStrokeWidth());

        // Shapes after the group are back at the top level with default paint
        assertEquals(0, sketch.getShapes().get(1).getFill());
    }

    @Test
    void testGroupTransformMatchesDirectTransform() throws IOException {
        // Child at (5,0) in a group moved to (30,-40) and turned 90deg lands at (30,-35), turned 90deg
        Sketch sketch = parse("""
                group at=30,-40 rot=90
                  rect 10 20 5 0
                end
                rect 10 20 30 -35 rot=90
                """);
        Group group = (Group) sketch.getShapes().get(0);
        assertTrue(group.toString().contains("transform=\"translate(30.0 40.0) rotate(-90.0)\""));

        // Apply the group's SVG transform by hand: rotate(-90) maps (x,y) -> (y,-x), then translate(30,40)
        float[][] child = svgPoints(group.getChildren().get(0));
        float[][] expected = svgPoints(sketch.getShapes().get(1));
        for (int i = 0; i < child.length; i++) {
            float sx = child[i][0], sy = -child[i][1]; // back to raw SVG coordinates
            float gx = sy + 30, gy = -sx + 40;
            assertEquals(expected[i][0], gx, 1e-4f);
            assertEquals(expected[i][1], -gy, 1e-4f);
        }
    }

    // Runs the action and returns everything it printed to stderr
    private static String stderrOf(ThrowingRunnable action) throws IOException {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.setErr(originalErr);
        }
        return err.toString(StandardCharsets.UTF_8);
    }

    private interface ThrowingRunnable {
        void run() throws IOException;
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
