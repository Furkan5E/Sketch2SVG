package com.sketch2svg.core;

import com.sketch2svg.svg.*;
import com.sketch2svg.math.*;

// SVG shape with a geometric transform
public abstract class Shape extends Elem {
    private Attrib attribStyle;

    private int fill = ColorInt.from(0,0);
    private int stroke = ColorInt.from(0);
    private String fillGradient;   // gradient id painting the fill instead of the colour, or null
    private String strokeGradient; // same for the stroke
    private float strokeWidth = 1.f;
    private float[] dash;     // stroke-dasharray, or null for a solid line
    private String lineCap;    // butt | round | square, or null for the SVG default (butt)
    private String lineJoin;   // miter | round | bevel, or null for the SVG default (miter)

    private Vec2 scale = new Vec2(1.f);
    private Vec2 rotation = new Vec2(1.f, 0.f); // in complex form
    private float rotationDeg = 0.f; // counter-clockwise, kept for shapes that emit an SVG transform
    private Vec2 pos = new Vec2(0.f);

    public Shape(){
        attribStyle = newAttrib("style");
    }


    // This updates the style element
    // If overriding in a subclass, make sure to call this method via super.updateAttribs().
    @Override
    protected void updateAttribs(){
        var style = "fill:" + (fillGradient != null ? "url(#" + fillGradient + ")"
                : ColorInt.isClear(fill) ? "none" : paint("fill", fill));
        if(strokeWidth > 0.f && (strokeGradient != null || !ColorInt.isClear(stroke))){
            style += ";stroke-width:" + Num.format(strokeWidth);
            style += ";stroke:" + (strokeGradient != null ? "url(#" + strokeGradient + ")" : paint("stroke", stroke));
            if(dash != null && dash.length > 0){
                StringBuilder d = new StringBuilder();
                for(float v : dash)
                    d.append(d.isEmpty() ? "" : " ").append(Num.format(v));
                style += ";stroke-dasharray:" + d;
            }
            if(lineCap != null)
                style += ";stroke-linecap:" + lineCap;
            if(lineJoin != null)
                style += ";stroke-linejoin:" + lineJoin;
        }
        attribStyle.val = hasStyle() ? style : null;
    }

    // "#RRGGBB" plus a separate opacity when translucent; 8-digit hex colors aren't supported by every renderer
    private static String paint(String property, int rgba){
        String color = "#" + ColorInt.rgbHexString(rgba);
        double opacity = ColorInt.opacity(rgba);
        return opacity < 1.0 ? color + ";" + property + "-opacity:" + Num.format(opacity) : color;
    }

    // Whether this element writes a style attribute (containers like groups don't)
    protected boolean hasStyle(){
        return true;
    }

    // API Helpers

    public Shape at(float x, float y) {
        return setPos(x, y);
    }

    public Shape fill(int color) {
        return setFill(color);
    }

    public Shape fill(String hexColor) {
        return setFill(ColorInt.parseColor(hexColor));
    }

    public Shape stroke(int color) {
        return setStroke(color);
    }

    public Shape stroke(String hexColor) {
        return setStroke(ColorInt.parseColor(hexColor));
    }

    public Shape strokeWidth(float width) {
        return setStrokeWidth(width);
    }

    public Shape rotate(float deg) {
        return setRotation(deg);
    }

    public Shape scale(float s) {
        return setScale(s);
    }

    public Shape scale(float sx, float sy) {
        return setScale(sx, sy);
    }

    //setters and getters
    public int getFill(){
        return fill;
    }
    public Shape setFill(int color){
        this.fill = color;
        this.fillGradient = null;
        return this;
    }
    public String getFillGradient(){
        return fillGradient;
    }
    // Paint the fill with the gradient of this id (defined in the SVG's <defs>)
    public Shape setFillGradient(String id){
        this.fillGradient = id;
        return this;
    }
    public String getStrokeGradient(){
        return strokeGradient;
    }
    public Shape setStrokeGradient(String id){
        this.strokeGradient = id;
        return this;
    }
    public int getStroke(){
        return stroke;
    }
    public Shape setStroke(int color){
        this.stroke = color;
        this.strokeGradient = null;
        return this;
    }
    public float getStrokeWidth(){
        return strokeWidth;
    }
    public Shape setStrokeWidth(float w){
        this.strokeWidth = w;
        return this;
    }
    public float[] getDash(){
        return dash;
    }
    // Alternating dash and gap lengths; null or empty for a solid line
    public Shape setDash(float... lengths){
        this.dash = lengths == null || lengths.length == 0 ? null : lengths.clone();
        return this;
    }
    public String getLineCap(){
        return lineCap;
    }
    public Shape setLineCap(String cap){
        this.lineCap = checkKeyword(cap, "butt", "round", "square");
        return this;
    }
    public String getLineJoin(){
        return lineJoin;
    }
    public Shape setLineJoin(String join){
        this.lineJoin = checkKeyword(join, "miter", "round", "bevel");
        return this;
    }
    private static String checkKeyword(String value, String... allowed){
        if(value == null)
            return null;
        for(String a : allowed)
            if(a.equals(value))
                return value;
        throw new IllegalArgumentException("Expected one of " + String.join(", ", allowed) + " but got " + value);
    }
    public Vec2 getPos(){
        return pos;
    }
    public Shape setPos(float x, float y){
        this.pos.set(x,y);
        return this;
    }
    public Vec2 getScale(){
        return scale;
    }
    public Shape setScale(float s){
        this.scale.set(s,s);
        return this;
    }
    public Shape setScale(float sx, float sy){
        scale.set(sx, sy);
        return this;
    }
    
    public float getRotation(){
        return rotationDeg;
    }
    public Shape setRotation(float deg){
        rotationDeg = deg;
        final float d2r = (float)(Math.PI / 180.);
        rotation.x = (float)Math.cos(deg*d2r);
        rotation.y = (float)Math.sin(deg*d2r);
        return this;
    }
    
    // Apply transform in-place on vector
    protected void transform(Vec2 p){
        p.mul(scale).cmul(rotation).add(pos);
    }

    // Adds this shape's extent in sketch space (+y up), including half its stroke, to `out`
    public void collectBounds(Bounds out){
        Bounds own = new Bounds();
        addGeometry(own);
        if(hasStyle() && strokeWidth > 0.f && (strokeGradient != null || !ColorInt.isClear(stroke)))
            own.expand(strokeWidth * 0.5f);
        out.add(own);
    }

    // Subclasses add their outline points (usually via addLocalPoint) so auto-fit can frame them
    protected void addGeometry(Bounds b){ /* no geometry by default */ }

    // Adds a point given in this shape's local coordinates, after scale, rotation and position
    protected final void addLocalPoint(Bounds b, float x, float y){
        Vec2 p = new Vec2(x, y);
        transform(p);
        b.add(p.x, p.y);
    }
}