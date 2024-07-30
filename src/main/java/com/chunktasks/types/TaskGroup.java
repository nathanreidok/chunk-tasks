package com.chunktasks.types;

public enum TaskGroup {
    SKILL ("Skill"),
    BIS ("Best in Slot"),
    QUEST ("Quest"),
    DIARY ("Diary"),
    OTHER ("Other");

    private final String displayText;
    TaskGroup(String displayText) {
        this.displayText = displayText;
    }

    public String displayText() {
        return this.displayText;
    }
}
