package tong.statmod.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ClientBookStudyEffectsContractTest {
    @Test
    void displaysTheValidatedBookWithVanillaActivationMotion() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/ClientBookStudyEffects.java"));
        assertTrue(source.contains("message.valid()"));
        assertTrue(source.contains(
                "gameRenderer.displayItemActivation(message.book())"));
        assertFalse(source.contains("TOTEM_OF_UNDYING"));
        assertFalse(source.contains("broadcastEntityEvent"));
        assertFalse(source.contains("TOTEM_EVENT"));
    }
}
