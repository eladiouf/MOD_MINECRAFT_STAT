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
}
