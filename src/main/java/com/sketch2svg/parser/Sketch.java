package com.sketch2svg.parser;

import com.sketch2svg.core.OutputStyle;
import com.sketch2svg.core.Shape;
import com.sketch2svg.math.Bounds;
import com.sketch2svg.math.ViewBox;
import com.sketch2svg.shapes.Group;
import com.sketch2svg.shapes.Line;
import com.sketch2svg.shapes.Rect;
import com.sketch2svg.svg.Defs;
import com.sketch2svg.svg.Gradient;
import com.sketch2svg.svg.SVG;
import com.sketch2svg.svg.TextElement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// A drawing: its shapes, canvas and output settings, built in code or loaded from a script
public class Sketch {
    // Store all parsed or programmatically added shapes
    private final List<Shape> shapes = new ArrayList<>();
    private Integer background; // RGBA fill for the whole canvas, or null for none
    private String backgroundGradient; // gradient name filling the canvas instead, or null
    private final Map<String, Gradient> gradients = new LinkedHashMap<>();
    private float[] canvas;          // explicit view {width, height, centreX, centreY}, or null for -100..100
    private Float autoFitPadding;    // non-null: size the view to the drawing plus this margin instead
    private Integer pixelWidth;      // width/height attributes on <svg>, or null to omit them
    private OutputStyle outputStyle = OutputStyle.DEFAULT;
    private boolean grid;            // debug overlay: grid lines and axes on top of the drawing
    private String title;            // <title> and <desc> for screen readers (and tooltips), or null
    private String description;
    private final Diagnostics diagnostics = new Diagnostics();
    private final List<Path> sourceFiles = new ArrayList<>();

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
        backgroundGradient = null;
        gradients.clear();
    }

    public Integer getBackground() {
        return background;
    }

    public Sketch setBackground(Integer rgba) {
        this.background = rgba;
        this.backgroundGradient = null;
        return this;
    }

    // Fill the canvas with a gradient defined via addGradient()
    public Sketch setBackgroundGradient(String name) {
        this.backgroundGradient = name;
        this.background = null;
        return this;
    }

    // Visible area: width x height centred on (cx, cy) in sketch space
    public Sketch setCanvas(float width, float height, float cx, float cy) {
        this.canvas = new float[]{width, height, cx, cy};
        this.autoFitPadding = null;
        return this;
    }

    // Size the visible area to fit everything drawn, plus a margin
    public Sketch setAutoFit(float padding) {
        this.autoFitPadding = padding;
        this.canvas = null;
        return this;
    }

    // Display size of the image: width in pixels (height follows the canvas), or null for none
    public Sketch setPixelWidth(Integer width) {
        if (width != null && width <= 0) {
            throw new IllegalArgumentException("Width must be positive: " + width);
        }
        this.pixelWidth = width;
        return this;
    }

    // Accessible name of the image (<title>)
    public Sketch setTitle(String title) {
        this.title = title;
        return this;
    }

    // Longer accessible description (<desc>)
    public Sketch setDescription(String description) {
        this.description = description;
        return this;
    }

    // Makes a gradient available to shapes as setFillGradient(name) / setStrokeGradient(name)
    public Sketch addGradient(String name, Gradient gradient) {
        gradients.put(name, gradient);
        return this;
    }

    // Names usable wherever a colour is expected
    Set<String> gradientNames() {
        return gradients.keySet();
    }

    // Problems found by the last fromFile() / fromString(), in the order they came up; nothing is printed
    public List<Diagnostic> getDiagnostics() {
        return diagnostics.all();
    }

    // Counts of those problems: errors skip a line or block, warnings only ignore part of one
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

    // Converts <dir>/<name>.txt to <dir>/<name>.svg. Returns false (and writes nothing) if the script
    // could not be read; throws if the SVG could not be written.
    public boolean render(String dir, String name) throws IOException {
        clear();
        Path folder = Path.of(dir);
        if (!fromFile(folder.resolve(name + ".txt").toString())) {
            return false;
        }
        exportSVG(folder.resolve(name + ".svg").toString());
        return true;
    }

    // Draw a coordinate grid over the result, to help place shapes while sketching
    public Sketch setGrid(boolean grid) {
        this.grid = grid;
        return this;
    }

    // Layout of the written document: DEFAULT, PRETTY (indented) or MINIFIED
    public Sketch setOutputStyle(OutputStyle style) {
        this.outputStyle = style;
        return this;
    }

    public void exportSVG(String svgFilePath) throws IOException {
        buildSVG().toFile(svgFilePath, outputStyle);
    }

    // The complete SVG document as text
    public String toSVGString() {
        return buildSVG().toString(outputStyle);
    }

    private SVG buildSVG() {
        SVG svg = new SVG();
        svg.setPixelWidth(pixelWidth);
        ViewBox view = svg.getViewBox();
        if (autoFitPadding != null) {
            Bounds drawn = new Bounds();
            shapes.forEach(s -> s.collectBounds(drawn));
            if (!drawn.isEmpty()) {
                // Bounds are +y up; the viewBox is in SVG space where y points down
                view.fit(drawn.minX, -drawn.maxY, drawn.maxX, -drawn.minY, autoFitPadding);
            }
        } else if (canvas != null) {
            view.set(canvas[2] - canvas[0] * 0.5f, -(canvas[3] + canvas[1] * 0.5f), canvas[0], canvas[1]);
        }

        // Title and description come first, as the SVG spec recommends
        if (title != null) {
            svg.addContent(new TextElement("title", title));
        }
        if (description != null) {
            svg.addContent(new TextElement("desc", description));
        }
        if (!gradients.isEmpty()) {
            Defs defs = new Defs();
            gradients.values().forEach(defs::addContent);
            svg.addContent(defs);
        }
        if (background != null || backgroundGradient != null) {
            // Cover the whole viewBox; convert its SVG-space centre back to +y-up sketch space
            ViewBox vb = svg.getViewBox();
            Shape canvas = new Rect(vb.w, vb.h, vb.x + vb.w * 0.5f, -(vb.y + vb.h * 0.5f)).setStrokeWidth(0.f);
            if (backgroundGradient != null) canvas.setFillGradient(backgroundGradient);
            else canvas.setFill(background);
            svg.addContent(canvas);
        }
        for (Shape shape : shapes) {
            svg.addContent(shape);
        }
        if (grid) {
            svg.addContent(gridOverlay(view));
        }
        return svg;
    }

    // Grid lines every "nice" step (1, 2 or 5 x 10^n, about 20 across) plus the two axes, sized to the view
    private static Group gridOverlay(ViewBox view) {
        float extent = Math.max(view.w, view.h);
        double raw = extent / 20.0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
        double step = raw / magnitude < 1.5 ? magnitude : raw / magnitude < 3.5 ? 2 * magnitude : 5 * magnitude;
        float thin = extent / 1000.f;

        // View edges in sketch space (+y up)
        float left = view.x, right = view.x + view.w;
        float bottom = -(view.y + view.h), top = -view.y;

        Group overlay = new Group();
        // Count whole steps (rather than adding step repeatedly) so positions don't drift; 0 is drawn as an axis below
        for (long i = (long) Math.ceil(left / step); i * step <= right; i++) {
            if (i != 0) {
                overlay.add(gridLine((float) (i * step), bottom, (float) (i * step), top, thin, 0x80808066));
            }
        }
        for (long i = (long) Math.ceil(bottom / step); i * step <= top; i++) {
            if (i != 0) {
                overlay.add(gridLine(left, (float) (i * step), right, (float) (i * step), thin, 0x80808066));
            }
        }
        overlay.add(gridLine(left, 0, right, 0, thin * 2, 0xE63946CC)); // x axis
        overlay.add(gridLine(0, bottom, 0, top, thin * 2, 0x2A9D8FCC)); // y axis
        return overlay;
    }

    private static Shape gridLine(float x1, float y1, float x2, float y2, float width, int rgba) {
        return new Line(x1, y1, x2, y2).setStroke(rgba).setStrokeWidth(width);
    }

    // Returns false if the file could not be read; bad lines are skipped. Either way getDiagnostics() says why.
    public boolean fromFile(String filename) {
        diagnostics.reset();
        sourceFiles.clear();
        return new ScriptEngine(this, diagnostics, sourceFiles).runFile(filename);
    }

    // Parses a script held in memory (e.g. read from stdin); includes resolve against baseDir
    public void fromString(String source, Path baseDir) {
        diagnostics.reset();
        sourceFiles.clear();
        new ScriptEngine(this, diagnostics, sourceFiles).runSource(source, baseDir);
    }
}
