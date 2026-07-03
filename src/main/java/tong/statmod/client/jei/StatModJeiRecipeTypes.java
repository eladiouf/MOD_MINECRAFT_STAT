package tong.statmod.client.jei;

import mezz.jei.api.recipe.RecipeType;
import tong.statmod.STATMod;

public final class StatModJeiRecipeTypes {

    public static final RecipeType<InfusionJeiRecipe> INFUSION_FORGE =
            RecipeType.create(STATMod.MODID, "infusion_forge", InfusionJeiRecipe.class);
    public static final RecipeType<AssemblyJeiRecipe> INFUSION_FORGE_ASSEMBLY =
            RecipeType.create(STATMod.MODID, "infusion_forge_assembly", AssemblyJeiRecipe.class);
    public static final RecipeType<EnchantmentAnvilJeiRecipe> ENCHANTMENT_ANVIL =
            RecipeType.create(STATMod.MODID, "enchantment_anvil", EnchantmentAnvilJeiRecipe.class);
    public static final RecipeType<OvergearedForgingJeiRecipe> OVERGEARED_FORGING =
            RecipeType.create(STATMod.MODID, "overgeared_forging", OvergearedForgingJeiRecipe.class);

    private StatModJeiRecipeTypes() {}
}
