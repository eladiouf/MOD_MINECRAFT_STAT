package tong.statmod.integration.sdm;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SDMShopCatalogTest {
    private static final Set<String> EXPECTED_TABS = Set.of(
        "Minerais bruts", "Lingots et gemmes", "Matériaux avancés",
        "Forge et amélioration", "Armes légères", "Armes lourdes",
        "Lances et armes d'hast", "Armes à distance", "Armures classiques",
        "Armures fantastiques", "Magie et parchemins", "Runes et composants magiques",
        "Potions et soins", "Composants de monstres", "Nourriture", "Construction",
        "Utilitaires", "Objets rares contrôlés"
    );

    @Test
    void exposesDetailedCategoriesAndControlledVolume() {
        assertEquals(EXPECTED_TABS, SDMShopCatalog.tabs().stream()
            .map(SDMShopCatalog.ShopTab::name).collect(Collectors.toSet()));
        assertTrue(SDMShopCatalog.items().size() >= 350);
        assertTrue(SDMShopCatalog.items().size() <= 450);
    }

    @Test
    void containsNoDuplicatesOrForbiddenItems() {
        Set<String> ids = new HashSet<>();
        for (SDMShopCatalog.ShopItem item : SDMShopCatalog.items()) {
            assertTrue(ids.add(item.itemId()), item.itemId());
            assertFalse(SDMShopCatalog.isForbiddenItemId(item.itemId()), item.itemId());
        }
    }

    @Test
    void containsHighLevelItemsFromTheInstalledMods() {
        Set<String> ids = SDMShopCatalog.items().stream()
            .map(SDMShopCatalog.ShopItem::itemId)
            .collect(Collectors.toSet());

        assertTrue(ids.contains("irons_spellbooks:common_ink"));
        assertTrue(ids.contains("epicfight:uchigatana"));
        assertTrue(ids.contains("apotheosis:mythic_material"));
        assertTrue(ids.contains("iceandfire:dragonbone"));
        assertTrue(ids.contains("mutantmonsters:chemical_x"));
        assertTrue(ids.contains("minecraft:netherite_ingot"));
        assertTrue(ids.contains("minecraft:enchanted_golden_apple"));
    }

    @Test
    void allCatalogEntriesHaveValidValuesAndKnownTabs() {
        Set<String> tabs = SDMShopCatalog.tabs().stream()
            .map(SDMShopCatalog.ShopTab::name)
            .collect(Collectors.toSet());

        assertTrue(SDMShopCatalog.items().size() >= 350);
        for (SDMShopCatalog.ShopItem item : SDMShopCatalog.items()) {
            assertTrue(tabs.contains(item.tab()), item.itemId());
            assertTrue(item.itemId().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), item.itemId());
            assertTrue(item.price() > 0, item.itemId());
            assertTrue(item.count() > 0, item.itemId());
        }
    }

    @Test
    void detectsLegacyShopFilesThatNeedRegeneration() {
        assertTrue(!SDMShopCatalog.isCurrentVersion(1));
        assertTrue(SDMShopCatalog.isCurrentVersion(SDMShopCatalog.VERSION));
    }
}
