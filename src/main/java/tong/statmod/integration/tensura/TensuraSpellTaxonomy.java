package tong.statmod.integration.tensura;

import tong.statmod.stats.StatType;
import tong.statmod.perks.PerkTier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class TensuraSpellTaxonomy {
    private static final Map<String, TensuraSpellProfile> PROFILES = Map.ofEntries(
            entry("tensura:darkness", "pressure", StatType.ARCANE_POWER, List.of(StatType.INTIMIDATION, StatType.WILLPOWER), false),
            entry("tensura:shadow_bind", "control", StatType.ARCANE_POWER, List.of(StatType.WILLPOWER), false),
            entry("tensura:dark_cube", "control", StatType.ARCANE_POWER, List.of(StatType.WILLPOWER), false),
            entry("tensura:curse_bind", "pressure", StatType.ARCANE_POWER, List.of(StatType.WILLPOWER, StatType.INTIMIDATION), false),
            entry("tensura:darkness_cannon", "offense", StatType.ARCANE_POWER, List.of(StatType.INTIMIDATION, StatType.WILLPOWER), false),
            entry("tensura:true_darkness", "offense", StatType.ARCANE_POWER, List.of(StatType.INTIMIDATION, StatType.WILLPOWER), false),

            entry("tensura:healing", "support", StatType.WATER_AFFINITY, List.of(StatType.MANA_POOL), true),
            entry("tensura:recovery", "support", StatType.WATER_AFFINITY, List.of(StatType.MANA_POOL, StatType.ERUDITION), true),
            entry("tensura:antidote", "support", StatType.WATER_AFFINITY, List.of(StatType.ERUDITION), true),
            entry("tensura:water_jail", "control", StatType.WATER_AFFINITY, List.of(StatType.MANA_POOL), true),
            entry("tensura:healing_rain", "support", StatType.WATER_AFFINITY, List.of(StatType.MANA_POOL, StatType.ERUDITION), true),
            entry("tensura:full_recovery", "support", StatType.WATER_AFFINITY, List.of(StatType.MANA_POOL, StatType.ERUDITION), true),

            entry("tensura:earth_wall", "defense", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE), true),
            entry("tensura:earth_lock", "control", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE), true),
            entry("tensura:earth_spikes", "offense", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE), false),
            entry("tensura:earth_jail", "control", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE), false),
            entry("tensura:earth_storm", "pressure", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE), false),
            entry("tensura:magma_surge", "offense", StatType.EARTH_AFFINITY, List.of(StatType.ARCANE_POWER), false),

            entry("tensura:fire_aspectual", "offense", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER), true),
            entry("tensura:fire_ball", "offense", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER), true),
            entry("tensura:fire_lance", "offense", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER, StatType.CASTING_SPEED), true),
            entry("tensura:fire_wall", "defense", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER), true),
            entry("tensura:fire_bolt", "offense", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER), false),
            entry("tensura:fire_storm", "offense", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER, StatType.CASTING_SPEED), true),
            entry("tensura:hellfire", "offense", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER, StatType.WILLPOWER), false),

            entry("tensura:wind_gust", "mobility", StatType.AIR_AFFINITY, List.of(StatType.CASTING_SPEED), true),
            entry("tensura:wind_protection", "defense", StatType.AIR_AFFINITY, List.of(StatType.CASTING_SPEED, StatType.AGILITY), true),
            entry("tensura:wind_blade", "offense", StatType.AIR_AFFINITY, List.of(StatType.CASTING_SPEED), false),
            entry("tensura:tornado_blade", "offense", StatType.AIR_AFFINITY, List.of(StatType.CASTING_SPEED, StatType.AGILITY), true),
            entry("tensura:wind_cutter", "offense", StatType.AIR_AFFINITY, List.of(StatType.CASTING_SPEED), true),
            entry("tensura:lightning_lance", "offense", StatType.AIR_AFFINITY, List.of(StatType.CASTING_SPEED), false),
            entry("tensura:aerial_blade", "offense", StatType.AIR_AFFINITY, List.of(StatType.CASTING_SPEED, StatType.AGILITY), false),

            entry("tensura:magic_wall", "defense", StatType.MAGIC_RESISTANCE, List.of(StatType.WILLPOWER), true),
            entry("tensura:barrier", "defense", StatType.MAGIC_RESISTANCE, List.of(StatType.WILLPOWER), true),
            entry("tensura:reinforced_barrier", "defense", StatType.MAGIC_RESISTANCE, List.of(StatType.WILLPOWER, StatType.MANA_POOL), true),
            entry("tensura:anti_shock_area", "defense", StatType.MAGIC_RESISTANCE, List.of(StatType.WILLPOWER), true),
            entry("tensura:magic_barrier", "defense", StatType.MAGIC_RESISTANCE, List.of(StatType.WILLPOWER), true),
            entry("tensura:anti_magic_area", "defense", StatType.MAGIC_RESISTANCE, List.of(StatType.WILLPOWER), true),

            entry("tensura:lighten", "mobility", StatType.CASTING_SPEED, List.of(StatType.AIR_AFFINITY), true),
            entry("tensura:float", "mobility", StatType.CASTING_SPEED, List.of(StatType.AIR_AFFINITY), true),
            entry("tensura:escape", "mobility", StatType.CASTING_SPEED, List.of(StatType.AIR_AFFINITY), true),
            entry("tensura:warp_portal", "mobility", StatType.CASTING_SPEED, List.of(StatType.ERUDITION), true),
            entry("tensura:teleport", "mobility", StatType.CASTING_SPEED, List.of(StatType.AIR_AFFINITY), false),
            entry("tensura:gate", "mobility", StatType.CASTING_SPEED, List.of(StatType.ERUDITION, StatType.AIR_AFFINITY), false),

            entry("tensura:analyze", "utility", StatType.ERUDITION, List.of(StatType.WILLPOWER), true),
            entry("tensura:search_enemy", "utility", StatType.ERUDITION, List.of(StatType.WILLPOWER), true),
            entry("tensura:clairvoyance", "utility", StatType.ERUDITION, List.of(StatType.WILLPOWER), true),
            entry("tensura:doppelganger", "utility", StatType.ERUDITION, List.of(StatType.WILLPOWER), true),
            entry("tensura:spatial_storage", "utility", StatType.ERUDITION, List.of(StatType.MANA_POOL), true),
            entry("tensura:dimension_cutter", "offense", StatType.ERUDITION, List.of(StatType.CASTING_SPEED, StatType.ARCANE_POWER), true)
    );

    private TensuraSpellTaxonomy() {}

    private static Map.Entry<String, TensuraSpellProfile> entry(
            String skillId,
            String discipline,
            StatType primaryStat,
            List<StatType> secondaryStats,
            boolean elemental
    ) {
        return Map.entry(skillId, new TensuraSpellProfile(skillId, discipline, primaryStat, secondaryStats, elemental));
    }

    public static TensuraSpellProfile profile(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            return null;
        }
        return PROFILES.get(TensuraSkillIds.canonicalize(skillId));
    }

    public static StatType primaryStat(String skillId) {
        TensuraSpellProfile profile = profile(skillId);
        return profile == null ? StatType.ARCANE_POWER : profile.primaryStat();
    }

    public static List<StatType> secondaryStats(String skillId) {
        TensuraSpellProfile profile = profile(skillId);
        return profile == null ? Collections.emptyList() : profile.secondaryStats();
    }

    public static String discipline(String skillId) {
        TensuraSpellProfile profile = profile(skillId);
        return profile == null ? "offense" : profile.discipline();
    }

    public static String representativeSkillId(StatType stat) {
        return skillForTier(stat, PerkTier.MASTERY);
    }

    public static String skillForTier(StatType stat, PerkTier tier) {
        if (stat == null) {
            return null;
        }
        return switch (stat) {
            case ARCANE_POWER -> switch (tier) {
                case CORE -> "tensura:darkness";
                case ACTIVE -> "tensura:shadow_bind";
                case SYNERGY -> "tensura:dark_cube";
                case SITUATIONAL -> "tensura:curse_bind";
                case MASTERY -> "tensura:darkness_cannon";
                case TRANSCENDENCE -> "tensura:true_darkness";
            };
            case WATER_AFFINITY, MANA_POOL -> switch (tier) {
                case CORE -> "tensura:healing";
                case ACTIVE -> "tensura:recovery";
                case SYNERGY -> "tensura:antidote";
                case SITUATIONAL -> "tensura:water_jail";
                case MASTERY -> "tensura:healing_rain";
                case TRANSCENDENCE -> "tensura:full_recovery";
            };
            case EARTH_AFFINITY -> switch (tier) {
                case CORE -> "tensura:earth_wall";
                case ACTIVE -> "tensura:earth_lock";
                case SYNERGY -> "tensura:earth_spikes";
                case SITUATIONAL -> "tensura:earth_jail";
                case MASTERY -> "tensura:earth_storm";
                case TRANSCENDENCE -> "tensura:magma_surge";
            };
            case FIRE_AFFINITY -> switch (tier) {
                case CORE -> "tensura:fire_aspectual";
                case ACTIVE -> "tensura:fire_ball";
                case SYNERGY -> "tensura:fire_lance";
                case SITUATIONAL -> "tensura:fire_wall";
                case MASTERY -> "tensura:fire_storm";
                case TRANSCENDENCE -> "tensura:hellfire";
            };
            case AIR_AFFINITY -> switch (tier) {
                case CORE -> "tensura:wind_gust";
                case ACTIVE -> "tensura:wind_protection";
                case SYNERGY -> "tensura:wind_blade";
                case SITUATIONAL -> "tensura:tornado_blade";
                case MASTERY -> "tensura:lightning_lance";
                case TRANSCENDENCE -> "tensura:aerial_blade";
            };
            case MAGIC_RESISTANCE -> switch (tier) {
                case CORE -> "tensura:magic_wall";
                case ACTIVE -> "tensura:barrier";
                case SYNERGY -> "tensura:reinforced_barrier";
                case SITUATIONAL -> "tensura:anti_shock_area";
                case MASTERY -> "tensura:magic_barrier";
                case TRANSCENDENCE -> "tensura:anti_magic_area";
            };
            case CASTING_SPEED -> switch (tier) {
                case CORE -> "tensura:lighten";
                case ACTIVE -> "tensura:float";
                case SYNERGY -> "tensura:escape";
                case SITUATIONAL -> "tensura:warp_portal";
                case MASTERY -> "tensura:teleport";
                case TRANSCENDENCE -> "tensura:gate";
            };
            case ERUDITION -> switch (tier) {
                case CORE -> "tensura:analyze";
                case ACTIVE -> "tensura:search_enemy";
                case SYNERGY -> "tensura:clairvoyance";
                case SITUATIONAL -> "tensura:doppelganger";
                case MASTERY -> "tensura:spatial_storage";
                case TRANSCENDENCE -> "tensura:dimension_cutter";
            };
            default -> null;
        };
    }
}
