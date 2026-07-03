package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishSkillsScreenMixinConfigTest {
    @Test
    void screenMixinUsesInheritedScreenDimensionsInsteadOfShadowingThem() throws IOException {
        try (InputStream stream = PuffishSkillsScreenMixinConfigTest.class
                .getClassLoader()
                .getResourceAsStream("statmod.mixins.json")) {
            assertNotNull(stream);
            String mixinConfig = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(mixinConfig.contains("PuffishSkillsScreenMixin"));
        }

        Path mixinSource = Path.of("src", "main", "java", "tong", "statmod", "mixin", "PuffishSkillsScreenMixin.java");
        assertTrue(Files.exists(mixinSource));

        String source = Files.readString(mixinSource);
        assertTrue(source.contains("extends Screen"));
        assertFalse(source.contains("@Shadow public int width;"));
        assertFalse(source.contains("@Shadow public int height;"));
    }
}
