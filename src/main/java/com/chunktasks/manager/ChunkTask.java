package com.chunktasks.manager;

import com.chunktasks.TaskGroup;
import com.chunktasks.TaskType;
import net.runelite.api.Skill;

import java.util.HashMap;
import java.util.List;

public class ChunkTask {
    public String name;
    public boolean isComplete;
    public TaskGroup taskGroup;
    public TaskType taskType = TaskType.UNKONWN;

    public HashMap<Skill, Integer> skills;

    public List<String> items;
    public String output;

    public int level; //  Used for quest skill requirements

    public List<String> categories;


    public ChunkTask() {}
    public ChunkTask(String name, List<String> items)
    {
        this.name = name;
        this.items = items;
    }
}
