package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * « Vraie aventure » — l'objectif de chaque étage découle de son rôle (pur, sans Bootstrap).
 */
public class DungeonObjectiveTest {

    @Test
    void bossFloorsSlayBoss() {
        for (int f : new int[]{10, 20, 30, 100, 200, 1000}) {
            assertEquals(DungeonObjective.SLAY_BOSS, DungeonObjective.forFloor(f),
                    "Étage " + f + " devrait être SLAY_BOSS");
        }
    }

    @Test
    void treasureFloorsLootVault() {
        for (int f : new int[]{5, 15, 25, 35, 55, 95}) {
            assertEquals(DungeonObjective.LOOT_VAULT, DungeonObjective.forFloor(f),
                    "Étage " + f + " devrait être LOOT_VAULT");
        }
    }

    @Test
    void combatFloorsClearWave() {
        for (int f : new int[]{1, 2, 3, 4, 6, 7, 8, 9, 11, 13, 99}) {
            assertEquals(DungeonObjective.CLEAR_WAVE, DungeonObjective.forFloor(f),
                    "Étage " + f + " devrait être CLEAR_WAVE");
        }
    }

    @Test
    void bossTakesPrecedenceOverTreasure() {
        // Les multiples de 10 sont aussi multiples de 5 : le boss doit gagner.
        assertEquals(DungeonObjective.SLAY_BOSS, DungeonObjective.forFloor(10));
        assertEquals(DungeonObjective.SLAY_BOSS, DungeonObjective.forFloor(50));
        assertEquals(DungeonObjective.SLAY_BOSS, DungeonObjective.forFloor(100));
    }

    @Test
    void everyObjectiveHasTranslationKey() {
        for (DungeonObjective o : DungeonObjective.values()) {
            assertNotNull(o.translationKey());
            assertEquals(true, o.translationKey().startsWith("dungeon.objective."));
        }
    }

    @Test
    void nonPositiveFloorDefaultsToClearWave() {
        assertEquals(DungeonObjective.CLEAR_WAVE, DungeonObjective.forFloor(0));
        assertEquals(DungeonObjective.CLEAR_WAVE, DungeonObjective.forFloor(-5));
    }
}
