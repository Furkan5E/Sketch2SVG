package com.sketch2svg.math;

import com.sketch2svg.core.Num;

// Rectangle that represents an SVG coordinate viewing space
public class ViewBox {
    public float x, y; // top-left / origin corner in SVG space
    public float w, h; // extent: width and height

    public ViewBox() {
        set(-100, -100, 200, 200);
    }

    public ViewBox(float x, float y, float w, float h) {
        set(x, y, w, h);
    }

    public void set(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /**
     * Fits the viewBox around min/max coordinate extents (in SVG space) with optional border padding.
     * The result is rounded outwards to whole units for tidy output.
     */
    public void fit(float minX, float minY, float maxX, float maxY, float padding) {
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(maxX) || !Float.isFinite(maxY)
                || !Float.isFinite(padding)) {
            throw new IllegalArgumentException("ViewBox bounds must be finite");
        }
        float left = (float) Math.floor(minX - padding);
        float top = (float) Math.floor(minY - padding);
        float width = (float) Math.ceil(maxX + padding) - left;
        float height = (float) Math.ceil(maxY + padding) - top;

        // Fallback for empty/zero-size scenes
        if (width <= 0) width = 200;
        if (height <= 0) height = 200;

        set(left, top, width, height);
    }

    @Override
    public String toString() {
        return Num.format(x) + " " + Num.format(y) + " " + Num.format(w) + " " + Num.format(h);
    }
}
