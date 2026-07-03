package tong.statmod.client.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentAnvilScreenSourceTest {
    @Test
    void enchantmentAnvilScreenRendersExpectedSupportGhostForMissingOrWrongSupport() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "gui", "EnchantmentAnvilScreen.java"));

        assertTrue(source.contains("renderExpectedSupportGhost(graphics);"));
        assertTrue(source.contains("graphics.renderFakeItem(expectedSupport"));
        assertTrue(source.contains("case MISSING_SUPPORT, WRONG_SUPPORT -> true;"));
    }
}
