package tong.statmod.client.stats;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

class StatsLanguageResourcesTest {
    private static final List<String> REQUIRED_UI_KEYS = List.of(
            "key.categories.statmod",
            "key.statmod.open_stats",
            "screen.statmod.stats.title",
            "screen.statmod.level",
            "screen.statmod.xp",
            "screen.statmod.max",
            "screen.statmod.status.active",
            "screen.statmod.status.foundation",
            "notice.statmod.xp",
            "notice.statmod.level_up");

    @Test
    void bothLanguagesContainEveryRequiredStatsKey() throws Exception {
        for (String locale : List.of("fr_fr", "en_us")) {
            String json = Files.readString(Path.of(
                    "src/main/resources/assets/statmod/lang/" + locale + ".json"));
            for (StatType type : StatType.values()) {
                assertContains(json, "stat.statmod." + type.id(), locale);
                assertContains(json, "stat.statmod." + type.id() + ".description", locale);
            }
            for (StatFamily family : StatFamily.values()) {
                assertContains(json, "family.statmod." + family.slug(), locale);
            }
            for (String key : REQUIRED_UI_KEYS) {
                assertContains(json, key, locale);
            }
            assertFalse(json.contains("fire_affinity"));
            assertFalse(json.contains("water_affinity"));
            assertFalse(json.contains("earth_affinity"));
            assertFalse(json.contains("air_affinity"));
            assertFalse(json.contains("elemental_specialization"));
        }
    }

    private static void assertContains(String json, String key, String locale) {
        assertTrue(json.contains("\"" + key + "\""), () -> locale + " missing " + key);
    }
}
