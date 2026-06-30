package tong.statmod.magic;

import tong.statmod.stats.StatType;

public final class MagicNodeMigration {

    public static Condition defaultCondition(MagicNode node) {
        int arcane = universalArcane(node.kind(), node.tier());
        int erudition = universalErudition(node.kind(), node.tier());
        Condition tertiary = tertiaryCondition(node);

        Condition base = Condition.And.of(
                new Condition.StatCondition(StatType.ARCANE_POWER, arcane),
                new Condition.StatCondition(StatType.ERUDITION, erudition)
        );
        if (tertiary == null) return base;
        return Condition.And.of(base, tertiary);
    }

    private static int universalArcane(MagicNodeKind kind, MagicTier tier) {
        return switch (kind) {
            case TRUNK_FOUNDATION -> switch (tier) {
                case T1 -> 1; case T2 -> 2; case T3, T4 -> 3;
            };
            case BRANCH_OPENER -> 2;
            case BRANCH_TIER, SIGNATURE_SPELL -> switch (tier) {
                case T1 -> 2; case T2 -> 4; case T3 -> 6; case T4 -> 10;
            };
            case LATEGAME_GATE -> 10;
        };
    }

    private static int universalErudition(MagicNodeKind kind, MagicTier tier) {
        return switch (kind) {
            case TRUNK_FOUNDATION -> switch (tier) {
                case T1 -> 1; case T2 -> 2; case T3, T4 -> 3;
            };
            case BRANCH_OPENER -> 1;
            case BRANCH_TIER, SIGNATURE_SPELL -> switch (tier) {
                case T1 -> 2; case T2 -> 3; case T3 -> 5; case T4 -> 7;
            };
            case LATEGAME_GATE -> 7;
        };
    }

    private static Condition tertiaryCondition(MagicNode node) {
        if (node.kind() == MagicNodeKind.TRUNK_FOUNDATION) return null;

        return switch (node.id()) {
            // ── Branch openers ──
            case "fire/opener/ignition" -> stat(StatType.FIRE_AFFINITY, 2);
            case "water/opener/ice_awakening" -> stat(StatType.WATER_AFFINITY, 2);
            case "air/opener/spark_awakening" -> stat(StatType.AIR_AFFINITY, 2);
            case "earth/opener/nature_awakening" -> stat(StatType.EARTH_AFFINITY, 2);
            case "holy/opener/bless_awakening" -> stat(StatType.WILLPOWER, 2);
            case "holy/opener/light_awakening" -> stat(StatType.WILLPOWER, 2);
            case "blood/opener/hemo_awakening" -> stat(StatType.PHYSICAL_ENDURANCE, 2);
            case "ender/opener/void_awakening" -> stat(StatType.KEEN_SENSES, 2);
            case "evocation/opener/trick_awakening" -> stat(StatType.TRACKING, 2);
            case "eldritch/opener/dark_awakening" -> stat(StatType.WILLPOWER, 2);

            // ── Fire tiers ──
            case "fire/tier/ember_path" -> stat(StatType.FIRE_AFFINITY, 1);
            case "fire/tier/flame_path" -> stat(StatType.FIRE_AFFINITY, 3);
            case "fire/tier/inferno_path" -> stat(StatType.INTIMIDATION, 5);

            // ── Water tiers ──
            case "water/tier/frost_path" -> stat(StatType.WATER_AFFINITY, 1);
            case "water/tier/chill_path" -> stat(StatType.WATER_AFFINITY, 3);
            case "water/tier/glacier_path" -> stat(StatType.MAGIC_RESISTANCE, 5);

            // ── Air tiers ──
            case "air/tier/spark_path" -> stat(StatType.AIR_AFFINITY, 1);
            case "air/tier/storm_path" -> stat(StatType.AIR_AFFINITY, 3);
            case "air/tier/thunder_path" -> stat(StatType.AGILITY, 5);

            // ── Earth tiers ──
            case "earth/tier/poison_path" -> stat(StatType.EARTH_AFFINITY, 1);
            case "earth/tier/toxin_path" -> stat(StatType.EARTH_AFFINITY, 3);
            case "earth/tier/grand_nature_path" -> stat(StatType.PHYSICAL_ENDURANCE, 5);

            // ── Holy tiers ──
            case "holy/tier/bless_path" -> stat(StatType.WILLPOWER, 1);
            case "holy/tier/grace_path" -> stat(StatType.ERUDITION, 3);
            case "holy/tier/divine_path" -> stat(StatType.WILLPOWER, 5);

            // ── Blood tiers ──
            case "blood/tier/hemo_path" -> stat(StatType.PHYSICAL_ENDURANCE, 1);
            case "blood/tier/drain_path" -> stat(StatType.INTIMIDATION, 3);
            case "blood/tier/exsanguinate_path" -> stat(StatType.WILLPOWER, 5);

            // ── Ender tiers ──
            case "ender/tier/void_path" -> stat(StatType.KEEN_SENSES, 1);
            case "ender/tier/rift_path" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/tier/abyss_path" -> stat(StatType.KEEN_SENSES, 5);

            // ── Evocation tiers ──
            case "evocation/tier/trick_path" -> stat(StatType.ERUDITION, 1);
            case "evocation/tier/illusion_path" -> stat(StatType.TRACKING, 3);
            case "evocation/tier/mastery_path" -> stat(StatType.ERUDITION, 5);

            // ── Eldritch tiers ──
            case "eldritch/tier/dark_path" -> stat(StatType.MAGIC_RESISTANCE, 1);
            case "eldritch/tier/void_gaze_path" -> stat(StatType.WILLPOWER, 3);
            case "eldritch/tier/abyssal_path" -> stat(StatType.WILLPOWER, 5);

            // ── Fire signature spells ──
            case "fire/signature/firebolt" -> stat(StatType.FIRE_AFFINITY, 1);
            case "fire/signature/fireball" -> stat(StatType.INTIMIDATION, 1);
            case "fire/signature/scorch" -> stat(StatType.FIRE_AFFINITY, 1);
            case "fire/signature/blaze_storm" -> stat(StatType.INTIMIDATION, 3);
            case "fire/signature/meteor" -> stat(StatType.INTIMIDATION, 5);
            case "fire/signature/burning_dash" -> stat(StatType.AGILITY, 3);
            case "fire/signature/wall_of_fire" -> stat(StatType.WILLPOWER, 3);
            case "fire/signature/fire_arrow" -> stat(StatType.PRECISION, 1);
            case "fire/signature/heat_surge" -> stat(StatType.PHYSICAL_ENDURANCE, 3);
            case "fire/signature/pyromaniac" -> stat(StatType.PHYSICAL_ENDURANCE, 3);
            case "fire/signature/flaming_strike" -> stat(StatType.PHYSICAL_ENDURANCE, 3);
            case "fire/signature/fire_breath" -> stat(StatType.FIRE_AFFINITY, 1);
            case "fire/signature/blaze_strike" -> stat(StatType.FIRE_AFFINITY, 3);
            case "fire/signature/magma_bomb" -> stat(StatType.INTIMIDATION, 5);
            case "fire/signature/hellfire" -> stat(StatType.INTIMIDATION, 5);
            case "fire/signature/wildfire" -> stat(StatType.WILLPOWER, 5);
            case "fire/signature/blaze_storm_fire" -> stat(StatType.INTIMIDATION, 3);

            // ── Water signature spells ──
            case "water/signature/icicle" -> stat(StatType.WATER_AFFINITY, 1);
            case "water/signature/frost_step" -> stat(StatType.AGILITY, 1);
            case "water/signature/blizzard" -> stat(StatType.INTIMIDATION, 3);
            case "water/signature/healing_circle" -> stat(StatType.WILLPOWER, 5);
            case "water/signature/heal" -> stat(StatType.WILLPOWER, 1);
            case "water/signature/ray_of_frost" -> stat(StatType.PRECISION, 1);
            case "water/signature/ice_block" -> stat(StatType.MAGIC_RESISTANCE, 1);
            case "water/signature/cloud_of_regeneration" -> stat(StatType.WILLPOWER, 3);
            case "water/signature/water_breathing" -> stat(StatType.PHYSICAL_ENDURANCE, 1);
            case "water/signature/poison_splash" -> stat(StatType.WILLPOWER, 3);
            case "water/signature/ice_tomb" -> stat(StatType.CASTING_SPEED, 3);
            case "water/signature/ice_cage" -> stat(StatType.CASTING_SPEED, 3);
            case "water/signature/glacier" -> stat(StatType.MAGIC_RESISTANCE, 5);
            case "water/signature/ice_bomb" -> stat(StatType.WATER_AFFINITY, 5);
            case "water/signature/nature_heal" -> stat(StatType.WILLPOWER, 3);
            case "water/signature/blessing_of_life_water" -> stat(StatType.WILLPOWER, 5);
            case "water/signature/ice_spikes" -> stat(StatType.PRECISION, 1);
            case "water/signature/cleanse" -> stat(StatType.WILLPOWER, 1);

            // ── Air signature spells ──
            case "air/signature/lightning_bolt" -> stat(StatType.PRECISION, 1);
            case "air/signature/wind_jump" -> stat(StatType.AGILITY, 1);
            case "air/signature/thunderstorm" -> stat(StatType.INTIMIDATION, 3);
            case "air/signature/tornado" -> stat(StatType.AGILITY, 5);
            case "air/signature/lightning_lance" -> stat(StatType.PRECISION, 1);
            case "air/signature/charge" -> stat(StatType.PHYSICAL_ENDURANCE, 1);
            case "air/signature/gust_air" -> stat(StatType.AGILITY, 1);
            case "air/signature/tailwind" -> stat(StatType.PHYSICAL_ENDURANCE, 3);
            case "air/signature/ascension" -> stat(StatType.AGILITY, 3);
            case "air/signature/lightning_storm" -> stat(StatType.INTIMIDATION, 5);
            case "air/signature/chain_lightning" -> stat(StatType.AIR_AFFINITY, 3);
            case "air/signature/leap" -> stat(StatType.AGILITY, 1);
            case "air/signature/wind_blade" -> stat(StatType.PRECISION, 1);

            // ── Earth signature spells ──
            case "earth/signature/poison_breath" -> stat(StatType.WILLPOWER, 1);
            case "earth/signature/acid_rain" -> stat(StatType.WILLPOWER, 3);
            case "earth/signature/earthquake" -> stat(StatType.INTIMIDATION, 5);
            case "earth/signature/stomp" -> stat(StatType.CASTING_SPEED, 1);
            case "earth/signature/poison_arrow" -> stat(StatType.PRECISION, 1);
            case "earth/signature/iron_slash" -> stat(StatType.PRECISION, 1);
            case "earth/signature/root" -> stat(StatType.CASTING_SPEED, 1);
            case "earth/signature/earth_jail" -> stat(StatType.CASTING_SPEED, 3);
            case "earth/signature/earth_lock" -> stat(StatType.CASTING_SPEED, 1);
            case "earth/signature/oakskin" -> stat(StatType.PHYSICAL_ENDURANCE, 1);
            case "earth/signature/fire_storm" -> stat(StatType.INTIMIDATION, 3);
            case "earth/signature/poison_cloud" -> stat(StatType.WILLPOWER, 3);
            case "earth/signature/earth_spikes" -> stat(StatType.EARTH_AFFINITY, 1);

            // ── Holy signature spells ──
            case "holy/signature/heal" -> stat(StatType.WILLPOWER, 1);
            case "holy/signature/divine_smite" -> stat(StatType.PRECISION, 1);
            case "holy/signature/guardian_angel" -> stat(StatType.MAGIC_RESISTANCE, 3);
            case "holy/signature/blessing_of_life" -> stat(StatType.WILLPOWER, 5);
            case "holy/signature/holy_bolt" -> stat(StatType.PRECISION, 1);
            case "holy/signature/angel_wings" -> stat(StatType.PHYSICAL_ENDURANCE, 3);
            case "holy/signature/fortify" -> stat(StatType.PHYSICAL_ENDURANCE, 1);
            case "holy/signature/guiding_bolt" -> stat(StatType.PRECISION, 1);
            case "holy/signature/divine_flash" -> stat(StatType.PRECISION, 1);
            case "holy/signature/regeneration" -> stat(StatType.WILLPOWER, 3);
            case "holy/signature/greater_heal" -> stat(StatType.WILLPOWER, 5);
            case "holy/signature/barrier" -> stat(StatType.MAGIC_RESISTANCE, 1);
            case "holy/signature/magic_wall" -> stat(StatType.MAGIC_RESISTANCE, 1);
            case "holy/signature/healthcare" -> stat(StatType.WILLPOWER, 3);
            case "holy/signature/magic_barrier" -> stat(StatType.MAGIC_RESISTANCE, 3);
            case "holy/signature/multilayer_barrier" -> stat(StatType.MAGIC_RESISTANCE, 5);
            case "holy/signature/anti_magic_area" -> stat(StatType.MAGIC_RESISTANCE, 5);
            case "holy/signature/angel_wing" -> stat(StatType.PHYSICAL_ENDURANCE, 3);

            // ── Blood signature spells ──
            case "blood/signature/blood_slash" -> stat(StatType.PHYSICAL_ENDURANCE, 1);
            case "blood/signature/blood_needles" -> stat(StatType.PRECISION, 1);
            case "blood/signature/raise_dead" -> stat(StatType.TRACKING, 3);
            case "blood/signature/raise_hell" -> stat(StatType.INTIMIDATION, 5);
            case "blood/signature/blood_drain" -> stat(StatType.WILLPOWER, 1);
            case "blood/signature/siphon" -> stat(StatType.WILLPOWER, 1);
            case "blood/signature/bleed" -> stat(StatType.WILLPOWER, 1);
            case "blood/signature/curse" -> stat(StatType.WILLPOWER, 3);
            case "blood/signature/devour" -> stat(StatType.WILLPOWER, 3);
            case "blood/signature/hemo_barrier" -> stat(StatType.MAGIC_RESISTANCE, 3);
            case "blood/signature/sanguine_ward" -> stat(StatType.MAGIC_RESISTANCE, 3);
            case "blood/signature/life_steal" -> stat(StatType.PHYSICAL_ENDURANCE, 3);
            case "blood/signature/exsanguinate" -> stat(StatType.INTIMIDATION, 5);
            case "blood/signature/blood_explosion" -> stat(StatType.INTIMIDATION, 5);
            case "blood/signature/crimson_ritual" -> stat(StatType.WILLPOWER, 5);

            // ── Ender signature spells ──
            case "ender/signature/magic_arrow" -> stat(StatType.KEEN_SENSES, 1);
            case "ender/signature/magic_missile" -> stat(StatType.KEEN_SENSES, 1);
            case "ender/signature/teleport" -> stat(StatType.KEEN_SENSES, 1);
            case "ender/signature/summon_ender_chest" -> stat(StatType.KEEN_SENSES, 1);
            case "ender/signature/starfall" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/signature/recall" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/signature/portal" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/signature/evasion" -> stat(StatType.AGILITY, 3);
            case "ender/signature/shadow_slash" -> stat(StatType.PRECISION, 3);
            case "ender/signature/echoing_strikes" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/signature/dragon_breath" -> stat(StatType.KEEN_SENSES, 5);
            case "ender/signature/black_hole" -> stat(StatType.KEEN_SENSES, 5);
            case "ender/signature/summon_swords" -> stat(StatType.TRACKING, 5);
            case "ender/signature/counterspell" -> stat(StatType.CASTING_SPEED, 5);
            case "ender/signature/gravity_fissure" -> stat(StatType.KEEN_SENSES, 5);
            case "ender/signature/astral_sense" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/signature/displacement" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/signature/doppel_portal" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/signature/implosion" -> stat(StatType.KEEN_SENSES, 5);

            // ── Evocation signature spells ──
            case "evocation/signature/gust" -> stat(StatType.TRACKING, 1);
            case "evocation/signature/shield" -> stat(StatType.MAGIC_RESISTANCE, 1);
            case "evocation/signature/slow" -> stat(StatType.CASTING_SPEED, 1);
            case "evocation/signature/throw" -> stat(StatType.TRACKING, 1);
            case "evocation/signature/firecracker" -> stat(StatType.TRACKING, 1);
            case "evocation/signature/invisibility" -> stat(StatType.TRACKING, 3);
            case "evocation/signature/spectral_hammer" -> stat(StatType.TRACKING, 3);
            case "evocation/signature/fang_strike" -> stat(StatType.TRACKING, 3);
            case "evocation/signature/fang_ward" -> stat(StatType.MAGIC_RESISTANCE, 3);
            case "evocation/signature/arrow_volley" -> stat(StatType.TRACKING, 3);
            case "evocation/signature/summon_horse" -> stat(StatType.TRACKING, 3);
            case "evocation/signature/chain_creeper" -> stat(StatType.TRACKING, 5);
            case "evocation/signature/lob_creeper" -> stat(StatType.TRACKING, 5);
            case "evocation/signature/summon_vex" -> stat(StatType.TRACKING, 5);
            case "evocation/signature/arcane_shackle" -> stat(StatType.CASTING_SPEED, 5);
            case "evocation/signature/wololo" -> stat(StatType.TRACKING, 5);
            case "evocation/signature/fang_swirl" -> stat(StatType.TRACKING, 5);
            case "evocation/signature/lingering_strain" -> stat(StatType.ERUDITION, 3);
            case "evocation/signature/creeper_revenge" -> stat(StatType.TRACKING, 5);
            case "evocation/signature/nucreeper_strike" -> stat(StatType.INTIMIDATION, 5);
            case "evocation/signature/shotgun_creeper" -> stat(StatType.INTIMIDATION, 5);
            case "evocation/signature/elemental_burst" -> stat(StatType.INTIMIDATION, 5);
            case "evocation/signature/magic_shotgun" -> stat(StatType.TRACKING, 5);

            // ── Eldritch signature spells ──
            case "eldritch/signature/eldritch_blast" -> stat(StatType.WILLPOWER, 1);
            case "eldritch/signature/planar_sight" -> stat(StatType.KEEN_SENSES, 1);
            case "eldritch/signature/telekinesis" -> stat(StatType.MAGIC_RESISTANCE, 3);
            case "eldritch/signature/sonic_boom" -> stat(StatType.INTIMIDATION, 3);
            case "eldritch/signature/abyssal_shroud" -> stat(StatType.WILLPOWER, 5);
            case "eldritch/signature/sculk_tentacles" -> stat(StatType.WILLPOWER, 5);
            case "eldritch/signature/pocket_dimension" -> stat(StatType.KEEN_SENSES, 5);
            case "eldritch/signature/blackout" -> stat(StatType.WILLPOWER, 1);
            case "eldritch/signature/psychic_bolt" -> stat(StatType.WILLPOWER, 3);
            case "eldritch/signature/spectral_blink" -> stat(StatType.AGILITY, 3);
            case "eldritch/signature/reversal" -> stat(StatType.KEEN_SENSES, 5);
            case "eldritch/signature/void_tentacles" -> stat(StatType.WILLPOWER, 5);
            case "eldritch/signature/summon_doppel_minion" -> stat(StatType.TRACKING, 5);

            // ── LATEGAME_GATE nodes ──
            // ── Tensura signatures (branch affinity by tier) ──
            default -> {
                if (node.id().contains("/tensura_")) {
                    StatType branchAff = branchAffinity(node.branch());
                    if (branchAff != null) {
                        int threshold = switch (node.tier()) {
                            case T1 -> 1; case T2 -> 3; case T3 -> 5; case T4 -> 7;
                        };
                        yield stat(branchAff, threshold);
                    }
                }
                yield null;
            }
        };
    }

    private static Condition stat(StatType type, int level) {
        return new Condition.StatCondition(type, level);
    }

    private static StatType branchAffinity(MagicBranch branch) {
        return switch (branch) {
            case FIRE -> StatType.FIRE_AFFINITY;
            case WATER -> StatType.WATER_AFFINITY;
            case AIR -> StatType.AIR_AFFINITY;
            case EARTH -> StatType.EARTH_AFFINITY;
            case HOLY, BLOOD, ELDRITCH -> StatType.WILLPOWER;
            case ENDER -> StatType.KEEN_SENSES;
            case EVOCATION -> StatType.TRACKING;
            case COMMON -> null;
        };
    }

    private MagicNodeMigration() {}
}
