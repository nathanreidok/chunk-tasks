package com.chunktasks.managers;

import com.chunktasks.ChunkTasksConfig;
import com.chunktasks.events.ChunkTasksEventBus;
import com.chunktasks.events.ChunkTasksEventType;
import com.chunktasks.panel.ChunkTasksPanel;
import com.chunktasks.tasks.*;
import com.chunktasks.types.TaskType;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.client.config.ConfigManager;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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

    @Inject
    private ChunkTasksConfig config;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ChunkTasksPanel panel;

    @Inject
    private ChunkTasksEventBus eventBus;

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

    public void importChunkTasks() {
        if (!config.allowApiConnections()) {
            JOptionPane.showMessageDialog(panel,
                    "Please enable Chunk Picker website connections in the plugin config",
                    "API Requests not Authorized",
                    JOptionPane.ERROR_MESSAGE);
            eventBus.publish(ChunkTasksEventType.TASKS_IMPORTED, false);
            return;
        }
        String mapCode = config.mapCode();

        if (mapCode == null || mapCode.isBlank()) {
            JOptionPane.showMessageDialog(panel,
                    "Please enter you Chunk Picker map code in the plugin config",
                    "Missing Map Code",
                    JOptionPane.ERROR_MESSAGE);
            eventBus.publish(ChunkTasksEventType.TASKS_IMPORTED, false);
            return;
        }

        String url = "https://getpluginoutput-hfy4fvnsxa-uc.a.run.app/?mapcode=" + mapCode.toLowerCase();

        Request r = new Request.Builder()
                .url(url)
                .build();
        okHttpClient.newCall(r).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug("Error retrieving chunk tasks", e);
                eventBus.publish(ChunkTasksEventType.TASKS_IMPORTED, false);
            }

            @Override
            public void onResponse( Call call,  Response response) {
                if (response.isSuccessful()) {
                    try {
                        Type type = new TypeToken<ArrayList<ChunkTask>>() {}.getType();
                        ResponseBody body = response.body();
                        String tasksJson = body == null ? "" : body.string();
                        if (tasksJson.equals("null") || tasksJson.isEmpty()) {
                            promptUserToRefreshChunkPicker();
                            eventBus.publish(ChunkTasksEventType.TASKS_IMPORTED, false);
                            return;
                        }

                        List<ChunkTask> chunkTasks = GSON.fromJson(tasksJson, type);
                        matchTaskType(chunkTasks);
                        importTasks(chunkTasks);
                        eventBus.publish(ChunkTasksEventType.TASKS_IMPORTED, true);
                    }
                    catch (IOException | JsonSyntaxException e) {
                        log.debug(e.getMessage());
                        eventBus.publish(ChunkTasksEventType.TASKS_IMPORTED, false);
                    }
                }
                else {
                    promptUserToRefreshChunkPicker();
                    eventBus.publish(ChunkTasksEventType.TASKS_IMPORTED, false);
                }
            }
        });
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
            log.debug(e.getMessage());
        }
        return null;
    }

    private void promptUserToRefreshChunkPicker() {
        JOptionPane.showMessageDialog(panel,
                "On the Chunk Picker website settings, please opt-in to generate data used in the Chunk Tasks plugin then try again.",
                "Chunk Picker Opt-in or Refresh Needed",
                JOptionPane.ERROR_MESSAGE);
    }

    private void matchTaskType(List<ChunkTask> chunkTasks) {
        //Load task triggers
        Map<String, TaskType> taskTriggers = loadFromFile("/task-triggers.json", new TypeToken<>() {});
        //Load interaction tasks
        Map<String, String> interactionTasks = loadFromFile("/interaction-tasks.json", new TypeToken<>() {});
        //Load movement tasks
        Map<String, ArrayList<MapMovement>> movementTasks = loadFromFile("/movement-tasks.json", new TypeToken<>() {});
        //Load location tasks
        Map<String, MapBoundary> locationTasks = loadFromFile("/location-tasks.json", new TypeToken<>() {});
        //Obtain Item Id tasks
        Map<String, ArrayList<Integer>> obtainIdTasks = loadFromFile("/obtain-id-tasks.json", new TypeToken<>() {});
        //Equip Item Id tasks
        Map<String, ArrayList<Integer>> equipIdTasks = loadFromFile("/equip-id-tasks.json", new TypeToken<>() {});
        //Chat message tasks
        Map<String, ChatMessageConfig> chatMessageTasks = loadFromFile("/chat-message-tasks.json", new TypeToken<>() {});
        //Xp tasks
        Map<String, XpTaskConfig> xpTasks = loadFromFile("/xp-tasks.json", new TypeToken<>() {});
        //Prayer tasks
        Map<String, Prayer> prayerTasks = loadFromFile("/prayer-tasks.json", new TypeToken<>() {});
        //Farming Patch tasks
        Map<String, FarmingPatchConfig> farmingPatchTasks = loadFromFile("/farming-patch-tasks.json", new TypeToken<>() {});
        //Custom requirement tasks
        Map<String, TaskType> customTasks = loadFromFile("/custom-tasks.json", new TypeToken<>() {});
        //Set task types
        for (ChunkTask chunkTask : chunkTasks) {
            if (interactionTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = TaskType.INTERACTION;
                chunkTask.targetRequirement = interactionTasks.get(chunkTask.name);
                continue;
            }

            if (movementTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = TaskType.MOVEMENT;
                chunkTask.movementRequirement = movementTasks.get(chunkTask.name);
                continue;
            }

            if (locationTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = TaskType.LOCATION;
                chunkTask.locationRequirement = locationTasks.get(chunkTask.name);
                continue;
            }

            if (obtainIdTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = TaskType.OBTAIN_ITEM_ID;
                chunkTask.itemIds = obtainIdTasks.get(chunkTask.name);
                continue;
            }

            if (equipIdTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = TaskType.EQUIP_ITEM_ID;
                chunkTask.itemIds = equipIdTasks.get(chunkTask.name);
                continue;
            }

            if (chatMessageTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = TaskType.CHAT_MESSAGE;
                chunkTask.chatMessageConfig = chatMessageTasks.get(chunkTask.name);
                continue;
            }

            if (xpTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = TaskType.XP;
                chunkTask.xpTaskConfig = xpTasks.get(chunkTask.name);
                continue;
            }

            if (prayerTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = TaskType.PRAYER;
                chunkTask.prayer = prayerTasks.get(chunkTask.name);
            }

            if (farmingPatchTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = TaskType.FARMING_PATCH;
                chunkTask.farmingPatchConfig = farmingPatchTasks.get(chunkTask.name);
            }

            if (customTasks.containsKey(chunkTask.name)) {
                chunkTask.taskType = customTasks.get(chunkTask.name);
                chunkTask.isCustom = true;
                continue;
            }

            for (Map.Entry<String, TaskType> entry : taskTriggers.entrySet()) {
                if (Pattern.matches(entry.getKey(), chunkTask.name)) {
                    chunkTask.taskType = entry.getValue();
                    break;
                }
            }
        }
    }

    private <T> T loadFromFile(String resourceName, TypeToken<T> tokenType) {
        InputStream stream = ChunkTasksPanel.class.getResourceAsStream(resourceName);
        Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        return GSON.fromJson(reader, tokenType.getType());
    }
}
