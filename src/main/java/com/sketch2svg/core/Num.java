package com.sketch2svg.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Formats numbers for SVG output: short, exact-looking, and locale independent
public final class Num {

    // 0.001 of a sketch unit is far below anything visible, and hides float noise like -4.371139E-8
    public static final int DECIMALS = 3;

    private Num() {}

    // Rounded to DECIMALS places, without trailing zeros, exponents or "-0" (e.g. 20.0 -> "20", 3.3471675 -> "3.347")
    public static String format(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Cannot write non-finite number " + value + " to SVG");
        }
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP);
        if (rounded.signum() == 0) {
            return "0";
        }
        return rounded.stripTrailingZeros().toPlainString();
    }
}
