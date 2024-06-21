package com.chunktasks;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;

@Slf4j
@Getter @Setter
public class MapMovement extends ArrayList<MapCoordinate> {

    public MapMovement() {}

    public boolean includes(MapMovement movementRequirement) {
        if (movementRequirement.size() > this.size()) {
            return false;
        }

        int coordinateCount = movementRequirement.size();
        for (int i = 0; i < coordinateCount; i++) {
            if (movementRequirement.get(coordinateCount - 1 - i).equals(this.get(this.size() - 1 - i))) {
                continue;
            }
            return false;
        }
        return true;
    }
}
