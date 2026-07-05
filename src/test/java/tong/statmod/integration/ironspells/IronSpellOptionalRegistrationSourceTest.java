package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IronSpellOptionalRegistrationSourceTest {
    @Test
    void magicXpBridgeIsRegisteredOnlyWhenIronSpellsIsLoaded() throws IOException {
        String bridge = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/MagicXpBridge.java"));
        String compat = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellsCompat.java"));

        assertFalse(bridge.contains("@EventBusSubscriber"),
                "MagicXpBridge imports Iron's event classes, so it must not auto-subscribe when Iron's is optional");
        assertTrue(compat.contains("NeoForge.EVENT_BUS.register(MagicXpBridge.class)"),
                "MagicXpBridge must be registered inside IronSpellsCompat after the ModList loaded check");
    }
}
