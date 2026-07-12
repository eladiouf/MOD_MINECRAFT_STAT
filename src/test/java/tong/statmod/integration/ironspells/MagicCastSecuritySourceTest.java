package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicCastSecuritySourceTest {
    @Test
    void mixinConfigurationRegistersManaBypass() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/statmod.mixins.json"));
        assertTrue(config.contains("IronSpellManaOverrideMixin"));
    }

    @Test
    void tensuraWrapperNeverLearnsDuringCast() throws IOException {
        String source = Files.readString(Path.of("src/main/java/tong/statmod/integration/ironspells/bridge/TensuraDelegatingSpell.java"));
        assertFalse(source.contains("storage.learnSkill(canonical)"));
        assertTrue(source.contains("storage.getSkill(canonical)"));
    }
}
