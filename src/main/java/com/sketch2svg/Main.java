package com.sketch2svg;
import com.sketch2svg.parser.Sketch;

public class Main {

	public static void main(String[] args) {
		
		var dir = "src/main/resources/";
		var sketch = new Sketch();
		// Sketch.render() attempts to read a sketch file and if successful, outputs an SVG file. THe first argument is the input/output directory and the second argument is the base name of the files. For example, if the base name is "foo" it will attempt to read in foo.txt and output foo.svg.
		sketch.render(dir, "sketch");
	}

}
