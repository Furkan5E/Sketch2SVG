package com.sketch2svg.svg;

import com.sketch2svg.core.Elem;
import com.sketch2svg.core.Xml;

// Element holding plain text, such as <title> or <desc>
public class TextElement extends Elem {

    private final String tag;
    private final String text;

    public TextElement(String tag, String text) {
        this.tag = tag;
        this.text = text;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    protected void updateAttribs() {
        setContent(Xml.escape(text));
    }
}
