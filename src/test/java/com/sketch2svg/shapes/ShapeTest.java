package com.sketch2svg.shapes;

import org.junit.jupiter.api.Test;
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
}