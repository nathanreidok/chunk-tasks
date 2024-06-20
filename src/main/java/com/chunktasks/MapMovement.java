package com.chunktasks;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter @Setter
public class MapMovement extends ArrayList<MapCoordinate> {
    public boolean contains(MapMovement movement) {
        int coordinateCount = movement.size();
        for (int i = 0; i < coordinateCount; i++) {
            if (movement.get(i).equals(this.get(i))) {
                continue;
            }
            if (movement.get(coordinateCount - 1 - i).equals(this.get(i))) {
                continue;
            }
            return false;
        }
        return true;

    }
}
