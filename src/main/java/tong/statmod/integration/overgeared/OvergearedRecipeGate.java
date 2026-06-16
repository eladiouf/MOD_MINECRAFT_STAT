package tong.statmod.integration.overgeared;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class OvergearedRecipeGate {
    private OvergearedRecipeGate() {}

    public static boolean isForgingRecipeResult(ResourceLocation resultId) {
        if (resultId == null) {
            return false;
        }
        return "statmod".equals(resultId.getNamespace())
                && ("perk_tome".equals(resultId.getPath()) || "respec_stone".equals(resultId.getPath()));
    }

    public static boolean isForgingRecipeResult(ItemStack resultStack) {
        if (resultStack == null || resultStack.isEmpty()) {
            return false;
        }
        return isForgingRecipeResult(BuiltInRegistries.ITEM.getKey(resultStack.getItem()));
    }

    public static int requiredForgingLevel(ResourceLocation resultId) {
        if (resultId == null || !"statmod".equals(resultId.getNamespace())) {
            return 0;
        }
        return switch (resultId.getPath()) {
            case "respec_stone" -> 20;
            case "perk_tome" -> 35;
            default -> 0;
        };
    }

    public static int requiredForgingLevel(ItemStack resultStack) {
        if (resultStack == null || resultStack.isEmpty()) {
            return 0;
        }
        return requiredForgingLevel(BuiltInRegistries.ITEM.getKey(resultStack.getItem()));
    }

    public static boolean canCraft(ResourceLocation resultId, int forgingLevel) {
        return Math.max(0, forgingLevel) >= requiredForgingLevel(resultId);
    }

    public static boolean canCraft(ItemStack resultStack, int forgingLevel) {
        return Math.max(0, forgingLevel) >= requiredForgingLevel(resultStack);
    }
}
