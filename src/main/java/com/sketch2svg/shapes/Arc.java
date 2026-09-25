package com.sketch2svg.shapes;

import com.sketch2svg.core.*;
import com.sketch2svg.math.Vec2;

// Part of a circle
public class Arc extends Shape {

    private Attrib attribData;
    private float angle = 0.f;
    private float length = 180.f;
    private float radius = 1.f;

    public Arc() {
        this(1.f, 0.f, 180.f, 0.f, 0.f);
    }

    public Arc(float radius, float angle, float length, float cx, float cy) {
        attribData = newAttrib("d");
        radius(radius);
        angles(angle, length);
        setPos(cx, cy);
    }

    public Arc radius(float r) {
        this.radius = r;
        return this;
    }

    public Arc angles(float angle, float length) {
        this.angle = angle;
        this.length = length;
        return this;
    }

    public Arc angle(float angle) {
        this.angle = angle;
        return this;
    }

    public Arc length(float length) {
        this.length = length;
        return this;
    }

    @Override
    public String getTag() {
        return "path";
    }

    @Override
    protected void updateAttribs() {
        super.updateAttribs();
        // Angles are counter-clockwise (+y up) like the rest of the engine; the arc runs from angle1 to angle2
        var angle1 = angle - length * 0.5f;
        var angle2 = angle + length * 0.5f;
        var a1 = angle1 * (float) Math.PI / 180;
        var a2 = angle2 * (float) Math.PI / 180;
        var p1 = new Vec2((float) Math.cos(a1), (float) Math.sin(a1)).mul(radius);
        var p2 = new Vec2((float) Math.cos(a2), (float) Math.sin(a2)).mul(radius);

        transform(p1);
        transform(p2);
        p1.negY();
        p2.negY();

        // Uneven scale makes an elliptical arc whose x-axis follows the rotation (clockwise in SVG)
        float rx = Math.abs(radius * getScale().x);
        float ry = Math.abs(radius * getScale().y);
        String axisRotation = rx == ry ? "0" : Num.format(-getRotation());

        int largeArcFlag = Math.abs(length) > 180.f ? 1 : 0;
        // After the y flip, counter-clockwise is SVG's negative-angle direction (sweep 0);
        // a mirroring scale (one negative factor) reverses the direction
        boolean mirrored = getScale().x * getScale().y < 0.f;
        int sweepFlag = (length > 0.f) != mirrored ? 0 : 1;

        attribData.val = "M " + Num.format(p1.x) + " " + Num.format(p1.y)
                + " A " + Num.format(rx) + " " + Num.format(ry) + " " + axisRotation
                + " " + largeArcFlag + " " + sweepFlag + " " + Num.format(p2.x) + " " + Num.format(p2.y);
    }
}