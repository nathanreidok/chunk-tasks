package com.chunktasks.managers;

import com.chunktasks.MapCoordinate;
import com.chunktasks.MapMovement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.*;

@Singleton
@Slf4j
@Getter
public class MapManager {
    private final MapMovement movementHistory = new MapMovement();

    public void addCoordinateToHistory(int x, int y) {
        MapCoordinate coordinate = new MapCoordinate(x, y);
        if (!movementHistory.isEmpty() && movementHistory.get(0).equals(coordinate)) {
            return;
        }
        movementHistory.add(coordinate);
        if (movementHistory.size() > 10) {
            movementHistory.remove(0);
        }
    }

    public MapCoordinate getCurrentLocation() {
        return movementHistory.get(movementHistory.size() - 1);
    }
}
