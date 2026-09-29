package com.sketch2svg.core;
import java.util.ArrayList;
import java.util.List;

// XML element consisting of a tag and content
public abstract class Elem{
    private ArrayList<Attrib> attribs = new ArrayList<Attrib>();
    // Child elements and raw (already escaped) text, rendered only when the document is written
    private final List<Object> content = new ArrayList<>();


    // Get this element's unadorned tag name, e.g., "svg" or "circle"
    public abstract String getTag();

    // Add an attribute to the tag; subclasses keep the returned Attrib and set its value in updateAttribs()
    protected final Attrib newAttrib(String key, String val){
        Attrib a = new Attrib(key, val);
        attribs.add(a);
        return a;
    }
    protected final Attrib newAttrib(String key){
        return newAttrib(key, "");
    }

    // Add a child element; each is placed on its own line (except in minified output)
    public final void addContent(Elem e){
        if(e == null)
            return;
        content.add(e);
    }

    // Clear content
    public final void clearContent(){
        content.clear();
    }

    // Replace the content with raw (already escaped) XML text
    protected final void setContent(String xml){
        content.clear();
        content.add(xml);
    }


    // Update values of attributes from current object state (i.e., numerical members)
    protected void updateAttribs(){ /* no attributes by default */ }


    /* Returns a fully-formed XML element string

        Attributes go inside the start tag and the content goes between the start and end tag.

            <tag_name attrib1="value1" attrib2="value2" ...>
                content...
            </tag_name>

        An element without content should generate an empty-element tag, i.e.,

            <tag_name attrib1="value1" attrib2="value2" ... />
    */
    @Override
    public final String toString(){
        return toString(OutputStyle.DEFAULT);
    }

    public final String toString(OutputStyle style){
        StringBuilder sb = new StringBuilder();
        write(sb, style, 0);
        return sb.toString();
    }

    private void write(StringBuilder sb, OutputStyle style, int depth){
        updateAttribs();
        String tag = getTag();
        String indent = style == OutputStyle.PRETTY ? "  ".repeat(depth) : "";
        sb.append(indent).append('<').append(tag);
        for(var a : attribs){
            if(a.val == null) // null means "omit this attribute"
                continue;
            sb.append(' ').append(a);
        }

        //empty element tag if no content
        if(content.stream().allMatch(c -> c instanceof String text && text.isBlank())) {
            sb.append("/>");
            return;
        }
        sb.append('>');
        String newline = style == OutputStyle.MINIFIED ? "" : "\n";
        String childIndent = style == OutputStyle.PRETTY ? "  ".repeat(depth + 1) : "";
        for(Object child : content){
            sb.append(newline);
            if(child instanceof Elem e)
                e.write(sb, style, depth + 1);
            else
                sb.append(childIndent).append(child);
        }
        sb.append(newline).append(indent).append("</").append(tag).append('>');
    }
}
