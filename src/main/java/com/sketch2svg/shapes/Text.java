package com.sketch2svg.shapes;

import com.sketch2svg.core.Attrib;
import com.sketch2svg.core.Shape;
import com.sketch2svg.math.Vec2;

public class Text extends Shape {

    private final Attrib attribX;
    private final Attrib attribY;
    private final Attrib attribFontSize;
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

        this.textContent = content;
        this.content = content;
        this.fontSize = fontSize;
        setPos(cx, cy);

        // Text defaults to white fill and no stroke
        setFill(0xFFFFFFFF);
        setStrokeWidth(0.0f);
    }

    public Text content(String text) {
        this.textContent = text;
        this.content = text;
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
        this.content = textContent;
    }
}