package com.chunktasks.panel;

import com.chunktasks.ChunkTasksPlugin;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.RuneLiteProperties;
//import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.SessionClose;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ChunkTasksPanel extends PluginPanel
{
    private final JLabel loggedLabel = new JLabel();
    private JPanel actionsContainer;

//    @Inject
//    @Nullable
//    private Client client;

//    @Inject
//    private EventBus eventBus;

    private static ImageIcon IMPORT_ICON;
    private static ImageIcon IMPORT_HOVER_ICON;

    private final ChunkTasksPlugin plugin;
//    private final JPanel northAnchoredPanel;
//    private final JPanel overviewTopPanel;
//    // The top panel when veiwing a setup
////    private final JPanel setupTopPanel;
//    private final JLabel mainTitle;
//    private final JLabel importMarker;


    static
    {
        final BufferedImage importIcon = ImageUtil.loadImageResource(ChunkTasksPlugin.class, "/import_icon.png");
        IMPORT_ICON = new ImageIcon(importIcon);
        IMPORT_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(importIcon, 0.53f));
    }

    public ChunkTasksPanel(ChunkTasksPlugin plugin)
    {
        super(false);
        this.plugin = plugin;
//
//        //Main Title
//        this.mainTitle = new JLabel();
//        mainTitle.setText("Chunk Tasks");
//        mainTitle.setForeground(Color.WHITE);
//
//        //Import Button
//        this.importMarker = new JLabel(IMPORT_ICON);
//        importMarker.setToolTipText("Import tasks");
//        importMarker.addMouseListener(new MouseAdapter()
//        {
//            @Override
//            public void mousePressed(MouseEvent e)
//            {
//                plugin.importChunkTasks();
////                if (SwingUtilities.isLeftMouseButton(e))
////                {
////
////                    final Point location = MouseInfo.getPointerInfo().getLocation();
////                    SwingUtilities.convertPointFromScreen(location, importMarker);
////                    singleImportExportMenu.show(importMarker, location.x, location.y);
////                }
//            }
//
//            @Override
//            public void mouseEntered(MouseEvent e)
//            {
//                importMarker.setIcon(IMPORT_HOVER_ICON);
//            }
//
//            @Override
//            public void mouseExited(MouseEvent e)
//            {
//                importMarker.setIcon(IMPORT_ICON);
//            }
//        });
//
//
//        JPanel overViewMarkers = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
//        overViewMarkers.add(importMarker);
//        importMarker.setBorder(new EmptyBorder(0, 8, 0, 0));
//
//        JPanel overviewTitleAndHelpButton = new JPanel();
//        overviewTitleAndHelpButton.setLayout(new BorderLayout());
//        overviewTitleAndHelpButton.add(mainTitle, BorderLayout.WEST);
//
//        // the panel on the top that holds the title and buttons
//        this.overviewTopPanel = new JPanel();
//        overviewTopPanel.setLayout(new BorderLayout());
//        overviewTopPanel.add(overviewTitleAndHelpButton, BorderLayout.NORTH);
//        overviewTopPanel.add(Box.createRigidArea(new Dimension(0, 3)), BorderLayout.CENTER);
//        overviewTopPanel.add(overViewMarkers, BorderLayout.SOUTH);
//
//        overviewTopPanel.setVisible(true);
//
//
////        this.setupTopPanel = new JPanel(new BorderLayout());
////        setupTopPanel.add(setupTitleAndButtons, BorderLayout.CENTER);
//
//        final JPanel topPanel = new JPanel();
//        topPanel.setLayout(new BorderLayout());
//        topPanel.add(overviewTopPanel, BorderLayout.NORTH);
////        topPanel.add(setupTopPanel, BorderLayout.SOUTH);
//
//
//
//        this.northAnchoredPanel = new JPanel();
//        northAnchoredPanel.setLayout(new BoxLayout(northAnchoredPanel, BoxLayout.Y_AXIS));
//        northAnchoredPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
//        northAnchoredPanel.add(topPanel);
//        northAnchoredPanel.add(Box.createRigidArea(new Dimension(0, 10)));
////        northAnchoredPanel.add(searchBar);
//
//        setLayout(new BorderLayout());
//        setBorder(new EmptyBorder(10, 10, 10, 10));
//        add(northAnchoredPanel, BorderLayout.NORTH);
//        add(this.contentWrapperPane, BorderLayout.CENTER);

        // make sure the invEq panel isn't visible upon startup
//        setupDisplayPanel.setVisible(false);
//        helpButton.setVisible(!plugin.getConfig().hideButton());
//        updateSectionViewMarker();
//        updatePanelViewMarker();
//        updateSortingMarker();
    }

    public void init()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel();
        titleLabel.setText("Chunk Tasks");
        titleLabel.setForeground(Color.WHITE);

        JLabel importButton = new JLabel(IMPORT_ICON);
        importButton.setToolTipText("Import chunk tasks");
        importButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e))
                {
                    plugin.importChunkTasks();
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

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topPanel.add(titleLabel, BorderLayout.LINE_START);
        topPanel.add(importButton, BorderLayout.LINE_END);



//        //Main Title
//        this.mainTitle = new JLabel();
//        mainTitle.setText("Chunk Tasks");
//        mainTitle.setForeground(Color.WHITE);

        JPanel versionPanel = new JPanel();
        versionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        versionPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        versionPanel.setLayout(new GridLayout(0, 1));

        final Font smallFont = FontManager.getRunescapeSmallFont();

        JLabel version = new JLabel(htmlLabel("RuneLite version: ", "0.0.0.0"));
        version.setFont(smallFont);

        JLabel revision = new JLabel();
        revision.setFont(smallFont);

        String engineVer = "Unknown";
//        if (client != null)
//        {
//            engineVer = String.format("Rev %d", client.getRevision());
//        }

        revision.setText(htmlLabel("Oldschool revision: ", engineVer));

        JLabel launcher = new JLabel(htmlLabel("Launcher version: ", MoreObjects
                .firstNonNull(RuneLiteProperties.getLauncherVersion(), "Unknown")));
        launcher.setFont(smallFont);

        loggedLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        loggedLabel.setFont(smallFont);

        versionPanel.add(version);
        versionPanel.add(revision);
        versionPanel.add(launcher);
        versionPanel.add(Box.createGlue());
        versionPanel.add(loggedLabel);

        actionsContainer = new JPanel();
        actionsContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        actionsContainer.setLayout(new GridLayout(0, 1, 0, 10));

        add(topPanel, BorderLayout.NORTH);
//        add(versionPanel, BorderLayout.NORTH);
        add(actionsContainer, BorderLayout.CENTER);

//        updateLoggedIn();
//        eventBus.register(this);
    }

//    void deinit()
//    {
//        eventBus.unregister(this);
//    }

    /**
     * Builds a link panel with a given icon, text and url to redirect to.
     */
    private static JPanel buildLinkPanel(ImageIcon icon, String topText, String bottomText, String url)
    {
        return buildLinkPanel(icon, topText, bottomText, () -> LinkBrowser.browse(url));
    }

    /**
     * Builds a link panel with a given icon, text and callable to call.
     */
    private static JPanel buildLinkPanel(ImageIcon icon, String topText, String bottomText, Runnable callback)
    {
        JPanel container = new JPanel();
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.setLayout(new BorderLayout());
        container.setBorder(new EmptyBorder(10, 10, 10, 10));

        final Color hoverColor = ColorScheme.DARKER_GRAY_HOVER_COLOR;
        final Color pressedColor = ColorScheme.DARKER_GRAY_COLOR.brighter();

        JLabel iconLabel = new JLabel(icon);
        container.add(iconLabel, BorderLayout.WEST);

        JPanel textContainer = new JPanel();
        textContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        textContainer.setLayout(new GridLayout(2, 1));
        textContainer.setBorder(new EmptyBorder(5, 10, 5, 10));

        container.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                container.setBackground(pressedColor);
                textContainer.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                callback.run();
                container.setBackground(hoverColor);
                textContainer.setBackground(hoverColor);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                container.setBackground(hoverColor);
                textContainer.setBackground(hoverColor);
                container.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                textContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                container.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        JLabel topLine = new JLabel(topText);
        topLine.setForeground(Color.WHITE);
        topLine.setFont(FontManager.getRunescapeSmallFont());

        JLabel bottomLine = new JLabel(bottomText);
        bottomLine.setForeground(Color.WHITE);
        bottomLine.setFont(FontManager.getRunescapeSmallFont());

        textContainer.add(topLine);
        textContainer.add(bottomLine);

        container.add(textContainer, BorderLayout.CENTER);

//        JLabel arrowLabel = new JLabel(ARROW_RIGHT_ICON);
//        container.add(arrowLabel, BorderLayout.EAST);

        return container;
    }

    private static String htmlLabel(String key, String value)
    {
        return "<html><body style = 'color:#a5a5a5'>" + key + "<span style = 'color:white'>" + value + "</span></body></html>";
    }
}
