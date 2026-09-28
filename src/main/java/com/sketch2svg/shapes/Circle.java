package com.sketch2svg.shapes;

import com.sketch2svg.core.*;
import com.sketch2svg.math.Bounds;
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
    private float radiusY = 1.f; // equal to radius for a circle; differs for an ellipse

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
        this.radiusY = r;
        return this;
    }

    // Horizontal and vertical radius, before rotation
    public Circle radii(float rx, float ry) {
        this.radius = rx;
        this.radiusY = ry;
        return this;
    }

    public float getRadius() {
        return radius;
    }

    public float getRadiusY() {
        return radiusY;
    }

    // Different radii, or uneven scale (e.g. scale=2,1), make an ellipse
    private boolean isEllipse(){
        return Math.abs(radius * getScale().x) != Math.abs(radiusY * getScale().y);
    }

    @Override
    public String getTag(){
        return isEllipse() ? "ellipse" : "circle";
    }

    // Sampled outline, so rotated and stretched ellipses get a tight box
    @Override
    protected void addGeometry(Bounds b){
        final int samples = 72;
        for(int i = 0; i < samples; i++){
            double t = 2 * Math.PI * i / samples;
            addLocalPoint(b, (float)(radius * Math.cos(t)), (float)(radiusY * Math.sin(t)));
        }
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
        float ry = Math.abs(radiusY * getScale().y);
        boolean ellipse = isEllipse();
        attribR.val = ellipse ? null : Num.format(rx);
        attribRx.val = ellipse ? Num.format(rx) : null;
        attribRy.val = ellipse ? Num.format(ry) : null;
        // Only an ellipse looks different when rotated; SVG angles are clockwise
        attribTransform.val = ellipse && getRotation() != 0.f
                ? "rotate(" + Num.format(-getRotation()) + " " + Num.format(c.x) + " " + Num.format(c.y) + ")" : null;
    }
}
