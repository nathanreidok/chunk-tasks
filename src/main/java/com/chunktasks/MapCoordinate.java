package com.chunktasks;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MapCoordinate {
    private Integer x;
    private Integer y;

    public MapCoordinate(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(MapCoordinate mapCoordinate) {
        return (this.x == null || mapCoordinate.getX() == null || this.x.equals(mapCoordinate.getX()))
            && (this.y == null || mapCoordinate.getY() == null || this.y.equals(mapCoordinate.getY()));
    }
}
