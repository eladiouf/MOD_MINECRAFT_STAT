package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Étages à thème « Solo Leveling » — sélection déterministe (pur, sans Bootstrap).
 */
public class DungeonThemeTest {

    @Test
    void themedFloorsAreThoseEndingInThree() {
        for (int f : new int[]{3, 13, 23, 33, 43, 53, 63, 73, 83, 93}) {
            assertTrue(DungeonTheme.isThemed(f), "Étage " + f + " devrait être à thème");
            assertNotNull(DungeonTheme.forFloor(f));
        }
    }

    @Test
    void bossAndTreasureFloorsAreNeverThemed() {
        for (int f : new int[]{5, 10, 15, 20, 25, 30, 50, 100}) {
            assertNull(DungeonTheme.forFloor(f), "Étage " + f + " (boss/trésor) ne doit pas être à thème");
            assertFalse(DungeonTheme.isThemed(f));
        }
    }

    @Test
    void ordinaryCombatFloorsAreNotThemed() {
        for (int f : new int[]{1, 2, 4, 6, 7, 8, 9, 11, 12, 14}) {
            assertNull(DungeonTheme.forFloor(f), "Étage " + f + " (combat normal) ne doit pas être à thème");
        }
    }

    @Test
    void themeRotatesAcrossTiers() {
        // Les étages ×3 successifs (3, 13, 23…) doivent parcourir des thèmes différents.
        DungeonTheme f3 = DungeonTheme.forFloor(3);
        DungeonTheme f13 = DungeonTheme.forFloor(13);
        DungeonTheme f23 = DungeonTheme.forFloor(23);
        assertNotNull(f3);
        assertTrue(f3 != f13 || f13 != f23, "Les thèmes devraient tourner entre paliers");
    }

    @Test
    void deterministicSameFloorSameTheme() {
        for (int f = 1; f <= 200; f++) {
            assertEquals(DungeonTheme.forFloor(f), DungeonTheme.forFloor(f),
                    "forFloor doit être déterministe à l'étage " + f);
        }
    }

    @Test
    void everyThemeHasAddsAndMiniBoss() {
        for (DungeonTheme t : DungeonTheme.values()) {
            assertFalse(t.addIds().isEmpty(), t + " doit avoir des mobs de base");
            assertFalse(t.miniBossIds().isEmpty(), t + " doit avoir un mini-boss");
            assertNotNull(t.displayName());
            assertNotNull(t.bossName());
        }
    }

    @Test
    void nonPositiveFloorHasNoTheme() {
        assertNull(DungeonTheme.forFloor(0));
        assertNull(DungeonTheme.forFloor(-3));
    }
}
