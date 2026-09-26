package com.sketch2svg.shapes;

import com.sketch2svg.core.Attrib;
import com.sketch2svg.core.Num;
import com.sketch2svg.core.Shape;
import com.sketch2svg.math.Vec2;

import java.util.ArrayList;
import java.util.List;

// Outline made of straight lines and quadratic/cubic Bezier curves.
// Points are affine-transformed like any other shape, which is exact for Bezier curves under rotation and scale.
public class PathShape extends Shape {

    // One command: M (move), L (line), Q (quadratic), C (cubic) or Z (close), with its points as x,y pairs
    private record Segment(char command, float[] points) {}

    private final Attrib attribData;
    private final List<Segment> segments = new ArrayList<>();

    public PathShape() {
        attribData = newAttrib("d");
    }

    public PathShape moveTo(float x, float y) {
        segments.add(new Segment('M', new float[]{x, y}));
        return this;
    }

    public PathShape lineTo(float x, float y) {
        segments.add(new Segment('L', new float[]{x, y}));
        return this;
    }

    public PathShape quadTo(float cx, float cy, float x, float y) {
        segments.add(new Segment('Q', new float[]{cx, cy, x, y}));
        return this;
    }

    public PathShape cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y) {
        segments.add(new Segment('C', new float[]{c1x, c1y, c2x, c2y, x, y}));
        return this;
    }

    public PathShape close() {
        segments.add(new Segment('Z', new float[0]));
        return this;
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    // Moves the points so they are relative to their bounding-box centre, which becomes the position.
    // Rotation and scale then pivot around the shape itself instead of the origin.
    public PathShape recentre() {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (Segment s : segments) {
            for (int i = 0; i < s.points().length; i += 2) {
                minX = Math.min(minX, s.points()[i]);
                maxX = Math.max(maxX, s.points()[i]);
                minY = Math.min(minY, s.points()[i + 1]);
                maxY = Math.max(maxY, s.points()[i + 1]);
            }
        }
        if (minX > maxX) {
            return this; // no points
        }
        float cx = (minX + maxX) * 0.5f, cy = (minY + maxY) * 0.5f;
        for (Segment s : segments) {
            for (int i = 0; i < s.points().length; i += 2) {
                s.points()[i] -= cx;
                s.points()[i + 1] -= cy;
            }
        }
        setPos(getPos().x + cx, getPos().y + cy);
        return this;
    }

    @Override
    public String getTag() {
        return "path";
    }

    @Override
    protected void updateAttribs() {
        super.updateAttribs();
        StringBuilder d = new StringBuilder();
        Vec2 p = new Vec2();
        for (Segment s : segments) {
            if (!d.isEmpty()) d.append(' ');
            d.append(s.command());
            for (int i = 0; i < s.points().length; i += 2) {
                p.set(s.points()[i], s.points()[i + 1]);
                transform(p);
                p.negY(); // SVG has flipped y-axis
                d.append(' ').append(Num.format(p.x)).append(' ').append(Num.format(p.y));
            }
        }
        attribData.val = d.toString();
    }
}
