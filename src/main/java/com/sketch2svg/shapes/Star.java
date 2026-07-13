package com.sketch2svg.shapes;

public class Star extends LineStrip {

	public Star(){
		this(5, 20, 10, 0, 0);
	}

	public Star(int points, float outerR, float innerR, float cx, float cy){
		super();
		closed = true;

		if(points < 2)
			points = 2;

		int n = points * 2;
		setNumVertices(n);
		final float twoPi = (float)(Math.PI * 2.0);
		float phase = (float)Math.PI / 2f; // start at top
		
		for(int i=0; i<n; i++){
			float r = (i % 2 == 0) ? outerR : innerR;
			float t = twoPi * i / n + phase;

			float x = (float)Math.cos(t) * r;
			float y = (float)Math.sin(t) * r;
			setVertex(i, x, y);
		}
		setPos(cx, cy);
	}
}
