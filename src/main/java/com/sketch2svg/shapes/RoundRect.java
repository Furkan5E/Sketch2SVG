package com.sketch2svg.shapes;

// Rectangle with rounded corners, centred on its position
public class RoundRect extends PathShape {

    // Control-point distance for a cubic Bezier quarter circle (error < 0.03% of the radius)
    private static final float KAPPA = 0.5522848f;

    public RoundRect(float w, float h, float radius, float cx, float cy) {
        float hw = w * 0.5f, hh = h * 0.5f;
        float r = Math.max(0.f, Math.min(radius, Math.min(hw, hh))); // corners can't overlap
        float k = r * KAPPA;

        // Clockwise from the start of the bottom edge (+y is up), one quarter circle per corner
        moveTo(-hw + r, -hh);
        lineTo(hw - r, -hh);
        cubicTo(hw - r + k, -hh, hw, -hh + r - k, hw, -hh + r);
        lineTo(hw, hh - r);
        cubicTo(hw, hh - r + k, hw - r + k, hh, hw - r, hh);
        lineTo(-hw + r, hh);
        cubicTo(-hw + r - k, hh, -hw, hh - r + k, -hw, hh - r);
        lineTo(-hw, -hh + r);
        cubicTo(-hw, -hh + r - k, -hw + r - k, -hh, -hw + r, -hh);
        close();
        setPos(cx, cy);
    }
}
