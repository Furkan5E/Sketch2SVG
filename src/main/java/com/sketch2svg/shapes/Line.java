package com.sketch2svg.shapes;

public class Line extends LineStrip{

	public Line(){
		super();
		setNumVertices(2);
		closed = false;
	}

	// Construct from two endpoints
	public Line(float x1, float y1, float x2, float y2){
		this();
		setVertex(0, x1, y1);
		setVertex(1, x2, y2);
	}
}
