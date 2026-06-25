package tong.statmod.integration.tensura;

import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

public final class TensuraXpMultiplier {
    private static final float PRIMARY_STAT_BONUS_PER_LEVEL = 0.05f;
    private static final float SECONDARY_STAT_BONUS_PER_LEVEL = 0.025f;

    private TensuraXpMultiplier() {}

    public static float getEpMultiplier(PlayerStatData data, String actionType) {
        if (data == null) {
            return 1.0f;
        }

        String normalized = normalizeActionType(actionType);
        StatType primary = primaryStatForAction(normalized);
        if (primary == null) {
            return 1.0f;
        }

        float multiplier = 1.0f + statLevel(data, primary) * PRIMARY_STAT_BONUS_PER_LEVEL;
        for (StatType secondary : secondaryStatsForAction(normalized)) {
            multiplier += statLevel(data, secondary) * SECONDARY_STAT_BONUS_PER_LEVEL;
        }
        return multiplier;
    }

    public static double applyEpMultiplier(PlayerStatData data, String actionType, double baseEp) {
        return Math.max(0.0d, baseEp) * getEpMultiplier(data, actionType);
    }

    private static String normalizeActionType(String actionType) {
        return actionType == null ? "" : TensuraSkillIds.canonicalize(actionType.trim().toLowerCase());
    }

    private static StatType primaryStatForAction(String actionType) {
        TensuraSpellProfile profile = TensuraSpellTaxonomy.profile(actionType);
        if (profile != null) {
            return profile.primaryStat();
        }

        return switch (actionType) {
            case "melee_sword" -> StatType.BLADE_TECHNIQUE;
            case "melee_axe" -> StatType.BRUTE_FORCE;
            case "ranged" -> StatType.PRECISION;
            case "magic_fire" -> StatType.FIRE_AFFINITY;
            case "magic_water" -> StatType.WATER_AFFINITY;
            case "magic_earth" -> StatType.EARTH_AFFINITY;
            case "magic_air" -> StatType.AIR_AFFINITY;
            case "magic_light" -> StatType.MAGIC_RESISTANCE;
            case "magic_dark", "magic_arcane" -> StatType.ARCANE_POWER;
            case "magic_spatial" -> StatType.ERUDITION;
            default -> null;
        };
    }

    private static List<StatType> secondaryStatsForAction(String actionType) {
        TensuraSpellProfile profile = TensuraSpellTaxonomy.profile(actionType);
        if (profile != null) {
            return profile.secondaryStats();
        }

        return switch (actionType) {
            case "magic_fire", "magic_dark", "magic_arcane" -> List.of(StatType.ARCANE_POWER);
            case "magic_water" -> List.of(StatType.MANA_POOL, StatType.ERUDITION);
            case "magic_earth" -> List.of(StatType.MAGIC_RESISTANCE);
            case "magic_air" -> List.of(StatType.CASTING_SPEED);
            case "magic_light" -> List.of(StatType.WILLPOWER);
            case "magic_spatial" -> List.of(StatType.CASTING_SPEED, StatType.AIR_AFFINITY);
            default -> List.of();
        };
    }

    private static int statLevel(PlayerStatData data, StatType stat) {
        return stat == null ? 0 : Math.max(0, data.getLevel(stat.index));
    }
}
