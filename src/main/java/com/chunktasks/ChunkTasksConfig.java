package com.chunktasks;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("chunktasks")
public interface ChunkTasksConfig extends Config
{
	String CONFIG_GROUP = "chunk-tasks";

	@ConfigItem(
			keyName = "mapCode",
			name = "Chunk Picker Map Code",
			description = "https://source-chunk.github.io/chunk-picker-v2",
			position = 0
	)
	default String mapCode() { return ""; }

	@ConfigItem(
			keyName = "notifyOnManualCheck",
			name = "Notify on Manual Check",
			description = "Show popups when manually marking tasks as complete",
			position = 1
	)
	default boolean notifyOnManualCheck() { return true; }
}
