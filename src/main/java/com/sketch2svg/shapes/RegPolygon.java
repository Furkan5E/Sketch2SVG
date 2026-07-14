package com.sketch2svg.shapes;

public class RegPolygon extends LineStrip {

    private int sides = 3;

    public RegPolygon() {
        this(3, 1.f, 0.f, 0.f);
    }

    public RegPolygon(int sides, float radius, float cx, float cy) {
        super();
        closed = true;
        this.sides = Math.max(3, sides);
        rebuildVertices();
        setScale(radius);
        setPos(cx, cy);
    }

    public RegPolygon sides(int n) {
        this.sides = Math.max(3, n);
        rebuildVertices();
        return this;
    }

    public RegPolygon radius(float r) {
        setScale(r);
        return this;
    }

    private void rebuildVertices() {
        setNumVertices(sides);
        final double twoPi = Math.PI * 2.0;
        final double phase = Math.PI / 2.0; // vertex-up for ALL n

        for (int i = 0; i < sides; i++) {
            double t = twoPi * i / sides + phase;
            setVertex(i, (float) Math.cos(t), (float) Math.sin(t));
        }
    }
}