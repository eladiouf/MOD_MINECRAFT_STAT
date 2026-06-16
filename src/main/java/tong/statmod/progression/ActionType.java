package tong.statmod.progression;

import tong.statmod.stats.StatType;

public enum ActionType {
    MINING(StatType.FORGING),
    CRAFTING(StatType.FORGING),
    SMELTING(StatType.FORGING),
    COOKING(StatType.COOKING),
    FISHING(StatType.PRECISION),
    ENCHANTING(StatType.ALCHEMY),
    BREWING(StatType.ALCHEMY),
    PARKOUR(StatType.AGILITY),
    SWIMMING(StatType.PHYSICAL_ENDURANCE);

    private final StatType primaryStat;

    ActionType(StatType primaryStat) {
        this.primaryStat = primaryStat;
    }

    public StatType primaryStat() {
        return primaryStat;
    }
}
