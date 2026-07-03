package tong.statmod.client.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeStationLangResourcesTest {
    private static final Path RESOURCES = Paths.get("src", "main", "resources", "assets", "statmod", "lang");

    @Test
    void forgeStationContainerTranslationsExistInEnglishAndFrench() throws IOException {
        String en = Files.readString(RESOURCES.resolve("en_us.json"));
        String fr = Files.readString(RESOURCES.resolve("fr_fr.json"));

        assertTrue(en.contains("\"container.statmod.infusion_forge\""));
        assertTrue(en.contains("\"container.statmod.enchantment_anvil\""));
        assertTrue(fr.contains("\"container.statmod.infusion_forge\""));
        assertTrue(fr.contains("\"container.statmod.enchantment_anvil\""));
    }
}
