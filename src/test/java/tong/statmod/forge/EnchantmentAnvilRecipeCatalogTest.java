package tong.statmod.forge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EnchantmentAnvilRecipeCatalogTest {

    @Test
    void catalog_matchesFlameKatanaUsingCatalystStackCountAndGripSupport() {
        EnchantmentAnvilRecipeCatalog.RecipeSpec recipe =
                EnchantmentAnvilRecipeCatalog.match(
                        "statmod:rough_blade_arcane", 1,
                        "slu:flame_shard", 2,
                        "", 0,
                        "statmod:basic_forge_tongs", 1);

        assertNotNull(recipe);
        assertEquals("simplyswords:runic_katana", recipe.resultId());
        assertEquals(2, recipe.primaryCount());
        assertEquals(2, recipe.enchantments().get("minecraft:fire_aspect"));
    }

    @Test
    void catalog_matchesWitherClaymoreUsingTripleShardCount() {
        EnchantmentAnvilRecipeCatalog.RecipeSpec recipe =
                EnchantmentAnvilRecipeCatalog.match(
                        "statmod:rough_blade_high_magisteel", 1,
                        "slu:wither_shard", 3,
                        "", 0,
                        "statmod:basic_smithing_hammer", 1);

        assertNotNull(recipe);
        assertEquals("simplyswords:runic_claymore", recipe.resultId());
        assertEquals(3, recipe.primaryCount());
        assertEquals(4, recipe.enchantments().get("minecraft:smite"));
    }

    @Test
    void catalog_rejectsLegacyGripSupportForAdvancedRecipes() {
        EnchantmentAnvilRecipeCatalog.RecipeSpec recipe =
                EnchantmentAnvilRecipeCatalog.match(
                        "statmod:rough_blade_arcane", 1,
                        "slu:flame_shard", 2,
                        "", 0,
                        "statmod:runic_grip", 1);

        assertEquals(null, recipe);
    }
}
