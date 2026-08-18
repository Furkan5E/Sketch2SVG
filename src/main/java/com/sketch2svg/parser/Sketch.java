package com.sketch2svg.parser;

import com.sketch2svg.core.Shape;
import com.sketch2svg.shapes.*;
import com.sketch2svg.svg.SVG;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Sketch {
    // Store all parsed or programmatically added shapes
    private final List<Shape> shapes = new ArrayList<>();

    public Sketch add(Shape shape) {
        if (shape != null) {
            shapes.add(shape);
        }
        return this;
    }

    public List<Shape> getShapes() {
        return shapes;
    }

    public void clear() {
        shapes.clear();
    }

    public void render(String dir, String name) {
        clear();
        String txtFilePath = dir + name + ".txt";
        String svgFilePath = dir + name + ".svg";
        
        fromFile(txtFilePath);
        exportSVG(svgFilePath);
    }

    public void exportSVG(String svgFilePath) {
        SVG svg = new SVG();
        for (Shape shape : shapes) {
            svg.addContent(shape);
        }
        svg.toFile(svgFilePath);
    }

    public void fromFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            System.err.println("Error: File not found: " + filename);
            return;
        }

        int lineNum = 0;
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                lineNum++;
                String line = sc.nextLine().trim();

                // Ignore blanks and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                try (Scanner ls = new Scanner(line)) {
                    if (!ls.hasNext()) {
                        continue;
                    }

                    String type = ls.next().toLowerCase();
                    Shape shape = parseShape(type, ls);

                    if (shape != null) {
                        applyOptionalStyle(ls, shape);
                        shapes.add(shape);
                    } else {
                        System.err.printf("[Warning] Line %d: Unknown shape command '%s'%n", lineNum, type);
                    }
				} catch (NoSuchElementException e) {		
                    System.err.printf("[Syntax Error] Line %d: Invalid or missing parameters in '%s'%n", lineNum, line);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could not open: " + filename);
        }
    }

    private Shape parseShape(String type, Scanner ls) {
        return switch (type) {
            case "circle" -> {
                float r = ls.nextFloat();
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Circle(r, cx, cy);
            }
            case "arc" -> {
                float radius = ls.nextFloat();
                float angle = ls.nextFloat();
                float length = ls.nextFloat();
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Arc(radius, angle, length, cx, cy);
            }
            case "line" -> {
                float x1 = ls.nextFloat();
                float y1 = ls.nextFloat();
                float x2 = ls.nextFloat();
                float y2 = ls.nextFloat();
                yield new Line(x1, y1, x2, y2);
            }
            case "rect" -> {
                float w = ls.nextFloat();
                float h = ls.nextFloat();
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Rect(w, h, cx, cy);
            }
            case "square" -> {
                float w = ls.nextFloat();
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Square(w, cx, cy);
            }
            case "ngon" -> {
                int sides = ls.nextInt();
                float radius = ls.nextFloat();
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new RegPolygon(sides, radius, cx, cy);
            }
            case "trapezoid" -> {
                float topW = ls.nextFloat();
                float botW = ls.nextFloat();
                float h = ls.nextFloat();
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Trapezoid(topW, botW, h, cx, cy);
            }
            case "star" -> {
                int points = ls.nextInt();
                float outerR = ls.nextFloat();
                float innerR = ls.nextFloat();
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Star(points, outerR, innerR, cx, cy);
            }
            case "arrow" -> {
                float length = ls.nextFloat();
                float width = ls.nextFloat();
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                float rot = ls.hasNextFloat() ? ls.nextFloat() : 0f;
                Arrow arrow = new Arrow(length, width, cx, cy);
                arrow.setRotation(rot);
                yield arrow;
            }
            case "text" -> {
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                float fontSize = ls.nextFloat();
                
                String content;
                // Direct line search extracts quoted text cleanly across whitespace tokens
                String quoted = ls.findInLine("\"([^\"]*)\"");
                if (quoted != null) {
                    content = quoted.substring(1, quoted.length() - 1);
                } else {
                    content = ls.next();
                }
                
                yield new Text(content, cx, cy, fontSize);
            }
            default -> null;
        };
    }

    private static boolean isHexRGBA(String s) {
        return s.matches("(?i)[0-9a-f]{8}");
    }

    private static void applyOptionalStyle(Scanner ls, Shape shape) {
        Float strokeW = null;
        ArrayList<Integer> hexes = new ArrayList<>();

        while (ls.hasNext()) {
            String tok = ls.next();

            if (isHexRGBA(tok)) {
                int rgba = (int) Long.parseLong(tok, 16);
                hexes.add(rgba);
            } else {
                try {
                    strokeW = Float.parseFloat(tok);
                } catch (NumberFormatException ignored) {
                    // Ignore unrecognized token
                }
            }
        }

        if (strokeW != null) {
            shape.setStrokeWidth(strokeW);
        }
        if (!hexes.isEmpty()) {
            shape.setStroke(hexes.get(0));
        }
        if (hexes.size() >= 2) {
            shape.setFill(hexes.get(1));
        }
    }
}