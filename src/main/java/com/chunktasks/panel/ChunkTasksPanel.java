package com.chunktasks.panel;

import com.chunktasks.*;
import com.chunktasks.tasks.*;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.services.ChunkTaskNotifier;
import com.chunktasks.types.TaskGroup;
import com.chunktasks.types.TaskType;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Prayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import okhttp3.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
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

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
public class ChunkTasksPanel extends PluginPanel
{
    @Inject private ChunkTasksConfig config;
    @Inject private ChunkTasksManager chunkTasksManager;
    @Inject private ChunkTaskNotifier chunkTaskNotifier;
    @Inject private ClientThread clientThread;
    @Inject private OkHttpClient okHttpClient;

    private boolean isLoggedIn;
    private JPanel tasksPanel;
    private JPanel topPanel;
    private JPanel topPanelButtons;

    private static final ImageIcon REFRESH_ICON;
    private static final ImageIcon REFRESH_HOVER_ICON;
    private static final ImageIcon EYE_ICON;
    private static final ImageIcon EYE_HOVER_ICON;
    private static final ImageIcon EYE_SLASH_ICON;
    private static final ImageIcon EYE_SLASH_HOVER_ICON;
    private static final ImageIcon BROKEN_LINK_ICON;
    private static final ImageIcon EXPANDED_ICON;
    private static final ImageIcon EXPANDED_HOVER_ICON;
    private static final ImageIcon COLLAPSED_ICON;
    private static final ImageIcon COLLAPSED_HOVER_ICON;

    private final List<TaskGroup> collapsedTaskGroups = new ArrayList<>();

    private boolean hideCompletedTasks = false;

    static
    {
        final BufferedImage refreshIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/refresh_icon.png");
        final BufferedImage eyeIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/eye_icon.png");
        final BufferedImage eyeSlashIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/eye_slash_icon.png");
        final BufferedImage brokenLinkIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/broken_link_icon.png");
        final BufferedImage expandedIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/expanded_icon.png");
        final BufferedImage collapsedIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/collapsed_icon.png");
        REFRESH_ICON = new ImageIcon(refreshIcon);
        REFRESH_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(refreshIcon, 0.53f));
        EYE_ICON = new ImageIcon(eyeIcon);
        EYE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(eyeIcon, 0.53f));
        EYE_SLASH_ICON = new ImageIcon(eyeSlashIcon);
        EYE_SLASH_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(eyeSlashIcon, 0.53f));
        BROKEN_LINK_ICON = new ImageIcon(brokenLinkIcon);
        EXPANDED_ICON = new ImageIcon(expandedIcon);
        EXPANDED_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(expandedIcon, 0.53f));
        COLLAPSED_ICON = new ImageIcon(collapsedIcon);
        COLLAPSED_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(collapsedIcon, 0.53f));
    }

    public void init(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        addTopPanel();
        addTasksPanel();

        redrawChunkTasks();
    }

    public void setLoggedIn(boolean isLoggedIn) {
        if (this.isLoggedIn == isLoggedIn) {
            return;
        }

        this.isLoggedIn = isLoggedIn;
        if (this.isLoggedIn) {
            topPanel.add(topPanelButtons, BorderLayout.EAST);
        } else {
            topPanel.remove(topPanelButtons);
        }
        redrawChunkTasks();
    }

    private void addTopPanel() {
        topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(0,0,5,0));

        JLabel titleLabel = new JLabel("Chunk Tasks");
        titleLabel.setForeground(Color.WHITE);

        topPanelButtons = new JPanel();
        topPanelButtons.setLayout(new BoxLayout(topPanelButtons, BoxLayout.LINE_AXIS));
        topPanelButtons.add(getShowHideButton());
        topPanelButtons.add(getRefreshButton());

        topPanel.add(titleLabel, BorderLayout.WEST);
        if (isLoggedIn) {
            topPanel.add(topPanelButtons, BorderLayout.EAST);
        }

        add(topPanel);
    }

    private JLabel getShowHideButton() {
        JLabel showHideButton = new JLabel(EYE_ICON);
        showHideButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    hideCompletedTasks = !hideCompletedTasks;
                    showHideButton.setIcon(hideCompletedTasks ? EYE_SLASH_ICON : EYE_ICON);
                    redrawChunkTasks();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                showHideButton.setIcon(hideCompletedTasks ? EYE_SLASH_HOVER_ICON : EYE_HOVER_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                showHideButton.setIcon(hideCompletedTasks ? EYE_SLASH_ICON : EYE_ICON);
            }
        });
        return showHideButton;
    }

    private JLabel getRefreshButton() {
        JLabel refreshButton = new JLabel(REFRESH_ICON);
        refreshButton.setToolTipText("Import chunk tasks from Chunk Picker");
        refreshButton.setBorder(new EmptyBorder(0,5,0,0));
        refreshButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    importChunkTasks();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                refreshButton.setIcon(REFRESH_HOVER_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                refreshButton.setIcon(REFRESH_ICON);
            }
        });
        return refreshButton;
    }

    private void addTasksPanel() {
        tasksPanel = new JPanel();
        tasksPanel.setLayout(new BoxLayout(tasksPanel, BoxLayout.PAGE_AXIS));
        tasksPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(tasksPanel);
    }

    public void redrawChunkTasks() {
        List<ChunkTask> chunkTasks = chunkTasksManager.getChunkTasks();
        tasksPanel.removeAll();

        if (!isLoggedIn || chunkTasks == null || chunkTasks.isEmpty()) {
            tasksPanel.add(getGeneralInfoPanel());
        } else {
            for (TaskGroup taskGroup : TaskGroup.values()) {
                List<ChunkTask> taskGroupTasks = chunkTasks.stream()
                        .filter(t -> t.taskGroup == taskGroup)
                        .collect(Collectors.toList());
                if (!taskGroupTasks.isEmpty()) {
                    tasksPanel.add(getTaskGroupPanel(taskGroup, taskGroupTasks));
                }
            }
        }

        revalidate();
        repaint();
    }

    private JPanel getGeneralInfoPanel() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("<html>Log in and enter map code in plugin"
                + "<br/>config to load Chunk Tasks"
                + "<br/><br/>Please submit any issues here:"
                + "<br/>github.com/nathanreidok/chunk-tasks"
                + "<br/><br/>For general questions, message me:"
                + "<br/>-> In game (FortisChunk)"
                + "<br/>-> On discord (@FortisChunk)</html>");
        panel.add(label);
        return panel;
    }

    private JPanel getTaskGroupPanel(TaskGroup taskGroup, List<ChunkTask> chunkTasks) {
        JPanel taskGroupPanel = new JPanel();
        taskGroupPanel.setLayout(new BoxLayout(taskGroupPanel, BoxLayout.PAGE_AXIS));
        taskGroupPanel.setBorder((new EmptyBorder(10,10,10,10)));
        taskGroupPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        taskGroupPanel.add(getTaskGroupHeader(taskGroup, chunkTasks));
        if (!collapsedTaskGroups.contains(taskGroup)) {
            for (ChunkTask task : chunkTasks) {
                if (!hideCompletedTasks || !task.isComplete) {
                    taskGroupPanel.add(getTaskPanel(task));
                }
            }
        }

        return taskGroupPanel;
    }

    private JPanel getTaskGroupHeader(TaskGroup taskGroup, List<ChunkTask> chunkTasks) {
        String taskGroupText = taskGroup.displayText() + " Tasks (" + chunkTasks.stream().filter(x -> x.isComplete).count() + "/" + chunkTasks.size() + ")";
        JLabel taskGroupLabel = new JLabel(taskGroupText);
        taskGroupLabel.setForeground(Color.WHITE);
        taskGroupLabel.setBorder(new EmptyBorder(0,5,0,0));

        JPanel taskGroupHeaderContent = new JPanel();
        taskGroupHeaderContent.setLayout(new BoxLayout(taskGroupHeaderContent, BoxLayout.LINE_AXIS));
        taskGroupHeaderContent.setForeground(Color.WHITE);
        taskGroupHeaderContent.add(getExpandCollapseButton(taskGroup));
        taskGroupHeaderContent.add(taskGroupLabel);

        JPanel taskGroupHeaderPanel = new JPanel();
        taskGroupHeaderPanel.setLayout(new BorderLayout());
        taskGroupHeaderPanel.add(taskGroupHeaderContent, BorderLayout.WEST);
        taskGroupHeaderPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        return taskGroupHeaderPanel;
    }

    private JLabel getExpandCollapseButton(TaskGroup taskGroup) {
        JLabel expandCollapseButton = new JLabel(collapsedTaskGroups.contains(taskGroup) ? COLLAPSED_ICON : EXPANDED_ICON);
        expandCollapseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e))
                {
                    if (collapsedTaskGroups.contains(taskGroup)) {
                        collapsedTaskGroups.remove(taskGroup);
                    } else {
                        collapsedTaskGroups.add(taskGroup);
                    }
                    redrawChunkTasks();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                expandCollapseButton.setIcon(collapsedTaskGroups.contains(taskGroup) ? COLLAPSED_HOVER_ICON : EXPANDED_HOVER_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                expandCollapseButton.setIcon(collapsedTaskGroups.contains(taskGroup) ? COLLAPSED_ICON : EXPANDED_ICON);
            }
        });
        return expandCollapseButton;
    }

    private JPanel getTaskPanel(ChunkTask chunkTask) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setLayout(new BorderLayout());
        checkBox.setText(getTaskNameHtml(chunkTask.name, chunkTask.isComplete));
        checkBox.setSelected(chunkTask.isComplete);
        checkBox.addActionListener(e -> {
            JCheckBox cb = (JCheckBox)e.getSource();
            if (cb.isSelected()) {
                cb.setText(getTaskNameHtml(chunkTask.name, true));
                clientThread.invokeLater(() -> {
                    chunkTaskNotifier.completeTask(chunkTask, config.notifyOnManualCheck());
                    redrawChunkTasks();
                });
            } else {
                cb.setText(getTaskNameHtml(chunkTask.name, false));
                chunkTasksManager.uncompleteTask(chunkTask);
                redrawChunkTasks();
            }
        });

        JPanel taskPanel = new JPanel();
        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.LINE_AXIS));
        taskPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        taskPanel.add(checkBox);

        if (chunkTask.taskType == TaskType.UNKNOWN) {
            JLabel brokenLinkLabel = new JLabel(BROKEN_LINK_ICON);
            brokenLinkLabel.setToolTipText("Auto-detection of this chunk task is not available");
            taskPanel.add(brokenLinkLabel);
        }

        return taskPanel;
    }

    private String getTaskNameHtml(String taskName, boolean isComplete) {
        String sanitizedTaskName = taskName
                .replace("~", "")
                .replace("|", "");
        return isComplete
                ? "<html><strike>" + sanitizedTaskName + "</strike></html>"
                : "<html>" + sanitizedTaskName + "</html>";
    }

    public void importChunkTasks() {
        if (!config.allowApiConnections()) {
            JOptionPane.showMessageDialog(this,
                "Please enable Chunk Picker website connections in the plugin config",
                "API Requests not Authorized",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        String mapCode = config.mapCode();

        if (mapCode == null || mapCode.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter you Chunk Picker map code in the plugin config",
                    "Missing Map Code",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String url = "https://chunkpicker.firebaseio.com/maps/" + mapCode + "/pluginOutput.json";

        Request r = new Request.Builder()
                .url(url)
                .build();
        okHttpClient.newCall(r).enqueue(new Callback()
        {
            @Override
            public void onFailure( Call call,  IOException e) {
                log.debug("Error retrieving chunk tasks", e);
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
                            return;
                        }

                        List<ChunkTask> chunkTasks = GSON.fromJson(tasksJson, type);
                        matchTaskType(chunkTasks);
                        chunkTasksManager.importTasks(chunkTasks);
                        redrawChunkTasks();
                    }
                    catch (IOException | JsonSyntaxException e) {
                        log.debug(e.getMessage());
                    }
                }
                else {
                    log.debug("Get request unsuccessful");
                }
            }
        });
    }

    private void promptUserToRefreshChunkPicker() {
        JOptionPane.showMessageDialog(this,
                "Please refresh tasks on the Chunk Picker website and try again.",
                "Chunk Picker Refresh Needed",
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
