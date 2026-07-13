package com.sketch2svg.math;

// Rectangle that represents a viewing space
public class ViewBox {
	public int x,y; // bottom-left corner
	public int w,h; // extent: width and height

	public ViewBox(){
		set(-100,-100, 200,200);
	}

	public void set(int x, int y, int w, int h){
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	@Override
	public String toString(){
		return x + " " + y + " " + w + " " + h;
	}
}
