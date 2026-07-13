package tong.statmod.integration.sdm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verrouille la cohérence entre le mapping métier→onglet des PNJ marchands et le
 * catalogue SDM réel. Régression du 2026-07-12 : la refonte du catalogue (26 onglets)
 * avait laissé le bridge taguer les PNJ avec des onglets disparus (« Armes »,
 * « Armures », « Potions ») → le shop retombait silencieusement en mode complet.
 */
class MerchantTabMappingTest {

    /** Les 6 métiers utilisés par les étals du donjon + généralistes. */
    private static final List<String> STALL_PROFESSIONS = List.of(
            "weaponsmith", "toolsmith", "armorer", "cleric", "fletcher", "librarian", "farmer");

    @Test
    void everyMappedTabExistsInCatalog() {
        Set<String> catalog = SDMShopCatalog.tabs().stream()
                .map(SDMShopCatalog.ShopTab::name)
                .collect(Collectors.toSet());

        for (String profession : STALL_PROFESSIONS) {
            String spec = MerchantTabMapping.tabsFor(profession);
            for (String tab : spec.isEmpty() ? new String[0] : spec.split(",")) {
                assertTrue(catalog.contains(tab.trim()),
                        "Métier '" + profession + "' référence l'onglet inexistant '" + tab.trim()
                                + "'. Onglets du catalogue : " + catalog);
            }
        }
    }

    @Test
    void combatCraftProfessionsAreSpecialized() {
        // Les métiers piliers des étals doivent ouvrir un rayon dédié, pas le shop complet.
        for (String profession : List.of("weaponsmith", "toolsmith", "armorer", "cleric")) {
            assertFalse(MerchantTabMapping.tabsFor(profession).isEmpty(),
                    "Métier '" + profession + "' devrait être un marchand spécialisé");
        }
    }

    @Test
    void unknownProfessionIsGeneralist() {
        assertEquals("", MerchantTabMapping.tabsFor("nitwit"));
        assertEquals("", MerchantTabMapping.tabsFor("mason"));
    }

    @Test
    void allTabsExistValidation() {
        assertTrue(MerchantTabMapping.allTabsExist(""));
        assertTrue(MerchantTabMapping.allTabsExist(null));
        assertTrue(MerchantTabMapping.allTabsExist("Potions et soins"));
        assertFalse(MerchantTabMapping.allTabsExist("Armes"));
        assertFalse(MerchantTabMapping.allTabsExist("Potions et soins,OngletFantome"));
    }
}
