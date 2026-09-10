package com.sketch2svg.parser;

import com.sketch2svg.core.Shape;
import com.sketch2svg.math.Vec2;
import com.sketch2svg.shapes.*;
import com.sketch2svg.svg.ColorInt;
import com.sketch2svg.svg.SVG;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

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

    public boolean render(String dir, String name) {
        clear();
        String txtFilePath = dir + name + ".txt";
        String svgFilePath = dir + name + ".svg";

        return fromFile(txtFilePath) && exportSVG(svgFilePath);
    }

    // Returns false if the file could not be written
    public boolean exportSVG(String svgFilePath) {
        SVG svg = new SVG();
        for (Shape shape : shapes) {
            svg.addContent(shape);
        }
        return svg.toFile(svgFilePath);
    }

    // Returns false if the file could not be read; bad lines are reported but skipped
    public boolean fromFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            System.err.println("Error: File not found: " + filename);
            return false;
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

                try (Scanner ls = new Scanner(line).useLocale(Locale.ROOT)) {
                    if (!ls.hasNext()) {
                        continue;
                    }

                    String type = ls.next().toLowerCase(Locale.ROOT);
                    Shape shape = parseShape(type, ls);

                    if (shape != null) {
                        applyOptionalStyle(ls, shape, lineNum);
                        shapes.add(shape);
                    } else {
                        System.err.printf("[Warning] Line %d: Unknown shape command '%s'%n", lineNum, type);
                    }
                } catch (NoSuchElementException e) {
                    String reason = e.getMessage() != null ? " (" + e.getMessage() + ")" : "";
                    System.err.printf("[Syntax Error] Line %d: Invalid or missing parameters in '%s'%s%n", lineNum, line, reason);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could not open: " + filename);
            return false;
        }
        return true;
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
                yield new Arrow(length, width, cx, cy);
            }
            case "polygon", "polyline" -> {
                boolean closed = type.equals("polygon");
                List<Vec2> points = new ArrayList<>();
                while (ls.hasNext(POINT)) {
                    String[] xy = ls.next().split(",");
                    points.add(new Vec2(Float.parseFloat(xy[0]), Float.parseFloat(xy[1])));
                }
                if (points.size() < (closed ? 3 : 2)) {
                    throw new InputMismatchException(type + " needs at least " + (closed ? 3 : 2) + " x,y points");
                }
                yield new Polygon(points, closed);
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

    // Optional "rot=<degrees>" token accepted by every shape (counter-clockwise)
    private static final String ROTATION_KEY = "rot=";

    // Plain decimal numbers only (Float.parseFloat alone would also accept "NaN", "Infinity", "1f", hex floats)
    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

    // "x,y" point used by polygon/polyline
    private static final Pattern POINT = Pattern.compile(NUMBER.pattern() + "," + NUMBER.pattern());

    private static void applyOptionalStyle(Scanner ls, Shape shape, int lineNum) {
        ArrayList<Float> numbers = new ArrayList<>();
        ArrayList<Integer> hexes = new ArrayList<>();
        boolean namedRotation = false;

        while (ls.hasNext()) {
            String tok = ls.next();

            if (tok.toLowerCase(Locale.ROOT).startsWith(ROTATION_KEY)) {
                // Named so it can sit anywhere among the optional style tokens
                try {
                    shape.setRotation(Float.parseFloat(tok.substring(ROTATION_KEY.length())));
                    namedRotation = true;
                } catch (NumberFormatException e) {
                    throw new InputMismatchException("Invalid rotation: " + tok);
                }
            } else if (ColorInt.isHex(tok)) {
                hexes.add(ColorInt.parseHex(tok));
            } else if (NUMBER.matcher(tok).matches()) {
                numbers.add(Float.parseFloat(tok));
            } else if (tok.startsWith("#")) {
                break; // trailing comment
            } else {
                System.err.printf("[Warning] Line %d: Ignored unrecognized argument '%s'%n", lineNum, tok);
            }
        }

        // Legacy arrow form "arrow ... <rot> <strokeWidth>": a lone number is the stroke width like every other shape
        if (shape instanceof Arrow && !namedRotation && numbers.size() >= 2) {
            System.err.printf("[Warning] Line %d: Positional arrow rotation is deprecated, use rot=%s%n",
                    lineNum, numbers.get(0));
            shape.setRotation(numbers.remove(0));
        }

        if (numbers.size() > 1) {
            System.err.printf("[Warning] Line %d: Ignored extra numbers %s (only one stroke width is allowed)%n",
                    lineNum, numbers.subList(1, numbers.size()));
        }
        if (hexes.size() > 2) {
            System.err.printf("[Warning] Line %d: Ignored %d extra color(s) (expected stroke then fill)%n",
                    lineNum, hexes.size() - 2);
        }

        if (!numbers.isEmpty()) {
            shape.setStrokeWidth(numbers.get(0));
        }
        if (!hexes.isEmpty()) {
            shape.setStroke(hexes.get(0));
        }
        if (hexes.size() >= 2) {
            shape.setFill(hexes.get(1));
        }
    }
}