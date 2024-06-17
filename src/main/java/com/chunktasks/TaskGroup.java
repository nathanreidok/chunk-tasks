package com.chunktasks;

public enum TaskGroup {
    SKILL ("Skill"),
    BIS ("Best in Slot"),
    QUEST ("Quest"),
    OTHER ("Other");

    private final String displayText;
    TaskGroup(String displayText) {
        this.displayText = displayText;
    }

    public String displayText() {
        return this.displayText;
    }
}
