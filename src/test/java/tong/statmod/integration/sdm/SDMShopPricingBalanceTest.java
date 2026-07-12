package tong.statmod.integration.sdm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SDMShopPricingBalanceTest {
    private static final List<String> FORBIDDEN_ID_PARTS = List.of(
        "spawn_egg", "boss_summoner", "creative", "command_block",
        "structure_block", "debug_stick"
    );
    private static final Map<String, int[]> REMAINING_CATEGORY_PRICE_BANDS = Map.ofEntries(
        Map.entry("Fleurs, plantes et bois", new int[] {100, 800}),
        Map.entry("Nourriture", new int[] {100, 800}),
        Map.entry("Construction", new int[] {100, 800}),
        Map.entry("Mécanismes et Redstone", new int[] {100, 800}),
        Map.entry("Forge et amélioration", new int[] {800, 3000}),
        Map.entry("Utilitaires", new int[] {800, 3000}),
        Map.entry("Mobilité et transport", new int[] {800, 3000}),
        Map.entry("Trophées et décoration", new int[] {800, 3000}),
        Map.entry("Matériaux avancés", new int[] {3000, 12000}),
        Map.entry("Runes et composants magiques", new int[] {3000, 12000}),
        Map.entry("Magie et parchemins", new int[] {3000, 12000}),
        Map.entry("Armures classiques", new int[] {3000, 12000}),
        Map.entry("Armes à distance", new int[] {3000, 12000}),
        Map.entry("Lances et armes d'hast", new int[] {3000, 12000}),
        Map.entry("Armes légères", new int[] {12000, 30000}),
        Map.entry("Armes lourdes", new int[] {12000, 30000}),
        Map.entry("Armes de Tensura", new int[] {12000, 30000}),
        Map.entry("Armures fantastiques", new int[] {12000, 30000}),
        Map.entry("Armures historiques", new int[] {12000, 30000}),
        Map.entry("Composants de monstres", new int[] {12000, 30000})
    );

    private static final Map<String, int[]> TIER_ONE_BANDS = Map.of(
        "Minerais bruts", new int[] {100, 800},
        "Potions et soins", new int[] {100, 2000},
        "Lingots et gemmes", new int[] {800, 3000},
        "Armures magiques", new int[] {12000, 30000},
        "Armes uniques et légendaires", new int[] {12000, 30000},
        "Objets rares contrôlés", new int[] {12000, 30000}
    );

    @Test
    void earlyCategoriesStayCheapAndHighTierTabsStayExpensive() {
        for (Map.Entry<String, int[]> band : TIER_ONE_BANDS.entrySet()) {
            assertPriceBand(band.getKey(), band.getValue()[0], band.getValue()[1]);
        }
    }

    @Test
    void keyProgressionAnchorsIncreaseMonotonically() {
        assertPriceLessThan("minecraft:raw_iron", "minecraft:diamond");
        assertPriceLessThan("minecraft:diamond", "minecraft:netherite_ingot");
        assertPriceLessThan("minecraft:diamond_chestplate", "simplyswords:diamond_katana");
        assertPriceLessThan("simplyswords:diamond_katana", "simplyswords:runic_katana");
        assertPriceLessThan("minecraft:diamond_chestplate", "minecraft:netherite_chestplate");
    }

    @Test
    void noCategoryContainsOutOfBandPricesOrForbiddenIds() {
        for (Map.Entry<String, int[]> categoryBand : REMAINING_CATEGORY_PRICE_BANDS.entrySet()) {
            int[] band = categoryBand.getValue();
            assertPriceBand(categoryBand.getKey(), band[0], band[1]);
        }
        for (SDMShopCatalog.ShopItem item : SDMShopCatalog.items()) {
            assertFalse(isForbiddenItemId(item.itemId()), item.itemId());
            assertTrue(item.price() > 0, item.itemId());
            assertTrue(item.count() > 0, item.itemId());
        }
    }

    private void assertPriceBand(String tab, int minInclusive, int maxInclusive) {
        List<SDMShopCatalog.ShopItem> itemsInTab = SDMShopCatalog.items().stream()
            .filter(item -> item.tab().equals(tab))
            .toList();
        assertFalse(itemsInTab.isEmpty(), tab);
        for (SDMShopCatalog.ShopItem item : itemsInTab) {
            assertTrue(item.price() >= minInclusive,
                () -> item.itemId() + " below band for " + tab + ": " + item.price());
            assertTrue(item.price() <= maxInclusive,
                () -> item.itemId() + " above band for " + tab + ": " + item.price());
        }
    }

    private void assertPriceLessThan(String cheaperItemId, String pricierItemId) {
        SDMShopCatalog.ShopItem cheaper = find(cheaperItemId);
        SDMShopCatalog.ShopItem pricier = find(pricierItemId);
        assertTrue(cheaper.price() < pricier.price(),
            () -> cheaperItemId + "=" + cheaper.price() + " should be cheaper than "
                + pricierItemId + "=" + pricier.price());
    }

    private SDMShopCatalog.ShopItem find(String itemId) {
        return SDMShopCatalog.items().stream()
            .filter(item -> item.itemId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing shop item: " + itemId));
    }

    private boolean isForbiddenItemId(String itemId) {
        return FORBIDDEN_ID_PARTS.stream().anyMatch(itemId::contains);
    }
}
