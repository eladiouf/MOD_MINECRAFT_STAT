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

    @Test
    void catalog_describesSupportFeedbackForKnownRecipeFamilies() {
        EnchantmentAnvilRecipeCatalog.InputFeedback missingSupport =
                EnchantmentAnvilRecipeCatalog.describeInputFeedback(
                        "statmod:rough_blade_arcane",
                        "slu:flame_shard",
                        "",
                        "");
        assertEquals(EnchantmentAnvilRecipeCatalog.InputState.MISSING_SUPPORT, missingSupport.state());
        assertEquals("statmod:basic_forge_tongs", missingSupport.expectedSupportId());

        EnchantmentAnvilRecipeCatalog.InputFeedback wrongSupport =
                EnchantmentAnvilRecipeCatalog.describeInputFeedback(
                        "statmod:rough_blade_high_magisteel",
                        "slu:wither_shard",
                        "",
                        "statmod:basic_forge_tongs");
        assertEquals(EnchantmentAnvilRecipeCatalog.InputState.WRONG_SUPPORT, wrongSupport.state());
        assertEquals("statmod:basic_smithing_hammer", wrongSupport.expectedSupportId());

        EnchantmentAnvilRecipeCatalog.InputFeedback supportOk =
                EnchantmentAnvilRecipeCatalog.describeInputFeedback(
                        "statmod:rough_spear_tip_mithril",
                        "irons_spellbooks:permafrost_shard",
                        "",
                        "statmod:basic_forge_tongs");
        assertEquals(EnchantmentAnvilRecipeCatalog.InputState.SUPPORT_OK, supportOk.state());
        assertEquals("statmod:basic_forge_tongs", supportOk.expectedSupportId());
    }

    @Test
    void catalog_marksUnknownFamiliesAsInvalidForFeedback() {
        EnchantmentAnvilRecipeCatalog.InputFeedback invalid =
                EnchantmentAnvilRecipeCatalog.describeInputFeedback(
                        "statmod:rough_staff_core_arcane",
                        "minecraft:diamond",
                        "",
                        "");

        assertEquals(EnchantmentAnvilRecipeCatalog.InputState.INVALID_RECIPE, invalid.state());
        assertEquals("", invalid.expectedSupportId());
    }
}
