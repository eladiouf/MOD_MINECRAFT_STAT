package tong.statmod.integration.mahou;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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
        return requiredArcanePowerForPath(path(stack));
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

    public static boolean canCast(ItemStack stack, int arcanePower) {
        return arcanePower >= requiredArcanePower(stack);
    }

    private static String path(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return "";
        return id.getPath().toLowerCase(Locale.ROOT);
    }
}
