package com.sketch2svg.shapes;

import com.sketch2svg.core.Attrib;
import com.sketch2svg.core.Num;
import com.sketch2svg.core.Shape;
import com.sketch2svg.core.Xml;
import com.sketch2svg.math.Vec2;

public class Text extends Shape {

    private final Attrib attribX;
    private final Attrib attribY;
    private final Attrib attribFontSize;
    private final Attrib attribTransform;
    private String textContent = "";
    private float fontSize = 16.0f;

    public Text() {
        this("", 0.0f, 0.0f, 16.0f);
    }

    public Text(String content, float cx, float cy, float fontSize) {
        attribX = newAttrib("x");
        attribY = newAttrib("y");
        attribFontSize = newAttrib("font-size");
        newAttrib("text-anchor", "middle");
        newAttrib("dominant-baseline", "middle");
        attribTransform = newAttrib("transform");

        this.textContent = content;
        this.fontSize = fontSize;
        setPos(cx, cy);

        // Text defaults to white fill and no stroke
        setFill(0xFFFFFFFF);
        setStrokeWidth(0.0f);
    }

    public Text content(String text) {
        this.textContent = text;
        return this;
    }

    public Text fontSize(float size) {
        this.fontSize = size;
        return this;
    }

    @Override
    public String getTag() {
        return "text";
    }

    @Override
    protected void updateAttribs() {
        super.updateAttribs();

        Vec2 p = new Vec2(0, 0);
        transform(p);
        p.negY(); // Invert Y to match SVG coordinate space

        String x = Num.format(p.x), y = Num.format(p.y);
        attribX.val = x;
        attribY.val = y;
        attribFontSize.val = Num.format(fontSize);
        // Glyphs can't be transformed via vertices, so rotate and scale around the anchor.
        // SVG applies the list right to left: scale first, then rotate; SVG angles are clockwise.
        String transform = "";
        if (getRotation() != 0.f) {
            transform += "rotate(" + Num.format(-getRotation()) + " " + x + " " + y + ")";
        }
        Vec2 scale = getScale();
        if (scale.x != 1.f || scale.y != 1.f) {
            transform += " translate(" + x + " " + y + ") scale(" + Num.format(scale.x) + " " + Num.format(scale.y) + ")"
                    + " translate(" + Num.format(-p.x) + " " + Num.format(-p.y) + ")";
        }
        attribTransform.val = transform.isEmpty() ? null : transform.trim();
        setContent(Xml.escape(textContent));
    }
}