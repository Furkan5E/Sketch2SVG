package com.sketch2svg.svg;

import com.sketch2svg.math.*;
import com.sketch2svg.core.*;

import java.io.FileWriter;
import java.io.IOException;

public class SVG extends Elem{
    private ViewBox viewBox = new ViewBox();
    private Attrib attribXmlns;
    private Attrib attribViewBox;
    private Attrib attribWidth;
    private Attrib attribHeight;
    private Integer pixelWidth; // display width; null leaves the size to whatever embeds the SVG

    public SVG(){
        attribXmlns = newAttrib("xmlns", "http://www.w3.org/2000/svg");
        attribWidth = newAttrib("width");
        attribHeight = newAttrib("height");
        attribViewBox = newAttrib("viewBox");
    }

    // Display width in pixels; the height follows the viewBox's aspect ratio
    public void setPixelWidth(Integer width){
        if(width != null && width <= 0)
            throw new IllegalArgumentException("Width must be positive: " + width);
        this.pixelWidth = width;
    }

    @Override
    protected void updateAttribs(){
        attribViewBox.val = viewBox.toString();
        attribWidth.val = pixelWidth == null ? null : pixelWidth.toString();
        attribHeight.val = pixelWidth == null ? null
                : Long.toString(Math.max(1, Math.round(pixelWidth * (double) viewBox.h / viewBox.w)));
    }

    @Override
    public String getTag(){
        return "svg";
    }

    public ViewBox getViewBox(){
        return viewBox;
    }

    // Returns false if the file could not be written
    public boolean toFile(String filename){
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(toString());
            System.out.println("wrote SVG file: " + filename);
            return true;
        }
        catch (IOException error) {
            System.err.println("could not write SVG file: " + filename + " (" + error.getMessage() + ")");
            return false;
        }
    }
}
