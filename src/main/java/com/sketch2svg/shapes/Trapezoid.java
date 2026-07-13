package com.sketch2svg.shapes;

public class Trapezoid extends LineStrip {

	public Trapezoid(){
		this(20, 40, 20, 0, 0);
	}

	public Trapezoid(float topW, float bottomW, float h, float cx, float cy){
		super();
		closed = true;
		setNumVertices(4);

		// Define in local space (centered at origin), then scale/pos is not needed.
		// We'll directly place correct-sized vertices and then setPos to center.
		float ht = h * 0.5f;
		float top = topW * 0.5f;
		float bot = bottomW * 0.5f;

		// top-left, top-right, bottom-right, bottom-left
		setVertex(0, -top,  ht);
		setVertex(1,  top,  ht);
		setVertex(2,  bot, -ht);
		setVertex(3, -bot, -ht);

		setPos(cx, cy);
	}
}
