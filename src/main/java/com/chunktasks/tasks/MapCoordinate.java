package com.chunktasks.tasks;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MapCoordinate {
    private Integer x;
    private Integer y;
    private Integer z;

    public MapCoordinate(Integer x, Integer y, Integer z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean equals(MapCoordinate mapCoordinate) {
        return (this.x == null || mapCoordinate.getX() == null || this.x.equals(mapCoordinate.getX()))
            && (this.y == null || mapCoordinate.getY() == null || this.y.equals(mapCoordinate.getY()))
            && (this.z == null || mapCoordinate.getZ() == null || this.z.equals(mapCoordinate.getZ()));
    }
}
