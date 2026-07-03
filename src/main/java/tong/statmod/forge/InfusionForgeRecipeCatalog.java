package tong.statmod.forge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class InfusionForgeRecipeCatalog {

    private static final List<RecipeSpec> RECIPES = List.of(
            new RecipeSpec("statmod:rough_dagger_blade_arcane", "statmod:rune_essence_arcane", "statmod:runic_grip",
                    "simplyswords:runic_rapier"),
            new RecipeSpec("statmod:rough_blade_arcane", "statmod:rune_essence_arcane", "statmod:runic_grip",
                    "simplyswords:runic_katana"),
            new RecipeSpec("statmod:rough_blade_mithril", "statmod:rune_essence_mithril", "statmod:runic_grip",
                    "simplyswords:runic_claymore"),
            new RecipeSpec("statmod:rough_spear_tip_arcane", "statmod:rune_essence_arcane", "statmod:runic_grip",
                    "simplyswords:runic_spear"),
            new RecipeSpec("statmod:rough_blade_pure_magisteel", "statmod:rune_essence_pyrium", "statmod:runic_grip",
                    "simplyswords:brimstone_cutlass"));

    private static final Map<String, String> RESULT_BY_SIGNATURE = RECIPES.stream()
            .collect(Collectors.toUnmodifiableMap(
                    recipe -> signature(recipe.baseId(), recipe.essenceId(), recipe.gripId()),
                    RecipeSpec::resultId));

    private InfusionForgeRecipeCatalog() {}

    public static List<RecipeSpec> allRecipes() {
        return RECIPES;
    }

    public static String resultIdForIds(String baseId, String essenceId, String gripId) {
        return RESULT_BY_SIGNATURE.getOrDefault(signature(baseId, essenceId, gripId), "");
    }

    public static ItemStack resultForIds(String baseId, String essenceId, String gripId) {
        String resultId = resultIdForIds(baseId, essenceId, gripId);
        if (resultId.isBlank()) {
            return ItemStack.EMPTY;
        }

        ResourceLocation key = ResourceLocation.parse(resultId);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(key));
    }

    private static String signature(String baseId, String essenceId, String gripId) {
        return baseId + "|" + essenceId + "|" + gripId;
    }

    public record RecipeSpec(String baseId, String essenceId, String gripId, String resultId) {
    }
}
