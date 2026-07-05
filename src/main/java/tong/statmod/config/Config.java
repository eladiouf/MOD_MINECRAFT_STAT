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
    public static final ModConfigSpec.DoubleValue WEAPON_DAMAGE_SCALE;
    public static final ModConfigSpec.DoubleValue WEAPON_DAMAGE_BASE;
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
        WEAPON_DAMAGE_BASE = BUILDER
                .comment("Multiplicateur de dégâts de MÊLÉE de base du joueur (niveau 0 de stat).",
                        "1.0 = dégâts d'arme bruts. Monte ce chiffre si TOUTES les armes tapent trop faible.")
                .defineInRange("weaponDamageBase", 1.5, 0.5, 10.0);
        WEAPON_DAMAGE_SCALE = BUILDER
                .comment("Amplitude du bonus de dégâts par la stat de combat, au niveau MAX.",
                        "À 3.0, une stat de combat maxée ajoute ×3 par-dessus la base (donc ~×4.5 total).")
                .defineInRange("weaponDamageScale", 3.0, 0.0, 20.0);
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
                .comment("Intégration L2 Hostility : facteur de niveau par étage.",
                        "0.0 = utiliser la COURBE D'ÉQUILIBRAGE INTÉGRÉE (recommandé) : douce au début,",
                        "  qui accélère en profondeur (étage 10≈niv 7, 50≈38, 100≈78, plafonnée par le cap).",
                        "> 0.0 = mode manuel : niveau = floor × ce facteur (écrase la courbe intégrée).")
                .defineInRange("l2HostilityPerFloor", 0.0, 0.0, 0.5);
        DUNGEON_HOSTILITY_CAP = BUILDER
                .comment("Intégration L2 Hostility : plafond du niveau appliqué par étage (courbe et mode manuel).")
                .defineInRange("l2HostilityCap", 100, 0, 500);
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

    public static double getWeaponDamageBase() {
        try {
            return WEAPON_DAMAGE_BASE.get();
        } catch (IllegalStateException e) {
            return 1.5;
        }
    }

    public static double getWeaponDamageScale() {
        try {
            return WEAPON_DAMAGE_SCALE.get();
        } catch (IllegalStateException e) {
            return 3.0;
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
