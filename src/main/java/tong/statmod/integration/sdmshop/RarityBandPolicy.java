package tong.statmod.integration.sdmshop;

import java.util.List;

public final class RarityBandPolicy {
    private static final String[] NAMES = {"common", "uncommon", "rare", "epic", "legendary"};

    private RarityBandPolicy() {
    }

    public static String rarity(int minimumRarity, int maximumRarity, int maximumLevel,
            int level, List<Double> rawWeights) {
        if (minimumRarity < 0 || maximumRarity >= NAMES.length
                || minimumRarity > maximumRarity) {
            throw new IllegalArgumentException("invalid rarity range");
        }
        if (maximumLevel < 1 || level < 1 || level > maximumLevel) {
            throw new IllegalArgumentException("invalid spell level");
        }
        if (maximumLevel == 1) {
            return NAMES[minimumRarity];
        }
        if (level >= maximumLevel) {
            return NAMES[maximumRarity];
        }
        if (rawWeights == null || rawWeights.size() <= maximumRarity) {
            throw new IllegalArgumentException("missing rarity weights");
        }

        double total = 0D;
        for (int rarity = minimumRarity; rarity <= maximumRarity; rarity++) {
            total += rawWeights.get(rarity);
        }
        if (total <= 0D) {
            throw new IllegalArgumentException("rarity weights must be positive");
        }

        double ratio = (double) level / maximumLevel;
        double cumulative = 0D;
        for (int rarity = minimumRarity; rarity <= maximumRarity; rarity++) {
            cumulative += rawWeights.get(rarity) / total;
            if (ratio <= cumulative) {
                return NAMES[rarity];
            }
        }
        return NAMES[maximumRarity];
    }
}
