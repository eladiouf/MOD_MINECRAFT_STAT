package tong.statmod.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue COMBAT_XP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue NON_COMBAT_XP_MULTIPLIER;
    public static final ModConfigSpec.IntValue MAX_STAT_LEVEL;
    public static final ModConfigSpec.IntValue BASE_PERK_POINTS_PER_LEVEL;

    static {
        BUILDER.push("general");
        COMBAT_XP_MULTIPLIER = BUILDER
                .comment("Multiplier for combat XP gain (0.0 to 10.0)")
                .defineInRange("combatXpMultiplier", 1.0, 0.0, 10.0);
        NON_COMBAT_XP_MULTIPLIER = BUILDER
                .comment("Multiplier for non-combat XP gain (0.0 to 10.0)")
                .defineInRange("nonCombatXpMultiplier", 1.0, 0.0, 10.0);
        MAX_STAT_LEVEL = BUILDER
                .comment("Maximum stat level (1 to 100)")
                .defineInRange("maxStatLevel", 100, 1, 100);
        BASE_PERK_POINTS_PER_LEVEL = BUILDER
                .comment("Base perk points granted per stat level")
                .defineInRange("basePerkPointsPerLevel", 1, 0, 10);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static ModConfigSpec getSpec() {
        return SPEC;
    }

    public static int getMaxStatLevel() {
        try {
            return MAX_STAT_LEVEL.get();
        } catch (IllegalStateException e) {
            return 100;
        }
    }

    public static double getCombatXpMultiplier() {
        try {
            return COMBAT_XP_MULTIPLIER.get();
        } catch (IllegalStateException e) {
            return 1.0;
        }
    }

    public static double getNonCombatXpMultiplier() {
        try {
            return NON_COMBAT_XP_MULTIPLIER.get();
        } catch (IllegalStateException e) {
            return 1.0;
        }
    }

    public static int getBasePerkPointsPerLevel() {
        try {
            return BASE_PERK_POINTS_PER_LEVEL.get();
        } catch (IllegalStateException e) {
            return 1;
        }
    }
}
