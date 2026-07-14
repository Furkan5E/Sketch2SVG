package com.sketch2svg.core;

import com.sketch2svg.svg.*;
import com.sketch2svg.math.*;

// SVG shape with a geometric transform
public abstract class Shape extends Elem {
	private Attrib attribStyle;

	private int fill = ColorInt.from(0,0);
	private int stroke = ColorInt.from(0);
	private float strokeWidth = 1.f;

	private Vec2 scale = new Vec2(1.f);
	private Vec2 rotation = new Vec2(1.f, 0.f); // in complex form
	private Vec2 pos = new Vec2(0.f);

	public Shape(){
		attribStyle = newAttrib("style");
	}


	// This updates the style element
	// If overriding in a subclass, make sure to call this method via super.updateAttribs().
	@Override
	protected void updateAttribs(){
		var style = "fill:" + (ColorInt.isClear(fill) ? "none" : "#"+ColorInt.hexString(fill));
		if(strokeWidth > 0.f && !ColorInt.isClear(stroke)){
			style += ";stroke-width:" + strokeWidth;
			style += ";stroke:#" + ColorInt.hexString(stroke);
		}
		attribStyle.val = style;
	}

	// API Helpers

    public Shape at(float x, float y) {
        return setPos(x, y);
    }

    public Shape fill(int color) {
        return setFill(color);
    }

    public Shape fill(String hexColor) {
        String cleanHex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        this.fill = (int) Long.parseLong(cleanHex, 16);
        return this;
    }

    public Shape stroke(int color) {
        return setStroke(color);
    }

    public Shape stroke(String hexColor) {
        String cleanHex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        this.stroke = (int) Long.parseLong(cleanHex, 16);
        return this;
    }

    public Shape strokeWidth(float width) {
        return setStrokeWidth(width);
    }

    public Shape rotate(float deg) {
        return setRotation(deg);
    }

    public Shape scale(float s) {
        return setScale(s);
    }

    public Shape scale(float sx, float sy) {
        return setScale(sx, sy);
    }

	//setters and getters
	public int getFill(){
		return fill;
	}
	public Shape setFill(int color){
		this.fill = color;
		return this;
	}
	public int getStroke(){
		return stroke;
	}
	public Shape setStroke(int color){
		this.stroke = color;
		return this;
	}
	public float getStrokeWidth(){
		return strokeWidth;
	}
	public Shape setStrokeWidth(float w){
		this.strokeWidth = w;
		return this;
	}
	public Vec2 getPos(){
		return pos;
	}
	public Shape setPos(float x, float y){
		this.pos.set(x,y);
		return this;
	}
	public Vec2 getScale(){
		return scale;
	}
	public Shape setScale(float s){
		this.scale.set(s,s);
		return this;
	}
	public Shape setScale(float sx, float sy){
		scale.set(sx, sy);
		return this;
	}
	
	public Shape setRotation(float deg){
		final float d2r = (float)(Math.PI / 180.);
		rotation.x = (float)Math.cos(deg*d2r);
		rotation.y = (float)Math.sin(deg*d2r);
		return this;
	}
	
	// Apply transform in-place on vector
	protected void transform(Vec2 p){
		p.mul(scale).cmul(rotation).add(pos);
	}
}