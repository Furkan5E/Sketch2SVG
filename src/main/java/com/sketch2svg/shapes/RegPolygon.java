package com.sketch2svg.shapes;

public class RegPolygon extends LineStrip{
    public RegPolygon(int sides, float radius, float cx, float cy){
        super();
        closed = true;

        if (sides < 3)
            sides = 3;
        setNumVertices(sides);

        final double twoPi = Math.PI * 2.0;
        final double phase = Math.PI / 2.0; // vertex-up for ALL n

        for (int i = 0; i < sides; i++) {
            double t = twoPi * i / sides + phase;
            setVertex(i, (float)Math.cos(t), (float)Math.sin(t));
        }

        setScale(radius);
        setPos(cx, cy);
    }
}
