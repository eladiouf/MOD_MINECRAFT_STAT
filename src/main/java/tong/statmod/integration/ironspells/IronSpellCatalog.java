package tong.statmod.integration.ironspells;

import java.util.List;

public final class IronSpellCatalog {
    private static final String NS = "irons_spellbooks:";

    private IronSpellCatalog() {}

    public static List<String> spellIds(IronSpellProfile profile, IronSpellIntent intent) {
        return switch (intent) {
            case DIRECT_DAMAGE -> direct(profile);
            case AREA_DAMAGE -> area(profile);
            case CONTROL -> control(profile);
            case DEFENSE -> defense(profile);
            case MOBILITY -> mobility(profile);
            case ALLY_SUPPORT -> support(profile);
            case SUMMON -> summon(profile);
        };
    }

    private static List<String> direct(IronSpellProfile profile) {
        return switch (profile) {
            case FIRE_ARTILLERY -> ids("firebolt", "fireball", "scorch", "fire_arrow");
            case FROST_CONTROLLER -> ids("icicle", "snowball", "ray_of_frost");
            case STORM_HUNTER -> ids("lightning_bolt", "lightning_lance", "electrocute");
            case ARCANE_DUELIST -> ids("magic_missile", "magic_arrow", "guiding_bolt");
            case NECROMANTIC_PRESSURE -> ids("wither_skull", "blood_slash", "acid_orb");
            case HOLY_SUPPORT -> ids("guiding_bolt", "divine_smite", "sunbeam");
        };
    }

    private static List<String> area(IronSpellProfile profile) {
        return switch (profile) {
            case FIRE_ARTILLERY -> ids("magma_bomb", "flaming_barrage", "blaze_storm", "wall_of_fire");
            case FROST_CONTROLLER -> ids("frostwave", "ice_spikes", "blizzard", "cone_of_cold");
            case STORM_HUNTER -> ids("chain_lightning", "ball_lightning", "thunderstorm", "shockwave");
            case ARCANE_DUELIST -> ids("starfall", "arrow_volley", "black_hole", "gravity_fissure");
            case NECROMANTIC_PRESSURE -> ids("blood_needles", "sculk_tentacles", "fang_swirl", "earthquake");
            case HOLY_SUPPORT -> ids("healing_circle", "cloud_of_regeneration", "sunbeam", "divine_smite");
        };
    }

    private static List<String> control(IronSpellProfile profile) {
        return switch (profile) {
            case FIRE_ARTILLERY -> ids("wall_of_fire", "heat_surge", "fire_breath");
            case FROST_CONTROLLER -> ids("ice_tomb", "frostbite", "cone_of_cold", "frostwave");
            case STORM_HUNTER -> ids("shockwave", "charge", "electrocute");
            case ARCANE_DUELIST -> ids("arcane_shackle", "counterspell", "telekinesis", "slow");
            case NECROMANTIC_PRESSURE -> ids("blight", "root", "ray_of_siphoning");
            case HOLY_SUPPORT -> ids("slow", "spectral_hammer", "gust");
        };
    }

    private static List<String> defense(IronSpellProfile profile) {
        return switch (profile) {
            case FIRE_ARTILLERY -> ids("shield", "fortify", "heat_surge");
            case FROST_CONTROLLER -> ids("ice_block", "shield", "fortify");
            case STORM_HUNTER -> ids("evasion", "shield", "fortify");
            case ARCANE_DUELIST -> ids("shield", "evasion", "abyssal_shroud");
            case NECROMANTIC_PRESSURE -> ids("abyssal_shroud", "oakskin", "shield");
            case HOLY_SUPPORT -> ids("fortify", "shield", "blessing_of_life");
        };
    }

    private static List<String> mobility(IronSpellProfile profile) {
        return switch (profile) {
            case FIRE_ARTILLERY -> ids("burning_dash", "teleport");
            case FROST_CONTROLLER -> ids("frost_step", "teleport");
            case STORM_HUNTER -> ids("thunder_step", "ascension", "teleport");
            case ARCANE_DUELIST -> ids("teleport", "evasion");
            case NECROMANTIC_PRESSURE -> ids("blood_step", "teleport");
            case HOLY_SUPPORT -> ids("angel_wings", "haste", "teleport");
        };
    }

    private static List<String> support(IronSpellProfile profile) {
        return switch (profile) {
            case FIRE_ARTILLERY -> ids("haste", "fortify", "cleanse");
            case FROST_CONTROLLER -> ids("fortify", "cleanse", "healing_circle");
            case STORM_HUNTER -> ids("haste", "fortify", "healing_circle");
            case ARCANE_DUELIST -> ids("haste", "cleanse", "healing_circle");
            case NECROMANTIC_PRESSURE -> ids("heal", "cloud_of_regeneration", "oakskin");
            case HOLY_SUPPORT -> ids("healing_circle", "greater_heal", "cleanse", "haste");
        };
    }

    private static List<String> summon(IronSpellProfile profile) {
        return switch (profile) {
            case FIRE_ARTILLERY -> ids("raise_hell", "summon_vex");
            case FROST_CONTROLLER -> ids("summon_polar_bear", "summon_vex");
            case STORM_HUNTER -> ids("summon_swords", "summon_vex");
            case ARCANE_DUELIST -> ids("summon_swords", "summon_vex");
            case NECROMANTIC_PRESSURE -> ids("raise_dead", "summon_vex", "scapegoat");
            case HOLY_SUPPORT -> ids("wisp", "summon_swords", "summon_vex");
        };
    }

    private static List<String> ids(String... paths) {
        return java.util.Arrays.stream(paths).map(path -> NS + path).toList();
    }
}
