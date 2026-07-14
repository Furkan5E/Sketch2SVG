package com.sketch2svg.shapes;

public class Trapezoid extends LineStrip {

    private float topW = 20.f;
    private float botW = 40.f;
    private float h = 20.f;

    public Trapezoid() {
        this(20.f, 40.f, 20.f, 0.f, 0.f);
    }

    public Trapezoid(float topW, float botW, float h, float cx, float cy) {
        super();
        closed = true;
        dimensions(topW, botW, h);
        setPos(cx, cy);
    }

    public Trapezoid dimensions(float topW, float botW, float h) {
        this.topW = topW;
        this.botW = botW;
        this.h = h;
        rebuildVertices();
        return this;
    }

    private void rebuildVertices() {
        setNumVertices(4);
        float ht = h * 0.5f;
        float top = topW * 0.5f;
        float bot = botW * 0.5f;

        setVertex(0, -top, ht);
        setVertex(1, top, ht);
        setVertex(2, bot, -ht);
        setVertex(3, -bot, -ht);
    }
}