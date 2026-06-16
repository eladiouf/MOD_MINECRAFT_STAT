package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OvergearedSmithingScreenMixinConfigTest {
    @Test
    void screenMixinIsNotRegisteredUntilItsTargetIsVerified() throws IOException {
        try (InputStream stream = OvergearedSmithingScreenMixinConfigTest.class
                .getClassLoader()
                .getResourceAsStream("statmod.mixins.json")) {
            assertNotNull(stream);
            String mixinConfig = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(mixinConfig.contains("OvergearedSmithingScreenMixin"));
        }
    }
}
