package com.chunktasks;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface ChunkTasksConfig extends Config
{
	String CONFIG_GROUP = "chunk-tasks";
	String CONFIG_KEY = "tasks";

	@ConfigItem(
			keyName = "volume",
			name = "Volume",
			description = "Adjust how loud the audio is played",
			position = 0
	)
	default int volume() {
		return 100;
	}

	@ConfigItem(
			keyName = "notifyOnManualCheck",
			name = "Notify on Manual Check",
			description = "Show popups when manually marking tasks as complete",
			position = 1
	)
	default boolean notifyOnManualCheck() { return true; }
}
