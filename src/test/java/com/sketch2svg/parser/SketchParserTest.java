package com.sketch2svg.parser;

import com.sketch2svg.shapes.Circle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

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
        assertDoesNotThrow(() -> sketch.fromFile("non_existent_file.txt"));
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
            assertEquals(2.5f, circle.getScale().x);
            assertFalse(sketch.getShapes().get(1).toString().contains(","));
        } finally {
            Locale.setDefault(original);
        }
    }
}
