package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DungeonMeleeBoostTest {

    @Test
    void dungeonDoesNotRegisterAStandaloneMeleeDamageBoost() throws IOException {
        assertFalse(Files.exists(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonMeleeBoost.java")));

        String configSource = Files.readString(Path.of(
                "src/main/java/tong/statmod/config/Config.java"));
        assertFalse(configSource.contains("DUNGEON_MELEE_DAMAGE_MULTIPLIER"));
        assertFalse(configSource.contains("meleeDamageMultiplier"));
        assertFalse(configSource.contains("getDungeonMeleeDamageMultiplier"));
    }
}
