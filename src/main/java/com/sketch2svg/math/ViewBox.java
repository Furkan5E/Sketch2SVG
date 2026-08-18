package com.sketch2svg.math;

// Rectangle that represents an SVG coordinate viewing space
public class ViewBox {
    public int x, y; // top-left / origin corner in SVG space
    public int w, h; // extent: width and height

    public ViewBox() {
        set(-100, -100, 200, 200);
    }

    public ViewBox(int x, int y, int w, int h) {
        set(x, y, w, h);
    }

    public void set(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /**
     * Fits the viewBox around min/max coordinate extents with optional border padding.
     */
    public void fit(float minX, float minY, float maxX, float maxY, float padding) {
        int left = (int) Math.floor(minX - padding);
        int top = (int) Math.floor(minY - padding);
        int width = (int) Math.ceil((maxX - minX) + padding * 2);
        int height = (int) Math.ceil((maxY - minY) + padding * 2);

        // Fallback for empty/zero-size scenes
        if (width <= 0) width = 200;
        if (height <= 0) height = 200;

        set(left, top, width, height);
    }

    @Override
    public String toString() {
        return x + " " + y + " " + w + " " + h;
    }
}