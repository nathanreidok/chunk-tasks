package com.chunktasks.tasks;

import lombok.Getter;
import net.runelite.api.ChatMessageType;

@Getter
public class ChatMessageConfig {
    private ChatMessageType chatMessageType;
    private String message;
    private MapBoundary location;
}
