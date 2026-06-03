package com.chunktasks.panel;

import com.chunktasks.ChunkTasksConfig;
import com.chunktasks.ChunkTasksPlugin;
import com.chunktasks.events.ChunkTasksEventBus;
import com.chunktasks.events.ChunkTasksEventType;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.services.ChunkTaskNotifier;
import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.types.TaskGroup;
import com.chunktasks.types.TaskType;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ChunkTasksPanel extends PluginPanel
{
    @Inject private ChunkTasksConfig config;
    @Inject private ChunkTasksManager chunkTasksManager;
    @Inject private ChunkTaskNotifier chunkTaskNotifier;
    @Inject private ClientThread clientThread;
    @Inject private ChunkTasksEventBus eventBus;

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
    private final Set<String> collapsedCustomSections = new HashSet<>();

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

        eventBus.subscribe(ChunkTasksEventType.TASKS_IMPORTED, (success) -> {
            if ((boolean)success) {
                redrawChunkTasks();
            }
        });
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
                    chunkTasksManager.importChunkTasks();
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
                if (taskGroupTasks.isEmpty()) {
                    continue;
                }

                if (taskGroup == TaskGroup.OTHER) {
                    addOtherTaskSections(taskGroupTasks);
                } else {
                    tasksPanel.add(getTaskGroupPanel(taskGroup, taskGroupTasks));
                }
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

        if (!regularOther.isEmpty()) {
            tasksPanel.add(getTaskGroupPanel(TaskGroup.OTHER, regularOther));
        }

        if (!killXTasks.isEmpty()) {
            tasksPanel.add(getCustomSectionPanel("Kill X", killXTasks));
        }

        for (Map.Entry<String, List<ChunkTask>> entry : everyDropByMonster.entrySet()) {
            String sectionName = "Every Drop: " + entry.getKey();
            tasksPanel.add(getCustomSectionPanel(sectionName, entry.getValue()));
        }
    }

    /**
     * Extracts the monster name from an Every Drop task name.
     * E.g. "Air elemental: Air rune (1/42.67)" -> "Air elemental"
     */
    private static String extractMonsterName(String taskName) {
        String cleaned = taskName.replace("~", "").replace("|", "");
        int colonIdx = cleaned.indexOf(':');
        if (colonIdx > 0) {
            String monster = cleaned.substring(0, colonIdx).trim();
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
}
