package com.sketch2svg.shapes;

import com.sketch2svg.core.Attrib;
import com.sketch2svg.core.Num;
import com.sketch2svg.math.Vec2;

public class Line extends LineStrip{

    private float x1, y1, x2, y2;
    private boolean arrowStart, arrowEnd;
    private final Attrib attribData;

    public Line(){
        super();
        setNumVertices(2);
        closed = false;
        attribData = newAttrib("d");
    }

    public Line(float x1, float y1, float x2, float y2) {
        this();
        from(x1, y1);
        to(x2, y2);
    }

    public Line from(float x1, float y1) {
        this.x1 = x1;
        this.y1 = y1;
        rebuildVertices();
        return this;
    }

    public Line to(float x2, float y2) {
        this.x2 = x2;
        this.y2 = y2;
        rebuildVertices();
        return this;
    }

    // Arrowheads at "start", "end", "both" or "none"
    public Line setArrows(String where) {
        arrowStart = where.equals("start") || where.equals("both");
        arrowEnd = where.equals("end") || where.equals("both");
        return this;
    }

    public boolean hasArrowStart() {
        return arrowStart;
    }

    public boolean hasArrowEnd() {
        return arrowEnd;
    }

    // Store endpoints relative to the midpoint so rotation and scale pivot around the line's centre
    private void rebuildVertices() {
        float cx = (x1 + x2) * 0.5f;
        float cy = (y1 + y2) * 0.5f;
        setPos(cx, cy);
        setVertex(0, x1 - cx, y1 - cy);
        setVertex(1, x2 - cx, y2 - cy);
    }

    // A line with arrowheads is written as a path so the heads share its stroke (colour, width, dashes)
    @Override
    public String getTag(){
        return arrowStart || arrowEnd ? "path" : super.getTag();
    }

    @Override
    protected void updateAttribs(){
        super.updateAttribs();
        if (!arrowStart && !arrowEnd) {
            attribData.val = null;
            return;
        }
        attribPoints.val = null;

        Vec2 a = svgPoint(0), b = svgPoint(1);
        StringBuilder d = new StringBuilder("M ").append(xy(a)).append(" L ").append(xy(b));
        if (arrowEnd) appendHead(d, b, a);
        if (arrowStart) appendHead(d, a, b);
        attribData.val = d.toString();
    }

    private Vec2 svgPoint(int i) {
        Vec2 p = new Vec2(getVertex(i, 0), getVertex(i, 1));
        transform(p);
        return p.negY();
    }

    // Open chevron at tip, pointing away from `from`; sized from the stroke width so it stays visible
    private void appendHead(StringBuilder d, Vec2 tip, Vec2 from) {
        float dx = tip.x - from.x, dy = tip.y - from.y;
        float len = (float) Math.hypot(dx, dy);
        if (len == 0.f) {
            return; // no direction to point in
        }
        float ux = dx / len, uy = dy / len;
        float size = Math.max(3.f, 4.f * getStrokeWidth());
        final float cos = (float) Math.cos(Math.toRadians(30)), sin = (float) Math.sin(Math.toRadians(30));
        // The two wings: the reversed direction turned by +/-30 degrees
        Vec2 left = new Vec2(tip.x - size * (ux * cos - uy * sin), tip.y - size * (uy * cos + ux * sin));
        Vec2 right = new Vec2(tip.x - size * (ux * cos + uy * sin), tip.y - size * (uy * cos - ux * sin));
        d.append(" M ").append(xy(left)).append(" L ").append(xy(tip)).append(" L ").append(xy(right));
    }

    private static String xy(Vec2 p) {
        return Num.format(p.x) + " " + Num.format(p.y);
    }
}
