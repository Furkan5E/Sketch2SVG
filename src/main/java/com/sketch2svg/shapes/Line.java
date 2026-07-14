package com.sketch2svg.shapes;

public class Line extends LineStrip{

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
        setVertex(0, x1, y1);
        return this;
    }

    public Line to(float x2, float y2) {
        setVertex(1, x2, y2);
        return this;
    }
}