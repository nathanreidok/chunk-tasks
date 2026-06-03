package com.chunktasks.ui.component;

import com.chunktasks.ChunkTasksPlugin;
import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.ui.generic.BorderTheme;
import com.chunktasks.ui.generic.UIComponent;
import com.chunktasks.ui.generic.UIGridContainer;
import com.chunktasks.ui.generic.UIScrollableContainer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class TaskList extends UIComponent<TaskList> {
	public static final int TASK_COMPONENT_WIDTH = 500;
	public static final int TASK_COMPONENT_HEIGHT = 50;
	public static final int TASK_COMPONENT_PADDING = 5;

	private final Widget background;
	private final UIScrollableContainer scrollableContainer;
	private final UIGridContainer taskGrid;

	private final List<@NonNull ChunkTask> tasks;

	private final List<TaskComponent> taskComponents = new ArrayList<>();

	public static TaskList createInside(Widget window, List<@NonNull ChunkTask> tasks) {
		return new TaskList(window.createChild(WidgetType.LAYER), tasks);
	}

	private TaskList(Widget widget, List<@NonNull ChunkTask> tasks) {
		super(widget, WidgetType.LAYER);
		ChunkTasksPlugin.getStaticInjector().injectMembers(this);

		this.tasks = tasks;

		background = widget.createChild(WidgetType.GRAPHIC);
		scrollableContainer = UIScrollableContainer.createInside(widget);
		taskGrid = new UIGridContainer(scrollableContainer.getContent());

		initializeWidgets();
	}

	private void initializeWidgets() {
		widget.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setSize(0, 0)
			.revalidate();

		background.setPos(0, 0)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setSize(0, 0)
			.setSpriteId(SpriteID.TRADEBACKING)
			.setSpriteTiling(true)
			.revalidate();

		scrollableContainer.setPos(0, 0)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setSize(0, 0)
			.setDrawScrollbar(true)
			.revalidate();

		taskGrid.setPos(0, 0)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setOriginalWidth(0)
			.revalidate();

		log.warn("grid width: " + taskGrid.getWidth());
		var taskComponentWidth = Math.min(TASK_COMPONENT_WIDTH, taskGrid.getWidth() - 24);
		for (ChunkTask task : tasks) {
			TaskComponent taskComponent = new TaskComponent(taskGrid.createItem(WidgetType.LAYER))
				.setPaddingSize(TASK_COMPONENT_PADDING)
				.setSize(taskComponentWidth, TASK_COMPONENT_HEIGHT)
				.setTask(task);

			taskComponent.revalidate();
			taskComponents.add(taskComponent);
		}

		scrollableContainer.revalidate();
		taskGrid.revalidate();
	}

	@Override
	public void revalidate() {
		super.revalidate();
		scrollableContainer.revalidate();
		taskGrid.revalidate();

		for (TaskComponent taskComponent : taskComponents) {
			ChunkTask task = taskComponent.getTask();

			if (task.isComplete) {
				taskComponent.setOpacity(0)
					.setTheme(BorderTheme.ETCHED_GREEN_DYED)
					.revalidate();

				continue;
			}

			taskComponent.setOpacity(0)
				.setTheme(BorderTheme.ETCHED)
				.revalidate();
		}
	}

	@Override
	public void unregister() {
		scrollableContainer.unregister();
	}
}
