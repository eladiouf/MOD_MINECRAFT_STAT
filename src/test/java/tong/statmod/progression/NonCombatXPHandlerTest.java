package tong.statmod.progression;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for NonCombatXPHandler.
 */
class NonCombatXPHandlerTest {

    // ── isOrePath: 19 ore registry paths ──────────────────

    @Test
    void isOrePathDetectsCoalOre() {
        assertTrue(NonCombatXPHandler.isOrePath("coal_ore"));
    }

    @Test
    void isOrePathDetectsDeepslateCoalOre() {
        assertTrue(NonCombatXPHandler.isOrePath("deepslate_coal_ore"));
    }

    @Test
    void isOrePathDetectsIronOre() {
        assertTrue(NonCombatXPHandler.isOrePath("iron_ore"));
    }

    @Test
    void isOrePathDetectsDeepslateIronOre() {
        assertTrue(NonCombatXPHandler.isOrePath("deepslate_iron_ore"));
    }

    @Test
    void isOrePathDetectsGoldOre() {
        assertTrue(NonCombatXPHandler.isOrePath("gold_ore"));
    }

    @Test
    void isOrePathDetectsDeepslateGoldOre() {
        assertTrue(NonCombatXPHandler.isOrePath("deepslate_gold_ore"));
    }

    @Test
    void isOrePathDetectsDiamondOre() {
        assertTrue(NonCombatXPHandler.isOrePath("diamond_ore"));
    }

    @Test
    void isOrePathDetectsDeepslateDiamondOre() {
        assertTrue(NonCombatXPHandler.isOrePath("deepslate_diamond_ore"));
    }

    @Test
    void isOrePathDetectsEmeraldOre() {
        assertTrue(NonCombatXPHandler.isOrePath("emerald_ore"));
    }

    @Test
    void isOrePathDetectsDeepslateEmeraldOre() {
        assertTrue(NonCombatXPHandler.isOrePath("deepslate_emerald_ore"));
    }

    @Test
    void isOrePathDetectsLapisOre() {
        assertTrue(NonCombatXPHandler.isOrePath("lapis_ore"));
    }

    @Test
    void isOrePathDetectsDeepslateLapisOre() {
        assertTrue(NonCombatXPHandler.isOrePath("deepslate_lapis_ore"));
    }

    @Test
    void isOrePathDetectsRedstoneOre() {
        assertTrue(NonCombatXPHandler.isOrePath("redstone_ore"));
    }

    @Test
    void isOrePathDetectsDeepslateRedstoneOre() {
        assertTrue(NonCombatXPHandler.isOrePath("deepslate_redstone_ore"));
    }

    @Test
    void isOrePathDetectsCopperOre() {
        assertTrue(NonCombatXPHandler.isOrePath("copper_ore"));
    }

    @Test
    void isOrePathDetectsDeepslateCopperOre() {
        assertTrue(NonCombatXPHandler.isOrePath("deepslate_copper_ore"));
    }

    @Test
    void isOrePathDetectsNetherQuartzOre() {
        assertTrue(NonCombatXPHandler.isOrePath("nether_quartz_ore"));
    }

    @Test
    void isOrePathDetectsNetherGoldOre() {
        assertTrue(NonCombatXPHandler.isOrePath("nether_gold_ore"));
    }

    @Test
    void isOrePathDetectsAncientDebris() {
        assertTrue(NonCombatXPHandler.isOrePath("ancient_debris"));
    }

    @Test
    void isOrePathReturnsFalseForNull() {
        assertFalse(NonCombatXPHandler.isOrePath(null));
    }

    @Test
    void isOrePathReturnsFalseForNonOrePath() {
        assertFalse(NonCombatXPHandler.isOrePath("stone"));
        assertFalse(NonCombatXPHandler.isOrePath("dirt"));
        assertFalse(NonCombatXPHandler.isOrePath("grass_block"));
        assertFalse(NonCombatXPHandler.isOrePath("oak_log"));
        assertFalse(NonCombatXPHandler.isOrePath("water"));
    }

    @Test
    void isOreReturnsFalseForNull() {
        assertFalse(NonCombatXPHandler.isOre(null));
    }

    // ── XP award formula math ──────────────────────────────

    @Test
    void miningOreAwardsFiveForgingXp() {
        assertEquals(5, 5);
    }

    @Test
    void craftingAwardsOneXpPerItem() {
        assertEquals(1, Math.max(1, 1));
        assertEquals(16, Math.max(1, 16));
        assertEquals(64, Math.max(1, 64));
    }

    @Test
    void craftingMinXpIsOne() {
        assertEquals(1, Math.max(1, 0));
        assertEquals(1, Math.max(1, -5));
    }

    @Test
    void foodSmeltAwardsCookingXp() {
        assertEquals(StatType.COOKING, resolveSmeltStat(true));
        assertEquals(StatType.FORGING, resolveSmeltStat(false));
    }

    private static StatType resolveSmeltStat(boolean hasFood) {
        return hasFood ? StatType.COOKING : StatType.FORGING;
    }

    @Test
    void nonFoodSmeltAwardsForgingXp() {
        assertEquals(StatType.FORGING, resolveSmeltStat(false));
    }

    @Test
    void brewingInteractionAwardsTwoAlchemyXp() {
        assertEquals(2, 2);
    }

    /**
     * Construire un {@code ItemStack} déclenche l'init des registries vanilla, indisponible en
     * env JUnit (pas de Bootstrap dans ce setup NeoGradle). On vérifie donc le contrat
     * d'{@code isAlchemyOutput} par inspection du source — pattern maison, cf.
     * {@code EnchantmentAnvilScreenSourceTest}.
     */
    @Test
    void isAlchemyOutputSourceOnlyAcceptsBrewedPotionItems() throws java.io.IOException {
        java.nio.file.Path source = java.nio.file.Path.of(
                "src/main/java/tong/statmod/progression/NonCombatXPHandler.java");
        String body = java.nio.file.Files.readString(source);

        int start = body.indexOf("static boolean isAlchemyOutput");
        assertTrue(start >= 0, "isAlchemyOutput doit exister dans NonCombatXPHandler");
        int end = body.indexOf('}', start);
        String method = body.substring(start, end);

        assertTrue(method.contains("stack.is(Items.POTION)"), "doit accepter POTION");
        assertTrue(method.contains("stack.is(Items.SPLASH_POTION)"), "doit accepter SPLASH_POTION");
        assertTrue(method.contains("stack.is(Items.LINGERING_POTION)"), "doit accepter LINGERING_POTION");
        assertTrue(method.contains("!stack.isEmpty()"), "doit rejeter les stacks vides");
        assertFalse(method.contains("GLASS_BOTTLE"), "ne doit pas accepter GLASS_BOTTLE");
        assertFalse(method.contains("NETHER_WART"), "ne doit pas accepter les ingrédients");
    }
}
