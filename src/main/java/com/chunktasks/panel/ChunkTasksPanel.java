package com.chunktasks.panel;

import com.chunktasks.ChunkTaskNotifier;
import com.chunktasks.TaskGroup;
import com.chunktasks.ChunkTasksPlugin;
import com.chunktasks.TaskType;
import com.chunktasks.manager.ChunkTask;
import com.chunktasks.manager.ChunkTasksManager;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
public class ChunkTasksPanel extends PluginPanel
{
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

        for (TaskGroup taskGroup : TaskGroup.values()) {
            List<ChunkTask> taskGroupTasks = chunkTasks.stream().filter(t -> t.taskGroup == taskGroup).collect(Collectors.toList());
            if (!taskGroupTasks.isEmpty()) {
                taskListPanel.add(createTaskGroupPanel(taskGroup, taskGroupTasks));
            }
        }

        revalidate();
        repaint();
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
                    chunkTaskNotifier.completeTask(chunkTask);
                });
                redrawChunkTasks();
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
            String chunkTasksString = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);

            Type type = new TypeToken<ArrayList<ChunkTask>>(){}.getType();
            final ArrayList<ChunkTask> chunkTasks = GSON.fromJson(chunkTasksString, type);

            //Load task triggers
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream stream = classloader.getResourceAsStream("task-triggers.json");
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            Map<String, TaskType> taskTriggers = GSON.fromJson(reader,
                    new TypeToken<HashMap<String, TaskType>>() {}.getType()
            );

            //Set task triggers
            for (ChunkTask chunkTask : chunkTasks) {
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

        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Please copy tasks to clipboard from https://source-chunk.github.io/chunk-picker-v2 under Settings -> Export to clipboard -> Chunk Tasks Plugin",
                    "Invalid Chunk Tasks Format",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void importChunkTasksFromFile() {
        try {
            final Path path = showImportFolderDialog();
            if (path == null)
            {
                return;
            }

            //Read tasks from file
            final String json = new String(Files.readAllBytes(path));
            Type typeSetups = new TypeToken<ArrayList<ChunkTask>>(){}.getType();
            final ArrayList<ChunkTask> newChunkTasks = GSON.fromJson(json, typeSetups);

            //Load task triggers
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream stream = classloader.getResourceAsStream("task-triggers.json");
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            Map<String, TaskType> taskTriggers = GSON.fromJson(reader,
                new TypeToken<HashMap<String, TaskType>>() {}.getType()
            );

            //Set task triggers
            for (ChunkTask chunkTask : newChunkTasks) {
                for (Map.Entry<String, TaskType> entry : taskTriggers.entrySet()) {
                    if (Pattern.matches(entry.getKey(), chunkTask.name)) {
                        chunkTask.taskType = entry.getValue();
                        break;
                    }
                }
            }

            chunkTasksManager.importTasks(newChunkTasks);
            this.redrawChunkTasks();
//			SwingUtilities.invokeLater(this::redrawChunkTasks);

        }
        catch (Exception e) {
            log.error("Couldn't mass import setups", e);
            JOptionPane.showMessageDialog(this,
                    "Invalid setup data.",
                    "Mass Import Setup Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private Path showImportFolderDialog()
    {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Choose Import File");
        FileFilter jsonFilter = new FileNameExtensionFilter("JSON files", "json");
        fileChooser.setFileFilter(jsonFilter);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            return Paths.get(fileChooser.getSelectedFile().getAbsolutePath());
        }
        else
        {
            return null;
        }
    }
}
