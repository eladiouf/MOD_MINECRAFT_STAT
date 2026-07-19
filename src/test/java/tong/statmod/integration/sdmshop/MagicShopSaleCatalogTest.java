package tong.statmod.integration.sdmshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MagicShopSaleCatalogTest {
    @Test
    void containsTheApprovedReferenceBundles() {
        Map<String, MagicShopSaleCatalog.SaleOffer> byItem = MagicShopSaleCatalog.offers().stream()
                .collect(Collectors.toMap(MagicShopSaleCatalog.SaleOffer::itemId, offer -> offer));
        assertOffer(byItem, "minecraft:cobblestone", 64, 8);
        assertOffer(byItem, "minecraft:oak_log", 16, 20);
        assertOffer(byItem, "minecraft:wheat", 32, 25);
        assertOffer(byItem, "minecraft:coal", 16, 40);
        assertOffer(byItem, "minecraft:raw_copper", 8, 60);
        assertOffer(byItem, "minecraft:raw_iron", 8, 80);
        assertOffer(byItem, "minecraft:raw_gold", 4, 100);
        assertOffer(byItem, "minecraft:rotten_flesh", 16, 20);
        assertOffer(byItem, "minecraft:bone", 16, 45);
        assertOffer(byItem, "minecraft:blaze_rod", 16, 70);
        assertOffer(byItem, "minecraft:diamond", 1, 180);
    }

    @Test
    void everyOfferIsUniquePositiveAndSellOnly() {
        var offers = MagicShopSaleCatalog.offers();
        assertEquals(offers.size(), new HashSet<>(offers.stream()
                .map(MagicShopSaleCatalog.SaleOffer::id).toList()).size());
        assertTrue(offers.stream().allMatch(offer -> offer.count() > 0 && offer.value() > 0));
        assertTrue(offers.stream().allMatch(MagicShopSaleCatalog.SaleOffer::sellOnly));
    }

    @Test
    void excludesEmeraldsAndStatefulGearCategories() {
        var ids = MagicShopSaleCatalog.offers().stream()
                .map(MagicShopSaleCatalog.SaleOffer::itemId).toList();
        assertFalse(ids.contains("minecraft:emerald"));
        assertFalse(ids.stream().anyMatch(id -> id.contains("sword") || id.contains("helmet")
                || id.contains("scroll") || id.contains("potion") || id.contains("book")));
    }

    private static void assertOffer(Map<String, MagicShopSaleCatalog.SaleOffer> offers,
            String itemId, int count, long value) {
        MagicShopSaleCatalog.SaleOffer offer = offers.get(itemId);
        assertEquals(count, offer.count(), itemId);
        assertEquals(value, offer.value(), itemId);
    }
}
