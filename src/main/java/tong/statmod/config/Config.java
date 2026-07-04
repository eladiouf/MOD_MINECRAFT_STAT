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
    public static final ModConfigSpec.DoubleValue DUNGEON_XP_BASE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DUNGEON_XP_PER_FLOOR;
    public static final ModConfigSpec.IntValue DUNGEON_BOSS_STAT_GAIN;
    public static final ModConfigSpec.DoubleValue DUNGEON_HOSTILITY_PER_FLOOR;
    public static final ModConfigSpec.IntValue DUNGEON_HOSTILITY_CAP;

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

        BUILDER.push("trial_dungeon");
        DUNGEON_XP_BASE_MULTIPLIER = BUILDER
                .comment("Multiplier de base pour l'XP gagné dans le Trial Dungeon (ajouté à floor*perFloor).")
                .defineInRange("xpBaseMultiplier", 1.0, 0.0, 10.0);
        DUNGEON_XP_PER_FLOOR = BUILDER
                .comment("Multiplier XP additionnel par étage — étage 20 donne XP*(base + 20*perFloor).")
                .defineInRange("xpPerFloor", 0.05, 0.0, 1.0);
        DUNGEON_BOSS_STAT_GAIN = BUILDER
                .comment("Nombre de niveaux de stat gagnés directement à la mort d'un boss d'étage.")
                .defineInRange("bossStatGain", 2, 0, 10);
        DUNGEON_HOSTILITY_PER_FLOOR = BUILDER
                .comment("Intégration L2 Hostility : niveau de hostilité appliqué aux mobs = floor * ce facteur.",
                        "0.0 = désactivé (recommandé pour éviter surdifficulté avec config L2 par défaut).",
                        "L2 Hostility a un niveau de base de 20, donc 0.1 = étage 10 = niveau 21 (déjà trop fort).")
                .defineInRange("l2HostilityPerFloor", 0.0, 0.0, 0.5);
        DUNGEON_HOSTILITY_CAP = BUILDER
                .comment("Intégration L2 Hostility : plafond du niveau de hostilité appliqué par étage.",
                        "Avec l2HostilityPerFloor = 0.0, ce paramètre n'a pas d'effet.")
                .defineInRange("l2HostilityCap", 30, 0, 500);
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

    public static double getDungeonXpBaseMultiplier() {
        try {
            return DUNGEON_XP_BASE_MULTIPLIER.get();
        } catch (IllegalStateException e) {
            return 1.0;
        }
    }

    public static double getDungeonXpPerFloor() {
        try {
            return DUNGEON_XP_PER_FLOOR.get();
        } catch (IllegalStateException e) {
            return 0.05;
        }
    }

    public static int getDungeonBossStatGain() {
        try {
            return DUNGEON_BOSS_STAT_GAIN.get();
        } catch (IllegalStateException e) {
            return 2;
        }
    }

    public static double getDungeonHostilityPerFloor() {
        try {
            return DUNGEON_HOSTILITY_PER_FLOOR.get();
        } catch (IllegalStateException e) {
            return 1.0;
        }
    }

    public static int getDungeonHostilityCap() {
        try {
            return DUNGEON_HOSTILITY_CAP.get();
        } catch (IllegalStateException e) {
            return 200;
        }
    }
}
