package com.chunktasks.managers;

import com.chunktasks.*;
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

    public List<MapMovement> movementRequirement;
    public MapBoundary locationRequirement;
}
