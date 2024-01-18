package com.chunktasks;

import com.chunktasks.manager.ChunkTask;
import com.chunktasks.manager.ChunkTasksManager;
import com.chunktasks.panel.ChunkTasksPanel;
import com.chunktasks.sound.Sound;
import com.chunktasks.sound.SoundEngine;
import com.chunktasks.sound.SoundFileManager;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(
		name = "Chunk Tasks"
)
public class ChunkTasksPlugin extends Plugin {
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ItemManager itemManager;

	@Inject
	private ChunkTasksConfig config;
	@Inject
	private SoundEngine soundEngine;
	@Inject
	private OkHttpClient okHttpClient;
	@Inject
	private ScheduledExecutorService executor;
	@Inject
	private ChunkTasksManager chunkTasksManager;

	@Inject
	private ClientToolbar clientToolbar;

	private ChunkTasksPanel panel;
	private NavigationButton navButton;

	private static final Pattern COLLECTION_LOG_ITEM_REGEX = Pattern.compile("New item added to your collection log:.*");
	private static final int RESIZABLE_CLASSIC_LAYOUT = (161 << 16) | 13;
	private static final int RESIZABLE_MODERN_LAYOUT = (164 << 16) | 13;
	private static final int FIXED_CLASSIC_LAYOUT = 35913770;
	@Override
	protected void startUp() throws Exception {
		log.info("Example started!");
		executor.submit(() -> {
			SoundFileManager.ensureDownloadDirectoryExists();
			SoundFileManager.downloadAllMissingSounds(okHttpClient);
		});

		panel = new ChunkTasksPanel(this);
		panel.init();

		final BufferedImage worldIcon = ImageUtil.loadImageResource(getClass(), "/world.png");
		navButton = NavigationButton.builder()
				.tooltip("Chunk Tasks")
				.icon(worldIcon)
				.priority(10)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

//		chunkTasksManager.save();
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			chunkTasksManager.loadChunkTasksData();
		}
//		else if(gameStateChanged.getGameState().equals(GameState.LOGIN_SCREEN)) {
//			chunkTasksManager.save();
//		}
	}

//	@Subscribe
//	public void onChatMessage(ChatMessage chatMessage)
//	{
//		if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE && chatMessage.getType() != ChatMessageType.SPAM) {
//			return;
//		}
//
//		if (COLLECTION_LOG_ITEM_REGEX.matcher(chatMessage.getMessage()).matches()) {
//			client.addChatMessage(ChatMessageType.PUBLICCHAT, "Chunk Tasks", "Chunk Task Complete!", null);
//			soundEngine.playClip(Sound.CHUNK_TASK_COMPLETE);
//		}
//	}

	@Subscribe
	public void onPlayerChanged(PlayerChanged playerChanged)
	{
		checkWieldTasks();

	}

	private void checkWieldTasks()
	{
		List<ChunkTask> tasks = chunkTasksManager.getActiveChunkTasks();

		List<ChunkTask> wieldTasks = tasks.stream()
				.filter(t -> t.description.startsWith("Wield a"))
				.collect(Collectors.toList());

		if (!wieldTasks.isEmpty())
		{
			int[] ids = client.getLocalPlayer().getPlayerComposition().getEquipmentIds();

			List<String> equippedItems = Arrays.stream(ids).mapToObj(x -> {
				if (x > 512)
				{
					int id = x - 512;
					return client.getItemDefinition(id).getName();
				}
				else
				{
					return "";
				}
			}).collect(Collectors.toList());

			for (ChunkTask task : wieldTasks)
			{
				boolean itemEquipped = !Collections.disjoint(task.items, equippedItems);
				if (itemEquipped)
				{
					chunkTasksManager.completeTask(task);
					showChunkTaskCompletePopup("Chunk Task Complete", task.description);
				}
			}

		}
	}

	private void showChunkTaskCompletePopup(String title, String message)
	{
		int componentId = client.isResized()
				? client.getVarbitValue(Varbits.SIDE_PANELS) == 1
				? RESIZABLE_MODERN_LAYOUT
				: RESIZABLE_CLASSIC_LAYOUT
				: FIXED_CLASSIC_LAYOUT;

		WidgetNode widgetNode = client.openInterface(componentId, 660, WidgetModalMode.MODAL_CLICKTHROUGH);
		client.runScript(3343, title, message, -1);

		client.addChatMessage(ChatMessageType.PUBLICCHAT, "Chunk Tasks", title + ": " + message, null);
//		client.addChatMessage(ChatMessageType.CLAN_CHAT, "Chunk Tasks", title + ": " + message, null);
		soundEngine.playClip(Sound.CHUNK_TASK_COMPLETE);

		clientThread.invokeLater(() -> {
			Widget w = client.getWidget(660, 1);
			if (w.getWidth() > 0) {
				return false;
			}

			client.closeInterface(widgetNode, true);
			return true;
		});
	}

	public void importChunkTasks()
	{
		try
		{
			final Path path = showImportFolderDialog();
			if (path == null)
			{
				return;
			}
			final String json = new String(Files.readAllBytes(path));

			Type typeSetups = new TypeToken<ArrayList<ChunkTask>>()
			{

			}.getType();

			final ArrayList<ChunkTask> newChunkTasks = GSON.fromJson(json, typeSetups);

			// It's possible that the gson call succeeds but returns setups that have basically nothing
			// This can occur if trying to import a section file instead of a inventory setup file, since they share fields
			// Therefore, do some additional checking for required fields
//			for (final InventorySetup setup : newSetups)
//			{
//				if (isImportedSetupInvalid(setup))
//				{
//					throw new RuntimeException("Mass import section file was missing required fields");
//				}
//			}

//			for (final InventorySetup inventorySetup : newSetups)
//			{
//				preProcessNewSetup(inventorySetup);
//				cache.addSetup(inventorySetup);
//				inventorySetups.add(inventorySetup);
//			}

			chunkTasksManager.importTasks(newChunkTasks);
//			SwingUtilities.invokeLater(() -> panel.redrawOverviewPanel(false));

		}
		catch (Exception e)
		{
			log.error("Couldn't mass import setups", e);
			JOptionPane.showMessageDialog(panel,
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

		int returnValue = fileChooser.showOpenDialog(panel);

		if (returnValue == JFileChooser.APPROVE_OPTION)
		{
			return Paths.get(fileChooser.getSelectedFile().getAbsolutePath());
		}
		else
		{
			return null;
		}
	}

	@Provides
	ChunkTasksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChunkTasksConfig.class);
	}
}
