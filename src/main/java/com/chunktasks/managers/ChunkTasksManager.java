package com.chunktasks.managers;

import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.types.TaskType;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.chunktasks.ChunkTasksConfig.CONFIG_GROUP;
import static net.runelite.http.api.RuneLiteAPI.GSON;

@Singleton
@Slf4j
public class ChunkTasksManager {

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Getter
    private List<ChunkTask> chunkTasks;

    public void loadChunkTasksData() {
        chunkTasks = loadChunkTasksDataFromRLProfile();
        if (chunkTasks == null) {
            chunkTasks = new ArrayList<>();
        }
    }

    public void importTasks(List<ChunkTask> tasks) {
        chunkTasks = tasks;
        save();
    }

    public void save() {
        String json = GSON.toJson(chunkTasks);
        String profileName = configManager.getProfile().getName();
        configManager.setRSProfileConfiguration(CONFIG_GROUP, profileName, json);
    }

    public List<ChunkTask> getActiveChunkTasksByType(TaskType taskType) {
        return chunkTasks.stream().filter(t -> !t.isComplete && t.taskType == taskType).collect(Collectors.toList());
    }

    public void completeTask(ChunkTask task) {
        task.isComplete = true;
        this.save();
    }

    public void uncompleteTask(ChunkTask task) {
        task.isComplete = false;
        this.save();
    }

    private ArrayList<ChunkTask> loadChunkTasksDataFromRLProfile() {
        String profileName = configManager.getProfile().getName();
        String chunkTasksDataJson = configManager.getRSProfileConfiguration(CONFIG_GROUP, profileName, String.class);
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
}
