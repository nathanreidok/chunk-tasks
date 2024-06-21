package com.chunktasks;

import com.chunktasks.managers.InventoryManager;
import com.chunktasks.managers.ChunkTask;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.managers.MapManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ChunkTaskChecker {
    @Inject private Client client;
    @Inject private ChunkTasksManager chunkTasksManager;
    @Inject private InventoryManager inventoryManager;
    @Inject private MapManager mapManager;

    public List<ChunkTask> checkMovementTasks() {
        List<ChunkTask> completedTasks = new ArrayList<>();
        List<ChunkTask> tasksToCheck = chunkTasksManager.getActiveChunkTasksByType(TaskType.MOVEMENT);
        if (tasksToCheck.isEmpty())
            return completedTasks;

        MapMovement movementHistory = mapManager.getMovementHistory();
        for (ChunkTask task : tasksToCheck) {
            if (task.movementRequirement.stream().anyMatch(movementHistory::includes)) {
                completedTasks.add(task);
            }
        }
        return completedTasks;
    }

    public List<ChunkTask> checkLocationTasks() {
        List<ChunkTask> completedTasks = new ArrayList<>();
        List<ChunkTask> tasksToCheck = chunkTasksManager.getActiveChunkTasksByType(TaskType.LOCATION);
        if (tasksToCheck.isEmpty())
            return completedTasks;

        MapCoordinate coordinate = mapManager.getCurrentLocation();
        for (ChunkTask task : tasksToCheck) {
            if (task.locationRequirement.contains(coordinate)) {
                completedTasks.add(task);
            }
        }
        return completedTasks;
    }

    public List<ChunkTask> checkQuestSkillRequirementTasks(Skill changedSkill) {
        List<ChunkTask> completedTasks = new ArrayList<>();

        List<ChunkTask> tasksToCheck = chunkTasksManager.getActiveChunkTasksByType(TaskType.QUEST_SKILL_REQUIREMENT).stream()
                .filter(t -> t.skills != null && t.skills.containsKey(changedSkill))
                .collect(Collectors.toList());

        if (tasksToCheck.isEmpty())
            return completedTasks;

        for (ChunkTask task : tasksToCheck) {
            boolean skillRequirementsMet = true;
            for (Map.Entry<Skill, Integer> skillRequirement : task.skills.entrySet()) {
                int boostedSkillLevel = client.getRealSkillLevel(skillRequirement.getKey());
                int requiredSkillLevel = skillRequirement.getValue();
                if (boostedSkillLevel < requiredSkillLevel) {
                    skillRequirementsMet = false;
                    break;
                }
            }
            if (!skillRequirementsMet)
                continue;

            completedTasks.add(task);
        }

        return completedTasks;
    }

    public List<ChunkTask> checkEquipItemTasks() {
        List<ChunkTask> completedTasks = new ArrayList<>();

        List<ChunkTask> equipItemTasks = chunkTasksManager.getActiveChunkTasksByType(TaskType.EQUIP_ITEM);

        if (equipItemTasks.isEmpty())
            return completedTasks;

        final ItemContainer itemContainer = client.getItemContainer(InventoryID.EQUIPMENT);
        if (itemContainer == null)
            return completedTasks;

        List<String> equipment = Arrays.stream(itemContainer.getItems())
                .map(item -> sanitizeItemName(client.getItemDefinition(item.getId()).getName()))
                .collect(Collectors.toList());

        if (equipment.isEmpty())
            return completedTasks;

        for (ChunkTask task : equipItemTasks) {
            List<String> potentialItems = getPotentialTaskItems(task);

            boolean itemObtained = !Collections.disjoint(potentialItems, equipment);
            if (!itemObtained)
                continue;

            completedTasks.add(task);
        }

        return completedTasks;
    }

    public List<ChunkTask> checkObtainItemTasks() {
        List<ChunkTask> completedTasks = new ArrayList<>();

        List<ChunkTask> inventoryChangedTasks = chunkTasksManager.getActiveChunkTasksByType(TaskType.OBTAIN_ITEM);

        if (inventoryChangedTasks.isEmpty())
            return completedTasks;


        List<String> newInventoryItems = getNewInventoryItems();

        if (newInventoryItems == null || newInventoryItems.isEmpty())
            return completedTasks;

        for (ChunkTask task : inventoryChangedTasks) {
            List<String> potentialItems = getPotentialTaskItems(task);

            boolean itemObtained = !Collections.disjoint(potentialItems, newInventoryItems);
            if (!itemObtained)
                continue;

            completedTasks.add(task);
        }

        return completedTasks;
    }

    public List<ChunkTask> checkSkillingItemTasks() {
        List<ChunkTask> completedTasks = new ArrayList<>();

        List<ChunkTask> inventoryChangedTasks = chunkTasksManager.getActiveChunkTasksByType(TaskType.SKILLING_ITEM);

        if (inventoryChangedTasks.isEmpty())
            return completedTasks;

        List<String> newInventoryItems = getNewInventoryItems();

        if (newInventoryItems == null)
            return completedTasks;

        for (ChunkTask task : inventoryChangedTasks) {
            List<String> potentialItems = getPotentialTaskItems(task);

            boolean itemObtained = !Collections.disjoint(potentialItems, newInventoryItems);
            if (!itemObtained)
                continue;

            if (task.taskType == TaskType.SKILLING_ITEM && task.skills != null) {
                boolean skillRequirementsMet = true;
                for (Map.Entry<Skill, Integer> skillRequirement : task.skills.entrySet()) {
                    int boostedSkillLevel = client.getBoostedSkillLevel(skillRequirement.getKey());
                    int requiredSkillLevel = skillRequirement.getValue();
                    if (boostedSkillLevel < requiredSkillLevel) {
                        skillRequirementsMet = false;
                        break;
                    }
                }
                if (!skillRequirementsMet)
                    continue;
            }

            completedTasks.add(task);
        }

        return completedTasks;
    }

    private List<String> getNewInventoryItems() {
        List<String> inventoryItems = inventoryManager.getInventory();
        List<String> previousItems = inventoryManager.getPreviousInventory();

        if (inventoryItems == null || previousItems == null) {
            return new ArrayList<>();
        }
        return inventoryItems.stream()
                .filter(i -> !previousItems.contains(i))
                .map(this::sanitizeItemName)
                .collect(Collectors.toList());
    }

    private List<String> getPotentialTaskItems(ChunkTask task) {
        List<String> potentialItems = new ArrayList<>();
        if (task.output != null) {
            potentialItems.add(sanitizeItemName(task.output));
        }

        int startIndex = task.name.indexOf("~|");
        int endIndex = task.name.indexOf("|~");
        if (startIndex >= 0 && endIndex >= 0) {
            potentialItems.add(sanitizeItemName(task.name.substring(startIndex + 2, endIndex)));
        }

        if ((task.taskType == TaskType.OBTAIN_ITEM || task.taskType == TaskType.EQUIP_ITEM) && task.items != null) {
            potentialItems.addAll(task.items.stream().map(this::sanitizeItemName).collect(Collectors.toList()));
        }

        return potentialItems;
    }

    private String sanitizeItemName(String itemName) {
        return itemName
                .toLowerCase()
                .replace(" \\(\\d*\\)", ""); //Remove quantity at the end of item name. i.e. (3)
    }
}
