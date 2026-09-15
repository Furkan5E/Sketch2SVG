package com.sketch2svg.shapes;

import com.sketch2svg.core.Attrib;
import com.sketch2svg.core.Shape;
import com.sketch2svg.math.Vec2;

import java.util.ArrayList;
import java.util.List;

// SVG <g> that moves, rotates and scales its children together
public class Group extends Shape {

    private final Attrib attribTransform;
    private final List<Shape> children = new ArrayList<>();

    public Group() {
        attribTransform = newAttrib("transform");
    }

    public Group add(Shape shape) {
        if (shape != null) {
            children.add(shape);
        }
        return this;
    }

    public List<Shape> getChildren() {
        return children;
    }

    @Override
    public String getTag() {
        return "g";
    }

    // Paint is applied to the children directly, so the group itself has no style
    @Override
    protected boolean hasStyle() {
        return false;
    }

    @Override
    protected void updateAttribs() {
        super.updateAttribs();

        // Same order as Shape.transform (scale, then rotate, then translate), mirrored into
        // SVG space where y points down: translate(x, -y) and clockwise-positive rotate(-deg)
        StringBuilder t = new StringBuilder();
        Vec2 pos = getPos();
        if (pos.x != 0.f || pos.y != 0.f) {
            t.append("translate(").append(pos.x).append(' ').append(0.f - pos.y).append(") ");
        }
        if (getRotation() != 0.f) {
            t.append("rotate(").append(-getRotation()).append(") ");
        }
        Vec2 scale = getScale();
        if (scale.x != 1.f || scale.y != 1.f) {
            t.append("scale(").append(scale.x).append(' ').append(scale.y).append(")");
        }
        attribTransform.val = t.isEmpty() ? null : t.toString().trim();

        clearContent();
        for (Shape child : children) {
            addContent(child);
        }
    }
}
