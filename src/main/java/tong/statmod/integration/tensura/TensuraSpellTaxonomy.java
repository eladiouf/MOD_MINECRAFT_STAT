package tong.statmod.integration.tensura;

import tong.statmod.stats.StatType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class TensuraSpellTaxonomy {
    private static final Map<String, TensuraSpellProfile> PROFILES = Map.of(
            "tensura:fire_bolt", new TensuraSpellProfile("tensura:fire_bolt", "offense", StatType.FIRE_AFFINITY, List.of(StatType.ARCANE_POWER), true),
            "tensura:healing_rain", new TensuraSpellProfile("tensura:healing_rain", "support", StatType.WATER_AFFINITY, List.of(StatType.MANA_POOL, StatType.ERUDITION), true),
            "tensura:earth_barrier", new TensuraSpellProfile("tensura:earth_barrier", "defense", StatType.EARTH_AFFINITY, List.of(StatType.MAGIC_RESISTANCE), true),
            "tensura:wind_cutter", new TensuraSpellProfile("tensura:wind_cutter", "offense", StatType.AIR_AFFINITY, List.of(StatType.CASTING_SPEED), true),
            "tensura:light_binding", new TensuraSpellProfile("tensura:light_binding", "control", StatType.MAGIC_RESISTANCE, List.of(StatType.WILLPOWER), false),
            "tensura:darkness", new TensuraSpellProfile("tensura:darkness", "pressure", StatType.ARCANE_POWER, List.of(StatType.INTIMIDATION, StatType.WILLPOWER), false),
            "tensura:spatial_movement", new TensuraSpellProfile("tensura:spatial_movement", "mobility", StatType.ERUDITION, List.of(StatType.CASTING_SPEED, StatType.AIR_AFFINITY), false)
    );

    private TensuraSpellTaxonomy() {}

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
        if (stat == null) {
            return null;
        }
        return switch (stat) {
            case ARCANE_POWER -> "tensura:darkness";
            case WATER_AFFINITY, MANA_POOL -> "tensura:healing_rain";
            case EARTH_AFFINITY -> "tensura:earth_barrier";
            case FIRE_AFFINITY -> "tensura:fire_bolt";
            case AIR_AFFINITY -> "tensura:wind_cutter";
            case MAGIC_RESISTANCE -> "tensura:light_binding";
            case CASTING_SPEED, ERUDITION -> "tensura:spatial_movement";
            default -> null;
        };
    }
}
