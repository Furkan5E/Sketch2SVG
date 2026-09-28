package com.sketch2svg.shapes;

import com.sketch2svg.core.Attrib;
import com.sketch2svg.core.Num;
import com.sketch2svg.core.Shape;
import com.sketch2svg.math.Bounds;
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

    // Children's box, moved through this group's transform (its corners, since the group may rotate)
    @Override
    protected void addGeometry(Bounds b) {
        Bounds inner = new Bounds();
        for (Shape child : children) {
            child.collectBounds(inner);
        }
        if (!inner.isEmpty()) {
            addLocalPoint(b, inner.minX, inner.minY);
            addLocalPoint(b, inner.maxX, inner.minY);
            addLocalPoint(b, inner.minX, inner.maxY);
            addLocalPoint(b, inner.maxX, inner.maxY);
        }
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
            t.append("translate(").append(Num.format(pos.x)).append(' ').append(Num.format(-pos.y)).append(") ");
        }
        if (getRotation() != 0.f) {
            t.append("rotate(").append(Num.format(-getRotation())).append(") ");
        }
        Vec2 scale = getScale();
        if (scale.x != 1.f || scale.y != 1.f) {
            t.append("scale(").append(Num.format(scale.x)).append(' ').append(Num.format(scale.y)).append(")");
        }
        attribTransform.val = t.isEmpty() ? null : t.toString().trim();

        clearContent();
        for (Shape child : children) {
            addContent(child);
        }
    }
}
