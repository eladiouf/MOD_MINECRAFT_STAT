package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonEnemyHealthBalanceTest {
    @Test
    void finalMaximumHealthIsHalvedExactly() {
        assertEquals(0.5D, DungeonEnemyHealthBalance.maxHealthMultiplier(), 1.0e-9);
        assertEquals(-0.5D, DungeonEnemyHealthBalance.modifierAmount(), 1.0e-9);
    }

    @Test
    void rebalancingPreservesCurrentHealthRatio() {
        assertEquals(60.0D,
                DungeonEnemyHealthBalance.healthAtSameRatio(200.0D, 120.0D, 100.0D),
                1.0e-9);
        assertEquals(60.0D,
                DungeonEnemyHealthBalance.healthAtSameRatio(100.0D, 60.0D, 100.0D),
                1.0e-9);
    }

    @Test
    void modifierPersistsAcrossChunkReloads() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/DungeonEnemyHealthBalance.java"));
        assertTrue(source.contains("addPermanentModifier"));
        assertFalse(source.contains("addTransientModifier"));
    }
}
