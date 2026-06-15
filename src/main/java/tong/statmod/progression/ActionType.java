package tong.statmod.progression;

import tong.statmod.stats.StatType;

public enum ActionType {
    COMBAT(StatType.BRUTE_FORCE, StatType.BLADE_TECHNIQUE, StatType.RAPIDITE),
    MINING(StatType.FORGING),
    CRAFTING(StatType.FORGING, StatType.ALCHEMY),
    SMELTING(StatType.FORGING),
    FARMING(StatType.COOKING),
    FISHING(StatType.PRECISION),
    ENCHANTING(StatType.ALCHEMY),
    BREWING(StatType.ALCHEMY, StatType.ERUDITION);

    public final StatType[] primaryStats;

    ActionType(StatType... primaryStats) {
        this.primaryStats = primaryStats;
    }
}
