package com.chunktasks.panel;

import com.chunktasks.*;
import com.chunktasks.tasks.*;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.services.ChunkTaskNotifier;
import com.chunktasks.services.ChunkTasksSyncService;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
public class ChunkTasksPanel extends PluginPanel
{
    @Inject private ChunkTasksConfig config;
    @Inject private ChunkTasksManager chunkTasksManager;
    @Inject private ChunkTaskNotifier chunkTaskNotifier;
    @Inject private ChunkTasksSyncService chunkTasksSyncService;
    @Inject private ClientThread clientThread;
    @Inject private OkHttpClient okHttpClient;
    @Inject private ScheduledExecutorService executor;

    private boolean isLoggedIn;
    private JPanel tasksPanel;
    private JPanel topPanel;
    private JPanel topPanelButtons;

    private static final ImageIcon REFRESH_ICON;
    private static final ImageIcon REFRESH_HOVER_ICON;
    private static final ImageIcon UPLOAD_ICON;
    private static final ImageIcon UPLOAD_HOVER_ICON;
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
    private final Set<String> collapsedCustomSections = new HashSet<>();
    private boolean backlogCollapsed = false;

    private boolean hideCompletedTasks = false;

    static
    {
        final BufferedImage refreshIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/refresh_icon.png");
        final BufferedImage uploadIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/upload_icon.png");
        final BufferedImage eyeIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/eye_icon.png");
        final BufferedImage eyeSlashIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/eye_slash_icon.png");
        final BufferedImage brokenLinkIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/broken_link_icon.png");
        final BufferedImage expandedIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/expanded_icon.png");
        final BufferedImage collapsedIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/images/collapsed_icon.png");
        REFRESH_ICON = new ImageIcon(refreshIcon);
        REFRESH_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(refreshIcon, 0.53f));
        UPLOAD_ICON = new ImageIcon(uploadIcon);
        UPLOAD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(uploadIcon, 0.53f));
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
        topPanelButtons.add(getSyncButton());
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

    private JLabel getSyncButton() {
        JLabel syncButton = new JLabel(UPLOAD_ICON);
        syncButton.setToolTipText("Sync completed tasks to Chunk Picker");
        syncButton.setBorder(new EmptyBorder(0,5,0,0));
        syncButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    syncCompletedTasks();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                syncButton.setIcon(UPLOAD_HOVER_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                syncButton.setIcon(UPLOAD_ICON);
            }
        });
        return syncButton;
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
            // Active tasks (non-backlogged) grouped by TaskGroup
            for (TaskGroup taskGroup : TaskGroup.values()) {
                List<ChunkTask> taskGroupTasks = chunkTasks.stream()
                        .filter(t -> t.taskGroup == taskGroup && !t.isBacklogged)
                        .collect(Collectors.toList());
                if (taskGroupTasks.isEmpty()) {
                    continue;
                }

                if (taskGroup == TaskGroup.OTHER) {
                    // Split OTHER into: Kill X, Every Drop (by monster), and remaining Other
                    addOtherTaskSections(taskGroupTasks);
                } else {
                    tasksPanel.add(getTaskGroupPanel(taskGroup, taskGroupTasks));
                }
            }

            // Backlogged tasks section
            List<ChunkTask> backloggedTasks = chunkTasks.stream()
                    .filter(t -> t.isBacklogged)
                    .collect(Collectors.toList());
            if (!backloggedTasks.isEmpty()) {
                tasksPanel.add(getBacklogPanel(backloggedTasks));
            }
        }

        revalidate();
        repaint();
    }

    /**
     * Splits OTHER tasks into separate sections:
     * - Regular "Other Tasks" (anything not Kill X or Every Drop)
     * - "Kill X" section
     * - "Every Drop: {Monster}" sections (one per monster)
     */
    private void addOtherTaskSections(List<ChunkTask> otherTasks) {
        List<ChunkTask> regularOther = new ArrayList<>();
        List<ChunkTask> killXTasks = new ArrayList<>();
        // Every Drop tasks grouped by monster name
        Map<String, List<ChunkTask>> everyDropByMonster = new LinkedHashMap<>();

        for (ChunkTask task : otherTasks) {
            String prefix = task.prefix != null ? task.prefix : "";
            if (prefix.startsWith("[Kill X]")) {
                killXTasks.add(task);
            } else if (prefix.startsWith("[Every Drop]")) {
                String monster = extractMonsterName(task.name);
                everyDropByMonster.computeIfAbsent(monster, k -> new ArrayList<>()).add(task);
            } else {
                regularOther.add(task);
            }
        }

        // Regular other tasks
        if (!regularOther.isEmpty()) {
            tasksPanel.add(getTaskGroupPanel(TaskGroup.OTHER, regularOther));
        }

        // Kill X section
        if (!killXTasks.isEmpty()) {
            tasksPanel.add(getCustomSectionPanel("Kill X", killXTasks));
        }

        // Every Drop sections per monster
        for (Map.Entry<String, List<ChunkTask>> entry : everyDropByMonster.entrySet()) {
            String sectionName = "Every Drop: " + entry.getKey();
            tasksPanel.add(getCustomSectionPanel(sectionName, entry.getValue()));
        }
    }

    /**
     * Extracts the monster name from an Every Drop task name.
     * Task names look like: "Air elemental: Air rune (1/42.67)" or
     * "Earth elemental#Normal variant: Rock (elemental) (Always)"
     * Returns the part before the first colon, cleaned up.
     */
    private static String extractMonsterName(String taskName) {
        // Remove ~| formatting markers
        String cleaned = taskName.replace("~", "").replace("|", "");
        // Take everything before the first ':'
        int colonIdx = cleaned.indexOf(':');
        if (colonIdx > 0) {
            String monster = cleaned.substring(0, colonIdx).trim();
            // Clean up variant suffixes like "#Normal variant"
            int hashIdx = monster.indexOf('#');
            if (hashIdx > 0) {
                monster = monster.substring(0, hashIdx).trim();
            }
            return monster;
        }
        return cleaned;
    }

    /**
     * Creates a collapsible section panel with a custom string key for collapse tracking.
     */
    private JPanel getCustomSectionPanel(String sectionName, List<ChunkTask> tasks) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        boolean isCollapsed = collapsedCustomSections.contains(sectionName);

        // Header
        String headerText = sectionName + " (" + tasks.stream().filter(t -> t.isComplete).count() + "/" + tasks.size() + ")";
        JLabel headerLabel = new JLabel(headerText);
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(new EmptyBorder(0, 5, 0, 0));

        JLabel expandCollapseBtn = new JLabel(isCollapsed ? COLLAPSED_ICON : EXPANDED_ICON);
        expandCollapseBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (collapsedCustomSections.contains(sectionName)) {
                        collapsedCustomSections.remove(sectionName);
                    } else {
                        collapsedCustomSections.add(sectionName);
                    }
                    redrawChunkTasks();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                expandCollapseBtn.setIcon(collapsedCustomSections.contains(sectionName) ? COLLAPSED_HOVER_ICON : EXPANDED_HOVER_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                expandCollapseBtn.setIcon(collapsedCustomSections.contains(sectionName) ? COLLAPSED_ICON : EXPANDED_ICON);
            }
        });

        JPanel headerContent = new JPanel();
        headerContent.setLayout(new BoxLayout(headerContent, BoxLayout.LINE_AXIS));
        headerContent.add(expandCollapseBtn);
        headerContent.add(headerLabel);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.add(headerContent, BorderLayout.WEST);
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        panel.add(headerPanel);

        if (!isCollapsed) {
            for (ChunkTask task : tasks) {
                if (!hideCompletedTasks || !task.isComplete) {
                    panel.add(getTaskPanel(task));
                }
            }
        }

        return panel;
    }

    private JPanel getGeneralInfoPanel() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("<html>Log in and enter map code in the"
                + "<br/>plugin config to load Chunk Tasks"
                + "<br/><br/>Please submit any issues here:"
                + "<br/>github.com/nathanreidok/chunk-tasks"
                + "<br/><br/>For general questions, message me:"
                + "<br/>-> In game (Burner Chunk)"
                + "<br/>-> On discord (@Burner Chunk)</html>");
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

    private JPanel getBacklogPanel(List<ChunkTask> backloggedTasks) {
        JPanel backlogPanel = new JPanel();
        backlogPanel.setLayout(new BoxLayout(backlogPanel, BoxLayout.PAGE_AXIS));
        backlogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        backlogPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        backlogPanel.add(getBacklogHeader(backloggedTasks));
        if (!backlogCollapsed) {
            for (ChunkTask task : backloggedTasks) {
                backlogPanel.add(getBacklogTaskPanel(task));
            }
        }

        return backlogPanel;
    }

    private JPanel getBacklogHeader(List<ChunkTask> backloggedTasks) {
        String headerText = "Backlogged (" + backloggedTasks.size() + ")";
        JLabel headerLabel = new JLabel(headerText);
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(new EmptyBorder(0, 5, 0, 0));

        JLabel expandCollapseButton = new JLabel(backlogCollapsed ? COLLAPSED_ICON : EXPANDED_ICON);
        expandCollapseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    backlogCollapsed = !backlogCollapsed;
                    redrawChunkTasks();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                expandCollapseButton.setIcon(backlogCollapsed ? COLLAPSED_HOVER_ICON : EXPANDED_HOVER_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                expandCollapseButton.setIcon(backlogCollapsed ? COLLAPSED_ICON : EXPANDED_ICON);
            }
        });

        JPanel headerContent = new JPanel();
        headerContent.setLayout(new BoxLayout(headerContent, BoxLayout.LINE_AXIS));
        headerContent.setForeground(Color.WHITE);
        headerContent.add(expandCollapseButton);
        headerContent.add(headerLabel);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.add(headerContent, BorderLayout.WEST);
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        return headerPanel;
    }

    private JPanel getBacklogTaskPanel(ChunkTask task) {
        String taskName = config.showChunkTaskPrefix() ? task.getNameWithPrefix() : task.name;
        String sanitized = taskName.replace("~", "").replace("|", "");
        JLabel label = new JLabel("<html><i>" + sanitized + "</i></html>");
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setBorder(new EmptyBorder(2, 5, 2, 0));

        JPanel taskPanel = new JPanel();
        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.LINE_AXIS));
        taskPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        taskPanel.add(label);

        return taskPanel;
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

        String taskName = config.showChunkTaskPrefix() ? chunkTask.getNameWithPrefix() : chunkTask.name;
        JCheckBox checkBox = new JCheckBox();
        checkBox.setLayout(new BorderLayout());
        checkBox.setText(getTaskNameHtml(taskName, chunkTask.isComplete));
        checkBox.setSelected(chunkTask.isComplete);
        checkBox.addActionListener(e -> {
            JCheckBox cb = (JCheckBox)e.getSource();
            if (cb.isSelected()) {
                cb.setText(getTaskNameHtml(taskName, true));
                clientThread.invokeLater(() -> {
                    chunkTaskNotifier.completeTask(chunkTask, config.notifyOnManualCheck());
                    redrawChunkTasks();
                });
            } else {
                cb.setText(getTaskNameHtml(taskName, false));
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

        String url = "https://getpluginoutput-hfy4fvnsxa-uc.a.run.app/?mapcode=" + mapCode.toLowerCase();

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

                        // Fetch backlog data if password is configured
                        fetchAndApplyBacklog(chunkTasks);

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

    /**
     * Syncs completed tasks to the Chunk Picker server.
     * Flow:
     * 1. Authenticate with Firebase
     * 2. Fetch fresh tasks from server (to avoid overwriting other completions)
     * 3. Merge: keep tasks complete if complete locally OR on server
     * 4. Push newly completed tasks to Firebase (checkedChallenges + pluginOutput)
     * 5. Save merged state locally and redraw
     */
    public void syncCompletedTasks() {
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
                    "Please enter your Chunk Picker map code in the plugin config",
                    "Missing Map Code",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String password = config.chunkPickerPassword();
        if (password == null || password.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter your Chunk Picker password in the plugin config to sync completed tasks",
                    "Missing Password",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Capture locally completed task names before async work
        List<ChunkTask> localTasks = chunkTasksManager.getChunkTasks();
        if (localTasks == null || localTasks.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No tasks loaded. Import tasks first.",
                    "No Tasks",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Set<String> locallyCompletedNames = localTasks.stream()
                .filter(t -> t.isComplete)
                .map(t -> t.name)
                .collect(Collectors.toSet());

        executor.submit(() -> {
            try {
                // Step 1: Authenticate
                String authToken = chunkTasksSyncService.authenticate();

                // Step 2: Fetch fresh tasks from server
                String pluginOutputJson = chunkTasksSyncService.readPluginOutput(authToken);
                if (pluginOutputJson == null || pluginOutputJson.equals("null") || pluginOutputJson.isEmpty()) {
                    SwingUtilities.invokeLater(() -> promptUserToRefreshChunkPicker());
                    return;
                }

                Type type = new TypeToken<ArrayList<ChunkTask>>() {}.getType();
                List<ChunkTask> serverTasks = GSON.fromJson(pluginOutputJson, type);

                // Step 3: Merge completion status
                // Build a map of server tasks by name for quick lookup
                Map<String, ChunkTask> serverTaskMap = new LinkedHashMap<>();
                for (ChunkTask task : serverTasks) {
                    serverTaskMap.put(task.name, task);
                }

                // Track newly completed tasks (complete locally but not on server)
                Map<String, Set<String>> newlyCompletedBySkill = new HashMap<>();

                for (ChunkTask serverTask : serverTasks) {
                    boolean locallyComplete = locallyCompletedNames.contains(serverTask.name);
                    boolean serverComplete = serverTask.isComplete;

                    if (locallyComplete && !serverComplete) {
                        // Newly completed: need to push to server
                        serverTask.isComplete = true;
                        String skill = ChunkTasksSyncService.getCheckedChallengesKey(serverTask);
                        newlyCompletedBySkill
                                .computeIfAbsent(skill, k -> new HashSet<>())
                                .add(serverTask.name);
                    } else if (serverComplete && !locallyComplete) {
                        // Server has it complete, keep it (already true on serverTask)
                    }
                    // If both complete or both incomplete, no change needed
                }

                if (!newlyCompletedBySkill.isEmpty()) {
                    // Step 4a: Push updated checkedChallenges to Firebase
                    chunkTasksSyncService.patchCheckedChallenges(authToken, newlyCompletedBySkill);

                    // Step 4b: Push updated pluginOutput to Firebase
                    chunkTasksSyncService.writePluginOutput(authToken, serverTasks);

                    int totalSynced = newlyCompletedBySkill.values().stream().mapToInt(Set::size).sum();
                    log.info("Synced {} completed task(s) to Chunk Picker", totalSynced);
                } else {
                    log.info("No new completed tasks to sync");
                }

                // Step 5: Fetch backlog and re-match task types, save locally
                fetchAndApplyBacklog(serverTasks, authToken);
                matchTaskType(serverTasks);
                chunkTasksManager.importTasks(serverTasks);

                SwingUtilities.invokeLater(() -> {
                    redrawChunkTasks();
                    int totalSynced = newlyCompletedBySkill.values().stream().mapToInt(Set::size).sum();
                    if (totalSynced > 0) {
                        JOptionPane.showMessageDialog(this,
                                "Successfully synced " + totalSynced + " completed task(s) to Chunk Picker",
                                "Sync Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "All tasks are already in sync with Chunk Picker",
                                "Sync Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                });

            } catch (Exception e) {
                log.error("Error syncing completed tasks", e);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Failed to sync tasks: " + e.getMessage(),
                                "Sync Error",
                                JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    private void promptUserToRefreshChunkPicker() {
        JOptionPane.showMessageDialog(this,
                "Please refresh tasks on the Chunk Picker website and try again.",
                "Chunk Picker Refresh Needed",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Fetches backlog data from Firebase (authenticating if needed) and marks
     * matching tasks as backlogged. Also creates backlog-only ChunkTask entries
     * for tasks that are in the backlog but not in pluginOutput.
     */
    private void fetchAndApplyBacklog(List<ChunkTask> tasks) {
        String password = config.chunkPickerPassword();
        if (password == null || password.isBlank()) {
            return; // Can't fetch backlog without password
        }
        try {
            String authToken = chunkTasksSyncService.authenticate();
            fetchAndApplyBacklog(tasks, authToken);
        } catch (IOException e) {
            log.debug("Failed to fetch backlog data: {}", e.getMessage());
        }
    }

    /**
     * Fetches backlog data from Firebase using an existing auth token and marks
     * matching tasks as backlogged. Also creates backlog-only ChunkTask entries
     * for tasks that are in the backlog but not in pluginOutput.
     */
    private void fetchAndApplyBacklog(List<ChunkTask> tasks, String authToken) {
        try {
            Map<String, Set<String>> backlog = chunkTasksSyncService.readBacklog(authToken);
            if (backlog.isEmpty()) {
                return;
            }

            // Build a set of all backlogged task names across all skills
            Set<String> allBackloggedNames = new HashSet<>();
            for (Set<String> names : backlog.values()) {
                allBackloggedNames.addAll(names);
            }

            // Mark existing tasks as backlogged if their name matches
            Set<String> matchedNames = new HashSet<>();
            for (ChunkTask task : tasks) {
                if (allBackloggedNames.contains(task.name)) {
                    task.isBacklogged = true;
                    matchedNames.add(task.name);
                }
            }

            // Create ChunkTask entries for backlogged tasks not in pluginOutput.
            // Use a dedicated set to avoid adding duplicates (same task under multiple skills).
            Set<String> addedBacklogNames = new HashSet<>();
            for (Map.Entry<String, Set<String>> entry : backlog.entrySet()) {
                String skill = entry.getKey();
                for (String taskName : entry.getValue()) {
                    if (!matchedNames.contains(taskName) && !addedBacklogNames.contains(taskName)) {
                        ChunkTask backlogTask = new ChunkTask();
                        backlogTask.name = taskName;
                        backlogTask.isBacklogged = true;
                        backlogTask.isComplete = false;
                        backlogTask.taskGroup = resolveTaskGroup(skill);
                        backlogTask.prefix = resolveBacklogPrefix(skill);
                        tasks.add(backlogTask);
                        addedBacklogNames.add(taskName);
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Failed to fetch backlog data: {}", e.getMessage());
        }
    }

    private static TaskGroup resolveTaskGroup(String skill) {
        switch (skill) {
            case "Quest": return TaskGroup.QUEST;
            case "Diary": return TaskGroup.DIARY;
            case "BiS": return TaskGroup.BIS;
            case "Extra": return TaskGroup.OTHER;
            default: return TaskGroup.SKILL; // skill names like Mining, Attack, etc.
        }
    }

    private static String resolveBacklogPrefix(String skill) {
        switch (skill) {
            case "Quest":
            case "Diary":
            case "BiS":
            case "Extra":
                return "[" + skill + "]";
            default:
                return "[" + skill + "]";
        }
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
