package com.sketch2svg.shapes;

import com.sketch2svg.core.*;
import com.sketch2svg.math.Vec2;

public class LineStrip extends Shape {

	private Attrib attribPoints;
	private float[] vertices; // 2D position data packed as [x1, y1, x2, y2, ...]
	protected boolean closed = false;

	public LineStrip(){
		attribPoints = newAttrib("points");	
	}

	// Returns "polygon" if closed, otherwise "polyline"
	@Override
	public String getTag(){
		if(closed)
			return "polygon";
		else
			return "polyline";
	}

	// (This method is finished. No need to edit.)
	@Override
	protected void updateAttribs(){
		super.updateAttribs();

		String s = "";
		var p = new Vec2();
		for(int i=0; i<getNumVertices(); i++){
			p.set(getVertex(i,0), getVertex(i,1));
			transform(p);
			p.negY(); // SVG has flipped y-axis. We negate y so +y is up.
			s += p.x + "," + p.y + " ";
		}
		attribPoints.val = s;
	}

	public void setNumVertices(int n){
		if(n < 0)
			n = 0;
		vertices = new float[n*2];
	}

	public int getNumVertices(){
		if(vertices == null)
			return 0;
		return vertices.length /2;
	}
	
	// Set vertex at index
	public void setVertex(int i, float x, float y){
		vertices[i*2] = x;
		vertices[i*2 + 1] = y;
	}
	
	// Set vertex at index
	public void setVertex(int i, Vec2 p){
		setVertex(i, p.x, p.y);
	}

	// Get vertex component (0:x or 1:y) at index
	public float getVertex(int i, int comp){
		return vertices[i*2 + comp];
	}

	public LineStrip closed(boolean closed) {
        this.closed = closed;
        return this;
    }

    public LineStrip vertex(int i, float x, float y) {
        setVertex(i, x, y);
        return this;
    }

    public LineStrip vertex(int i, Vec2 p) {
        setVertex(i, p);
        return this;
    }
}