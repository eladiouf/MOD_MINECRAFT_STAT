package tong.statmod.integration.elementals;

import tong.statmod.stats.StatType;

import java.util.Set;
import java.util.function.IntUnaryOperator;

public final class ElementalsMageRules {
    private ElementalsMageRules() {}

    public static int magicalTotal(IntUnaryOperator levels) {
        return level(levels, StatType.ARCANE_POWER)
                + level(levels, StatType.WATER_AFFINITY)
                + level(levels, StatType.EARTH_AFFINITY)
                + level(levels, StatType.FIRE_AFFINITY)
                + level(levels, StatType.AIR_AFFINITY)
                + level(levels, StatType.CASTING_SPEED)
                + level(levels, StatType.MANA_POOL)
                + level(levels, StatType.ERUDITION)
                + level(levels, StatType.MAGIC_RESISTANCE)
                + level(levels, StatType.WILLPOWER);
    }

    public static boolean canAwaken(MageRaceProfile profile, IntUnaryOperator levels) {
        if (profile == null || !profile.supported()) {
            return false;
        }
        int minimum = profile.beastfolk() ? 13 : 12;
        int totalRequirement = profile.beastfolk() ? 40 : 36;
        int qualifiedStats = 0;
        for (StatType stat : new StatType[]{
                StatType.ARCANE_POWER,
                StatType.CASTING_SPEED,
                StatType.MANA_POOL,
                StatType.ERUDITION,
                StatType.MAGIC_RESISTANCE,
                StatType.WILLPOWER}) {
            if (level(levels, stat) >= minimum) {
                qualifiedStats++;
            }
        }
        return qualifiedStats >= 2 && magicalTotal(levels) >= totalRequirement;
    }

    public static ElementState stateForBaseBranch(ElementalBranch branch, IntUnaryOperator levels, Set<Integer> unlockedPerks) {
        if (branch == null || !branch.isBaseBranch()) {
            return ElementState.LOCKED;
        }
        boolean mastered = level(levels, primaryStat(branch)) >= 18
                && level(levels, secondaryCoreStat(branch)) >= 14
                && magicalTotal(levels) >= 48
                && unlockedPerks.contains(ElementalsPerkBindings.masteryPerk(branch).id);
        return mastered ? ElementState.MASTERED : ElementState.AWAKENED;
    }

    public static boolean canUnlockThirdBase(MageRaceProfile profile, ElementalBranch branch, IntUnaryOperator levels, Set<Integer> unlockedPerks, int masteredBaseCount) {
        if (profile == null || branch == null || !branch.isBaseBranch()) {
            return false;
        }
        int totalRequirement = profile.human() ? 56 : profile.beastfolk() ? 66 : 60;
        return masteredBaseCount >= 1
                && level(levels, primaryStat(branch)) >= 22
                && level(levels, StatType.ERUDITION) >= 18
                && level(levels, StatType.ARCANE_POWER) >= 18
                && magicalTotal(levels) >= totalRequirement
                && unlockedPerks.containsAll(ElementalsPerkBindings.thirdUnlockPerks(branch));
    }

    public static boolean canUnlockFourthBase(MageRaceProfile profile, ElementalBranch branch, IntUnaryOperator levels, Set<Integer> unlockedPerks, int masteredBaseCount) {
        if (profile == null || branch == null || !branch.isBaseBranch()) {
            return false;
        }
        int totalRequirement = profile.human() ? 72 : profile.beastfolk() ? 82 : 76;
        return masteredBaseCount >= 2
                && level(levels, primaryStat(branch)) >= 26
                && level(levels, StatType.ERUDITION) >= 22
                && level(levels, StatType.ARCANE_POWER) >= 22
                && magicalTotal(levels) >= totalRequirement
                && unlockedPerks.containsAll(ElementalsPerkBindings.fourthUnlockPerks(branch));
    }

    public static boolean canUseRareGrimoire(ElementalBranch branch, IntUnaryOperator levels) {
        if (branch == null) {
            return false;
        }
        return switch (branch) {
            case LIGHTNING -> level(levels, StatType.CASTING_SPEED) >= 20
                    && level(levels, StatType.ARCANE_POWER) >= 20
                    && magicalTotal(levels) >= 58;
            case BLOOD -> level(levels, StatType.WILLPOWER) >= 20
                    && level(levels, StatType.ARCANE_POWER) >= 20
                    && magicalTotal(levels) >= 58;
            default -> false;
        };
    }

    private static int level(IntUnaryOperator levels, StatType stat) {
        return levels.applyAsInt(stat.index);
    }

    private static StatType primaryStat(ElementalBranch branch) {
        return switch (branch) {
            case AIR -> StatType.AIR_AFFINITY;
            case WATER -> StatType.WATER_AFFINITY;
            case EARTH -> StatType.EARTH_AFFINITY;
            case FIRE -> StatType.FIRE_AFFINITY;
            case LIGHTNING -> StatType.CASTING_SPEED;
            case BLOOD -> StatType.WILLPOWER;
        };
    }

    private static StatType secondaryCoreStat(ElementalBranch branch) {
        return switch (branch) {
            case AIR, FIRE -> StatType.CASTING_SPEED;
            case WATER, EARTH -> StatType.MANA_POOL;
            case LIGHTNING, BLOOD -> StatType.ARCANE_POWER;
        };
    }
}
