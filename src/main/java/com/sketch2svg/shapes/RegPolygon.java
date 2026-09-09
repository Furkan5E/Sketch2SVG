package com.sketch2svg.shapes;

public class RegPolygon extends LineStrip {

    private int sides = 3;
    private float radius = 1.f;

    public RegPolygon() {
        this(3, 1.f, 0.f, 0.f);
    }

    public RegPolygon(int sides, float radius, float cx, float cy) {
        super();
        closed = true;
        this.sides = Math.max(3, sides);
        this.radius = radius;
        rebuildVertices();
        setPos(cx, cy);
    }

    public RegPolygon sides(int n) {
        this.sides = Math.max(3, n);
        rebuildVertices();
        return this;
    }

    public RegPolygon radius(float r) {
        this.radius = r;
        rebuildVertices();
        return this;
    }

    // Radius is baked into the vertices so scale() multiplies it instead of replacing it

    private void rebuildVertices() {
        setNumVertices(sides);
        final double twoPi = Math.PI * 2.0;
        final double phase = Math.PI / 2.0; // vertex-up for ALL n

        for (int i = 0; i < sides; i++) {
            double t = twoPi * i / sides + phase;
            setVertex(i, (float) Math.cos(t) * radius, (float) Math.sin(t) * radius);
        }
    }
}