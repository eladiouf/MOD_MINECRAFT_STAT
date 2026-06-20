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
        int totalRequirement = awakeningTotalRequirement(profile);
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
        return stateForBaseBranch(branch, null, levels, unlockedPerks);
    }

    public static ElementState stateForBaseBranch(ElementalBranch branch,
                                                  MageRaceProfile profile,
                                                  IntUnaryOperator levels,
                                                  Set<Integer> unlockedPerks) {
        if (branch == null || !branch.isBaseBranch()) {
            return ElementState.LOCKED;
        }
        int supportRequirement = profile != null && profile.favors(branch) ? 12 : 14;
        int totalRequirement = masteryTotalRequirement(profile);
        boolean mastered = level(levels, primaryStat(branch)) >= 18
                && level(levels, secondaryCoreStat(branch)) >= supportRequirement
                && magicalTotal(levels) >= totalRequirement
                && unlockedPerks.contains(ElementalsPerkBindings.masteryPerk(branch).id);
        return mastered ? ElementState.MASTERED : ElementState.AWAKENED;
    }

    public static boolean canUnlockThirdBase(MageRaceProfile profile, ElementalBranch branch, IntUnaryOperator levels, Set<Integer> unlockedPerks, int masteredBaseCount) {
        if (profile == null || !profile.supported() || branch == null || !branch.isBaseBranch()) {
            return false;
        }
        int primaryRequirement = profile.human() ? 20 : profile.beastfolk() ? 24 : 22;
        int eruditionRequirement = 18;
        int arcaneRequirement = 18;
        if (profile.human()) {
            eruditionRequirement = 16;
            arcaneRequirement = 16;
        }
        int totalRequirement = profile.human() ? 54 : profile.beastfolk() ? 66 : 60;
        return masteredBaseCount >= 1
                && level(levels, primaryStat(branch)) >= primaryRequirement
                && level(levels, StatType.ERUDITION) >= eruditionRequirement
                && level(levels, StatType.ARCANE_POWER) >= arcaneRequirement
                && magicalTotal(levels) >= totalRequirement
                && unlockedPerks.containsAll(ElementalsPerkBindings.thirdUnlockPerks(branch));
    }

    public static boolean canUnlockFourthBase(MageRaceProfile profile, ElementalBranch branch, IntUnaryOperator levels, Set<Integer> unlockedPerks, int masteredBaseCount) {
        if (profile == null || !profile.supported() || branch == null || !branch.isBaseBranch()) {
            return false;
        }
        int primaryRequirement = profile.human() ? 24 : profile.beastfolk() ? 28 : 26;
        int eruditionRequirement = profile.human() ? 20 : profile.beastfolk() ? 24 : 22;
        int arcaneRequirement = profile.human() ? 20 : profile.beastfolk() ? 24 : 22;
        int totalRequirement = profile.human() ? 70 : profile.beastfolk() ? 86 : 78;
        return masteredBaseCount >= 2
                && level(levels, primaryStat(branch)) >= primaryRequirement
                && level(levels, StatType.ERUDITION) >= eruditionRequirement
                && level(levels, StatType.ARCANE_POWER) >= arcaneRequirement
                && magicalTotal(levels) >= totalRequirement
                && unlockedPerks.containsAll(ElementalsPerkBindings.fourthUnlockPerks(branch));
    }

    public static boolean canUseRareGrimoire(ElementalBranch branch, IntUnaryOperator levels) {
        return canUseRareGrimoire(null, branch, levels);
    }

    public static boolean canUseRareGrimoire(MageRaceProfile profile, ElementalBranch branch, IntUnaryOperator levels) {
        if (profile != null && !profile.supported()) {
            return false;
        }
        if (branch == null) {
            return false;
        }
        boolean human = profile != null && profile.human();
        boolean beastfolk = profile != null && profile.beastfolk();
        boolean elf = profile != null && profile.elf();
        boolean dwarf = profile != null && profile.dwarf();
        return switch (branch) {
            case LIGHTNING -> level(levels, StatType.CASTING_SPEED) >= (elf ? 18 : human ? 20 : 22)
                    && level(levels, StatType.ARCANE_POWER) >= 20
                    && magicalTotal(levels) >= (elf ? 58 : human ? 60 : 64);
            case BLOOD -> level(levels, StatType.WILLPOWER) >= ((human || dwarf) ? 20 : 22)
                    && level(levels, StatType.ARCANE_POWER) >= 20
                    && magicalTotal(levels) >= ((human || dwarf) ? 60 : beastfolk ? 64 : 62);
            case METAL -> level(levels, StatType.EARTH_AFFINITY) >= (dwarf ? 20 : human ? 22 : 24)
                    && level(levels, StatType.FIRE_AFFINITY) >= (dwarf ? 18 : human ? 20 : 22)
                    && level(levels, StatType.ARCANE_POWER) >= (dwarf ? 18 : 20)
                    && magicalTotal(levels) >= (dwarf ? 60 : human ? 66 : 72);
            default -> false;
        };
    }

    private static int awakeningTotalRequirement(MageRaceProfile profile) {
        if (profile == null) {
            return 48;
        }
        if (profile.elf()) {
            return 34;
        }
        if (profile.dwarf()) {
            return 36;
        }
        return profile.human() ? 38 : profile.beastfolk() ? 42 : 48;
    }

    private static int masteryTotalRequirement(MageRaceProfile profile) {
        if (profile == null) {
            return 48;
        }
        if (profile.human()) {
            return 46;
        }
        return profile.beastfolk() ? 52 : 48;
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
            case METAL -> StatType.EARTH_AFFINITY;
        };
    }

    private static StatType secondaryCoreStat(ElementalBranch branch) {
        return switch (branch) {
            case AIR, FIRE -> StatType.CASTING_SPEED;
            case WATER, EARTH -> StatType.MANA_POOL;
            case LIGHTNING, BLOOD -> StatType.ARCANE_POWER;
            case METAL -> StatType.FIRE_AFFINITY;
        };
    }
}
