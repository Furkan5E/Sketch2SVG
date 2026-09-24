package com.sketch2svg.shapes;

public class Line extends LineStrip{

    private float x1, y1, x2, y2;

    public Line(){
        super();
        setNumVertices(2);
        closed = false;
    }

    public Line(float x1, float y1, float x2, float y2) {
        this();
        from(x1, y1);
        to(x2, y2);
    }

    public Line from(float x1, float y1) {
        this.x1 = x1;
        this.y1 = y1;
        rebuildVertices();
        return this;
    }

    public Line to(float x2, float y2) {
        this.x2 = x2;
        this.y2 = y2;
        rebuildVertices();
        return this;
    }

    // Store endpoints relative to the midpoint so rotation and scale pivot around the line's centre
    private void rebuildVertices() {
        float cx = (x1 + x2) * 0.5f;
        float cy = (y1 + y2) * 0.5f;
        setPos(cx, cy);
        setVertex(0, x1 - cx, y1 - cy);
        setVertex(1, x2 - cx, y2 - cy);
    }
}
