package com.sketch2svg.svg;

import com.sketch2svg.core.Attrib;
import com.sketch2svg.core.Elem;
import com.sketch2svg.core.Num;

import java.util.List;

// <linearGradient> or <radialGradient> with evenly spaced colour stops, referenced as url(#id).
// Coordinates are relative to each painted shape's bounding box (SVG's default objectBoundingBox units).
public class Gradient extends Elem {

    private final boolean radial;
    private final List<Integer> colors;

    // Linear gradient running at angleDeg (counter-clockwise, 0 = left to right) across the shape
    public static Gradient linear(String id, float angleDeg, List<Integer> colors) {
        Gradient g = new Gradient(id, false, colors);
        // Direction in SVG space, where y points down
        double a = Math.toRadians(angleDeg);
        double dx = Math.cos(a) * 0.5, dy = -Math.sin(a) * 0.5;
        g.newAttrib("x1", Num.format(0.5 - dx));
        g.newAttrib("y1", Num.format(0.5 - dy));
        g.newAttrib("x2", Num.format(0.5 + dx));
        g.newAttrib("y2", Num.format(0.5 + dy));
        return g;
    }

    // Radial gradient from the shape's centre (first colour) to its edge (last colour)
    public static Gradient radial(String id, List<Integer> colors) {
        return new Gradient(id, true, colors);
    }

    private Gradient(String id, boolean radial, List<Integer> colors) {
        if (colors.size() < 2) {
            throw new IllegalArgumentException("A gradient needs at least two colors");
        }
        this.radial = radial;
        this.colors = List.copyOf(colors);
        newAttrib("id", id);
    }

    @Override
    public String getTag() {
        return radial ? "radialGradient" : "linearGradient";
    }

    @Override
    protected void updateAttribs() {
        clearContent();
        for (int i = 0; i < colors.size(); i++) {
            addContent(new Stop((float) i / (colors.size() - 1), colors.get(i)));
        }
    }

    // One colour stop; translucent colours use stop-opacity like shapes use fill-opacity
    private static class Stop extends Elem {
        Stop(float offset, int rgba) {
            newAttrib("offset", Num.format(offset));
            newAttrib("stop-color", "#" + ColorInt.rgbHexString(rgba));
            if (ColorInt.opacity(rgba) < 1.0) {
                newAttrib("stop-opacity", Num.format(ColorInt.opacity(rgba)));
            }
        }

        @Override
        public String getTag() {
            return "stop";
        }
    }
}
