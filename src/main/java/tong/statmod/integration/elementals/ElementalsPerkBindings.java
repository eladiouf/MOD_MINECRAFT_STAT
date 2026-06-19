package tong.statmod.integration.elementals;

import tong.statmod.perks.Perk;

import java.util.Set;

public final class ElementalsPerkBindings {
    private ElementalsPerkBindings() {}

    public static Perk masteryPerk(ElementalBranch branch) {
        return switch (branch) {
            case AIR -> Perk.AIR_MASTERY;
            case WATER -> Perk.WATER_MASTERY;
            case EARTH -> Perk.EARTH_MASTERY;
            case FIRE -> Perk.FIRE_MASTERY;
            default -> throw new IllegalArgumentException("No mastery perk for " + branch);
        };
    }

    public static Set<Integer> thirdUnlockPerks(ElementalBranch branch) {
        return Set.of(Perk.ERUDITION_CORE.id, branchCorePerk(branch).id);
    }

    public static Set<Integer> fourthUnlockPerks(ElementalBranch branch) {
        return Set.of(Perk.ERUDITION_MASTERY.id, branchActivePerk(branch).id);
    }

    public static Perk rareRewardPerk(ElementalBranch branch) {
        return switch (branch) {
            case LIGHTNING -> Perk.AIR_TRANSCENDENCE;
            case BLOOD -> Perk.WILL_TRANSCENDENCE;
            default -> throw new IllegalArgumentException("No rare reward perk for " + branch);
        };
    }

    private static Perk branchCorePerk(ElementalBranch branch) {
        return switch (branch) {
            case AIR -> Perk.AIR_CORE;
            case WATER -> Perk.WATER_CORE;
            case EARTH -> Perk.EARTH_CORE;
            case FIRE -> Perk.FIRE_CORE;
            default -> throw new IllegalArgumentException("No core perk for " + branch);
        };
    }

    private static Perk branchActivePerk(ElementalBranch branch) {
        return switch (branch) {
            case AIR -> Perk.AIR_ACTIVE;
            case WATER -> Perk.WATER_ACTIVE;
            case EARTH -> Perk.EARTH_ACTIVE;
            case FIRE -> Perk.FIRE_ACTIVE;
            default -> throw new IllegalArgumentException("No active perk for " + branch);
        };
    }
}
