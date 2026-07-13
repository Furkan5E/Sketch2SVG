package com.sketch2svg.shapes;

//arrow shape pointing to the right by default
public class Arrow extends LineStrip {

	public Arrow(){
		this(20, 8, 0,0);
	}

	public Arrow(float length,float width, float cx, float cy) {
		super();
		closed = true;

		float halfW = width * 0.5f;
		float headLen = length *0.35f;

		setNumVertices(7);

		//create arrow shape
		setVertex(0, -length/2, -halfW);
		setVertex(1,  length/2 - headLen, -halfW);
		setVertex(2,  length/2 - headLen, -width);
		setVertex(3,  length/2, 0);
		setVertex(4,  length/2 - headLen,  width);
		setVertex(5,  length/2 - headLen,  halfW);
		setVertex(6, -length/2,  halfW);
        //position arrow
		setPos(cx, cy);
	}
}
