package tong.statmod.integration.sdm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SDMShopPricingBalanceTest {
    @Test
    void earlyCategoriesStayCheapAndHighTierTabsStayExpensive() {
        assertPriceBand("Minerais bruts", 100, 800);
        assertPriceBand("Lingots et gemmes", 300, 2500);
        assertPriceBand("Potions et soins", 200, 3000);
        assertPriceBand("Armures magiques", 4000, 14000);
        assertPriceBand("Armes uniques et légendaires", 8000, 30000);
        assertPriceBand("Objets rares contrôlés", 10000, 30000);
    }

    @Test
    void keyProgressionAnchorsIncreaseMonotonically() {
        assertPriceLessThan("minecraft:raw_iron", "minecraft:diamond");
        assertPriceLessThan("minecraft:diamond", "minecraft:netherite_ingot");
        assertPriceLessThan("epicfight:diamond_dagger", "simplyswords:netherite_katana");
        assertPriceLessThan("simplyswords:diamond_katana", "simplyswords:runic_katana");
        assertPriceLessThan("minecraft:diamond_chestplate", "minecraft:netherite_chestplate");
    }

    @Test
    void noCategoryContainsOutOfBandPricesOrForbiddenIds() {
        for (SDMShopCatalog.ShopItem item : SDMShopCatalog.items()) {
            assertFalse(SDMShopCatalog.isForbiddenItemId(item.itemId()), item.itemId());
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
}
