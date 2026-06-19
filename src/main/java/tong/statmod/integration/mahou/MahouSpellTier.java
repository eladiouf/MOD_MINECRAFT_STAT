package tong.statmod.integration.mahou;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tong.statmod.stats.StatType;

import java.util.Locale;

public final class MahouSpellTier {
    private MahouSpellTier() {}

    public static int xpMultiplier(ItemStack stack) {
        return xpMultiplierForPath(path(stack));
    }

    public static int xpMultiplierForPath(String path) {
        if (path == null) {
            return 1;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        if (normalized.contains("ultimate") || normalized.contains("master") || normalized.contains("grand")) {
            return 3;
        }
        if (normalized.contains("advanced") || normalized.contains("greater") || normalized.contains("major")) {
            return 2;
        }
        return 1;
    }

    public static int requiredArcanePower(ItemStack stack) {
        return requiredArcanePowerForItemId(itemId(stack));
    }

    public static int requiredPrimaryStatLevel(String itemId) {
        MahouSpellProfile profile = MahouSpellTaxonomy.profile(itemId);
        if (profile == null) {
            return 0;
        }
        return switch (profile.family()) {
            case "arcane_offense" -> 10;
            case "barrier" -> 15;
            case "mastery" -> 20;
            case "ritual" -> 20;
            case "cataclysm" -> 30;
            default -> 0;
        };
    }

    public static int requiredArcanePowerForItemId(String itemId) {
        MahouSpellProfile profile = MahouSpellTaxonomy.profile(itemId);
        if (profile == null) {
            return requiredArcanePowerForPath(itemId);
        }
        return switch (profile.family()) {
            case "arcane_offense" -> 10;
            case "barrier" -> 20;
            case "mastery", "ritual" -> 25;
            case "cataclysm" -> 40;
            default -> requiredArcanePowerForPath(itemId);
        };
    }

    public static int requiredArcanePowerForPath(String path) {
        if (path == null) {
            return 0;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        if (normalized.contains("ultimate") || normalized.contains("master") || normalized.contains("grand")) {
            return 40;
        }
        if (normalized.contains("advanced") || normalized.contains("greater") || normalized.contains("major")) {
            return 20;
        }
        return 0;
    }

    public static boolean canCast(int arcanePower, String path) {
        return arcanePower >= requiredArcanePowerForPath(path);
    }

    public static boolean canCast(int arcanePower, int primaryStatLevel, String itemId) {
        return arcanePower >= requiredArcanePowerForItemId(itemId)
                && primaryStatLevel >= requiredPrimaryStatLevel(itemId);
    }

    public static boolean canCast(ItemStack stack, int arcanePower) {
        return arcanePower >= requiredArcanePower(stack);
    }

    public static StatType primaryStat(String itemId) {
        return MahouSpellTaxonomy.primaryStat(itemId);
    }

    private static String path(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return "";
        return id.getPath().toLowerCase(Locale.ROOT);
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
    }
}
