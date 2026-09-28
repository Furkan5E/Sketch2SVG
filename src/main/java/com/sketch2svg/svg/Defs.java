package com.sketch2svg.svg;

import com.sketch2svg.core.Elem;

// <defs>: reusable definitions such as gradients, which are only drawn where referenced
public class Defs extends Elem {

    @Override
    public String getTag() {
        return "defs";
    }
}
