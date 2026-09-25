package com.sketch2svg.shapes;

import com.sketch2svg.core.*;
import com.sketch2svg.math.Vec2;

// Circle; becomes an SVG <ellipse> when scaled unevenly
public class Circle extends Shape{

    private Attrib attribX;
    private Attrib attribY;
    private Attrib attribR;
    private Attrib attribRx;
    private Attrib attribRy;
    private Attrib attribTransform;
    private float radius = 1.f;

    public Circle(){
        attribX = newAttrib("cx");
        attribY = newAttrib("cy");
        attribR = newAttrib("r");
        attribRx = newAttrib("rx");
        attribRy = newAttrib("ry");
        attribTransform = newAttrib("transform");
    }

    public Circle(float r, float x, float y){
        this();
        radius(r);
        setPos(x,y);
    }

    public Circle radius(float r) {
        this.radius = r;
        return this;
    }

    public float getRadius() {
        return radius;
    }

    // Uneven scale (e.g. scale=2,1) turns the circle into an ellipse
    private boolean isEllipse(){
        return Math.abs(getScale().x) != Math.abs(getScale().y);
    }

    @Override
    public String getTag(){
        return isEllipse() ? "ellipse" : "circle";
    }
    @Override
    protected void updateAttribs() {
        super.updateAttribs();
        var c = new Vec2(0,0);
        transform(c);
        c.negY(); //flip y for svg
        attribX.val = Num.format(c.x);
        attribY.val = Num.format(c.y);

        // Negative scales mirror, which a circle or ellipse doesn't need; unused attributes are omitted (null)
        float rx = Math.abs(radius * getScale().x);
        float ry = Math.abs(radius * getScale().y);
        boolean ellipse = isEllipse();
        attribR.val = ellipse ? null : Num.format(rx);
        attribRx.val = ellipse ? Num.format(rx) : null;
        attribRy.val = ellipse ? Num.format(ry) : null;
        // Only an ellipse looks different when rotated; SVG angles are clockwise
        attribTransform.val = ellipse && getRotation() != 0.f
                ? "rotate(" + Num.format(-getRotation()) + " " + Num.format(c.x) + " " + Num.format(c.y) + ")" : null;
    }
}
