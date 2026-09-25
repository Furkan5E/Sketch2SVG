package com.sketch2svg.svg;

import java.util.Locale;
import java.util.Map;

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

    // Common CSS color names (plus "none"/"transparent"), as RGBA
    private static final Map<String, Integer> NAMED = Map.ofEntries(
        Map.entry("none", 0x00000000),
        Map.entry("transparent", 0x00000000),
        Map.entry("black", 0x000000FF),
        Map.entry("white", 0xFFFFFFFF),
        Map.entry("gray", 0x808080FF),
        Map.entry("grey", 0x808080FF),
        Map.entry("silver", 0xC0C0C0FF),
        Map.entry("red", 0xFF0000FF),
        Map.entry("maroon", 0x800000FF),
        Map.entry("orange", 0xFFA500FF),
        Map.entry("gold", 0xFFD700FF),
        Map.entry("yellow", 0xFFFF00FF),
        Map.entry("olive", 0x808000FF),
        Map.entry("lime", 0x00FF00FF),
        Map.entry("green", 0x008000FF),
        Map.entry("teal", 0x008080FF),
        Map.entry("cyan", 0x00FFFFFF),
        Map.entry("blue", 0x0000FFFF),
        Map.entry("navy", 0x000080FF),
        Map.entry("purple", 0x800080FF),
        Map.entry("magenta", 0xFF00FFFF),
        Map.entry("pink", 0xFFC0CBFF),
        Map.entry("brown", 0xA52A2AFF)
    );

    // Whether s is a hex color: RRGGBB or RRGGBBAA, optionally prefixed with '#'
    public static boolean isHex(String s){
        return s.matches("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})");
    }

    // Parse RRGGBB (opaque) or RRGGBBAA, optionally prefixed with '#'
    public static int parseHex(String s){
        if(!isHex(s))
            throw new IllegalArgumentException("Invalid hex color (expected RRGGBB or RRGGBBAA): " + s);
        String hex = s.startsWith("#") ? s.substring(1) : s;
        if(hex.length() == 6)
            hex += "FF";
        return (int) Long.parseLong(hex, 16);
    }

    // Whether s is any supported color: hex (see isHex), #RGB / #RGBA shorthand, or a color name
    public static boolean isColor(String s){
        return isHex(s)
            || s.matches("#([0-9a-fA-F]{3}|[0-9a-fA-F]{4})") // '#' required so "100" stays a number
            || NAMED.containsKey(s.toLowerCase(Locale.ROOT));
    }

    // Parse any supported color (see isColor); names are case-insensitive
    public static int parseColor(String s){
        if(isHex(s))
            return parseHex(s);
        Integer named = NAMED.get(s.toLowerCase(Locale.ROOT));
        if(named != null)
            return named;
        if(!isColor(s))
            throw new IllegalArgumentException("Invalid color (expected RRGGBB, RRGGBBAA, #RGB, #RGBA or a name): " + s);

        // Expand #RGB / #RGBA by doubling each digit
        StringBuilder hex = new StringBuilder();
        for(char c : s.substring(1).toCharArray())
            hex.append(c).append(c);
        return parseHex(hex.toString());
    }

    public static String hexString(int rgba){
        return "%08X".formatted(rgba);
    }

    // RRGGBB without alpha, for SVG paint (pair with opacity() for widely supported output)
    public static String rgbHexString(int rgba){
        return "%06X".formatted(rgba >>> 8);
    }

    // Alpha as 0..1
    public static double opacity(int rgba){
        return (rgba & maskAlpha) / 255.0;
    }
}