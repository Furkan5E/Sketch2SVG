package com.sketch2svg.shapes;

public class Rect extends LineStrip {
	public Rect(){
		super();
		closed = true;
		setNumVertices(4);

		// Unit rectangle centered at origin
		setVertex(0, -0.5f, -0.5f);
		setVertex(1,  0.5f, -0.5f);
		setVertex(2,  0.5f,  0.5f);
		setVertex(3, -0.5f,  0.5f);
	}

	public Rect(float w, float h, float cx, float cy){
		this();
		setScale(w, h);
		setPos(cx, cy);
	}

	public Rect size(float w, float h) {
        setScale(w, h);
        return this;
    }

    public Rect size(float s) {
        setScale(s, s);
        return this;
    }
}
