package com.sketch2svg.parser;

import com.sketch2svg.core.Shape;
import com.sketch2svg.shapes.*;
import com.sketch2svg.svg.SVG;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;

public class Sketch {
	//store all the parsed shapes
    private ArrayList<Shape> shapes = new ArrayList<>();
    public void render(String dir, String name) {
        shapes.clear();
        String txtFilePath = dir + name + ".txt";
        String svgFilePath = dir + name + ".svg";
        fromFile(txtFilePath);
        
        SVG svg = new SVG();
        for(int i = 0; i<shapes.size(); i++){
            Shape shape = shapes.get(i);
            svg.addContent(shape);
        }
        svg.toFile(svgFilePath);
    }

   public void fromFile(String filename){
		try (Scanner sc = new Scanner(new File(filename))) {
			//process txt file one line at a time
			while (sc.hasNextLine()) {
				String line = sc.nextLine().trim();

				// ignore blanks and comments
				if (line.isEmpty())
					continue;
				if (line.startsWith("#"))
					continue;
				Scanner ls = new Scanner(line);
				if (!ls.hasNext()) { 
					ls.close();
					continue;
				}
				//find the shape type
				String type = ls.next().toLowerCase();
				Shape shape = null;
				
				switch (type) {
					case "circle": {
						float r = ls.nextFloat();
						float cx = ls.nextFloat();
						float cy = ls.nextFloat();
						shape = new Circle(r, cx, cy);
					} break;

					case "arc": {
						float radius = ls.nextFloat();
						float angle  = ls.nextFloat();
						float length = ls.nextFloat();
						float cx = ls.nextFloat();
						float cy = ls.nextFloat();
						shape = new Arc(radius, angle, length, cx, cy);
					} break;
					case "line": {
						float x1 = ls.nextFloat();
						float y1 = ls.nextFloat();
						float x2 = ls.nextFloat();
						float y2 = ls.nextFloat();
						shape = new Line(x1, y1, x2, y2);
					} break;
					case "rect": {
						float w = ls.nextFloat();
						float h = ls.nextFloat();
						float cx = ls.nextFloat();
						float cy = ls.nextFloat();
						shape = new Rect(w, h, cx, cy);
					} break;
					case"square": {
						float w = ls.nextFloat();
						float cx = ls.nextFloat();
						float cy = ls.nextFloat();
						shape = new Square(w, cx, cy);
					} break;
					case "ngon":{
						int sides = ls.nextInt();
						float radius = ls.nextFloat();
						float cx = ls.nextFloat();
						float cy = ls.nextFloat();
						shape = new RegPolygon(sides, radius, cx, cy);
					} break;
					
					case "trapezoid": {
						float topW = ls.nextFloat();
						float botW = ls.nextFloat();
						float h = ls.nextFloat();
						float cx = ls.nextFloat();
						float cy = ls.nextFloat();
						shape = new Trapezoid(topW, botW, h, cx, cy);
					} break;
					case "star": {
						int points = ls.nextInt();
						float outerR = ls.nextFloat();
						float innerR = ls.nextFloat();
						float cx = ls.nextFloat();
						float cy = ls.nextFloat();
						shape = new Star(points, outerR, innerR, cx, cy);
					} break;
					case "arrow":{
						float length = ls.nextFloat();
						float width  = ls.nextFloat();
						float cx = ls.nextFloat();
						float cy = ls.nextFloat();
						float rot = 0f;
						if (ls.hasNextFloat()) rot = ls.nextFloat();

						shape = new Arrow(length, width, cx, cy);
						shape.setRotation(rot);
					} break;

					default:
						//unknown command ignore line
						break;
				}

				//optional style parameters strokeWidth, strokeColor, fillColor
				if (shape != null) {
					applyOptionalStyle(ls, shape);
					shapes.add(shape);
				}
				ls.close();
			}

		} catch (FileNotFoundException e) {
			System.out.println("Could not open: " + filename);
		}
	}
	private static boolean isHexRGBA(String s) {
		//8 hex digits
		return s.matches("(?i)[0-9a-f]{8}");
	}
	private static void applyOptionalStyle(Scanner ls, Shape shape) {
		Float strokeW = null;
		ArrayList<Integer> hexes = new ArrayList<>();

		while (ls.hasNext()) {
			String tok = ls.next();

			if (isHexRGBA(tok)) {
				//parse unsigned 32bit hex into int
				int rgba = (int) Long.parseLong(tok, 16);
				hexes.add(rgba);
			}
			else {
				//treat as stroke width if it parses as float
				try{
					strokeW = Float.parseFloat(tok);
				}
				catch (NumberFormatException ignored) {
					//ignore unrecognised
				}
			}
		}

		//apply parsed optional values
		if (strokeW != null)
			shape.setStrokeWidth(strokeW);

		if (hexes.size() >= 1)
			shape.setStroke(hexes.get(0));
		if (hexes.size() >= 2)
			shape.setFill(hexes.get(1));
    }
}
