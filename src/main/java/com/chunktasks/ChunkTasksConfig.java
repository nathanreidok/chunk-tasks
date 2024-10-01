package com.chunktasks;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("chunktasks")
public interface ChunkTasksConfig extends Config
{
	String CONFIG_GROUP = "chunk-tasks";

	@ConfigItem(
			keyName = "allowChunkTasksDownload",
			name = "Allow Chunk Picker connections",
			description = "Allows tasks data to be pulled from Chunk Picker website",
			position = 0,
			warning = "This plugin submits your IP address to a 3rd party website not controlled or verified by the RuneLite Developers."
	)
	default boolean allowApiConnections() { return false; }

	@ConfigItem(
			keyName = "mapCode",
			name = "Chunk Picker Map Code",
			description = "https://source-chunk.github.io/chunk-picker-v2",
			position = 1
	)
	default String mapCode() { return ""; }

	@ConfigItem(
			keyName = "notifyOnManualCheck",
			name = "Notify on Manual Check",
			description = "Show popups when manually marking tasks as complete",
			position = 2
	)
	default boolean notifyOnManualCheck() { return true; }
}
