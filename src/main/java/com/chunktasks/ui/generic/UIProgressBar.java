package com.chunktasks.ui.generic;

import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;

import java.awt.*;

public class UIProgressBar extends UIComponent<UIProgressBar> {
	private final Widget background;
	private final Widget foreground;
	private final Widget leftEdge;
	private final Widget topEdge;
	private final Widget rightEdge;
	private final Widget bottomEdge;
	private final Widget rectangle25;
	private final Widget rectangle50;
	private final Widget rectangle75;

	public static UIProgressBar createInside(Widget window) {
		return new UIProgressBar(window.createChild(WidgetType.LAYER));
	}

	public UIProgressBar(Widget widget) {
		super(widget, WidgetType.LAYER);

		background = widget.createChild(WidgetType.RECTANGLE);
		foreground = widget.createChild(WidgetType.RECTANGLE);
		leftEdge = widget.createChild(WidgetType.RECTANGLE);
		topEdge = widget.createChild(WidgetType.RECTANGLE);
		rightEdge = widget.createChild(WidgetType.RECTANGLE);
		bottomEdge = widget.createChild(WidgetType.RECTANGLE);
		rectangle25 = widget.createChild(WidgetType.RECTANGLE);
		rectangle50 = widget.createChild(WidgetType.RECTANGLE);
		rectangle75 = widget.createChild(WidgetType.RECTANGLE);

				initializeWidgets();
	}

	private void initializeWidgets() {
		widget
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setFilled(true)
			.setTextColor(new Color(51, 45, 39).getRGB())
			.setSize(70, 8)
			.revalidate();

		background
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setFilled(true)
			.setTextColor(new Color(51, 45, 39).getRGB())
			.setSize(0, 0)
			.revalidate();

		foreground
			.setHeightMode(WidgetSizeMode.MINUS)
			.setFilled(true)
			.setSize(0, 0)
			.revalidate();

		leftEdge
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setPos(0, 0)
			.setOriginalWidth(1)
			.setFilled(true)
			.revalidate();

		topEdge
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setPos(0, 0)
			.setOriginalHeight(1)
			.setFilled(true)
			.revalidate();

		rightEdge
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setPos(0, 0)
			.setOriginalWidth(1)
			.setFilled(true)
			.revalidate();

		bottomEdge
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setPos(0, 0)
			.setOriginalHeight(1)
			.setFilled(true)
			.revalidate();

		rectangle25
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setPos(0, (int)(widget.getOriginalX() * 0.25))
			.setOriginalWidth(1)
			.setFilled(true)
			.revalidate();

		rectangle50
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setPos(0, (int)(widget.getOriginalX() * 0.50))
			.setOriginalWidth(1)
			.setFilled(true)
			.revalidate();

		rectangle75
			.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setPos((int)(widget.getOriginalWidth() * 0.75), 0)
			.setOriginalWidth(1)
			.setFilled(true)
			.revalidate();
	}

	public void setProgress(float percentage) {
		foreground.setTextColor(UIUtil.getCompletionColor(percentage))
			.setOriginalWidth((int)(widget.getOriginalWidth() * percentage))
			.revalidate();
	}


	@Override
	public void revalidate() {
		widget.revalidate();

		background.revalidate();
		leftEdge.revalidate();
		rightEdge.revalidate();
		topEdge.revalidate();
		bottomEdge.revalidate();
		rectangle25
				.setPos((int)(widget.getOriginalWidth() * 0.25), 0)
				.revalidate();
		rectangle50
				.setPos((int)(widget.getOriginalWidth() * 0.50), 0)
				.revalidate();
		rectangle75
				.setPos((int)(widget.getOriginalWidth() * 0.75), 0)
				.revalidate();
	}
}