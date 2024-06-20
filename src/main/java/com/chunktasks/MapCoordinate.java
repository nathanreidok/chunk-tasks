package com.chunktasks;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MapCoordinate {
    private int x;
    private int y;

    public MapCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(MapCoordinate mapCoordinate) {
        return this.x == mapCoordinate.getX() && this.y == mapCoordinate.getY();
    }
}
