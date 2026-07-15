package tong.statmod.progression.xp;

import tong.statmod.stats.StatType;

public record XpProgressNotice(
        StatType stat, int awardedXp, int newLevel, int levelsGained) {
}
