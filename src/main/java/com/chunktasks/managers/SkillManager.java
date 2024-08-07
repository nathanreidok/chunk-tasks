package com.chunktasks.managers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

import javax.inject.Singleton;
import java.util.Arrays;

@Singleton
@Slf4j
public class SkillManager {
    private static final int[] skillXp = new int[Skill.values().length];

    public void clearSkills() {
        Arrays.fill(skillXp, -1);
    }

    public void resetSkills(int[] newSkillXp) {
        System.arraycopy(newSkillXp, 0, skillXp, 0, skillXp.length);
    }

    public int updateXp(Skill skill, int xp) {
        int currentXp = skillXp[skill.ordinal()];
        skillXp[skill.ordinal()] = xp;
        return currentXp == -1 ? 0 : xp - currentXp;
    }
}
