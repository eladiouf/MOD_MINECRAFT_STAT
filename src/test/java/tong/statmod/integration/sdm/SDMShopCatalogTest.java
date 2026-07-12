package tong.statmod.integration.sdm;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SDMShopCatalogTest {
    private static final int EXPECTED_ITEM_COUNT = 1000;
    private static final Set<String> EXPECTED_TABS = Set.of(
        "Minerais bruts", "Lingots et gemmes", "Matériaux avancés",
        "Forge et amélioration", "Armes légères", "Armes lourdes",
        "Lances et armes d'hast", "Armes à distance",
        "Armes de Tensura", "Armes uniques et légendaires",
        "Armures classiques", "Armures fantastiques",
        "Armures historiques", "Armures magiques",
        "Magie et parchemins", "Runes et composants magiques",
        "Potions et soins", "Composants de monstres", "Nourriture",
        "Fleurs, plantes et bois", "Construction",
        "Mobilité et transport", "Trophées et décoration",
        "Mécanismes et Redstone",
        "Utilitaires", "Objets rares contrôlés"
    );

    @Test
    void exposesDetailedCategoriesAndControlledVolume() {
        assertEquals(EXPECTED_TABS, SDMShopCatalog.tabs().stream()
            .map(SDMShopCatalog.ShopTab::name).collect(Collectors.toSet()));
        assertEquals(EXPECTED_ITEM_COUNT, SDMShopCatalog.items().size());
    }

    @Test
    void containsNoDuplicatesOrForbiddenItems() {
        Set<String> ids = new HashSet<>();
        for (SDMShopCatalog.ShopItem item : SDMShopCatalog.items()) {
            String identity = item.itemId() + "#" + item.potionId();
            assertTrue(ids.add(identity), identity);
            assertFalse(SDMShopCatalog.isForbiddenItemId(item.itemId()), item.itemId());
        }
    }

    @Test
    void includesStrongHealingAndStrengthPotions() {
        Set<String> potionIds = SDMShopCatalog.items().stream()
            .map(SDMShopCatalog.ShopItem::potionId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        assertTrue(potionIds.contains("strong_healing"));
        assertTrue(potionIds.contains("strong_strength"));
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

        assertEquals(EXPECTED_ITEM_COUNT, SDMShopCatalog.items().size());
        for (SDMShopCatalog.ShopItem item : SDMShopCatalog.items()) {
            assertTrue(tabs.contains(item.tab()), item.itemId());
            assertTrue(item.itemId().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), item.itemId());
            assertTrue(item.price() > 0, item.itemId());
            assertTrue(item.count() > 0, item.itemId());
        }
    }

    @Test
    void coversEveryRequiredContentFamily() {
        Set<String> namespaces = SDMShopCatalog.items().stream()
            .map(item -> item.itemId().substring(0, item.itemId().indexOf(':')))
            .collect(Collectors.toSet());
        assertTrue(namespaces.containsAll(Set.of(
            "minecraft", "statmod", "tensura", "irons_spellbooks",
            "iceandfire", "apotheosis", "epicfight", "simplyswords",
            "magistuarmory", "overgeared"
        )), namespaces.toString());
    }

    @Test
    void economyProgressionAnchorsAreConsistent() {
        assertTrue(find("minecraft:coal").price() <= 800,
            "coal=" + find("minecraft:coal").price() + " should be affordable in early game");
        assertTrue(find("minecraft:netherite_ingot").price() >= 800,
            "netherite=" + find("minecraft:netherite_ingot").price() + " should cost more than early mats");
        assertTrue(find("simplyswords:runic_katana").price() >= 12000,
            "runic_katana=" + find("simplyswords:runic_katana").price() + " should be endgame tier");
        assertTrue(find("apotheosis:gems/core/dragonfire_spessartite").price() >= 12000,
            "dragonfire=" + find("apotheosis:gems/core/dragonfire_spessartite").price() + " should be endgame tier");
    }

    private SDMShopCatalog.ShopItem find(String itemId) {
        return SDMShopCatalog.items().stream()
            .filter(item -> item.itemId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing shop item: " + itemId));
    }

    @Test
    void detectsLegacyShopFilesThatNeedRegeneration() {
        assertTrue(!SDMShopCatalog.isCurrentVersion(1));
        assertTrue(SDMShopCatalog.isCurrentVersion(SDMShopCatalog.VERSION));
    }

    @Test
    void detailedCatalogUsesANewMigrationVersion() {
        assertTrue(SDMShopCatalog.VERSION >= 3);
        assertTrue(SDMShopCatalog.isCurrentVersion(SDMShopCatalog.VERSION));
        assertFalse(SDMShopCatalog.isCurrentVersion(2));
    }
}
