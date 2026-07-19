package tong.statmod.client.stats;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import tong.statmod.perks.AutomaticPerkCatalog;

class AutomaticPerkLanguageTest {
    @Test
    void bothLocalesDescribeEveryAutomaticPerkAndListHeading() throws Exception {
        for (String locale : List.of("fr_fr", "en_us")) {
            String json = Files.readString(Path.of(
                    "src/main/resources/assets/statmod/lang/" + locale + ".json"));
            assertTrue(json.contains("\"screen.statmod.stats.active_perks\""));
            assertTrue(json.contains("\"screen.statmod.stats.more_perks\""));
            AutomaticPerkCatalog.definitions().forEach(definition -> {
                String path = definition.id().substring("statmod:".length());
                assertTrue(json.contains("\"perk.statmod." + path + "\""),
                        () -> locale + " missing name for " + definition.id());
                assertTrue(json.contains("\"perk.statmod." + path + ".description\""),
                        () -> locale + " missing description for " + definition.id());
            });
            assertFalse(json.contains("fire_affinity"));
            assertFalse(json.contains("water_affinity"));
            assertFalse(json.contains("earth_affinity"));
            assertFalse(json.contains("air_affinity"));
        }
    }

    @Test
    void bothLocalesStateTheExactClassifiedCombatPerkAmounts() throws Exception {
        String english = Files.readString(Path.of(
                "src/main/resources/assets/statmod/lang/en_us.json"));
        String french = Files.readString(Path.of(
                "src/main/resources/assets/statmod/lang/fr_fr.json"));

        for (String path : List.of("brute_force", "blade_technique", "precision")) {
            assertTrue(english.contains("\"perk.statmod." + path
                    + "_25.description\": \"+5%"));
            assertTrue(french.contains("\"perk.statmod." + path
                    + "_25.description\": \"+5 %"));
        }
        assertTrue(english.contains("\"perk.statmod.physical_resistance_25.description\""
                + ": \"+2 physical-damage reduction percentage points."));
        assertTrue(french.contains("\"perk.statmod.physical_resistance_25.description\""
                + ": \"+2 points de pourcentage de réduction des dégâts physiques."));
        assertTrue(english.contains(
                "\"perk.statmod.tracking_25.description\": \"+2 seconds and +4 blocks"));
        assertTrue(french.contains(
                "\"perk.statmod.tracking_25.description\": \"+2 secondes et +4 blocs"));
        assertTrue(english.contains(
                "\"perk.statmod.keen_senses_25.description\": \"+2 blocks"));
        assertTrue(french.contains(
                "\"perk.statmod.keen_senses_25.description\": \"+2 blocs"));
        assertTrue(english.contains("\"perk.statmod.mana_pool_25.description\""
                + ": \"+3% maximum mana and +0.5 mana/s regeneration.\""));
        assertTrue(french.contains("\"perk.statmod.mana_pool_25.description\""
                + ": \"+3 % de mana maximal et +0,5 mana/s de régénération.\""));
    }
}
