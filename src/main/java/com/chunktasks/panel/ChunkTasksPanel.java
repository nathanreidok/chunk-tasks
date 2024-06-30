package com.chunktasks.panel;

import com.chunktasks.*;
import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.services.ChunkTaskNotifier;
import com.chunktasks.tasks.ChatMessageConfig;
import com.chunktasks.tasks.MapBoundary;
import com.chunktasks.tasks.MapMovement;
import com.chunktasks.tasks.XpTaskConfig;
import com.chunktasks.types.TaskGroup;
import com.chunktasks.types.TaskType;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.api.Prayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

    private JPanel taskListPanel;
    private JPanel topPanel;
    private JLabel importButton;

//    @Inject
//    private EventBus eventBus;

    private static final ImageIcon IMPORT_ICON;
    private static final ImageIcon IMPORT_HOVER_ICON;
    private static final ImageIcon BROKEN_LINK_ICON;

    static
    {
        final BufferedImage importIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/import_icon.png");
        final BufferedImage brokenLinkIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/broken_link_icon.png");
        IMPORT_ICON = new ImageIcon(importIcon);
        IMPORT_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(importIcon, 0.53f));
        BROKEN_LINK_ICON = new ImageIcon(brokenLinkIcon);
    }

    public void init(boolean isLoggedIn) {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        taskListPanel = new JPanel();
        taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));
        taskListPanel.add(createGeneralInfoLabel());

        add(createTopPanel(isLoggedIn), BorderLayout.NORTH);
        add(taskListPanel, BorderLayout.CENTER);

//        updateLoggedIn();
//        eventBus.register(this);
    }

    public void showHideImportButton(boolean isLoggedIn) {
        if (isLoggedIn) {
            topPanel.add(importButton, BorderLayout.LINE_END);
        } else {
            topPanel.remove(importButton);
        }
    }

    public void redrawChunkTasks() {
        List<ChunkTask> chunkTasks = chunkTasksManager.getChunkTasks();
        taskListPanel.removeAll();

        if (chunkTasks.isEmpty()) {

            taskListPanel.add(createGeneralInfoLabel());
        } else {
            for (TaskGroup taskGroup : TaskGroup.values()) {
                List<ChunkTask> taskGroupTasks = chunkTasks.stream().filter(t -> t.taskGroup == taskGroup).collect(Collectors.toList());
                if (!taskGroupTasks.isEmpty()) {
                    taskListPanel.add(createTaskGroupPanel(taskGroup, taskGroupTasks));
                }
            }
        }

        revalidate();
        repaint();
    }

    public JLabel createGeneralInfoLabel() {
        JLabel infoLabel = new JLabel();
        infoLabel.setText("<html>Log in to import chunk tasks copied"
                + "<br/>from the chunk picker under:"
                + "<br/><br/>-> Settings"
                + "<br/>-> Export to clipboard"
                + "<br/>-> Chunk Tasks Plugin"
                + "<br/><br/>Please submit any issues here:"
                + "<br/>github.com/nathanreidok/chunk-tasks"
                + "<br/><br/>For general questions, feel to reach out to me in game (FortisChunk) or on discord (@FortisChunk)</html>"
        );
        return infoLabel;
    }

    private JPanel createTopPanel(boolean isLoggedIn) {
        JLabel titleLabel = new JLabel();
        titleLabel.setText("Chunk Tasks");
        titleLabel.setForeground(Color.WHITE);

        importButton = new JLabel(IMPORT_ICON);
//        importButton.setToolTipText("Import chunk tasks and overwrite existing tasks");
        importButton.setToolTipText("Import chunk tasks from clipboard");
        importButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e))
                {
                    importChunkTasks();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                importButton.setIcon(IMPORT_HOVER_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                importButton.setIcon(IMPORT_ICON);
            }
        });

        topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topPanel.add(titleLabel, BorderLayout.LINE_START);
        topPanel.setBorder(new EmptyBorder(0,0,5,0));
        if (isLoggedIn) {
            topPanel.add(importButton, BorderLayout.LINE_END);
        }

        return topPanel;
    }

    private JPanel createTaskGroupPanel(TaskGroup taskGroup, List<ChunkTask> chunkTasks) {
        JPanel tasksPanel = new JPanel();
        tasksPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tasksPanel.setLayout(new BoxLayout(tasksPanel, BoxLayout.PAGE_AXIS));
        tasksPanel.setBorder((new EmptyBorder(10,10,10,10)));

        JPanel taskGroupPanel = new JPanel();
        taskGroupPanel.setLayout(new BorderLayout());
        taskGroupPanel.setBorder(new EmptyBorder(5,10,5,0));

        JLabel taskGroupLabel = new JLabel(taskGroup.displayText() + " Tasks");
        taskGroupLabel.setForeground(Color.WHITE);
        taskGroupLabel.setLayout(new BorderLayout());
        taskGroupPanel.add(taskGroupLabel, BorderLayout.CENTER);

        tasksPanel.add(taskGroupPanel);

        for (ChunkTask task : chunkTasks)
        {
            tasksPanel.add(createTaskPanel(task));
        }

        return tasksPanel;
    }

    private JPanel createTaskPanel(ChunkTask chunkTask) {
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

        JPanel panel = new JPanel();
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
//        panel.setLayout(new BorderLayout());
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(checkBox);

        if (chunkTask.taskType == TaskType.UNKONWN) {
            JLabel brokenLinkLabel = new JLabel(BROKEN_LINK_ICON);
            brokenLinkLabel.setToolTipText("Auto-detection of this chunk task is not available");
            panel.add(brokenLinkLabel);
        }

        return panel;
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
        try {
            String chunkTasksString = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);

            Type type = new TypeToken<ArrayList<ChunkTask>>() {}.getType();
            final ArrayList<ChunkTask> chunkTasks = GSON.fromJson(chunkTasksString, type);

            //Load task triggers
            Map<String, TaskType> taskTriggers = loadFromFile("task-triggers.json", new TypeToken<>() {});
            //Load interaction tasks
            Map<String, String> interactionTasks = loadFromFile("interaction-tasks.json", new TypeToken<>() {});
            //Load movement tasks
            Map<String, ArrayList<MapMovement>> movementTasks = loadFromFile("movement-tasks.json", new TypeToken<>() {});
            //Load location tasks
            Map<String, MapBoundary> locationTasks = loadFromFile("location-tasks.json", new TypeToken<>() {});
            //Obtain Item Id tasks
            Map<String, ArrayList<Integer>> obtainIdTasks = loadFromFile("obtain-id-tasks.json", new TypeToken<>() {});
            //Equip Item Id tasks
            Map<String, ArrayList<Integer>> equipIdTasks = loadFromFile("equip-id-tasks.json", new TypeToken<>() {});
            //Chat message tasks
            Map<String, ChatMessageConfig> chatMessageTasks = loadFromFile("chat-message-tasks.json", new TypeToken<>() {});
            //Xp tasks
            Map<String, XpTaskConfig> xpTasks = loadFromFile("xp-tasks.json", new TypeToken<>() {});
            //Prayer tasks
            Map<String, Prayer> prayerTasks = loadFromFile("prayer-tasks.json", new TypeToken<>() {});
            //Combat requirement tasks
            Map<String, TaskType> customTasks = loadFromFile("custom-tasks.json", new TypeToken<>() {});
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

            chunkTasksManager.importTasks(chunkTasks);
            this.redrawChunkTasks();
            //			SwingUtilities.invokeLater(this::redrawChunkTasks);

        } catch (Exception e) {
            log.error(e.getMessage());
            log.error(Arrays.toString(e.getStackTrace()));
            JOptionPane.showMessageDialog(this,
                    "Please copy tasks to clipboard from https://source-chunk.github.io/chunk-picker-v2 under Settings -> Export to clipboard -> Chunk Tasks Plugin",
                    "Invalid Chunk Tasks Format",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private <T> T loadFromFile(String resourceName, TypeToken<T> tokenType) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = classLoader.getResourceAsStream(resourceName);
        Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        return GSON.fromJson(reader, tokenType.getType());
    }
}
