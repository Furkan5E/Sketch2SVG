package com.sketch2svg.parser;

import com.sketch2svg.core.Shape;
import com.sketch2svg.svg.ColorInt;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

// Optional arguments that follow a command's required parameters:
//   positional  [strokeWidth] [strokeColor] [fillColor]
//   named       rot=<deg> stroke=<color|width> fill=<color> stroke-width=<n> (sw=) at=<x,y> scale=<s|sx,sy>
//               dash=<a,b,...|none> cap=<butt|round|square> join=<miter|round|bevel>
// Named arguments may appear in any order and override positional ones.
final class StyleArgs {

    // Plain decimal numbers only (Float.parseFloat alone would also accept "NaN", "Infinity", "1f", hex floats)
    static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

    // "x,y" pair used by points, at= and scale=
    static final Pattern POINT = Pattern.compile(NUMBER.pattern() + "," + NUMBER.pattern());

    Float strokeWidth;
    Integer stroke;
    Integer fill;
    Float rotation;
    float[] at;
    float[] scale;
    float[] dash;   // empty = explicitly solid (dash=none), so a shape can undo its group's dashes
    String cap;
    String join;

    // Reads the remaining tokens of a line. `where` prefixes warnings, e.g. "Line 5".
    // legacyArrowRotation enables the deprecated "arrow ... <rot> <strokeWidth>" form.
    static StyleArgs parse(Scanner ls, boolean legacyArrowRotation, String where, Diagnostics diagnostics) {
        StyleArgs args = new StyleArgs();
        List<Float> numbers = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        while (ls.hasNext()) {
            String tok = ls.next();
            int eq = tok.indexOf('=');

            if (eq > 0) {
                args.applyNamed(tok.substring(0, eq).toLowerCase(Locale.ROOT), tok.substring(eq + 1), tok, where, diagnostics);
            } else if (ColorInt.isColor(tok)) {
                colors.add(ColorInt.parseColor(tok));
            } else if (NUMBER.matcher(tok).matches()) {
                numbers.add(Float.parseFloat(tok));
            } else if (tok.startsWith("#")) {
                break; // trailing comment
            } else {
                diagnostics.warning(where, "Ignored unrecognized argument '" + tok + "'");
            }
        }

        if (legacyArrowRotation && args.rotation == null && numbers.size() >= 2) {
            diagnostics.warning(where, "Positional arrow rotation is deprecated, use rot=" + numbers.get(0));
            args.rotation = numbers.remove(0);
        }
        if (numbers.size() > 1) {
            diagnostics.warning(where, "Ignored extra numbers " + numbers.subList(1, numbers.size()) + " (only one stroke width is allowed)");
        }
        if (colors.size() > 2) {
            diagnostics.warning(where, "Ignored " + (colors.size() - 2) + " extra color(s) (expected stroke then fill)");
        }

        // Named values win over positional ones
        if (args.strokeWidth == null && !numbers.isEmpty()) args.strokeWidth = numbers.get(0);
        if (args.stroke == null && !colors.isEmpty()) args.stroke = colors.get(0);
        if (args.fill == null && colors.size() >= 2) args.fill = colors.get(1);

        if (args.strokeWidth != null && args.strokeWidth < 0) {
            throw new InputMismatchException("stroke width must not be negative");
        }
        return args;
    }

    private void applyNamed(String key, String value, String tok, String where, Diagnostics diagnostics) {
        switch (key) {
            case "rot" -> rotation = number(value, tok);
            case "fill" -> fill = color(value, tok);
            case "stroke" -> {
                // stroke=<color> sets the colour, stroke=<number> the width
                if (ColorInt.isColor(value)) stroke = ColorInt.parseColor(value);
                else strokeWidth = number(value, tok);
            }
            case "stroke-width", "sw" -> strokeWidth = number(value, tok);
            case "at" -> at = pair(value, tok);
            case "scale" -> scale = NUMBER.matcher(value).matches()
                    ? new float[]{number(value, tok), number(value, tok)}
                    : pair(value, tok);
            case "dash" -> dash = dashes(value, tok);
            case "cap" -> cap = keyword(value, tok, "butt", "round", "square");
            case "join" -> join = keyword(value, tok, "miter", "round", "bevel");
            default -> diagnostics.warning(where, "Ignored unrecognized argument '" + tok + "'");
        }
    }

    // Stroke and fill (what groups pass down to their shapes)
    void applyPaint(Shape shape) {
        if (strokeWidth != null) shape.setStrokeWidth(strokeWidth);
        if (stroke != null) shape.setStroke(stroke);
        if (fill != null) shape.setFill(fill);
        if (dash != null) shape.setDash(dash);
        if (cap != null) shape.setLineCap(cap);
        if (join != null) shape.setLineJoin(join);
    }

    // dash=<len,len,...> (dash and gap lengths) or dash=none
    private static float[] dashes(String value, String tok) {
        if (value.equalsIgnoreCase("none")) {
            return new float[0];
        }
        String[] parts = value.split(",");
        float[] lengths = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            lengths[i] = number(parts[i], tok);
            if (lengths[i] < 0) {
                throw new InputMismatchException("dash lengths must not be negative in " + tok);
            }
        }
        return lengths;
    }

    private static String keyword(String value, String tok, String... allowed) {
        String lower = value.toLowerCase(Locale.ROOT);
        for (String a : allowed) {
            if (a.equals(lower)) {
                return a;
            }
        }
        throw new InputMismatchException("Expected " + String.join("|", allowed) + " in " + tok);
    }

    // Position, rotation and scale
    void applyTransform(Shape shape) {
        if (at != null) shape.setPos(at[0], at[1]);
        if (rotation != null) shape.setRotation(rotation);
        if (scale != null) shape.setScale(scale[0], scale[1]);
    }

    void applyTo(Shape shape) {
        applyPaint(shape);
        applyTransform(shape);
    }

    private static float number(String value, String tok) {
        if (!NUMBER.matcher(value).matches())
            throw new InputMismatchException("Invalid number in " + tok);
        return Float.parseFloat(value);
    }

    private static int color(String value, String tok) {
        if (!ColorInt.isColor(value))
            throw new InputMismatchException("Invalid color in " + tok);
        return ColorInt.parseColor(value);
    }

    private static float[] pair(String value, String tok) {
        if (!POINT.matcher(value).matches())
            throw new InputMismatchException("Expected x,y in " + tok);
        String[] xy = value.split(",");
        return new float[]{Float.parseFloat(xy[0]), Float.parseFloat(xy[1])};
    }
}
