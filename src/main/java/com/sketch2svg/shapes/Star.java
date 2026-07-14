package com.sketch2svg.shapes;

public class Star extends LineStrip {

    private int points = 5;
    private float outerR = 20.f;
    private float innerR = 10.f;

    public Star() {
        this(5, 20.f, 10.f, 0.f, 0.f);
    }

    public Star(int points, float outerR, float innerR, float cx, float cy) {
        super();
        closed = true;
        this.points = Math.max(2, points);
        this.outerR = outerR;
        this.innerR = innerR;
        rebuildVertices();
        setPos(cx, cy);
    }

    public Star points(int p) {
        this.points = Math.max(2, p);
        rebuildVertices();
        return this;
    }

    public Star radii(float inner, float outer) {
        this.innerR = inner;
        this.outerR = outer;
        rebuildVertices();
        return this;
    }

    private void rebuildVertices() {
        int n = points * 2;
        setNumVertices(n);
        final float twoPi = (float) (Math.PI * 2.0);
        float phase = (float) Math.PI / 2f; // start at top

        for (int i = 0; i < n; i++) {
            float r = (i % 2 == 0) ? outerR : innerR;
            float t = twoPi * i / n + phase;

            float x = (float) Math.cos(t) * r;
            float y = (float) Math.sin(t) * r;
            setVertex(i, x, y);
        }
    }
}