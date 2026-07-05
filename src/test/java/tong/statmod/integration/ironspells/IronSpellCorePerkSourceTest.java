package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IronSpellCorePerkSourceTest {
    @Test
    void attributeBridgeReadsMagicCorePerksFromPlayerData() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellAttributeBridge.java"));

        assertTrue(source.contains("Perk.ARCANE_CORE.id"));
        assertTrue(source.contains("Perk.MAGIC_RESIST_CORE.id"));
        assertTrue(source.contains("Perk.CASTING_SPEED_CORE.id"));
        assertTrue(source.contains("Perk.MANA_POOL_CORE.id"));
        assertTrue(source.contains("Perk.ERUDITION_CORE.id"));
    }

    @Test
    void spellDamageBridgeReadsElementalCorePerksFromPlayerData() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java"));

        assertTrue(source.contains("Perk.FIRE_CORE.id"));
        assertTrue(source.contains("Perk.WATER_CORE.id"));
        assertTrue(source.contains("Perk.EARTH_CORE.id"));
        assertTrue(source.contains("Perk.AIR_CORE.id"));
    }
}
