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

    public void toFile(String filename){
		try (FileWriter fw = new FileWriter(filename)) {
			fw.write(toString());
			System.out.println("wrote SVG file: " + filename);
		}
		catch (IOException error) {
			System.out.println("could not write SVG file: " + filename);
		}
	}

	public void autoFit(float padding) {
        // Collects bounds from all shapes and adjusts viewBox
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        // Uses default bounds if no content exists
        if (content.isEmpty()) {
            return;
        }

        // Apply fitted bounds
        viewBox.fit(minX, minY, maxX, maxY, padding);
    }
}
