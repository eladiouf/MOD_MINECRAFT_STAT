package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.stirdrem.overgeared.ForgingQuality;

public final class OvergearedForgingBonus {
    private OvergearedForgingBonus() {}

    public static int scaledInteractionXp(int baseXp, ForgingQuality quality) {
        return OvergearedStatScaling.scaledXp(baseXp, quality);
    }

    public static float breakSpeedMultiplier(int forgingLevel) {
        return OvergearedStatScaling.digSpeedMultiplier(forgingLevel);
    }

    public static int adjustedDurabilityDamage(int baseDamage, int forgingLevel) {
        return OvergearedStatScaling.adjustedDurabilityDamage(baseDamage, forgingLevel);
    }

    public static int requiredForgingLevel(ResourceLocation resultId) {
        return OvergearedRecipeGate.requiredForgingLevel(resultId);
    }

    public static int requiredForgingLevel(ItemStack resultStack) {
        return OvergearedRecipeGate.requiredForgingLevel(resultStack);
    }

    public static boolean canCraftSpecialOutput(ResourceLocation resultId, int forgingLevel) {
        return OvergearedRecipeGate.canCraft(resultId, forgingLevel);
    }

    public static boolean canCraftSpecialOutput(ItemStack resultStack, int forgingLevel) {
        return OvergearedRecipeGate.canCraft(resultStack, forgingLevel);
    }
}
