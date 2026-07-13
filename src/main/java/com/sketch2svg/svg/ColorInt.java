package com.sketch2svg.svg;

// Utility class for an RGBA color represented as a 32-bit integer
public class ColorInt{

	public static final int maskAlpha = 0x000000FF;

	// Create RGBA from 8-bit values in [0,255]
	public static int from(int r, int g, int b, int a){
		final int M = maskAlpha; // mask for 8 LSBs
		return (r & M)<<24 | (g & M)<<16 | (b & M)<<8 | (a & M);
	}

	// Create opaque RGB from 8-bit values in [0,255]
	public static int from(int r, int g, int b){
		return from(r,g,b,255);
	}

	// Create transparent grayscale from 8-bit values in [0,255]
	public static int from(int gray, int a){
		return from(gray,gray,gray,a);
	}

	// Create opaque grayscale from 8-bit value in [0,255]
	public static int from(int gray){
		return from(gray,gray,gray);
	}

	// Create RGBA from real values in [0,1]
	public static int from(float r, float g, float b, float a){
		final float f2b = 255.999f;
		return from((int)(r*f2b), (int)(g*f2b), (int)(b*f2b), (int)(a*f2b));
	}

	// Create opaque RGB from real values in [0,1]
	public static int from(float r, float g, float b){
		return from(r,g,b,1.f);
	}

	// Create transparent grayscale from real values in [0,1]
	public static int from(float gray, float a){
		return from(gray,gray,gray,a);
	}

	// Create opaque grayscale from real value in [0,1]
	public static int from(float gray){
		return from(gray,gray,gray);
	}

	// Whether alpha is zero
	public static boolean isClear(int rgba){
		return (rgba & maskAlpha) == 0;
	}

	public static String hexString(int rgba){
		return "%08X".formatted(rgba);
	}
}