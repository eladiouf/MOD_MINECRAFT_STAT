package tong.statmod.integration.mahou;

import tong.statmod.stats.StatType;

public final class MahouPerkMap {
    private MahouPerkMap() {}

    public static StatType[] statsForSpellId(String itemId) {
        MahouSpellProfile profile = MahouSpellTaxonomy.profile(itemId);
        if (profile != null) {
            if (!profile.secondaryStats().isEmpty()) {
                return new StatType[]{profile.primaryStat(), profile.secondaryStats().getFirst()};
            }
            return new StatType[]{profile.primaryStat()};
        }
        return statsForElement(MahouElementMapper.elementForPath(itemId));
    }

    public static StatType[] statsForElement(String element) {
        return switch (element) {
            case "fire" -> new StatType[]{StatType.FIRE_AFFINITY, StatType.ARCANE_POWER};
            case "water" -> new StatType[]{StatType.WATER_AFFINITY, StatType.MANA_POOL};
            case "earth" -> new StatType[]{StatType.EARTH_AFFINITY, StatType.MAGIC_RESISTANCE};
            case "air" -> new StatType[]{StatType.AIR_AFFINITY, StatType.CASTING_SPEED};
            case "light" -> new StatType[]{StatType.MAGIC_RESISTANCE, StatType.WILLPOWER};
            case "dark" -> new StatType[]{StatType.INTIMIDATION, StatType.WILLPOWER};
            case "blood" -> new StatType[]{StatType.BRUTE_FORCE, StatType.INTIMIDATION};
            default -> new StatType[]{StatType.ARCANE_POWER, StatType.ERUDITION};
        };
    }
}
