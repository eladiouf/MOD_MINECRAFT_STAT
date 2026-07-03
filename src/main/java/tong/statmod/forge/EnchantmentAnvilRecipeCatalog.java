package tong.statmod.forge;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.List;
import java.util.Map;

public final class EnchantmentAnvilRecipeCatalog {

    public enum InputState {
        NONE,
        MISSING_SUPPORT,
        WRONG_SUPPORT,
        SUPPORT_OK,
        INVALID_RECIPE
    }

    public record InputFeedback(InputState state, String expectedSupportId) {
    }

    private static final List<RecipeSpec> RECIPES = List.of(
            new RecipeSpec("statmod:rough_blade_arcane", "slu:flame_shard", 2, "", 0,
                    "statmod:basic_forge_tongs", 1, "simplyswords:runic_katana",
                    Map.of("minecraft:fire_aspect", 2, "minecraft:sharpness", 3)),
            new RecipeSpec("statmod:rough_blade_adamantite", "slu:shadow_shard", 2, "", 0,
                    "statmod:basic_forge_tongs", 1, "simplyswords:runic_rapier",
                    Map.of("minecraft:knockback", 2, "minecraft:sharpness", 4)),
            new RecipeSpec("statmod:rough_blade_high_magisteel", "slu:wither_shard", 3, "", 0,
                    "statmod:basic_smithing_hammer", 1, "simplyswords:runic_claymore",
                    Map.of("minecraft:smite", 4, "minecraft:unbreaking", 2)),
            new RecipeSpec("statmod:rough_axe_head_mithril", "slu:estus_shard", 2, "", 0,
                    "statmod:basic_smithing_hammer", 1, "simplyswords:runic_greathammer",
                    Map.of("minecraft:mending", 1, "minecraft:unbreaking", 3)),
            new RecipeSpec("statmod:rough_axe_head_pure_magisteel", "slu:magma_shard", 2, "", 0,
                    "statmod:basic_smithing_hammer", 1, "simplyswords:runic_greataxe",
                    Map.of("minecraft:fire_aspect", 2, "minecraft:sharpness", 4)),
            new RecipeSpec("statmod:rough_spear_tip_mithril", "irons_spellbooks:permafrost_shard", 3, "", 0,
                    "statmod:basic_forge_tongs", 1, "simplyswords:runic_spear",
                    Map.of("minecraft:sharpness", 3, "minecraft:knockback", 1)),
            new RecipeSpec("statmod:rough_blade_arcane", "simplyswords:runefused_gem", 1, "", 0,
                    "statmod:basic_forge_tongs", 1, "simplyswords:runic_longsword",
                    Map.of("minecraft:sharpness", 5, "minecraft:sweeping_edge", 3)),
            new RecipeSpec("statmod:rough_spear_tip_pure_magisteel", "simplyswords:netherfused_gem", 1, "", 0,
                    "statmod:basic_forge_tongs", 1, "simplyswords:runic_glaive",
                    Map.of("minecraft:fire_aspect", 2, "minecraft:sharpness", 4, "minecraft:unbreaking", 2))
    );

    private EnchantmentAnvilRecipeCatalog() {}

    public static List<RecipeSpec> allRecipes() {
        return RECIPES;
    }

    public static RecipeSpec match(
            String baseId,
            int baseCount,
            String primaryId,
            int primaryCount,
            String secondaryId,
            int secondaryCount,
            String supportId,
            int supportCount) {
        if (baseCount < 1 || supportCount < 1) {
            return null;
        }

        for (RecipeSpec recipe : RECIPES) {
            if (!recipe.baseId.equals(baseId)) {
                continue;
            }
            if (!recipe.primaryId.equals(primaryId) || primaryCount < recipe.primaryCount) {
                continue;
            }
            if (!recipe.secondaryId.equals(secondaryId) || secondaryCount < recipe.secondaryCount) {
                continue;
            }
            if (!recipe.supportId.equals(supportId) || supportCount < recipe.supportCount) {
                continue;
            }
            return recipe;
        }
        return null;
    }

    public static InputFeedback describeInputFeedback(
            String baseId,
            String primaryId,
            String secondaryId,
            String supportId) {
        if (baseId.isEmpty() && primaryId.isEmpty() && secondaryId.isEmpty() && supportId.isEmpty()) {
            return new InputFeedback(InputState.NONE, "");
        }

        String expectedSupportId = expectedSupportIdForIds(baseId, primaryId, secondaryId);
        if (expectedSupportId.isEmpty()) {
            return new InputFeedback(InputState.INVALID_RECIPE, "");
        }
        if (supportId.isEmpty()) {
            return new InputFeedback(InputState.MISSING_SUPPORT, expectedSupportId);
        }
        if (!expectedSupportId.equals(supportId)) {
            return new InputFeedback(InputState.WRONG_SUPPORT, expectedSupportId);
        }
        return new InputFeedback(InputState.SUPPORT_OK, expectedSupportId);
    }

    public static String expectedSupportIdForIds(String baseId, String primaryId, String secondaryId) {
        for (RecipeSpec recipe : RECIPES) {
            if (!recipe.baseId.equals(baseId)) {
                continue;
            }
            if (!recipe.primaryId.equals(primaryId)) {
                continue;
            }
            if (!recipe.secondaryId.equals(secondaryId)) {
                continue;
            }
            return recipe.supportId;
        }
        return "";
    }

    public static ItemStack createResult(RecipeSpec recipe) {
        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        ResourceLocation itemKey = ResourceLocation.parse(recipe.resultId);
        if (!BuiltInRegistries.ITEM.containsKey(itemKey)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemKey));
        HolderLookup.RegistryLookup<Enchantment> enchantmentLookup = CommonHooks.resolveLookup(Registries.ENCHANTMENT);
        if (enchantmentLookup == null) {
            return stack;
        }

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        recipe.enchantments.forEach((enchantmentId, level) -> {
            ResourceLocation enchantmentKey = ResourceLocation.parse(enchantmentId);
            ResourceKey<Enchantment> enchantmentRef = ResourceKey.create(Registries.ENCHANTMENT, enchantmentKey);
            enchantmentLookup.get(enchantmentRef).ifPresent(holder -> mutable.set(holder, level));
        });
        EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
        return stack;
    }

    public record RecipeSpec(
            String baseId,
            String primaryId,
            int primaryCount,
            String secondaryId,
            int secondaryCount,
            String supportId,
            int supportCount,
            String resultId,
            Map<String, Integer> enchantments) {
    }
}
