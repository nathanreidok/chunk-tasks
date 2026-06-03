package com.chunktasks.ui.component;

import com.chunktasks.ChunkTasksConfig;
import com.chunktasks.ChunkTasksPlugin;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.managers.JsonFileManager;
import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.ui.InterfaceManager;
import com.chunktasks.ui.generic.BorderTheme;
import com.chunktasks.ui.generic.UIBorderedContainer;
import com.chunktasks.ui.generic.UIComponent;
import com.chunktasks.ui.generic.UIUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.FontID;
import net.runelite.api.ScriptEvent;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.*;
import net.runelite.client.game.SkillIconManager;

import javax.inject.Inject;

@Accessors(chain = true)
@Slf4j
public class TaskComponent extends UIComponent<TaskComponent> {
	private final UIBorderedContainer outerContainer;
	private final UIBorderedContainer imageContainer;
	private final Widget image;
	private final Widget name;

	@Getter
	@Setter
	private ChunkTask task = null;

	@Setter
	private int paddingSize = 10;

	@Inject private InterfaceManager interfaceManager;
	@Inject private ChunkTasksManager chunkTasksManager;
	@Inject private ChunkTasksConfig config;
	@Inject private SkillIconManager skillIconManager;

	public static TaskComponent createInside(Widget window) {
		return new TaskComponent(window.createChild(WidgetType.LAYER));
	}

	protected TaskComponent(Widget widget) {
		super(widget, WidgetType.LAYER);
		ChunkTasksPlugin.getStaticInjector().injectMembers(this);

		outerContainer = UIBorderedContainer.createInside(widget);
		imageContainer = new UIBorderedContainer(outerContainer.getContent(), WidgetType.GRAPHIC);
		image = imageContainer.getContent();
		name = widget.createChild(WidgetType.TEXT);

		initializeWidgets();
	}

	public TaskComponent setOpacity(int transparency) {
		image.setOpacity(transparency);
		name.setOpacity(transparency);
		return this;
	}

	public TaskComponent setTheme(BorderTheme theme) {
		outerContainer.setTheme(theme);
		imageContainer.setTheme(theme);
		return this;
	}

	private void onActionSelected(ScriptEvent e) {
		int actionIndex = e.getOp();
        if (actionIndex == 1) {
            if (task.isComplete) {
                chunkTasksManager.uncompleteTask(task);
                setOpacity(0).setTheme(BorderTheme.ETCHED).revalidate();
            } else {
                chunkTasksManager.completeTask(task);
                setOpacity(0).setTheme(BorderTheme.ETCHED_GREEN_DYED);
            }
			revalidate();
        }
	}

	private void initializeWidgets() {
		widget.setHasListener(true)
			.revalidate();

		widget.setOnOpListener((JavaScriptCallback) this::onActionSelected);

		outerContainer.setPos(0, 0)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setSize(0, 0)
			.setTheme(BorderTheme.ETCHED)
			.revalidate();

		imageContainer.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setTheme(BorderTheme.ETCHED)
			.revalidate();

		image.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setPos(0, 0)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setBorderType(1);

		image.revalidate();

		name.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER)
			.setWidthMode(WidgetSizeMode.MINUS)
			.setHeightMode(WidgetSizeMode.MINUS)
			.setXTextAlignment(WidgetTextAlignment.CENTER)
			.setYTextAlignment(WidgetTextAlignment.CENTER)
			.setFontId(FontID.BOLD_12)
			.setTextColor(0xFFFFFF)
			.revalidate();
	}

	@Override
	public void revalidate() {
		super.revalidate();

		String taskName = "No active task";
		if (task != null) {
			setImage(task);
			taskName = UIUtil.sanitizeName(config.showChunkTaskPrefix() ? task.getNameWithPrefix() : task.name);
			widget.setName(taskName);
			String actionPrefix = task.isComplete ? "Uncomplete" : "Complete";
			widget.setAction(0, actionPrefix);
		} else {
			widget.setAction(0, null);
		}

		outerContainer.revalidate();

		imageContainer.setPos(paddingSize, 0)
			.setSize(outerContainer.getHeight() - (paddingSize * 2), paddingSize * 2)
			.revalidate();

		name.setSize(outerContainer.getHeight() + paddingSize, 0)
			.setPos((outerContainer.getHeight() - paddingSize) / 2, 0)
			.setText(taskName)
			.revalidate();
	}

	private void setImage(ChunkTask task) {
		switch (task.taskGroup) {
			case BIS:
				setItemImage(task);
				break;
			case SKILL:
				setSkillImage(task);
				break;
			case QUEST:
				int questSpriteId = SpriteID.SideiconsInterface.QUESTS;
				image.setSpriteId(questSpriteId);
				break;
			case DIARY:
				int diarySpriteId = SpriteID.SideiconsInterface.ACHIEVEMENT_DIARIES;
				image.setSpriteId(diarySpriteId);
				break;
			case OTHER:
				setOtherImage(task);
				break;
			default:
				image.setItemId(ItemID._100GUIDE_GUIDECAKE);
				break;
		}

		image.setSize(paddingSize, paddingSize)
			.revalidate();
	}


	private void setItemImage(ChunkTask task) {
		int itemNameStartIndex = task.name.indexOf("~|");
		int itemNameEndIndex = task.name.indexOf("|~");
		if (itemNameStartIndex == -1 || itemNameEndIndex == -1) {
			image.setItemId(ItemID._100GUIDE_GUIDECAKE);
			return;
		}
		String itemName = task.name.substring(itemNameStartIndex + 2, itemNameEndIndex);

		Integer itemId = JsonFileManager.getNameToIdMap().get(itemName);
		if (itemId == null) {
			itemId = ItemID._100GUIDE_GUIDECAKE;
		}

		image.setItemId(itemId);
	}

	private void setSkillImage(ChunkTask task) {
		if (task.prefix == null || task.prefix.isEmpty() || task.skills.isEmpty()) {
			image.setSpriteId(SpriteID.Staticons2.TOTAL);
			return;
		}

		int skillStartIndex = task.prefix.lastIndexOf(' ');
		int skillEndIndex = task.prefix.length() - 1;
		if (skillStartIndex == -1) {
			image.setSpriteId(SpriteID.Staticons2.TOTAL);
			return;
		}

		var skillText = task.prefix.substring(skillStartIndex + 1, skillEndIndex);
		Skill skill;
		try {
			skill = Skill.valueOf(skillText.toUpperCase());
		} catch (Exception ex) {
			image.setSpriteId(SpriteID.Staticons2.TOTAL);
			return;
		}

		switch (skill) {
			case ATTACK: image.setSpriteId(SpriteID.Staticons.ATTACK); break;
			case STRENGTH: image.setSpriteId(SpriteID.Staticons.STRENGTH); break;
			case DEFENCE: image.setSpriteId(SpriteID.Staticons.DEFENCE); break;
			case RANGED: image.setSpriteId(SpriteID.Staticons.RANGED); break;
			case PRAYER: image.setSpriteId(SpriteID.Staticons.PRAYER); break;
			case MAGIC: image.setSpriteId(SpriteID.Staticons.MAGIC); break;
			case HITPOINTS: image.setSpriteId(SpriteID.Staticons.HITPOINTS); break;
			case AGILITY: image.setSpriteId(SpriteID.Staticons.AGILITY); break;
			case HERBLORE: image.setSpriteId(SpriteID.Staticons.HERBLORE); break;
			case THIEVING: image.setSpriteId(SpriteID.Staticons.THIEVING); break;
			case CRAFTING: image.setSpriteId(SpriteID.Staticons.CRAFTING); break;
			case FLETCHING: image.setSpriteId(SpriteID.Staticons.FLETCHING); break;
			case MINING: image.setSpriteId(SpriteID.Staticons.MINING); break;
			case SMITHING: image.setSpriteId(SpriteID.Staticons.SMITHING); break;
			case FISHING: image.setSpriteId(SpriteID.Staticons.FISHING); break;
			case COOKING: image.setSpriteId(SpriteID.Staticons.COOKING); break;
			case WOODCUTTING: image.setSpriteId(SpriteID.Staticons.WOODCUTTING); break;
			case FIREMAKING: image.setSpriteId(SpriteID.Staticons.FIREMAKING); break;
			case RUNECRAFT: image.setSpriteId(SpriteID.Staticons2.RUNECRAFT); break;
			case SLAYER : image.setSpriteId(SpriteID.Staticons2.SLAYER); break;
			case FARMING : image.setSpriteId(SpriteID.Staticons2.FARMING); break;
			case HUNTER: image.setSpriteId(SpriteID.Staticons2.HUNTER); break;
			case CONSTRUCTION: image.setSpriteId(SpriteID.Staticons2.CONSTRUCTION); break;
			case SAILING: image.setSpriteId(SpriteID.Staticons2.SAILING); break;
			default: image.setSpriteId(SpriteID.Staticons2.TOTAL); break;
		}
	}

	private void setOtherImage(ChunkTask task) {
		String prefix = task.prefix != null ? task.prefix : "";
		switch (prefix) {
			case "[Kill X]":
				image.setSpriteId(SpriteID.SideiconsInterface.COMBAT);
				break;
			case "[Every Drop]":
				image.setSpriteId(SpriteID.SideiconsInterface.INVENTORY);
				break;
			case "[Collection Log]":
				image.setSpriteId(SpriteID.AccountIcons._4); //Collection Log book
				break;
			case "[Fill POH]":
				image.setSpriteId(SpriteID.SideIcons.HOUSE);
				break;
			case "[Fill Stashes]":
				image.setItemId(ItemID.DADDYSHOME_REWARD);
				break;
			case "[Permanent Unlockables]":
				image.setItemId(ItemID.SHADES_LOCK_GOLD);
				break;
			case "[BIS Skilling]":
				setItemImage(task);
				break;
			case "[All Shops]":
				image.setSpriteId(SpriteID.Mapfunction.GENERAL_STORE);
				break;
			default:
				image.setItemId(ItemID._100GUIDE_GUIDECAKE);
				break;
		}
	}
}
