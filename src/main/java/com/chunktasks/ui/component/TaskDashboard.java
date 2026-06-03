package com.chunktasks.ui.component;

import com.chunktasks.ChunkTasksConfig;
import com.chunktasks.ChunkTasksPlugin;
import com.chunktasks.events.ChunkTasksEventBus;
import com.chunktasks.events.ChunkTasksEventType;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.types.TaskGroup;
import com.chunktasks.ui.generic.UIComponent;
import com.chunktasks.ui.generic.UIUtil;
import com.chunktasks.ui.generic.button.TaskGroupProgressButton;
import com.chunktasks.ui.generic.button.UIButton;
import com.chunktasks.ui.generic.button.UITextButton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.FontID;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Slf4j
public class TaskDashboard extends UIComponent<TaskDashboard> {
	public static final int BASE_GAP = 16;
	public static final int TITLE_HEIGHT = 50;
	public static final int BUTTON_HEIGHT = 30;
	public static final int BUTTON_WIDTH = 270;
	public static final int PROGRESS_BUTTON_SIZE = 90;
	public static final int PROGRESS_BUTTON_MAX_SIZE = 110;
	public static final int PROGRESS_BUTTON_MAX_GAP = 10;

	@Inject
	private ScheduledExecutorService executorService;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChunkTasksConfig config;

	@Inject
	private ChunkTasksManager chunkTasksManager;

	@Inject
	private ChunkTasksEventBus chunkTasksEventBus;

	private final Widget title;
	private final Widget progressButtonsContainer;
	private final List<TaskGroupProgressButton> progressButtons = new ArrayList<>();;
	private final UITextButton chunkPickerButton;
	private final UITextButton refreshButton;
	private final Widget progress;

	public static TaskDashboard createInside(Widget window) {
		return new TaskDashboard(window.createChild(WidgetType.LAYER));
	}

	protected TaskDashboard(Widget widget) {
		super(widget, WidgetType.LAYER);
		ChunkTasksPlugin.getStaticInjector().injectMembers(this);

		title = widget.createChild(WidgetType.TEXT);
		progressButtonsContainer = widget.createChild(WidgetType.LAYER);
		chunkPickerButton = UITextButton.createInside(widget);
		refreshButton = UITextButton.createInside(widget);
		progress = widget.createChild(WidgetType.TEXT);

		widget.createChild(WidgetType.LAYER);

		initializeWidgets();

		chunkTasksEventBus.subscribe(ChunkTasksEventType.TASKS_IMPORTED, (success) -> {
			refreshButton.setState(UIButton.State.DEFAULT);
			revalidate();
		});
	}

	private void initializeWidgets() {
		widget.setPos(0, 0)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setSize(0, 0)
			.revalidate();

		title.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setPos(0, BASE_GAP)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setSize(0, TITLE_HEIGHT)
			.setFontId(FontID.QUILL_CAPS_LARGE)
			.setTextColor(0xFFFFFF)
			.setTextShadowed(true)
			.setXTextAlignment(WidgetTextAlignment.CENTER)
			.setYTextAlignment(WidgetTextAlignment.CENTER)
			.setText("Chunk Tasks")
			.revalidate();

		progressButtonsContainer
				.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
				.setWidthMode(WidgetSizeMode.MINUS)
				.setPos(0, title.getRelativeY() + title.getHeight() + BASE_GAP)
				.setSize(0, PROGRESS_BUTTON_SIZE)
				.revalidate();

		for (TaskGroup taskGroup : TaskGroup.values()) {
			addTaskGroupProgressButton(taskGroup);
		}

		int progressButtonsY = progressButtonsContainer.getRelativeY() + progressButtonsContainer.getHeight() + BASE_GAP + 15;

		progress.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setPos(0, progressButtonsY)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setSize(0, BUTTON_HEIGHT)
			.setFontId(FontID.BOLD_12)
			.setTextColor(0xFFFFFF)
			.setTextShadowed(true)
			.setXTextAlignment(WidgetTextAlignment.CENTER)
			.setYTextAlignment(WidgetTextAlignment.CENTER)
			.revalidate();

		refreshButton.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setPos(BASE_GAP / 2, BASE_GAP / 2)
			.setSize(BUTTON_WIDTH / 2, BUTTON_HEIGHT)
			.setText("Refresh Tasks")
			.setName(UIUtil.formatName("Refresh Tasks"))
			.setAction("Visit", () -> {
				refreshButton.setState(UIButton.State.DISABLED);
				chunkTasksManager.importChunkTasks();
				revalidate();
			})
			.revalidate();

		chunkPickerButton.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setPos(BASE_GAP / 2, BASE_GAP / 2)
			.setSize(BUTTON_WIDTH / 2, BUTTON_HEIGHT)
			.setText("Chunk Picker")
			.setName(UIUtil.formatName("Chunk Picker"))
			.setAction("Check", () -> UIUtil.openChunkPicker(config.mapCode()))
			.revalidate();
	}

	@Override
	public void revalidate() {
		super.revalidate();

		title.revalidate();

		var progressContainerWidth = widget.getWidth();
		var extraSpace = progressContainerWidth - (PROGRESS_BUTTON_SIZE * progressButtons.size());
		var gap = (extraSpace - 20) / (progressButtons.size() - 1);
		gap = Math.min(gap, PROGRESS_BUTTON_MAX_GAP);
		gap = Math.max(gap, 0);
		extraSpace = extraSpace - (gap * (progressButtons.size() - 1)) - 20;
		var buttonSize = PROGRESS_BUTTON_SIZE + (extraSpace / progressButtons.size());
		buttonSize = Math.min(buttonSize, PROGRESS_BUTTON_MAX_SIZE);
		buttonSize = Math.max(buttonSize, PROGRESS_BUTTON_SIZE);

		progressButtonsContainer
			.setOriginalHeight(buttonSize)
			.revalidate();
		for (int i = 0; i < progressButtons.size(); i++) {
			var button = progressButtons.get(i);
			var xPos = (i - (progressButtons.size() / 2)) * (buttonSize + gap);
			var tasks = chunkTasksManager.getChunkTasks().stream()
					.filter(t -> t.taskGroup == button.getTaskGroup())
					.collect(Collectors.toList());
			button.setPos(xPos, 0)
				.setSize(buttonSize, buttonSize)
				.setTasks(tasks)
				.setAction("View " + button.getTaskGroup().displayText() + " Tasks", () -> {
					chunkTasksEventBus.publish(ChunkTasksEventType.CHANGE_TAB, button.getTaskGroup().displayText());
				})
				.revalidate();
		}

		String progressText = getProgressText();
		progress.setText(progressText)
				.revalidate();

		refreshButton.revalidate();
		chunkPickerButton.revalidate();
	}

	private String getProgressText() {
		List<ChunkTask> tasks = chunkTasksManager.getChunkTasks();
		int taskCount = tasks.size();
		int tasksCompleted = (int) tasks.stream().filter(t -> t.isComplete).count();
		float percent = (float) tasksCompleted / taskCount;

		return String.format(
				"<col=%x>%d/%d</col> Tasks Completed",
				UIUtil.getCompletionColor(percent),
				tasksCompleted,
				taskCount
		);
	}

	private void addTaskGroupProgressButton(TaskGroup taskGroup) {
		var button = TaskGroupProgressButton.createInside(progressButtonsContainer, taskGroup);
		var tasks = chunkTasksManager.getChunkTasks().stream()
				.filter(t -> t.taskGroup == taskGroup)
				.collect(Collectors.toList());

		button.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
				.setSize(PROGRESS_BUTTON_SIZE, PROGRESS_BUTTON_SIZE)
				.setTasks(tasks)
				.revalidate();
		progressButtons.add(button);
	}
}
