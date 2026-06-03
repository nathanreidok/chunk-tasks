package com.chunktasks.ui.component;


import com.chunktasks.ChunkTasksPlugin;
import com.chunktasks.events.ChunkTasksEventBus;
import com.chunktasks.events.ChunkTasksEventType;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.types.TaskGroup;
import com.chunktasks.ui.generic.*;
import com.chunktasks.ui.generic.button.UIButton;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO: maybe extract into reusable component, if we ever need tabs elsewhere
@Slf4j
public class MainTabbedContainer extends UIComponent<MainTabbedContainer> {
    public static final int TAB_GAP = 2;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ChunkTasksManager chunkTasksManager;

    @Inject
    private ChunkTasksEventBus eventBus;

    private final UIScrollableContainer scrollableContainer;
    private final Widget tabsContainer;
    private final Widget divider;
    private final Widget contentContainer;
    private UIComponent<?> contentComponent = null;

    private final List<UITab> tabs = new ArrayList<>();

    public static MainTabbedContainer createInside(Widget window) {
        return new MainTabbedContainer(window.createChild(WidgetType.LAYER));
    }

    protected MainTabbedContainer(Widget widget) {
        super(widget, WidgetType.LAYER);
        ChunkTasksPlugin.getStaticInjector().injectMembers(this);

        scrollableContainer = UIScrollableContainer.createInside(widget);
        tabsContainer = scrollableContainer.getContent();
        divider = tabsContainer.createChild(WidgetType.RECTANGLE);
        contentContainer = widget.createChild(WidgetType.LAYER);

        initializeWidgets();

        eventBus.subscribe(ChunkTasksEventType.CHANGE_TAB, (data) -> {
            var tab = tabs.stream().filter(t -> t.getText().equals(data.toString())).findFirst();
            tab.ifPresent(UIButton::triggerAction);
        });
    }

    private void addTab(String label, Function<Widget, UIComponent<?>> renderer) {
        UITab tab = UITab.createInside(tabsContainer)
                .setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
                .setOriginalY(0)
                .setText(label)
                .setName(UIUtil.formatName(label))
                .setAction("View", () -> activateTab(renderer));

        tabs.add(tab);
    }

    private void activateTab(Function<Widget, UIComponent<?>> renderer) {
        for (UITab tab : tabs) {
            tab.setState(UIButton.State.DEFAULT)
                    .revalidate();
        }

        for (Widget tabContent : contentContainer.getDynamicChildren()) {
            // ideally we should delete those widgets, but I couldn't figure out how\
            tabContent.setHidden(true);
        }

        clientThread.invoke(() -> {
            if (contentComponent != null) {
                contentComponent.unregister();
            }

            contentComponent = renderer.apply(contentContainer);
        });
    }

    private void initializeWidgets() {
        widget.setPos(0, 0)
                .setHeightMode(WidgetSizeMode.MINUS)
                .setWidthMode(WidgetSizeMode.MINUS)
                .setSize(0, 0)
                .revalidate();

        scrollableContainer.setPos(0, 0)
                .setWidthMode(WidgetSizeMode.MINUS)
                .setSize(0, UITab.TAB_HEIGHT + 1)
                .setScrollAxis(ScrollAxis.HORIZONTAL)
                .setScrollSensitivity(2)
                .revalidate();

        addTab("Dashboard", container -> {
            TaskDashboard dashboard = TaskDashboard.createInside(container);
            dashboard.revalidate();
            return dashboard;
        });

        addTab("All Tasks", container -> {
           TaskList taskList = TaskList.createInside(container, chunkTasksManager.getChunkTasks());
           taskList.revalidate();
           return taskList;
        });

        for (TaskGroup taskGroup : TaskGroup.values()) {
            addTab(taskGroup.displayText(), container -> {
                var chunkTasks = chunkTasksManager.getChunkTasks().stream()
                    .filter(t -> t.taskGroup == taskGroup)
                    .collect(Collectors.toList());
                TaskList taskList = TaskList.createInside(container, chunkTasks);
                taskList.revalidate();
                return taskList;
            });
        }

        divider.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
                .setPos(0, UITab.TAB_HEIGHT)
                .setWidthMode(WidgetSizeMode.MINUS)
                .setSize(0, 1)
                .setTextColor(0x606060)
                .revalidate();

        contentContainer.setPos(0, UITab.TAB_HEIGHT + 1)
                .setWidthMode(WidgetSizeMode.MINUS)
                .setHeightMode(WidgetSizeMode.MINUS)
                .setSize(0, UITab.TAB_HEIGHT + 1);

        // activate first tab
        tabs.get(0).triggerAction();
    }

    @Override
    public void revalidate() {
        super.revalidate();

        scrollableContainer.revalidate();

        int previousTabX = TAB_GAP;
        for (UITab tab : tabs) {
            tab.setOriginalX(previousTabX)
                    .revalidate();

            previousTabX += tab.getWidth() + TAB_GAP;
        }

        int tabsWidth = Math.max(previousTabX, scrollableContainer.getWidth());
        tabsContainer.setOriginalWidth(tabsWidth)
                .revalidate();

        divider.revalidate();

        contentContainer.revalidate();

        if (contentComponent != null) {
            contentComponent.revalidate();
        }
    }

    @Override
    public void unregister() {
        scrollableContainer.unregister();

        if (contentComponent != null) {
            contentComponent.unregister();
        }
    }
}