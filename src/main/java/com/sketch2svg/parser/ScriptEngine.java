package com.sketch2svg.parser;

import com.sketch2svg.core.Shape;
import com.sketch2svg.math.Vec2;
import com.sketch2svg.shapes.*;
import com.sketch2svg.svg.ColorInt;
import com.sketch2svg.svg.Gradient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

// Runs a sketch script: reads its lines (inlining includes), expands variables, loops and groups,
// and adds what each command draws to a Sketch
final class ScriptEngine {

    private final Sketch sketch;
    private final Diagnostics diagnostics;
    private final List<Path> sourceFiles; // every file read or tried to include, recorded for the sketch
    private final Variables variables = new Variables(); // each script starts with a fresh set

    // While running a "group ... end" block: where new shapes go, and the enclosing groups' paint (outermost first)
    private Group currentGroup;
    private final List<StyleArgs> groupPaint = new ArrayList<>();

    ScriptEngine(Sketch sketch, Diagnostics diagnostics, List<Path> sourceFiles) {
        this.sketch = sketch;
        this.diagnostics = diagnostics;
        this.sourceFiles = sourceFiles;
    }

    // Returns false if the file could not be read; bad lines are reported and skipped
    boolean runFile(String filename) {
        Path path;
        try {
            path = Path.of(filename);
        } catch (InvalidPathException e) {
            diagnostics.error(null, "Invalid file path: " + filename + " (" + e.getReason() + ")");
            return false;
        }
        if (!Files.isRegularFile(path)) {
            diagnostics.error(null, "File not found: " + filename);
            return false;
        }

        List<SourceLine> lines = new ArrayList<>();
        try {
            load(path, null, new ArrayDeque<>(), lines);
        } catch (IOException e) {
            diagnostics.error(null, "Could not open: " + filename);
            return false;
        }
        executeBlock(lines, 0, lines.size());
        return true;
    }

    // Runs a script held in memory (e.g. read from stdin); includes resolve against baseDir
    void runSource(String source, Path baseDir) {
        List<SourceLine> lines = new ArrayList<>();
        load(source.lines().toList(), baseDir.toAbsolutePath(), null, new ArrayDeque<>(), lines);
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
            Path included;
            try {
                included = baseDir.resolve(target);
            } catch (InvalidPathException e) {
                error(line, "Invalid include path: " + target + " (" + e.getReason() + ")");
                continue;
            }
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
            List<String> tokens = Tokens.split(line.text());
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
        try {
            Tokens ls = variables.substituteAll(Tokens.split(line.text()));
            ls.next(); // "group"
            args = StyleArgs.parse(ls, null, line.where(), diagnostics, sketch.gradientNames());
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
            List<String> tokens = Tokens.split(line.text());
            if (tokens.get(0).equalsIgnoreCase("set")) {
                executeSet(tokens, line);
            } else {
                executeCommand(variables.substituteAll(tokens), line);
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
    private void executeCommand(Tokens ls, SourceLine line) {
        String type = ls.next().toLowerCase(Locale.ROOT);
        if (type.equals("background")) {
            String color = ls.next();
            if (sketch.gradientNames().contains(color)) {
                sketch.setBackgroundGradient(color);
            } else if (ColorInt.isColor(color)) {
                sketch.setBackground(ColorInt.parseColor(color));
            } else {
                throw new InputMismatchException("Invalid color: " + color);
            }
            return;
        }
        if (type.equals("gradient")) {
            defineGradient(ls);
            return;
        }
        if (type.equals("title") || type.equals("desc")) {
            // title "text" (quotes optional; without them the rest of the line is used)
            String quoted = ls.hasNext() ? Tokens.quoted(ls.peek()) : null;
            String value = quoted != null ? quoted : ls.rest();
            if (value.isEmpty()) {
                throw new InputMismatchException(type + " needs some text");
            }
            if (type.equals("title")) sketch.setTitle(value);
            else sketch.setDescription(value);
            return;
        }
        if (type.equals("canvas")) {
            // canvas auto [padding] | canvas <width> <height> [cx cy]
            if (ls.hasNext("(?i)auto")) {
                ls.next();
                sketch.setAutoFit(ls.hasNext(StyleArgs.NUMBER) ? size(ls, "padding") : 10.f);
            } else {
                float w = size(ls, "width"), h = size(ls, "height");
                if (w == 0 || h == 0) {
                    throw new InputMismatchException("canvas size must be greater than zero");
                }
                float cx = 0.f, cy = 0.f;
                if (ls.hasNext(StyleArgs.NUMBER)) { // optional centre, always given as a pair
                    cx = number(ls);
                    cy = number(ls);
                }
                sketch.setCanvas(w, h, cx, cy);
            }
            return;
        }
        Shape shape = parseShape(type, ls);

        if (shape != null) {
            StyleArgs own = StyleArgs.parse(ls, shape, line.where(), diagnostics, sketch.gradientNames());
            for (StyleArgs inherited : groupPaint) {
                inherited.applyPaint(shape); // outer groups first, so inner ones win
            }
            own.applyTo(shape);
            addShape(shape);
        } else {
            diagnostics.error(line.where(), "Unknown shape command '" + type + "'");
        }
    }

    // gradient <name> linear [angle] <color> <color> ... | gradient <name> radial <color> <color> ...
    private void defineGradient(Tokens ls) {
        String name = ls.next();
        if (!Variables.NAME.matcher(name).matches()) {
            throw new InputMismatchException("Invalid gradient name '" + name + "'");
        }
        if (ColorInt.isColor(name)) {
            throw new InputMismatchException("Gradient name '" + name + "' is already a color name");
        }
        String type = ls.next().toLowerCase(Locale.ROOT);
        if (!type.equals("linear") && !type.equals("radial")) {
            throw new InputMismatchException("Expected linear or radial, not '" + type + "'");
        }
        // Optional angle: a number that isn't also a 6/8-digit hex colour such as 000000
        float angle = type.equals("linear") && ls.hasNext(StyleArgs.NUMBER) && !ls.hasNext("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}")
                ? number(ls) : 0.f;

        List<Integer> colors = new ArrayList<>();
        while (ls.hasNext() && !ls.hasNext("#|#[^0-9a-fA-F].*")) { // stop at a trailing # comment
            String tok = ls.next();
            if (!ColorInt.isColor(tok)) {
                throw new InputMismatchException("Invalid color: " + tok);
            }
            colors.add(ColorInt.parseColor(tok));
        }
        if (colors.size() < 2) {
            throw new InputMismatchException("A gradient needs at least two colors");
        }
        sketch.addGradient(name, type.equals("linear") ? Gradient.linear(name, angle, colors) : Gradient.radial(name, colors));
    }

    // Adds to the group being built, or to the sketch at the top level
    private void addShape(Shape shape) {
        if (currentGroup != null) {
            currentGroup.add(shape);
        } else {
            sketch.add(shape);
        }
    }

    private void error(SourceLine line, String message) {
        diagnostics.error(line.where(), message);
    }

    private Shape parseShape(String type, Tokens ls) {
        return switch (type) {
            case "circle" -> {
                float r = size(ls, "radius");
                float cx = number(ls);
                float cy = number(ls);
                yield new Circle(r, cx, cy);
            }
            case "ellipse" -> {
                float rx = size(ls, "x radius");
                float ry = size(ls, "y radius");
                float cx = number(ls);
                float cy = number(ls);
                yield new Circle(rx, cx, cy).radii(rx, ry);
            }
            case "arc" -> {
                float radius = size(ls, "radius");
                float angle = number(ls);
                float length = number(ls);
                float cx = number(ls);
                float cy = number(ls);
                yield new Arc(radius, angle, length, cx, cy);
            }
            case "line" -> {
                float x1 = number(ls);
                float y1 = number(ls);
                float x2 = number(ls);
                float y2 = number(ls);
                yield new Line(x1, y1, x2, y2);
            }
            case "rect" -> {
                float w = size(ls, "width");
                float h = size(ls, "height");
                float cx = number(ls);
                float cy = number(ls);
                yield new Rect(w, h, cx, cy);
            }
            case "square" -> {
                float w = size(ls, "size");
                float cx = number(ls);
                float cy = number(ls);
                yield new Square(w, cx, cy);
            }
            case "ngon" -> {
                int sides = ls.nextInt();
                float radius = size(ls, "radius");
                float cx = number(ls);
                float cy = number(ls);
                yield new RegPolygon(sides, radius, cx, cy);
            }
            case "trapezoid" -> {
                float topW = size(ls, "top width");
                float botW = size(ls, "bottom width");
                float h = size(ls, "height");
                float cx = number(ls);
                float cy = number(ls);
                yield new Trapezoid(topW, botW, h, cx, cy);
            }
            case "star" -> {
                int points = ls.nextInt();
                float outerR = size(ls, "outer radius");
                float innerR = size(ls, "inner radius");
                float cx = number(ls);
                float cy = number(ls);
                yield new Star(points, outerR, innerR, cx, cy);
            }
            case "arrow" -> {
                float length = size(ls, "length");
                float width = size(ls, "width");
                float cx = number(ls);
                float cy = number(ls);
                yield new Arrow(length, width, cx, cy);
            }
            case "polygon", "polyline" -> {
                boolean closed = type.equals("polygon");
                List<Vec2> points = new ArrayList<>();
                while (ls.hasNext(StyleArgs.POINT)) {
                    String tok = ls.next();
                    String[] xy = tok.split(",");
                    points.add(new Vec2(StyleArgs.finite(xy[0], tok), StyleArgs.finite(xy[1], tok)));
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
                float cx = number(ls);
                float cy = number(ls);
                yield new RoundRect(w, h, r, cx, cy);
            }
            case "text" -> {
                float cx = number(ls);
                float cy = number(ls);
                float fontSize = size(ls, "font size");
                
                // "Quoted content" is one token; quotes are only needed when it contains spaces
                String word = ls.next();
                String content = Tokens.quoted(word) != null ? Tokens.quoted(word) : word;
                
                yield new Text(content, cx, cy, fontSize);
            }
            default -> null;
        };
    }

    private static final Pattern NON_FINITE = Pattern.compile("[+-]?(NaN|Infinity)");

    // Reads a number; NaN, Infinity and values beyond float range are rejected, as SVG can't represent them
    private static float number(Tokens ls) {
        String tok = ls.next();
        boolean plain = StyleArgs.NUMBER.matcher(tok).matches();
        if (!plain && !NON_FINITE.matcher(tok).matches()) {
            throw new InputMismatchException();
        }
        float value = plain ? Float.parseFloat(tok) : Float.NaN;
        if (!Float.isFinite(value)) {
            throw new InputMismatchException("number " + tok + " is not finite or is too large");
        }
        return value;
    }

    // Reads a size parameter; negative sizes would produce invalid or mirrored SVG
    private static float size(Tokens ls, String name) {
        float value = number(ls);
        if (value < 0) {
            throw new InputMismatchException(name + " must not be negative");
        }
        return value;
    }

    // "M x,y L x,y Q c,c x,y C c,c c,c x,y Z" with absolute (uppercase) commands; extra points repeat
    // the previous command, and points after M continue as lines, like SVG
    private static final Pattern PATH_TOKEN = Pattern.compile("[MLQCZmlqcz]|" + StyleArgs.POINT.pattern());

    private static PathShape parsePath(Tokens ls) {
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
            pending.add(new float[]{StyleArgs.finite(xy[0], tok), StyleArgs.finite(xy[1], tok)});

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
