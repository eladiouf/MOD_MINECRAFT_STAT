package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvergearedSmithingScreenMixinConfigTest {
    @Test
    void screenMixinIsRegisteredWithInheritedMenuAccess() throws IOException {
        try (InputStream stream = OvergearedSmithingScreenMixinConfigTest.class
                .getClassLoader()
                .getResourceAsStream("statmod.mixins.json")) {
            assertNotNull(stream);
            String mixinConfig = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(mixinConfig.contains("OvergearedSmithingScreenMixin"));
        }

        Path mixinSource = Path.of("src", "main", "java", "tong", "statmod", "mixin", "OvergearedSmithingScreenMixin.java");
        assertTrue(Files.exists(mixinSource));

        String source = Files.readString(mixinSource);
        assertTrue(source.contains("extends AbstractContainerScreen<AbstractSmithingAnvilMenu>"));
        assertTrue(!source.contains("@Shadow @Final protected AbstractSmithingAnvilMenu menu"));
    }
}
