package tong.statmod.progression.xp;

import tong.statmod.stats.StatType;

public record StatXpAward(StatType stat, int amount, String reason) {
}
