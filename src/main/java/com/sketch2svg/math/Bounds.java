package com.sketch2svg.math;

// Axis-aligned bounding box in sketch space (+y up); starts empty and grows as points are added
public class Bounds {
    public float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
    public float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;

    public boolean isEmpty() {
        return minX > maxX;
    }

    public Bounds add(float x, float y) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        return this;
    }

    public Bounds add(Bounds other) {
        if (!other.isEmpty()) {
            add(other.minX, other.minY);
            add(other.maxX, other.maxY);
        }
        return this;
    }

    // Grow on every side, e.g. by half a stroke width
    public Bounds expand(float margin) {
        if (!isEmpty()) {
            minX -= margin;
            minY -= margin;
            maxX += margin;
            maxY += margin;
        }
        return this;
    }
}
