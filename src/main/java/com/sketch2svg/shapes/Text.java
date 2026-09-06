package com.sketch2svg.shapes;

import com.sketch2svg.core.Attrib;
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
        this.content = Xml.escape(content);
        this.fontSize = fontSize;
        setPos(cx, cy);

        // Text defaults to white fill and no stroke
        setFill(0xFFFFFFFF);
        setStrokeWidth(0.0f);
    }

    public Text content(String text) {
        this.textContent = text;
        this.content = Xml.escape(text);
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

        attribX.val = String.valueOf(p.x);
        attribY.val = String.valueOf(p.y);
        attribFontSize.val = String.valueOf(fontSize);
        // Glyphs can't be rotated via vertices, so rotate around the anchor; SVG angles are clockwise
        attribTransform.val = getRotation() == 0.f ? null
                : "rotate(" + (-getRotation()) + " " + p.x + " " + p.y + ")";
        this.content = Xml.escape(textContent);
    }
}