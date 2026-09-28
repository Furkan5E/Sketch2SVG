package com.sketch2svg.shapes;

import com.sketch2svg.core.Attrib;
import com.sketch2svg.core.Num;
import com.sketch2svg.core.Shape;
import com.sketch2svg.core.Xml;
import com.sketch2svg.math.Bounds;
import com.sketch2svg.math.Vec2;

public class Text extends Shape {

    private final Attrib attribX;
    private final Attrib attribY;
    private final Attrib attribFontSize;
    private final Attrib attribTransform;
    private final Attrib attribAnchor;
    private final Attrib attribFamily;
    private final Attrib attribWeight;
    private final Attrib attribStyle;
    private String textContent = "";
    private float fontSize = 16.0f;
    private String anchor = "middle"; // SVG text-anchor: start, middle or end
    private String family;            // null = renderer default
    private String weight;            // null = normal
    private boolean italic;

    public Text() {
        this("", 0.0f, 0.0f, 16.0f);
    }

    public Text(String content, float cx, float cy, float fontSize) {
        attribX = newAttrib("x");
        attribY = newAttrib("y");
        attribFontSize = newAttrib("font-size");
        attribFamily = newAttrib("font-family");
        attribWeight = newAttrib("font-weight");
        attribStyle = newAttrib("font-style");
        attribAnchor = newAttrib("text-anchor");
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

    // Font family such as "serif" or "Courier New"; null for the renderer's default
    public Text fontFamily(String family) {
        this.family = family;
        return this;
    }

    // "normal", "bold", "lighter", "bolder" or 100..900
    public Text fontWeight(String weight) {
        if (weight != null && !weight.matches("normal|bold|lighter|bolder|[1-9]00")) {
            throw new IllegalArgumentException("Invalid font weight: " + weight);
        }
        this.weight = weight;
        return this;
    }

    public Text italic(boolean italic) {
        this.italic = italic;
        return this;
    }

    // Which side of the text sits on its position: "left", "center" or "right"
    public Text align(String align) {
        this.anchor = switch (align) {
            case "left" -> "start";
            case "center" -> "middle";
            case "right" -> "end";
            default -> throw new IllegalArgumentException("Invalid alignment: " + align);
        };
        return this;
    }

    public String getFontFamily() {
        return family;
    }

    public String getFontWeight() {
        return weight;
    }

    public boolean isItalic() {
        return italic;
    }

    @Override
    public String getTag() {
        return "text";
    }

    // Glyph widths depend on the renderer's font, so estimate: ~0.6 em per character, one em tall
    @Override
    protected void addGeometry(Bounds b) {
        float w = 0.6f * fontSize * textContent.length();
        float h = fontSize;
        float left = switch (anchor) {
            case "start" -> 0.f;
            case "end" -> -w;
            default -> -w * 0.5f;
        };
        addLocalPoint(b, left, -h * 0.5f);
        addLocalPoint(b, left + w, -h * 0.5f);
        addLocalPoint(b, left, h * 0.5f);
        addLocalPoint(b, left + w, h * 0.5f);
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
        attribFamily.val = family;
        attribWeight.val = weight;
        attribStyle.val = italic ? "italic" : null;
        attribAnchor.val = anchor;
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