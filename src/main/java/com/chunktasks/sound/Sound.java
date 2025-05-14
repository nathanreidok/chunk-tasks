package com.chunktasks.sound;

public enum Sound {
    NONE(""),
    LEAGUES_TASK("task_completion_jingle.wav"),
    C_ENGINEER("task_completed.wav"),
    FANFARE("final_fantasy_victory_fanfare.wav"),
    MARIO_YAHOO("mario_yahoo.wav"),
    POKEMON_LEVEL_UP("pokemon_level_up.wav"),
    ZELDA_OPEN_CHEST("zelda_open_chest.wav");

    private final String resourceName;

    Sound(String resNam) {
        resourceName = resNam;
    }

    String getResourceName() {
        return resourceName;
    }
}
