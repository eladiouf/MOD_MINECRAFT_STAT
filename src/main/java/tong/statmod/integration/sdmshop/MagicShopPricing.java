package tong.statmod.integration.sdmshop;

import java.util.Locale;

public final class MagicShopPricing {
    private static final long[] LEVEL_MULTIPLIERS = {1, 2, 4, 7, 11, 16, 22, 29, 37, 46};

    private MagicShopPricing() {
    }

    public static long scrollPrice(String rarity, int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be positive");
        }
        String normalized = rarity == null ? "common" : rarity.toLowerCase(Locale.ROOT);
        long base = switch (normalized) {
            case "uncommon" -> 1_200L;
            case "rare" -> 3_000L;
            case "epic" -> 7_500L;
            case "legendary" -> 20_000L;
            default -> 500L;
        };
        long multiplier = level <= LEVEL_MULTIPLIERS.length
                ? LEVEL_MULTIPLIERS[level - 1]
                : Math.max(46L, Math.round(0.5D * level * level));
        return Math.multiplyExact(base, multiplier);
    }
}
