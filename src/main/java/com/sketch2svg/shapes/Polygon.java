package com.sketch2svg.shapes;

import com.sketch2svg.math.Vec2;

import java.util.List;

// Free-form polygon (closed) or polyline (open) through arbitrary points
public class Polygon extends LineStrip {

    public Polygon(List<Vec2> points, boolean closed) {
        super();
        this.closed = closed;
        points(points);
    }

    // Points are given in sketch space; they are stored relative to the bounding-box centre
    // so rotation and scale pivot around the shape instead of the origin
    public Polygon points(List<Vec2> points) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (Vec2 p : points) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }
        float cx = points.isEmpty() ? 0.f : (minX + maxX) * 0.5f;
        float cy = points.isEmpty() ? 0.f : (minY + maxY) * 0.5f;

        setNumVertices(points.size());
        for (int i = 0; i < points.size(); i++) {
            setVertex(i, points.get(i).x - cx, points.get(i).y - cy);
        }
        setPos(cx, cy);
        return this;
    }
}
