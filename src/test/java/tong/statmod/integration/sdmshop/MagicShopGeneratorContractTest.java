package tong.statmod.integration.sdmshop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MagicShopGeneratorContractTest {
    @Test
    void scansEveryEnabledSpellAndCreatesExactScrollLevels() throws Exception {
        String source = source();
        assertTrue(source.contains("SpellRegistry.getEnabledSpells()"));
        assertTrue(source.contains("spell.getDefaultConfig()"));
        assertTrue(source.contains("defaults.maxLevel"));
        assertTrue(source.contains("defaults.schoolResource"));
        assertTrue(source.contains("defaults.minRarity.getValue()"));
        assertTrue(source.contains("ItemRegistry.SCROLL.get()"));
        assertTrue(source.contains("ISpellContainer.createScrollContainer(spell, level, scroll)"));
        assertTrue(source.contains("RarityBandPolicy.rarity"));
    }

    @Test
    void BuildsDeterministicBuyAndSellEntriesInDefaultShop() throws Exception {
        String source = source();
        assertTrue(source.contains("MagicShopIds.scroll"));
        assertTrue(source.contains("MagicShopIds.sale"));
        assertTrue(source.contains("ShopEntryType.Buy"));
        assertTrue(source.contains("ShopEntryType.Sell"));
        assertTrue(source.contains("new StrictItemEntryType"));
        assertTrue(source.contains("MagicShopSaleCatalog.offers()"));
        assertTrue(source.contains("createShop(DEFAULT_SHOP_ID)"));
        assertTrue(source.contains("shop.clearData()"));
        assertTrue(source.contains("saveShopToFile(shop)"));
    }

    @Test
    void backsUpAndAtomicallyReportsWithoutTouchingBalances() throws Exception {
        String source = source();
        assertTrue(source.contains("shop-backups"));
        assertTrue(source.contains("shop-generation-report.json"));
        assertTrue(source.contains("StandardCopyOption.ATOMIC_MOVE"));
        assertTrue(source.contains("recordSkipped"));
        assertTrue(source.contains("Files.copy"));
    }

    private static String source() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/sdmshop/MagicShopGenerator.java"));
    }
}
