package com.chunktasks.tasks;

import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Skill;

import java.util.HashMap;

@Getter
public class ChatMessageConfig {
    private ChatMessageType chatMessageType;
    private String message;
    private MapBoundary location;
    private HashMap<Skill, Integer> skills;
    private String item;
}
