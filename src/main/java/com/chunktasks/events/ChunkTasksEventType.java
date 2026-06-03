package com.chunktasks.events;

public enum ChunkTasksEventType {
    TASKS_IMPORTED ("TASKS_IMPORTED"),
    CHANGE_TAB ("CHANGE_TAB");

    private final String displayText;
    ChunkTasksEventType(String displayText) {
        this.displayText = displayText;
    }

    public String displayText() {
        return this.displayText;
    }
}
