package tong.statmod.progression;

import tong.statmod.stats.StatType;

public enum ActionType {
    MELEE_HEAVY(StatType.BRUTE_FORCE, 5),
    MELEE_SLASH(StatType.BLADE_TECHNIQUE, 5),
    MELEE_RAPID(StatType.RAPIDITE, 3),
    DODGE(StatType.AGILITY, 5),
    TAKE_DAMAGE(StatType.PHYSICAL_RESISTANCE, 2),
    GUARD(StatType.PHYSICAL_ENDURANCE, 5),
    RANGED_HIT(StatType.PRECISION, 5),
    MAGIC_DAMAGE(StatType.ARCANE_POWER, 5),
    UNDERWATER_ACTION(StatType.WATER_AFFINITY, 3),
    MINE_BLOCK(StatType.EARTH_AFFINITY, 2),
    FIRE_ACTION(StatType.FIRE_AFFINITY, 3),
    AIR_ACTION(StatType.AIR_AFFINITY, 3),
    POTION_HIT(StatType.MAGIC_RESISTANCE, 2),
    USE_ITEM(StatType.CASTING_SPEED, 2),
    ENCHANT(StatType.ERUDITION, 5),
    KILL_MOB(StatType.TRACKING, 10),
    EXPLORE(StatType.KEEN_SENSES, 3),
    CRAFT(StatType.FORGING, 3),
    COOK(StatType.COOKING, 3),
    BREW(StatType.ALCHEMY, 5),
    KILL_BOSS(StatType.INTIMIDATION, 25),
    SURVIVE_LOW_HP(StatType.WILLPOWER, 5);

    public final StatType primaryStat;
    public final int baseXp;

    ActionType(StatType primaryStat, int baseXp) {
        this.primaryStat = primaryStat;
        this.baseXp = baseXp;
    }
}
