package com.sketch2svg.shapes;

public class Rect extends LineStrip {

    private float w = 1.f;
    private float h = 1.f;

    public Rect(){
        super();
        closed = true;
        setNumVertices(4);
        rebuildVertices(); // Unit rectangle centered at origin
    }

    public Rect(float w, float h, float cx, float cy){
        this();
        size(w, h);
        setPos(cx, cy);
    }

    public Rect size(float w, float h) {
        this.w = w;
        this.h = h;
        rebuildVertices();
        return this;
    }

    public Rect size(float s) {
        return size(s, s);
    }

    // Size is baked into the vertices so scale() multiplies it instead of replacing it
    private void rebuildVertices() {
        float hw = w * 0.5f;
        float hh = h * 0.5f;
        setVertex(0, -hw, -hh);
        setVertex(1,  hw, -hh);
        setVertex(2,  hw,  hh);
        setVertex(3, -hw,  hh);
    }
}
