package com.chunktasks.manager;

import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.chunktasks.ChunkTasksConfig.CONFIG_GROUP;
import static com.chunktasks.ChunkTasksConfig.CONFIG_KEY;
import static net.runelite.http.api.RuneLiteAPI.GSON;

@Singleton
@Slf4j
public class ChunkTasksManager {

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    private List<ChunkTask> chunkTasks;

    public void loadChunkTasksData()
    {
        chunkTasks = loadChunkTasksDataFromRLProfile();
        log.info("Loaded chunk tasks: " + GSON.toJson(chunkTasks));
        if (chunkTasks == null) {
            chunkTasks = initializeChunkTasksData();
        }
    }

    public List<ChunkTask> getActiveChunkTasks()
    {
        return chunkTasks.stream().filter(t -> !t.isComplete).collect(Collectors.toList());
    }

    public void save()
    {
        String json = GSON.toJson(chunkTasks);
        log.info("Saving tasks: " + json);
        configManager.setRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY, json);
    }

    public void importTasks(List<ChunkTask> tasks)
    {
        chunkTasks = tasks;
        save();
    }

    public void completeTask(ChunkTask task)
    {
        task.isComplete = true;
        this.save();
    }

//    public ChunkTask current() {
//        if (getSaveData().getActiveTaskPointer() == null) {
//            return null;
//        }
//        return getSaveData().getActiveTaskPointer().getTask();
//    }

    private ArrayList<ChunkTask> loadChunkTasksDataFromRLProfile() {
        String chunkTasksDataJson = configManager.getRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY, String.class);
        log.info("Tasks loaded: " + chunkTasksDataJson);
        if (chunkTasksDataJson == null) {
            return null;
        }
        try {
            return GSON.fromJson(chunkTasksDataJson, new TypeToken<ArrayList<ChunkTask>>() {}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<ChunkTask> initializeChunkTasksData() {
        ArrayList<ChunkTask> tasks = new ArrayList<>();

//        ChunkTask task = new ChunkTask();
//        task.description = "Wield an ~|osmumten's fang|~";
//        task.items = new ArrayList<>() {
//        {
//            add("Osmumten's fang");
//        }};
//        tasks.add(task);

        return tasks;
    }
}
