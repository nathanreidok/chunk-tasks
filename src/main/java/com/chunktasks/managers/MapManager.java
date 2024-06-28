package com.chunktasks.managers;

import com.chunktasks.tasks.MapCoordinate;
import com.chunktasks.tasks.MapMovement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Singleton
@Slf4j
@Getter
public class MapManager {
    private final MapMovement movementHistory = new MapMovement();

    public boolean addCoordinateToHistory(int x, int y, int z) {
        MapCoordinate coordinate = new MapCoordinate(x, y, z);
        if (!movementHistory.isEmpty() && getCurrentLocation().equals(coordinate)) {
            return false;
        }
        movementHistory.add(coordinate);
//        log.error(coordinate.getX() + "-" + coordinate.getY() + "-" + coordinate.getZ());
        if (movementHistory.size() > 10) {
            movementHistory.remove(0);
        }
        return true;
    }

    public MapCoordinate getCurrentLocation() {
        return movementHistory.get(movementHistory.size() - 1);
    }
}
