package com.sketch2svg.shapes;

import com.sketch2svg.core.*;
import com.sketch2svg.math.Vec2;

// Part of a circle
public class Arc extends Shape {

	private Attrib attribData;
	private float angle, length;

	public Arc(){
		this(1, 0, 180, 0,0);
	}

	public Arc(float radius, float angle, float length, float cx, float cy){
		attribData = newAttrib("d");
		setScale(radius);
		setPos(cx, cy);
		this.angle = angle;
		this.length = length;
	}

	@Override
	public String getTag(){ return "path"; }

	@Override
	protected void updateAttribs(){
		super.updateAttribs();
		// angles negated since SVG uses left-hand system (+ angle goes CW)
		var angle1 = -(angle - length*0.5f);
		var angle2 = -(angle + length*0.5f);
		var a1 = angle1 * (float)Math.PI/180;
		var a2 = angle2 * (float)Math.PI/180;
		var p1 = new Vec2((float)Math.cos(a1), (float)Math.sin(a1));
		var p2 = new Vec2((float)Math.cos(a2), (float)Math.sin(a2));
		// Apply scale, rotation and translation from members in Shape
		transform(p1);
		transform(p2);
		var r = getScale().x;
		// https://svg-tutorial.com/svg/arc/
		int largeArc = length>180 ? 1 : 0;
		attribData.val
			= "M " + p1.x + " " + -p1.y // start pt
			+ " A " + r + " " + r // radii
			+ " 0 " // rotation
			+ largeArc
			+ " 0 " // sweep flag
			+ p2.x + " " + -p2.y // end pt
		;
	}
}
