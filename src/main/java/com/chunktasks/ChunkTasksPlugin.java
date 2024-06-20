package com.chunktasks;

import com.chunktasks.managers.InventoryManager;
import com.chunktasks.managers.ChunkTask;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.managers.MapManager;
import com.chunktasks.panel.ChunkTasksPanel;
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

//		if (client.getGameState() == GameState.LOGGED_IN) {
//			clientThread.invokeLater(() -> {
//				int[] xps = client.getSkillExperiences();
//				System.arraycopy(xps, 0, previous_exp, 0, previous_exp.length);
//			});
//		} else {
//			Arrays.fill(previous_exp, 0);
//		}
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
//		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN || gameStateChanged.getGameState() == GameState.HOPPING) {
//			Arrays.fill(previous_exp, 0);
//		}
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

		mapManager.addCoordinateToHistory(worldPoint.getX(), worldPoint.getY());

		List<ChunkTask> completedMovementTasks = chunkTaskChecker.checkMovementTasks();
		List<ChunkTask> completedLocationTasks = chunkTaskChecker.checkLocationTasks();

		List<ChunkTask> completedTasks = Stream.concat(
				completedMovementTasks.stream(),
				completedLocationTasks.stream()
		).collect(Collectors.toList());
		if (!completedTasks.isEmpty()) {
			completeTasks(completedTasks);
		}
	}

//	@Subscribe
//	public void onPlayerChanged(PlayerChanged playerChanged) {
//		List<ChunkTask> tasks = chunkTasksManager.getActiveChunkTasks();
////		checkEquipTasks(tasks);
//	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged itemContainerChanged) {
		int containerId = itemContainerChanged.getContainerId();
		if (containerId == InventoryID.INVENTORY.getId())
			onInventoryChanged();
		else if (containerId == InventoryID.EQUIPMENT.getId())
			onEquipmentChanged();

//		List<ChunkTask> tasks = chunkTasksManager.getActiveChunkTasks();
//		checkObtainItemTasks(tasks);
//		checkObtainTasks(tasks);
//		checkSkillcapeTasks(tasks);
//		checkNonskillObtainTasks(tasks);
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

		List<ChunkTask> completedTasks = Stream.concat(
				completedObtainItemTasks.stream(),
				completedSkillingItemTasks.stream()
		).collect(Collectors.toList());
		if (!completedTasks.isEmpty()) {
			completeTasks(completedTasks);
		}
	}

	private void onEquipmentChanged() {
		List<ChunkTask> completedEquipTasks = chunkTaskChecker.checkEquipItemTasks();
		if (!completedEquipTasks.isEmpty())
			completeTasks(completedEquipTasks);
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged) {
		Skill changedSkill = statChanged.getSkill();
		List<ChunkTask> completedTasks = chunkTaskChecker.checkQuestSkillRequirementTasks(changedSkill);
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
