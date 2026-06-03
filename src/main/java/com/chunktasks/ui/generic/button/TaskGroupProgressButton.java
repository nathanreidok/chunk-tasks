package com.chunktasks.ui.generic.button;

import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.types.TaskGroup;
import com.chunktasks.ui.generic.UIBorderedContainer;
import com.chunktasks.ui.generic.UIProgressBar;
import com.chunktasks.ui.generic.UIUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.FontID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.*;

import java.util.List;

@Slf4j
public class TaskGroupProgressButton extends UIMultiSpriteButton<TaskGroupProgressButton> {

	public static TaskGroupProgressButton createInside(Widget window, TaskGroup group) {
		return new TaskGroupProgressButton(window.createChild(WidgetType.LAYER), group);
	}

    private UIBorderedContainer imageContainer;
	private Widget image;
	private Widget title;
	private Widget progress;
	private UIProgressBar progressBar;

	@Getter
	private final TaskGroup taskGroup;

	public TaskGroupProgressButton(Widget widget, TaskGroup group) {
		super(widget);
		taskGroup = group;
	}

	@Override
	protected void createContent(Widget widget) {
		content = widget.createChild(WidgetType.LAYER);

		imageContainer = UIBorderedContainer.createInside(content, WidgetType.GRAPHIC);
		image = imageContainer.getContent();
		title = content.createChild(WidgetType.TEXT);
		progress = content.createChild(WidgetType.TEXT);
		progressBar = UIProgressBar.createInside(content);
	}

	@Override
	protected void initializeWidgets() {
		super.initializeWidgets();

		content.setPos(0, 0)
				.setWidthMode(WidgetSizeMode.MINUS)
				.setHeightMode(WidgetSizeMode.MINUS)
				.setSize(0, 0)
				.setXTextAlignment(WidgetTextAlignment.CENTER)
				.setYTextAlignment(WidgetTextAlignment.CENTER)
				.setFontId(FontID.BOLD_12)
				.setTextShadowed(true)
				.revalidate();

		imageContainer
				.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
				.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
				.setHeightMode(WidgetSizeMode.ABSOLUTE)
				.setWidthMode(WidgetSizeMode.ABSOLUTE)
				.setPos(0, 28)
				.revalidate();

		image.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
				.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
				.setPos(0, 0)
				.setWidthMode(WidgetSizeMode.MINUS)
				.setHeightMode(WidgetSizeMode.MINUS)
				.setBorderType(1);

		image.setItemQuantityMode(ItemQuantityMode.NEVER)
				.revalidate();

		title.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
				.setPos(0, 3)
				.setWidthMode(WidgetSizeMode.MINUS)
				.setSize(0, 25)
				.setFontId(FontID.BOLD_12)
				.setTextColor(0xFFFFFF)
				.setTextShadowed(true)
				.setXTextAlignment(WidgetTextAlignment.CENTER)
				.setYTextAlignment(WidgetTextAlignment.CENTER)
				.revalidate();

		progress.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
				.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
				.setPos(0, 20)
				.setWidthMode(WidgetSizeMode.MINUS)
				.setSize(0, 15)
				.setFontId(FontID.BOLD_12)
				.setTextColor(0xFFFFFF)
				.setTextShadowed(true)
				.setXTextAlignment(WidgetTextAlignment.CENTER)
				.setYTextAlignment(WidgetTextAlignment.CENTER)
				.revalidate();

		progressBar
				.setOriginalY(10)
				.revalidate();
	}

	public TaskGroupProgressButton setTasks(List<ChunkTask> tasks) {
		title.setText(taskGroup.displayText());
		setImage(taskGroup);
		int completedTasks = (int)tasks.stream().filter(t -> t.isComplete).count();
		int totalTasks = tasks.size();
		final float percent = totalTasks == 0 ? 1.0f : (float)completedTasks / totalTasks;
		progress.setText(getProgressText(completedTasks, totalTasks));
		progressBar.setProgress(percent);
		return this;
	}

	@Override
	public void revalidate() {
		super.revalidate();
		var widgetHeight = widget.getHeight();

		content.setTextColor(getTheme().getTextColor());
		title.setTextColor(getTheme().getTextColor())
				.revalidate();
		progress.revalidate();
		progressBar.revalidate();

        float imageRatio = 35.00f / 90.00f;
        imageContainer
				.setPos(0, 28)
				.setSize((int)(widgetHeight * imageRatio), (int)(widgetHeight * imageRatio))
				.revalidate();
		image.revalidate();

		super.revalidate();
	}

	private String getProgressText(int tasksCompleted, int totalTasks) {
		return String.format(
			"<col=%x>%d/%d</col>",
			UIUtil.getCompletionColor(tasksCompleted, totalTasks),
			tasksCompleted,
			totalTasks
		);
	}

	private void setImage(TaskGroup taskGroup) {
		switch (taskGroup) {
			case SKILL:
				image.setSpriteId(SpriteID.SideiconsInterface.STATS);
				break;
			case BIS:
				image.setSpriteId(SpriteID.SideiconsInterface.COMBAT);
				break;
			case DIARY:
				image.setSpriteId(SpriteID.SideiconsInterface.ACHIEVEMENT_DIARIES);
				break;
			case QUEST:
				image.setSpriteId(SpriteID.SideiconsInterface.QUESTS);
				break;
			case OTHER:
				image.setSpriteId(SpriteID.SideiconsInterface.CHARACTER_SUMMARY);
				break;
			default:
				image.setItemId(ItemID._100GUIDE_GUIDECAKE);
				break;
		}
	}
}
