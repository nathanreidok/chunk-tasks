package com.chunktasks.tasks;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Skill;

@Getter
@Setter
public class XpTaskConfig {
    private Skill skill;
    private int xpMin;
    private int xpMax;
    private MapBoundary location;
}
