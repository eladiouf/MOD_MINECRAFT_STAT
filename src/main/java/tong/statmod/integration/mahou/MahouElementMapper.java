package tong.statmod.integration.mahou;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class MahouElementMapper {
    private MahouElementMapper() {}

    public static String elementFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "arcane";
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return "arcane";
        }

        return elementForItemId(id.toString());
    }

    public static String elementForItemId(String itemId) {
        if (itemId == null) {
            return "arcane";
        }
        MahouSpellProfile profile = MahouSpellTaxonomy.profile(itemId);
        if (profile != null) {
            return switch (profile.primaryStat()) {
                case FIRE_AFFINITY -> "fire";
                case WATER_AFFINITY -> "water";
                case EARTH_AFFINITY -> "earth";
                case AIR_AFFINITY -> "air";
                case MAGIC_RESISTANCE -> "light";
                default -> "arcane";
            };
        }
        return elementForPath(itemId);
    }

    public static String elementForPath(String path) {
        if (path == null) {
            return "arcane";
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        MahouSpellProfile profile = MahouSpellTaxonomy.profile(normalized);
        if (profile != null) {
            return elementForItemId(normalized);
        }
        int separator = normalized.indexOf(':');
        if (separator >= 0 && separator + 1 < normalized.length()) {
            normalized = normalized.substring(separator + 1);
        }
        if (normalized.contains("fire") || normalized.contains("flame") || normalized.contains("burn")) return "fire";
        if (normalized.contains("water") || normalized.contains("aqua") || normalized.contains("ice")) return "water";
        if (normalized.contains("earth") || normalized.contains("stone") || normalized.contains("rock")) return "earth";
        if (normalized.contains("air") || normalized.contains("wind") || normalized.contains("storm")) return "air";
        if (normalized.contains("light") || normalized.contains("holy") || normalized.contains("sun")) return "light";
        if (normalized.contains("dark") || normalized.contains("shadow") || normalized.contains("void")) return "dark";
        if (normalized.contains("blood") || normalized.contains("vamp")) return "blood";
        return "arcane";
    }
}
