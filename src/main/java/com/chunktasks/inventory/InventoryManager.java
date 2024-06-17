package com.chunktasks.inventory;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Singleton
@Slf4j
public class InventoryManager {
    private List<String> currentInventory = new ArrayList<>();
    private List<String> previousInventory = new ArrayList<>();

    public List<String> getInventory() {
        return currentInventory;
    }

    public List<String> getPreviousInventory() {
        return previousInventory;
    }

    public void setCurrentInventory(List<String> inventory) {
        previousInventory = currentInventory;
        currentInventory = inventory.stream().map(String::toLowerCase).collect(Collectors.toList());
        log.info("Current Inventory: " + GSON.toJson(currentInventory));
        log.info("Previous Inventory: " + GSON.toJson(previousInventory));
    }
}
