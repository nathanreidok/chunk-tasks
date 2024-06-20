package com.chunktasks;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MapBoundary {
    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;

    public MapBoundary(int xMin, int xMax, int yMin, int yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public boolean contains(MapCoordinate coordinate) {
        return coordinate.getX() >= xMin && coordinate.getX() <= xMax
            && coordinate.getY() >= yMin && coordinate.getY() <= yMax;
    }
}
