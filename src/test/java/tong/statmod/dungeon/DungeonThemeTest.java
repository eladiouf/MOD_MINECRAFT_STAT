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
    void themedFloorsAreThoseEndingInThreeFrom13() {
        for (int f : new int[]{13, 23, 33, 43, 53, 63, 73, 83, 93}) {
            assertTrue(DungeonTheme.isThemed(f), "Étage " + f + " devrait être à thème");
            assertNotNull(DungeonTheme.forFloor(f));
        }
    }

    @Test
    void earlyFloorsAreNeverThemed() {
        // Le palier 1 (1-10) reste normal — pas de mini-boss de thème trop tôt.
        for (int f = 1; f <= 12; f++) {
            assertNull(DungeonTheme.forFloor(f), "Étage " + f + " (avant 13) ne doit pas être à thème");
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
        // Les étages à thème successifs (13, 23, 33…) doivent parcourir des thèmes différents.
        DungeonTheme f13 = DungeonTheme.forFloor(13);
        DungeonTheme f23 = DungeonTheme.forFloor(23);
        DungeonTheme f33 = DungeonTheme.forFloor(33);
        assertNotNull(f13);
        assertTrue(f13 != f23 || f23 != f33, "Les thèmes devraient tourner entre paliers");
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
