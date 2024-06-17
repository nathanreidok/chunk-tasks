package com.chunktasks;

import lombok.Getter;
import lombok.Setter;

public class MapBoundary {
    @Getter @Setter
    private int xMin;
    @Getter @Setter
    private int xMax;
    @Getter @Setter
    private int yMin;    @Getter @Setter
    private int yMax;

    public MapBoundary(int xMin, int xMax, int yMin, int yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public boolean isInBounds(int x, int y) {
        return x >= xMin && x <= xMax && y >= yMin && y <= yMax;
    }
}
