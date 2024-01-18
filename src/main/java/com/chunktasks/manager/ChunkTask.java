package com.chunktasks.manager;

//import lombok.Getter;

import java.util.List;

//@Getter
public class ChunkTask {
    public String description;
    public boolean isComplete;

    public List<String> items;

    public ChunkTask() {}
    public ChunkTask(String description, List<String> items)
    {
        this.description = description;
        this.items = items;
    }
}
