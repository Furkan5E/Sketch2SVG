package com.sketch2svg.parser;

import com.sketch2svg.core.Shape;
import com.sketch2svg.math.Vec2;
import com.sketch2svg.math.ViewBox;
import com.sketch2svg.shapes.*;
import com.sketch2svg.svg.ColorInt;
import com.sketch2svg.svg.SVG;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Sketch {
    // Store all parsed or programmatically added shapes
    private final List<Shape> shapes = new ArrayList<>();
    private Integer background; // RGBA fill for the whole canvas, or null for none
    private final Variables variables = new Variables();

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
        background = null;
    }

    public Integer getBackground() {
        return background;
    }

    public Sketch setBackground(Integer rgba) {
        this.background = rgba;
        return this;
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
        if (background != null) {
            // Cover the whole viewBox; convert its SVG-space centre back to +y-up sketch space
            ViewBox vb = svg.getViewBox();
            svg.addContent(new Rect(vb.w, vb.h, vb.x + vb.w * 0.5f, -(vb.y + vb.h * 0.5f))
                    .setFill(background).setStrokeWidth(0.f));
        }
        for (Shape shape : shapes) {
            svg.addContent(shape);
        }
        return svg.toFile(svgFilePath);
    }

    // Returns false if the file could not be read; bad lines are reported but skipped
    public boolean fromFile(String filename) {
        Path path = Path.of(filename);
        if (!Files.isRegularFile(path)) {
            System.err.println("Error: File not found: " + filename);
            return false;
        }

        List<SourceLine> lines = new ArrayList<>();
        try {
            load(path, null, new ArrayDeque<>(), lines);
        } catch (IOException e) {
            System.err.println("Could not open: " + filename);
            return false;
        }

        variables.clear(); // each script starts with a fresh set of variables
        executeBlock(lines, 0, lines.size());
        return true;
    }

    // Reads a script into its meaningful lines, inlining "include <path>" (relative to the including file)
    private static void load(Path path, String label, Deque<Path> including, List<SourceLine> out) throws IOException {
        List<String> raw = Files.readAllLines(path);
        including.push(path.toRealPath());

        for (int i = 0; i < raw.size(); i++) {
            String text = raw.get(i).trim();
            if (text.isEmpty() || text.startsWith("#")) {
                continue; // blanks and comments
            }
            SourceLine line = new SourceLine(label, i + 1, text);

            String[] parts = text.split("\\s+", 2);
            if (!parts[0].equalsIgnoreCase("include")) {
                out.add(line);
                continue;
            }

            String target = parts.length > 1 ? includeTarget(parts[1], line) : null;
            if (target == null) {
                error(line, "include needs a file path");
                continue;
            }
            Path included = path.toAbsolutePath().getParent().resolve(target);
            if (!Files.isRegularFile(included)) {
                error(line, "Included file not found: " + target);
            } else if (including.contains(included.toRealPath())) {
                error(line, "Circular include of " + target);
            } else {
                try {
                    load(included, target, including, out);
                } catch (IOException e) {
                    error(line, "Could not read included file: " + target);
                }
            }
        }
        including.pop();
    }

    // First argument of an include line, optionally "quoted" to allow spaces; returns null if missing
    private static String includeTarget(String args, SourceLine line) {
        String target, rest;
        if (args.startsWith("\"")) {
            int close = args.indexOf('"', 1);
            if (close < 0) {
                return null;
            }
            target = args.substring(1, close);
            rest = args.substring(close + 1).trim();
        } else {
            String[] split = args.split("\\s+", 2);
            target = split[0];
            rest = split.length > 1 ? split[1] : "";
        }
        if (!rest.isEmpty() && !rest.startsWith("#")) {
            System.err.printf("[Warning] %s: Ignored unrecognized argument '%s'%n", line.where(), rest);
        }
        return target.isEmpty() ? null : target;
    }

    // Runs lines[start, end), expanding "repeat ... end" blocks
    private void executeBlock(List<SourceLine> lines, int start, int end) {
        int i = start;
        while (i < end) {
            SourceLine line = lines.get(i);
            String command = firstWord(line);

            if (isBlockStart(command)) {
                int close = findEnd(lines, i, end);
                if (close < 0) {
                    error(line, command + " without matching end (block runs to the end of the file)");
                    close = end;
                }
                executeRepeat(line, lines, i + 1, close);
                i = close + 1;
            } else if (command.equals("end")) {
                error(line, "end without matching repeat");
                i++;
            } else {
                executeLine(line);
                i++;
            }
        }
    }

    private static final int MAX_ITERATIONS = 100_000;

    // repeat <count> [index]: runs the body count times with index = 0, 1, ..., count-1
    private void executeRepeat(SourceLine line, List<SourceLine> lines, int bodyStart, int bodyEnd) {
        int count;
        String index;
        try {
            List<String> tokens = Variables.tokenize(line.text());
            if (tokens.size() < 2) {
                throw new InputMismatchException("repeat needs a count");
            }
            String countText = variables.substitute(tokens.get(1));
            if (!StyleArgs.NUMBER.matcher(countText).matches()) {
                throw new InputMismatchException("Invalid repeat count '" + countText + "'");
            }
            double value = Double.parseDouble(countText);
            if (value < 0 || value != Math.floor(value) || value > MAX_ITERATIONS) {
                throw new InputMismatchException("repeat count must be a whole number from 0 to " + MAX_ITERATIONS);
            }
            count = (int) value;

            index = tokens.size() > 2 && !tokens.get(2).startsWith("#") ? tokens.get(2) : null;
            if (index != null && !Variables.NAME.matcher(index).matches()) {
                throw new InputMismatchException("Invalid variable name '" + index + "'");
            }
        } catch (NoSuchElementException e) {
            reportSyntaxError(line, e);
            return;
        }

        String previous = index != null ? variables.get(index) : null;
        for (int k = 0; k < count; k++) {
            if (index != null) {
                variables.set(index, Integer.toString(k));
            }
            executeBlock(lines, bodyStart, bodyEnd);
        }
        if (index != null) {
            variables.restore(index, previous);
        }
    }

    // Index of the "end" closing the block opened at lines[open], or -1 if there is none before limit
    private static int findEnd(List<SourceLine> lines, int open, int limit) {
        int depth = 0;
        for (int i = open; i < limit; i++) {
            String command = firstWord(lines.get(i));
            if (isBlockStart(command)) {
                depth++;
            } else if (command.equals("end") && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isBlockStart(String command) {
        return command.equals("repeat");
    }

    private static String firstWord(SourceLine line) {
        return line.text().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }

    private void executeLine(SourceLine line) {
        try {
            List<String> tokens = Variables.tokenize(line.text());
            if (tokens.get(0).equalsIgnoreCase("set")) {
                executeSet(tokens, line);
            } else {
                executeCommand(variables.substituteLine(line.text()), line);
            }
        } catch (NoSuchElementException e) {
            reportSyntaxError(line, e);
        }
    }

    private static void reportSyntaxError(SourceLine line, NoSuchElementException e) {
        String reason = e.getMessage() != null ? " (" + e.getMessage() + ")" : "";
        System.err.printf("[Syntax Error] %s: Invalid or missing parameters in '%s'%s%n", line.where(), line.text(), reason);
    }

    // set <name> <value>: the value may be a number, {expression}, color, "text" or another variable
    private void executeSet(List<String> tokens, SourceLine line) {
        if (tokens.size() < 3) {
            throw new InputMismatchException("set needs a name and a value");
        }
        String name = tokens.get(1);
        if (!Variables.NAME.matcher(name).matches()) {
            throw new InputMismatchException("Invalid variable name '" + name + "'");
        }
        if (tokens.size() > 3 && !tokens.get(3).startsWith("#")) {
            System.err.printf("[Warning] %s: Ignored unrecognized argument '%s'%n", line.where(), tokens.get(3));
        }
        variables.set(name, variables.substitute(tokens.get(2)));
    }

    // Runs one already-substituted command line
    private void executeCommand(String text, SourceLine line) {
        try (Scanner ls = new Scanner(text).useLocale(Locale.ROOT)) {
            String type = ls.next().toLowerCase(Locale.ROOT);
            if (type.equals("background")) {
                String color = ls.next();
                if (!ColorInt.isColor(color)) {
                    throw new InputMismatchException("Invalid color: " + color);
                }
                background = ColorInt.parseColor(color);
                return;
            }
            Shape shape = parseShape(type, ls);

            if (shape != null) {
                StyleArgs.parse(ls, shape instanceof Arrow, line.where()).applyTo(shape);
                shapes.add(shape);
            } else {
                System.err.printf("[Warning] %s: Unknown shape command '%s'%n", line.where(), type);
            }
        }
    }

    private static void error(SourceLine line, String message) {
        System.err.printf("[Error] %s: %s%n", line.where(), message);
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
                while (ls.hasNext(StyleArgs.POINT)) {
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
}