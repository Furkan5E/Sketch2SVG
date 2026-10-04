package com.sketch2svg.parser;

import com.sketch2svg.core.Shape;

// What a stroke or fill is painted with: a plain colour, or a gradient referred to by name
sealed interface Paint {

    void applyToStroke(Shape shape);

    void applyToFill(Shape shape);

    record Color(int rgba) implements Paint {
        @Override
        public void applyToStroke(Shape shape) {
            shape.setStroke(rgba);
        }

        @Override
        public void applyToFill(Shape shape) {
            shape.setFill(rgba);
        }
    }

    record GradientRef(String name) implements Paint {
        @Override
        public void applyToStroke(Shape shape) {
            shape.setStrokeGradient(name);
        }

        @Override
        public void applyToFill(Shape shape) {
            shape.setFillGradient(name);
        }
    }
}
