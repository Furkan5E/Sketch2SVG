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
import java.util.regex.Pattern;

public class Sketch {
    // Store all parsed or programmatically added shapes
    private final List<Shape> shapes = new ArrayList<>();
    private Integer background; // RGBA fill for the whole canvas, or null for none
    private final Variables variables = new Variables();
    private final Diagnostics diagnostics = new Diagnostics();
    private final List<Path> sourceFiles = new ArrayList<>();

    // While running a "group ... end" block: where new shapes go, and the enclosing groups' paint (outermost first)
    private Group currentGroup;
    private final List<StyleArgs> groupPaint = new ArrayList<>();

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

    // Problems reported by the last fromFile(): errors skip a line or block, warnings only ignore part of one
    public int getErrorCount() {
        return diagnostics.errors();
    }

    public int getWarningCount() {
        return diagnostics.warnings();
    }

    // Every file the last parse read or tried to include (missing ones too, so watchers notice when they appear)
    public List<Path> getSourceFiles() {
        return List.copyOf(sourceFiles);
    }

    // Converts <dir>/<name>.txt to <dir>/<name>.svg
    public boolean render(String dir, String name) {
        clear();
        Path folder = Path.of(dir);
        return fromFile(folder.resolve(name + ".txt").toString())
                && exportSVG(folder.resolve(name + ".svg").toString());
    }

    // Returns false if the file could not be written
    public boolean exportSVG(String svgFilePath) {
        return buildSVG().toFile(svgFilePath);
    }

    // The complete SVG document as text
    public String toSVGString() {
        return buildSVG().toString();
    }

    private SVG buildSVG() {
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
        return svg;
    }

    // Returns false if the file could not be read; bad lines are reported but skipped
    public boolean fromFile(String filename) {
        diagnostics.reset();
        sourceFiles.clear();
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
        run(lines);
        return true;
    }

    // Parses a script held in memory (e.g. read from stdin); includes resolve against baseDir
    public void fromString(String source, Path baseDir) {
        diagnostics.reset();
        sourceFiles.clear();
        List<SourceLine> lines = new ArrayList<>();
        load(source.lines().toList(), baseDir.toAbsolutePath(), null, new ArrayDeque<>(), lines);
        run(lines);
    }

    private void run(List<SourceLine> lines) {
        variables.clear(); // each script starts with a fresh set of variables
        executeBlock(lines, 0, lines.size());
    }

    // Reads a script file into its meaningful lines, inlining "include <path>" (relative to the including file)
    private void load(Path path, String label, Deque<Path> including, List<SourceLine> out) throws IOException {
        List<String> raw = Files.readAllLines(path);
        including.push(path.toRealPath());
        sourceFiles.add(path.toRealPath());
        load(raw, path.toAbsolutePath().getParent(), label, including, out);
        including.pop();
    }

    private void load(List<String> raw, Path baseDir, String label, Deque<Path> including, List<SourceLine> out) {
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
            Path included = baseDir.resolve(target);
            if (!Files.isRegularFile(included)) {
                sourceFiles.add(included.toAbsolutePath().normalize());
                error(line, "Included file not found: " + target);
                continue;
            }
            try {
                if (including.contains(included.toRealPath())) {
                    error(line, "Circular include of " + target);
                } else {
                    load(included, target, including, out);
                }
            } catch (IOException e) {
                error(line, "Could not read included file: " + target);
            }
        }
    }

    // First argument of an include line, optionally "quoted" to allow spaces; returns null if missing
    private String includeTarget(String args, SourceLine line) {
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
            diagnostics.warning(line.where(), "Ignored unrecognized argument '" + rest + "'");
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
                if (command.equals("group")) {
                    executeGroup(line, lines, i + 1, close);
                } else {
                    executeRepeat(line, lines, i + 1, close);
                }
                i = close + 1;
            } else if (command.equals("end")) {
                error(line, "end without matching repeat or group");
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
            diagnostics.syntaxError(line, e);
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

    // group [options]: at=/rot=/scale= transform the group as a whole (SVG <g>);
    // stroke/fill options become defaults for the shapes inside, which can still override them
    private void executeGroup(SourceLine line, List<SourceLine> lines, int bodyStart, int bodyEnd) {
        Group group = new Group();
        StyleArgs args;
        try (Scanner ls = new Scanner(variables.substituteLine(line.text())).useLocale(Locale.ROOT)) {
            ls.next(); // "group"
            args = StyleArgs.parse(ls, null, line.where(), diagnostics);
        } catch (NoSuchElementException e) {
            diagnostics.syntaxError(line, e);
            return;
        }
        args.applyTransform(group);
        addShape(group);

        Group outer = currentGroup;
        currentGroup = group;
        groupPaint.add(args);
        try {
            executeBlock(lines, bodyStart, bodyEnd);
        } finally {
            groupPaint.remove(groupPaint.size() - 1);
            currentGroup = outer;
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
        return command.equals("repeat") || command.equals("group");
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
            diagnostics.syntaxError(line, e);
        }
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
            diagnostics.warning(line.where(), "Ignored unrecognized argument '" + tokens.get(3) + "'");
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
                StyleArgs own = StyleArgs.parse(ls, shape, line.where(), diagnostics);
                for (StyleArgs inherited : groupPaint) {
                    inherited.applyPaint(shape); // outer groups first, so inner ones win
                }
                own.applyTo(shape);
                addShape(shape);
            } else {
                diagnostics.error(line.where(), "Unknown shape command '" + type + "'");
            }
        }
    }

    // Adds to the group being built, or to the sketch at the top level
    private void addShape(Shape shape) {
        if (currentGroup != null) {
            currentGroup.add(shape);
        } else {
            shapes.add(shape);
        }
    }

    private void error(SourceLine line, String message) {
        diagnostics.error(line.where(), message);
    }

    private Shape parseShape(String type, Scanner ls) {
        return switch (type) {
            case "circle" -> {
                float r = size(ls, "radius");
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Circle(r, cx, cy);
            }
            case "ellipse" -> {
                float rx = size(ls, "x radius");
                float ry = size(ls, "y radius");
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Circle(rx, cx, cy).radii(rx, ry);
            }
            case "arc" -> {
                float radius = size(ls, "radius");
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
                float w = size(ls, "width");
                float h = size(ls, "height");
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Rect(w, h, cx, cy);
            }
            case "square" -> {
                float w = size(ls, "size");
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Square(w, cx, cy);
            }
            case "ngon" -> {
                int sides = ls.nextInt();
                float radius = size(ls, "radius");
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new RegPolygon(sides, radius, cx, cy);
            }
            case "trapezoid" -> {
                float topW = size(ls, "top width");
                float botW = size(ls, "bottom width");
                float h = size(ls, "height");
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Trapezoid(topW, botW, h, cx, cy);
            }
            case "star" -> {
                int points = ls.nextInt();
                float outerR = size(ls, "outer radius");
                float innerR = size(ls, "inner radius");
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new Star(points, outerR, innerR, cx, cy);
            }
            case "arrow" -> {
                float length = size(ls, "length");
                float width = size(ls, "width");
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
            case "path" -> parsePath(ls);
            case "roundrect" -> {
                float w = size(ls, "width");
                float h = size(ls, "height");
                float r = size(ls, "corner radius");
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                yield new RoundRect(w, h, r, cx, cy);
            }
            case "text" -> {
                float cx = ls.nextFloat();
                float cy = ls.nextFloat();
                float fontSize = size(ls, "font size");
                
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

    // Reads a size parameter; negative sizes would produce invalid or mirrored SVG
    private static float size(Scanner ls, String name) {
        float value = ls.nextFloat();
        if (value < 0) {
            throw new InputMismatchException(name + " must not be negative");
        }
        return value;
    }

    // "M x,y L x,y Q c,c x,y C c,c c,c x,y Z" with absolute (uppercase) commands; extra points repeat
    // the previous command, and points after M continue as lines, like SVG
    private static final Pattern PATH_TOKEN = Pattern.compile("[MLQCZmlqcz]|" + StyleArgs.POINT.pattern());

    private static PathShape parsePath(Scanner ls) {
        PathShape path = new PathShape();
        char command = 0;
        List<float[]> pending = new ArrayList<>();
        while (ls.hasNext(PATH_TOKEN)) {
            String tok = ls.next();
            if (Character.isLetter(tok.charAt(0))) {
                if (Character.isLowerCase(tok.charAt(0))) {
                    throw new InputMismatchException("relative path command '" + tok + "' is not supported, use " + tok.toUpperCase(Locale.ROOT));
                }
                if (!pending.isEmpty()) {
                    throw new InputMismatchException("incomplete " + command + " segment");
                }
                command = tok.charAt(0);
                if (command != 'M' && path.isEmpty()) {
                    throw new InputMismatchException("path must start with M");
                }
                if (command == 'Z') {
                    path.close();
                }
                continue;
            }
            if (command == 0 || command == 'Z') {
                throw new InputMismatchException("point " + tok + " needs a command before it");
            }
            String[] xy = tok.split(",");
            pending.add(new float[]{Float.parseFloat(xy[0]), Float.parseFloat(xy[1])});

            int needed = command == 'Q' ? 2 : command == 'C' ? 3 : 1;
            if (pending.size() == needed) {
                float[] a = pending.get(0);
                switch (command) {
                    case 'M' -> {
                        path.moveTo(a[0], a[1]);
                        command = 'L'; // further points are lines
                    }
                    case 'L' -> path.lineTo(a[0], a[1]);
                    case 'Q' -> path.quadTo(a[0], a[1], pending.get(1)[0], pending.get(1)[1]);
                    default -> path.cubicTo(a[0], a[1], pending.get(1)[0], pending.get(1)[1], pending.get(2)[0], pending.get(2)[1]);
                }
                pending.clear();
            }
        }
        if (!pending.isEmpty()) {
            throw new InputMismatchException("incomplete " + command + " segment");
        }
        if (path.isEmpty()) {
            throw new InputMismatchException("path needs at least M x,y");
        }
        return path.recentre();
    }
}
