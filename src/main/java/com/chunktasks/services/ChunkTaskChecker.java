package com.chunktasks.services;

import com.chunktasks.tasks.MapBoundary;
import com.chunktasks.tasks.MapCoordinate;
import com.chunktasks.tasks.MapMovement;
import com.chunktasks.types.TaskType;
import com.chunktasks.managers.InventoryManager;
import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.managers.MapManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;

import javax.inject.Inject;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ChunkTaskChecker {
    @Inject private Client client;
    @Inject private ChunkTasksManager chunkTasksManager;
    @Inject private InventoryManager inventoryManager;
    @Inject private MapManager mapManager;

    public List<ChunkTask> checkPrayerTasks() {
        return checkTasks(TaskType.PRAYER, (ChunkTask task) -> client.isPrayerActive(task.prayer));
    }

    public List<ChunkTask> checkXpTasks(Skill skill, int xpGained) {
        return checkTasks(TaskType.XP, (ChunkTask task) -> {
            MapBoundary locationRequirement = task.xpTaskConfig.getLocation();
            MapCoordinate currentLocation = mapManager.getCurrentLocation();
            if (locationRequirement != null && (currentLocation == null || !locationRequirement.contains(currentLocation)))
                return false;
            if (task.xpTaskConfig.getSkill() != skill)
                return false;
            return task.xpTaskConfig.getXpMin() <= xpGained && xpGained <= task.xpTaskConfig.getXpMax();
        });
    }

    public List<ChunkTask> checkInteractionTasks(String target) {
        return checkTasks(TaskType.INTERACTION, (ChunkTask task) -> task.targetRequirement.equalsIgnoreCase(target));
    }

    public List<ChunkTask> checkMovementTasks() {
        MapMovement movementHistory = mapManager.getMovementHistory();
        return checkTasks(TaskType.MOVEMENT, (ChunkTask task) -> task.movementRequirement.stream().anyMatch(movementHistory::includes));
    }

    public List<ChunkTask> checkLocationTasks() {
        MapCoordinate coordinate = mapManager.getCurrentLocation();
        return checkTasks(TaskType.MOVEMENT, (ChunkTask task) -> task.locationRequirement.contains(coordinate));
    }

    public List<ChunkTask> checkQuestSkillRequirementTasks(Skill changedSkill) {
        return checkTasks(TaskType.QUEST_SKILL_REQUIREMENT, (ChunkTask task) -> {
            if (task.skills == null || !task.skills.containsKey(changedSkill))
                return false;

            boolean skillRequirementsMet = true;
            for (Map.Entry<Skill, Integer> skillRequirement : task.skills.entrySet()) {
                int boostedSkillLevel = client.getRealSkillLevel(skillRequirement.getKey());
                int requiredSkillLevel = skillRequirement.getValue();
                if (boostedSkillLevel < requiredSkillLevel) {
                    skillRequirementsMet = false;
                    break;
                }
            }
            return skillRequirementsMet;
        });
    }

    public List<ChunkTask> checkEquipItemIdTasks() {
        final ItemContainer itemContainer = client.getItemContainer(InventoryID.EQUIPMENT);
        if (itemContainer == null)
            return new ArrayList<>();

        List<Integer> equipmentItemIds = Arrays.stream(itemContainer.getItems())
                .mapToInt(Item::getId)
                .boxed()
                .collect(Collectors.toList());
        if (equipmentItemIds.isEmpty())
            return new ArrayList<>();

        return checkTasks(TaskType.EQUIP_ITEM_ID, (ChunkTask task) -> task.itemIds.stream().anyMatch(equipmentItemIds::contains));
    }

    public List<ChunkTask> checkEquipItemTasks() {
        final ItemContainer itemContainer = client.getItemContainer(InventoryID.EQUIPMENT);
        if (itemContainer == null)
            return new ArrayList<>();

        List<String> equipment = Arrays.stream(itemContainer.getItems())
                .map(item -> sanitizeItemName(client.getItemDefinition(item.getId()).getName()))
                .collect(Collectors.toList());
        if (equipment.isEmpty())
            return new ArrayList<>();

        return checkTasks(TaskType.EQUIP_ITEM, (ChunkTask task) -> !Collections.disjoint(getPotentialTaskItems(task), equipment));
    }

    public List<ChunkTask> checkObtainItemTasks() {
        List<String> newInventoryItems = getNewInventoryItems();
        if (newInventoryItems == null || newInventoryItems.isEmpty())
            return new ArrayList<>();

        return checkTasks(TaskType.OBTAIN_ITEM, (ChunkTask task) -> !Collections.disjoint(getPotentialTaskItems(task), newInventoryItems));
    }

    public List<ChunkTask> checkObtainItemIdTasks() {
        List<Integer> inventoryItemIds = Arrays.stream(client.getItemContainer(InventoryID.INVENTORY).getItems())
                .mapToInt(Item::getId)
                .boxed()
                .collect(Collectors.toList());
        if (inventoryItemIds.isEmpty())
            return new ArrayList<>();

        return checkTasks(TaskType.OBTAIN_ITEM_ID, (ChunkTask task) -> task.itemIds.stream().anyMatch(inventoryItemIds::contains));
    }

    public List<ChunkTask> checkChatMessageTasks(ChatMessage chatMessage) {
        return checkTasks(TaskType.CHAT_MESSAGE, (ChunkTask task) -> {
            if (task.chatMessageConfig.getChatMessageType() != chatMessage.getType())
                return false;

            MapBoundary locationRequirement = task.chatMessageConfig.getLocation();
            if (locationRequirement != null && !locationRequirement.contains(mapManager.getCurrentLocation())) {
                return false;
            }
            return chatMessage.getMessage().contains(task.chatMessageConfig.getMessage());
        });
    }

    public List<ChunkTask> checkPlayerTasks() {
        return checkTasks(TaskType.PLAYER, (ChunkTask task) -> false);
    }

    public List<ChunkTask> checkSkillingItemTasks() {
        List<String> newInventoryItems = getNewInventoryItems();
        if (newInventoryItems == null)
            return new ArrayList<>();

        return checkTasks(TaskType.SKILLING_ITEM, (ChunkTask task) -> {
            List<String> potentialItems = getPotentialTaskItems(task);
            boolean itemObtained = !Collections.disjoint(potentialItems, newInventoryItems);
            if (!itemObtained)
                return false;

            if (task.skills != null) {
                boolean skillRequirementsMet = true;
                for (Map.Entry<Skill, Integer> skillRequirement : task.skills.entrySet()) {
                    int boostedSkillLevel = client.getBoostedSkillLevel(skillRequirement.getKey());
                    int requiredSkillLevel = skillRequirement.getValue();
                    if (boostedSkillLevel < requiredSkillLevel) {
                        skillRequirementsMet = false;
                        break;
                    }
                }
                return skillRequirementsMet;
            }

            return true;
        });
    }

    private List<ChunkTask> checkTasks(TaskType taskType, Function<ChunkTask, Boolean> taskChecker) {
        List<ChunkTask> completedTasks = new ArrayList<>();
        List<ChunkTask> tasksToCheck = chunkTasksManager.getActiveChunkTasksByType(taskType);
        if (tasksToCheck.isEmpty())
            return completedTasks;

        for (ChunkTask task : tasksToCheck) {
            if (task.isCustom) {
                if (checkCustomTask(task.name))
                    completedTasks.add(task);
                continue;
            }

            if (taskChecker.apply(task))
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

    private boolean checkCustomTask(String taskName) {
        switch (taskName) {
            case "~|Dream Mentor|~ Combat skill requirement":
                return client.getLocalPlayer().getCombatLevel() >= 85;
            default:
                return false;
        }
    }
}
