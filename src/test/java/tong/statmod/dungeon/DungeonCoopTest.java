package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Audit multi 2026-07-09 — verrouille les formules pures du gameplay co-op :
 * scaling de vague par nombre de joueurs et part d'assist des points de kill.
 */
public class DungeonCoopTest {

    // ── Scaling de vague ─────────────────────────────────────────────────────

    @Test
    void soloWaveKeepsHistoricalSizes() {
        assertEquals(30, DungeonMobSpawner.waveSizeForFloor(1, 1), "base 30 dès l'étage 1");
        assertEquals(48, DungeonMobSpawner.waveSizeForFloor(90, 1), "plafond solo 48");
        assertEquals(48, DungeonMobSpawner.waveSizeForFloor(9999, 1));
    }

    @Test
    void coopAddsHalfWavePerExtraPlayer() {
        int solo = DungeonMobSpawner.waveSizeForFloor(1, 1);
        assertEquals(Math.round(solo * 1.5f), DungeonMobSpawner.waveSizeForFloor(1, 2));
        assertEquals(solo * 2, DungeonMobSpawner.waveSizeForFloor(1, 3));
    }

    @Test
    void coopWaveIsGloballyCapped() {
        assertEquals(72, DungeonMobSpawner.waveSizeForFloor(90, 3), "plafond global 72 (santé du tick)");
        assertEquals(72, DungeonMobSpawner.waveSizeForFloor(90, 10));
    }

    @Test
    void zeroOrNegativePlayersBehavesLikeSolo() {
        assertEquals(DungeonMobSpawner.waveSizeForFloor(5, 1), DungeonMobSpawner.waveSizeForFloor(5, 0));
        assertEquals(DungeonMobSpawner.waveSizeForFloor(5, 1), DungeonMobSpawner.waveSizeForFloor(5, -3));
    }

    // ── Part d'assist ────────────────────────────────────────────────────────

    @Test
    void assistShareIsFortyPercentRoundedDown() {
        assertEquals(4, DungeonPoints.assistShare(10));
        assertEquals(0, DungeonPoints.assistShare(1), "un kill à 1 pt ne donne pas d'assist");
        assertEquals(0, DungeonPoints.assistShare(0));
        assertEquals(160, DungeonPoints.assistShare(400));
    }

    @Test
    void assistShareNeverExceedsKillerReward() {
        for (int pts = 0; pts <= 400; pts += 7) {
            assertTrue(DungeonPoints.assistShare(pts) <= pts,
                    "l'assist ne doit jamais dépasser la récompense du tueur");
        }
    }
}
