package com.chunktasks;

import com.chunktasks.managers.InventoryManager;
import com.chunktasks.managers.SkillManager;
import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.managers.MapManager;
import com.chunktasks.panel.ChunkTasksPanel;
import com.chunktasks.services.ChunkTaskChecker;
import com.chunktasks.services.ChunkTaskNotifier;
import com.chunktasks.sound.SoundFileManager;
import com.google.inject.Provides;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

@Slf4j
@PluginDescriptor(
		name = "Chunk Tasks"
)
public class ChunkTasksPlugin extends Plugin {
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ItemManager itemManager;
	@Inject private ChunkTaskChecker chunkTaskChecker;
	@Inject private ChunkTasksConfig config;
	@Inject private OkHttpClient okHttpClient;
	@Inject private ScheduledExecutorService executor;
	@Inject private ChunkTasksManager chunkTasksManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private InventoryManager inventoryManager;
	@Inject private ChunkTaskNotifier chunkTaskNotifier;
	@Inject private MapManager mapManager;
	@Inject private SkillManager skillManager;

	private ChunkTasksPanel panel;
	private NavigationButton navButton;

//	private static final int[] previous_exp = new int[Skill.values().length];

	@Override
	protected void startUp() throws Exception {
		executor.submit(() -> {
			SoundFileManager.ensureDownloadDirectoryExists();
			SoundFileManager.downloadAllMissingSounds(okHttpClient);
		});

		panel = injector.getInstance(ChunkTasksPanel.class);
		panel.init(false);

		final BufferedImage curvedBoneIcon = ImageUtil.loadImageResource(getClass(), "/curved_bone.png");
		navButton = NavigationButton.builder()
				.tooltip("Chunk Tasks")
				.icon(curvedBoneIcon)
				.priority(3)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

		if (client.getGameState() == GameState.LOGGED_IN) {
			clientThread.invokeLater(() -> {
				int[] xps = client.getSkillExperiences();
				skillManager.resetSkills(xps);
			});
		} else {
			skillManager.clearSkills();
		}
	}

	@Override
	protected void shutDown() throws Exception {
		if (navButton != null) {
			clientToolbar.removeNavigation(navButton);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		panel.showHideImportButton(gameStateChanged.getGameState() == GameState.LOGGED_IN);
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			chunkTasksManager.loadChunkTasksData();
			panel.redrawChunkTasks();
		}
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN || gameStateChanged.getGameState() == GameState.HOPPING) {
			skillManager.clearSkills();
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged profileChanged) {
		if (client.getGameState() == GameState.LOGGED_IN) {
			chunkTasksManager.loadChunkTasksData();
			panel.redrawChunkTasks();
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick) {
		var worldPoint = client.getLocalPlayer().getWorldLocation();

		List<ChunkTask> completedTasks = chunkTaskChecker.checkPrayerTasks();

		boolean isNewLocation = mapManager.addCoordinateToHistory(worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
		if (isNewLocation) {
			completedTasks.addAll(chunkTaskChecker.checkMovementTasks());
			completedTasks.addAll(chunkTaskChecker.checkLocationTasks());
		}

		if (!completedTasks.isEmpty()) {
			completeTasks(completedTasks);
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged interactingChanged) {
		Actor source = interactingChanged.getSource();
		Actor target = interactingChanged.getTarget();
		if (source == null || target == null || !Objects.equals(source.getName(), client.getLocalPlayer().getName())) {
			return;
		}

//		log.error("SOURCE: " + source.getName() + " | TARGET: " + target.getName());
		List<ChunkTask> completedEquipTasks = chunkTaskChecker.checkInteractionTasks(target.getName());
		if (!completedEquipTasks.isEmpty())
			completeTasks(completedEquipTasks);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged itemContainerChanged) {
		int containerId = itemContainerChanged.getContainerId();
		if (containerId == InventoryID.INVENTORY.getId())
			onInventoryChanged();
		else if (containerId == InventoryID.EQUIPMENT.getId())
			onEquipmentChanged();
	}

	private void onInventoryChanged() {
		final ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
		if (itemContainer == null)
			return;

		List<String> inventory = Arrays.stream(itemContainer.getItems())
				.map(item -> client.getItemDefinition(item.getId()).getName().toLowerCase())
				.collect(Collectors.toList());
		inventoryManager.setInventory(inventory);

		List<ChunkTask> completedObtainItemTasks = chunkTaskChecker.checkObtainItemTasks();
		List<ChunkTask> completedSkillingItemTasks = chunkTaskChecker.checkSkillingItemTasks();
		List<ChunkTask> completedObtainItemIdTasks = chunkTaskChecker.checkObtainItemIdTasks();

		List<ChunkTask> completedTasks = Stream.of(completedObtainItemTasks, completedSkillingItemTasks, completedObtainItemIdTasks)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
		if (!completedTasks.isEmpty()) {
			completeTasks(completedTasks);
		}
	}

	private void onEquipmentChanged() {
		List<ChunkTask> completedTasks = Stream.concat(
				chunkTaskChecker.checkEquipItemTasks().stream(),
				chunkTaskChecker.checkEquipItemIdTasks().stream()
		).collect(Collectors.toList());
		if (!completedTasks.isEmpty())
			completeTasks(completedTasks);
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged) {
		Skill skill = statChanged.getSkill();
		int xpGained = skillManager.updateXp(skill, statChanged.getXp());
		List<ChunkTask> completedQuestSkillRequirementTasks = chunkTaskChecker.checkQuestSkillRequirementTasks(skill);
		List<ChunkTask> completedXpTasks = chunkTaskChecker.checkXpTasks(skill, xpGained);


		List<ChunkTask> completedTasks = Stream.concat(
				completedQuestSkillRequirementTasks.stream(),
				completedXpTasks.stream()
		).collect(Collectors.toList());
		if (!completedTasks.isEmpty())
			completeTasks(completedTasks);
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
//		log.error(chatMessage.getType() + " - " + chatMessage.getMessage());
		List<ChunkTask> completedTasks = chunkTaskChecker.checkChatMessageTasks(chatMessage);
		if (!completedTasks.isEmpty())
			completeTasks(completedTasks);
	}

	@Subscribe
	public void onPlayerChanged(PlayerChanged playerChanged) {
		List<ChunkTask> completedTasks = chunkTaskChecker.checkPlayerTasks();
		if (!completedTasks.isEmpty())
			completeTasks(completedTasks);
	}

	@Provides
	ChunkTasksConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ChunkTasksConfig.class);
	}

	public void completeTasks(List<ChunkTask> chunkTasks) {
		chunkTasks.forEach(task -> chunkTaskNotifier.completeTask(task));
		panel.redrawChunkTasks();
	}
}
