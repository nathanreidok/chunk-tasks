package com.chunktasks.tasks;

import com.chunktasks.tasks.ChatMessageConfig;
import com.chunktasks.tasks.MapBoundary;
import com.chunktasks.tasks.MapMovement;
import com.chunktasks.types.TaskGroup;
import com.chunktasks.types.TaskType;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;

import java.util.HashMap;
import java.util.List;

public class ChunkTask {
    public String name;
    public boolean isComplete;
    public TaskGroup taskGroup;
    public HashMap<Skill, Integer> skills;
    public List<String> items;
    public String output;

    public TaskType taskType = TaskType.UNKONWN;
    public boolean isCustom;

    public List<MapMovement> movementRequirement;
    public MapBoundary locationRequirement;
    public String targetRequirement;
    public List<Integer> itemIds;
    public ChatMessageConfig chatMessageConfig;
    public XpTaskConfig xpTaskConfig;
    public Prayer prayer;
}
