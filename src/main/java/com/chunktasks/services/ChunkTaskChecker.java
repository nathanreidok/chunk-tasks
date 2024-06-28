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
import java.util.stream.Collectors;

@Slf4j
public class ChunkTaskChecker {
    @Inject private Client client;
    @Inject private ChunkTasksManager chunkTasksManager;
    @Inject private InventoryManager inventoryManager;
    @Inject private MapManager mapManager;

    public List<ChunkTask> checkXpTasks(Skill skill, int xpGained) {
        return checkTasks(TaskType.XP, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            MapBoundary locationRequirement = task.xpTaskConfig.getLocation();
            if (locationRequirement != null && !locationRequirement.contains(mapManager.getCurrentLocation()))
                return;
            if (task.xpTaskConfig.getSkill() != skill)
                return;
            if (task.xpTaskConfig.getXpMin() <= xpGained && xpGained <= task.xpTaskConfig.getXpMax())
                completedTasks.add(task);
        });
    }

    public List<ChunkTask> checkInteractionTasks(String target) {
        return checkTasks(TaskType.INTERACTION, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            if (task.targetRequirement.equalsIgnoreCase(target)) {
                completedTasks.add(task);
            }
        });
    }

    public List<ChunkTask> checkMovementTasks() {
        MapMovement movementHistory = mapManager.getMovementHistory();
        return checkTasks(TaskType.MOVEMENT, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            if (task.movementRequirement.stream().anyMatch(movementHistory::includes)) {
                completedTasks.add(task);
            }
        });
    }

    public List<ChunkTask> checkLocationTasks() {
        MapCoordinate coordinate = mapManager.getCurrentLocation();
        return checkTasks(TaskType.MOVEMENT, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            if (task.locationRequirement.contains(coordinate)) {
                completedTasks.add(task);
            }
        });
    }

    public List<ChunkTask> checkQuestSkillRequirementTasks(Skill changedSkill) {
        return checkTasks(TaskType.QUEST_SKILL_REQUIREMENT, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            if (task.skills == null || !task.skills.containsKey(changedSkill))
                return;

            boolean skillRequirementsMet = true;
            for (Map.Entry<Skill, Integer> skillRequirement : task.skills.entrySet()) {
                int boostedSkillLevel = client.getRealSkillLevel(skillRequirement.getKey());
                int requiredSkillLevel = skillRequirement.getValue();
                if (boostedSkillLevel < requiredSkillLevel) {
                    skillRequirementsMet = false;
                    break;
                }
            }
            if (skillRequirementsMet)
                completedTasks.add(task);
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

        return checkTasks(TaskType.EQUIP_ITEM_ID, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            if (task.itemIds.stream().anyMatch(equipmentItemIds::contains)) {
                completedTasks.add(task);
            }
        });
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

        return checkTasks(TaskType.EQUIP_ITEM, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            List<String> potentialItems = getPotentialTaskItems(task);

            boolean itemObtained = !Collections.disjoint(potentialItems, equipment);
            if (itemObtained)
                completedTasks.add(task);
        });
    }

    public List<ChunkTask> checkObtainItemTasks() {
        List<String> newInventoryItems = getNewInventoryItems();
        if (newInventoryItems == null || newInventoryItems.isEmpty())
            return new ArrayList<>();

        return checkTasks(TaskType.OBTAIN_ITEM, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            List<String> potentialItems = getPotentialTaskItems(task);

            boolean itemObtained = !Collections.disjoint(potentialItems, newInventoryItems);
            if (itemObtained)
                completedTasks.add(task);
        });
    }

    public List<ChunkTask> checkObtainItemIdTasks() {
        List<Integer> inventoryItemIds = Arrays.stream(client.getItemContainer(InventoryID.INVENTORY).getItems())
                .mapToInt(Item::getId)
                .boxed()
                .collect(Collectors.toList());
        if (inventoryItemIds.isEmpty())
            return new ArrayList<>();

        return checkTasks(TaskType.OBTAIN_ITEM_ID, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            if (task.itemIds.stream().anyMatch(inventoryItemIds::contains)) {
                completedTasks.add(task);
            }
        });
    }

    public List<ChunkTask> checkChatMessageTasks(ChatMessage chatMessage) {
        return checkTasks(TaskType.CHAT_MESSAGE, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            if (task.chatMessageConfig.getChatMessageType() != chatMessage.getType())
                return;

            MapBoundary locationRequirement = task.chatMessageConfig.getLocation();
            if (locationRequirement != null && !locationRequirement.contains(mapManager.getCurrentLocation())) {
                return;
            }
            if (chatMessage.getMessage().contains(task.chatMessageConfig.getMessage()))
                completedTasks.add(task);
        });
    }

    public List<ChunkTask> checkPlayerTasks() {
        return checkTasks(TaskType.PLAYER, (ChunkTask task, List<ChunkTask> completedTasks) -> {});
    }

    public List<ChunkTask> checkSkillingItemTasks() {
        List<String> newInventoryItems = getNewInventoryItems();
        if (newInventoryItems == null)
            return new ArrayList<>();

        return checkTasks(TaskType.SKILLING_ITEM, (ChunkTask task, List<ChunkTask> completedTasks) -> {
            List<String> potentialItems = getPotentialTaskItems(task);
            boolean itemObtained = !Collections.disjoint(potentialItems, newInventoryItems);
            if (!itemObtained)
                return;

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
                if (!skillRequirementsMet)
                    return;
            }

            completedTasks.add(task);
        });
    }

    private List<ChunkTask> checkTasks(TaskType taskType, BiConsumer<ChunkTask, List<ChunkTask>> taskChecker) {
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
            taskChecker.accept(task, completedTasks);
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
