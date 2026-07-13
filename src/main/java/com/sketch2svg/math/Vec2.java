package com.sketch2svg.math;

// 2-vector (point)
public class Vec2 {
	public float x;
	public float y;


	public Vec2(){}

	public Vec2(float x, float y){
		set(x,y);
	}

	public Vec2(float v){
		this(v,v);
	}

	public Vec2(Vec2 v){
		this(v.x, v.y);
	}


	public Vec2 set(float x, float y){
		this.x = x;
		this.y = y;
		return this;
	}

	// All arithmetic operations operate in-place to minimize object creation

	public Vec2 add(Vec2 v){
		return add(v.x, v.y);
	}

	public Vec2 add(float ax, float ay){
		return set(x+ax, y+ay);
	}

	public Vec2 mul(Vec2 v){
		return mul(v.x, v.y);
	}
	
	public Vec2 mul(float mx, float my){
		return set(x*mx, y*my);
	}

	public Vec2 mul(float m){
		return mul(m,m);
	}

	// Complex multiply (useful for rotations)
	public Vec2 cmul(Vec2 v){
		return set(
			x*v.x - y*v.y,
			x*v.y + y*v.x
		);
	}

	public Vec2 neg(){
		return negX().negY();
	}

	public Vec2 negX(){
		x = -x;
		return this;
	}

	public Vec2 negY(){
		y = -y;
		return this;
	}

	public Vec2 dup(){
		return new Vec2(this);
	}

	@Override
	public String toString(){ return x + "," + y; }
}