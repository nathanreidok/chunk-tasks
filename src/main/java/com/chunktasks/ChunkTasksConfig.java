package com.chunktasks;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface ChunkTasksConfig extends Config
{
	String CONFIG_GROUP = "chunk-tasks";
	String CONFIG_KEY = "tasks";

	String SAVE_DATA_KEY = "data";
	@ConfigItem(
			keyName = "greeting",
			name = "Welcome Greeting",
			description = "The message to show to the user when they login"
	)
	default String greeting()
	{
		return "Hello";
	}

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
			keyName = "tasks",
			name = "Chunk Tasks",
			description = "JSON export from Source Chunk"
	)
	default String tasks() { return ""; }
}
