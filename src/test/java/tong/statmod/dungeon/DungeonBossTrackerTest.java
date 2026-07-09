package tong.statmod.dungeon;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verrouille le contrat de {@link DungeonBossTracker} : c'est l'état sur lequel repose la
 * conquête des étages boss (combat duo/vague) et sur lequel {@link DungeonBossHandler} et
 * {@link DungeonMobSpawner#clearFloorMobs} s'appuient pour ne pas casser un combat en cours.
 * Aucun bootstrap Minecraft requis (état pur en mémoire).
 */
public class DungeonBossTrackerTest {

    private static final int FLOOR = 10;

    @AfterEach
    void cleanup() {
        DungeonBossTracker.clear(FLOOR);
    }

    @Test
    void freshFloorIsNotTracked() {
        assertFalse(DungeonBossTracker.isTracked(FLOOR));
        assertEquals(0, DungeonBossTracker.remaining(FLOOR));
    }

    @Test
    void registeringABossMarksFloorAsTracked() {
        UUID boss = UUID.randomUUID();
        DungeonBossTracker.register(FLOOR, boss);

        assertTrue(DungeonBossTracker.isTracked(FLOOR));
        assertTrue(DungeonBossTracker.isTrackedBoss(FLOOR, boss));
        assertEquals(1, DungeonBossTracker.remaining(FLOOR));
    }

    @Test
    void duoBoss_killingOneDoesNotClearTheFloor() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        DungeonBossTracker.register(FLOOR, first);
        DungeonBossTracker.register(FLOOR, second);

        boolean allDead = DungeonBossTracker.onBossDeath(FLOOR, first);

        assertFalse(allDead, "un seul boss d'un duo mort ne doit pas conquérir l'étage");
        assertTrue(DungeonBossTracker.isTracked(FLOOR), "le second boss doit rester suivi");
        assertEquals(1, DungeonBossTracker.remaining(FLOOR));
        assertTrue(DungeonBossTracker.isTrackedBoss(FLOOR, second));
    }

    @Test
    void duoBoss_killingBothClearsTheFloor() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        DungeonBossTracker.register(FLOOR, first);
        DungeonBossTracker.register(FLOOR, second);

        assertFalse(DungeonBossTracker.onBossDeath(FLOOR, first));
        boolean allDead = DungeonBossTracker.onBossDeath(FLOOR, second);

        assertTrue(allDead, "les deux boss morts doivent conquérir l'étage");
        assertFalse(DungeonBossTracker.isTracked(FLOOR), "le suivi doit s'effacer une fois l'étage clear");
    }

    @Test
    void killingAnUntrackedIdNeverClearsTheFloor() {
        UUID boss = UUID.randomUUID();
        DungeonBossTracker.register(FLOOR, boss);

        boolean allDead = DungeonBossTracker.onBossDeath(FLOOR, UUID.randomUUID());

        assertFalse(allDead, "un id non suivi (mob quelconque) ne doit jamais compter comme un kill de boss");
        assertTrue(DungeonBossTracker.isTracked(FLOOR));
        assertEquals(1, DungeonBossTracker.remaining(FLOOR));
    }

    @Test
    void clearForgetsTracking() {
        DungeonBossTracker.register(FLOOR, UUID.randomUUID());
        DungeonBossTracker.clear(FLOOR);

        assertFalse(DungeonBossTracker.isTracked(FLOOR));
        assertEquals(0, DungeonBossTracker.remaining(FLOOR));
    }

    @Test
    void tracksAreIndependentPerFloor() {
        int otherFloor = 20;
        UUID bossHere = UUID.randomUUID();
        UUID bossThere = UUID.randomUUID();
        DungeonBossTracker.register(FLOOR, bossHere);
        DungeonBossTracker.register(otherFloor, bossThere);

        assertTrue(DungeonBossTracker.onBossDeath(FLOOR, bossHere));
        assertTrue(DungeonBossTracker.isTracked(otherFloor), "un autre étage ne doit pas être affecté");

        DungeonBossTracker.clear(otherFloor);
    }
}
