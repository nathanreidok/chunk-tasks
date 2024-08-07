package com.chunktasks.managers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Singleton
@Slf4j
public class InventoryManager {
    private List<String> inventory = new ArrayList<>();
    private List<String> previousInventory = new ArrayList<>();

    public void setInventory(List<String> inventory) {
        previousInventory = this.inventory;
        this.inventory = inventory.stream().map(String::toLowerCase).collect(Collectors.toList());
    }
}
