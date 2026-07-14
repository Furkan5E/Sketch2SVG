package com.sketch2svg.shapes;

import com.sketch2svg.core.*;
import com.sketch2svg.math.Vec2;

// Part of a circle
public class Arc extends Shape {

    private Attrib attribData;
    private float angle = 0.f;
    private float length = 180.f;

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
        setScale(r);
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
        var angle1 = -(angle - length * 0.5f);
        var angle2 = -(angle + length * 0.5f);
        var a1 = angle1 * (float) Math.PI / 180;
        var a2 = angle2 * (float) Math.PI / 180;
        var p1 = new Vec2((float) Math.cos(a1), (float) Math.sin(a1));
        var p2 = new Vec2((float) Math.cos(a2), (float) Math.sin(a2));

        transform(p1);
        transform(p2);
        p1.negY();
        p2.negY();

        float r = getScale().x;
        int largeArcFlag = Math.abs(length) > 180.f ? 1 : 0;
        int sweepFlag = length > 0.f ? 1 : 0;

        attribData.val = String.format("M %f %f A %f %f 0 %d %d %f %f",
                p1.x, p1.y, r, r, largeArcFlag, sweepFlag, p2.x, p2.y);
    }
}