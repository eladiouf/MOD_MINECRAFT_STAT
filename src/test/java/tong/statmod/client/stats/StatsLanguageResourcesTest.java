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
            "notice.statmod.level_up",
            "statmod.scroll.learned",
            "statmod.scroll.upgraded",
            "statmod.scroll.already_known",
            "statmod.scroll.library_full",
            "statmod.scroll.invalid");

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

    @Test
    void magicalProgressionDescriptionsMatchImplementedGameplay() throws Exception {
        String french = Files.readString(Path.of(
                "src/main/resources/assets/statmod/lang/fr_fr.json"));
        String english = Files.readString(Path.of(
                "src/main/resources/assets/statmod/lang/en_us.json"));
        assertTrue(french.contains(
                "Progresse en étudiant des livres enchantés et en inscrivant de nouveaux sorts."));
        assertTrue(french.contains(
                "Progresse en survivant aux dégâts réellement infligés par des sorts."));
        assertTrue(english.contains(
                "Progresses by studying enchanted books and inscribing new spells."));
        assertTrue(english.contains(
                "Progresses by enduring damage actually dealt by spells."));
    }

    private static void assertContains(String json, String key, String locale) {
        assertTrue(json.contains("\"" + key + "\""), () -> locale + " missing " + key);
    }
}
