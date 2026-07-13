package com.sketch2svg.shapes;

import com.sketch2svg.core.*;
import com.sketch2svg.math.Vec2;

public class Circle extends Shape{

	private Attrib attribX;
	private Attrib attribY;
	private Attrib attribR;

	public Circle(){
		// Create attributes via Shape.newAttrib() and assign to members above. For example, here is the first one:
		attribX = newAttrib("cx");
		attribY = newAttrib("cy");
		attribR = newAttrib("r");
	}

	@Override
	public String getTag(){
		return "circle";
	}
	@Override
	protected void updateAttribs() {
		super.updateAttribs();
		var c = new Vec2(0,0);
		transform(c);
		c.negY(); //flip y for svg
		attribX.val = "" + c.x;
		attribY.val = "" + c.y;
		attribR.val = "" + getScale().x;
	}

	public Circle(float r, float x, float y){
		this();
		setScale(r);
		setPos(x,y);
	}
}
