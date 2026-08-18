package com.sketch2svg.parser;

import com.sketch2svg.shapes.Circle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SketchParserTest {

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
}