package com.sketch2svg.shapes;

// Arrow shape pointing to the right by default
public class Arrow extends LineStrip {

    private float length = 20.f;
    private float width = 8.f;

    public Arrow() {
        this(20.f, 8.f, 0.f, 0.f);
    }

    public Arrow(float length, float width, float cx, float cy) {
        super();
        closed = true;
        dimensions(length, width);
        setPos(cx, cy);
    }

    public Arrow dimensions(float length, float width) {
        this.length = length;
        this.width = width;
        rebuildVertices();
        return this;
    }

    public Arrow length(float length) {
        this.length = length;
        rebuildVertices();
        return this;
    }

    public Arrow width(float width) {
        this.width = width;
        rebuildVertices();
        return this;
    }

    private void rebuildVertices() {
        float halfW = width * 0.5f;
        float headLen = length * 0.35f;

        setNumVertices(7);
        setVertex(0, -length / 2, -halfW);
        setVertex(1, length / 2 - headLen, -halfW);
        setVertex(2, length / 2 - headLen, -width);
        setVertex(3, length / 2, 0);
        setVertex(4, length / 2 - headLen, width);
        setVertex(5, length / 2 - headLen, halfW);
        setVertex(6, -length / 2, halfW);
    }
}