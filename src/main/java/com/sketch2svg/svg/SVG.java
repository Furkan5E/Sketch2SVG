package com.sketch2svg.svg;

import com.sketch2svg.math.*;
import com.sketch2svg.core.*;

import java.io.FileWriter;
import java.io.IOException;

public class SVG extends Elem{
    private ViewBox viewBox = new ViewBox();
    private Attrib attribXmlns;
    private Attrib attribViewBox;

    public SVG(){
        attribXmlns = newAttrib("xmlns", "http://www.w3.org/2000/svg");
        attribViewBox = newAttrib("viewBox");
    }

    @Override
    protected void updateAttribs(){
        attribViewBox.val = viewBox.toString();
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
