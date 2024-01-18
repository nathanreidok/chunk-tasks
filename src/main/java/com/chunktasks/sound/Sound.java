package com.chunktasks.sound;

public enum Sound {
    CHUNK_TASK_COMPLETE("task_completion_jingle.wav");

    private final String resourceName;

    Sound(String resNam) {
        resourceName = resNam;
    }

    String getResourceName() {
        return resourceName;
    }
}
