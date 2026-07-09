package tong.statmod.dungeon;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * « Dungeon Rush » — le cœur pur de la couche d'addiction du Trial Dungeon : combo de kills
 * (fenêtre glissante, brisé quand on encaisse un coup), jackpots aléatoires (renforcement
 * variable) et étage « sans faute ». Temps injecté en ticks → déterministe, sans Bootstrap.
 */
public class DungeonRushTest {

    private final UUID player = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        DungeonRush.clear(player);
    }

    // ── Combo : fenêtre glissante ────────────────────────────────────────────

    @Test
    void firstKillStartsComboAtOne() {
        assertEquals(1, DungeonRush.onKill(player, 1000L));
    }

    @Test
    void killsInsideWindowChainTheCombo() {
        DungeonRush.onKill(player, 1000L);
        DungeonRush.onKill(player, 1000L + DungeonRush.COMBO_WINDOW_TICKS - 1);
        assertEquals(3, DungeonRush.onKill(player, 1000L + 2L * (DungeonRush.COMBO_WINDOW_TICKS - 1)));
    }

    @Test
    void killAfterWindowRestartsAtOne() {
        DungeonRush.onKill(player, 1000L);
        DungeonRush.onKill(player, 1001L);
        assertEquals(1, DungeonRush.onKill(player, 1001L + DungeonRush.COMBO_WINDOW_TICKS + 1));
    }

    @Test
    void takingAHitBreaksTheCombo() {
        DungeonRush.onKill(player, 1000L);
        DungeonRush.onKill(player, 1001L);
        DungeonRush.onHit(player);
        assertEquals(1, DungeonRush.onKill(player, 1002L), "après un coup reçu le combo repart à 1");
    }

    @Test
    void comboIsCappedAndPerPlayer() {
        UUID other = UUID.randomUUID();
        for (int i = 0; i < DungeonRush.COMBO_CAP + 10; i++) {
            DungeonRush.onKill(player, 1000L + i);
        }
        long withinWindow = 1000L + DungeonRush.COMBO_CAP + 20;
        assertEquals(DungeonRush.COMBO_CAP, DungeonRush.onKill(player, withinWindow));
        assertEquals(1, DungeonRush.onKill(other, withinWindow), "le combo est individuel");
        DungeonRush.clear(other);
    }

    // ── Multiplicateur ───────────────────────────────────────────────────────

    @Test
    void multiplierGrowsWithComboAndIsCapped() {
        assertEquals(1.0, DungeonRush.comboMultiplier(0), 1e-9);
        assertEquals(1.0, DungeonRush.comboMultiplier(1), 1e-9);
        assertTrue(DungeonRush.comboMultiplier(10) > DungeonRush.comboMultiplier(5),
                "plus long combo = plus de points");
        assertEquals(DungeonRush.MULTIPLIER_CAP, DungeonRush.comboMultiplier(10_000), 1e-9,
                "le multiplicateur est plafonné");
    }

    @Test
    void milestonesFireEveryFiveKillsFromFive() {
        assertFalse(DungeonRush.isComboMilestone(4));
        assertTrue(DungeonRush.isComboMilestone(5));
        assertFalse(DungeonRush.isComboMilestone(6));
        assertTrue(DungeonRush.isComboMilestone(10));
        assertTrue(DungeonRush.isComboMilestone(25));
    }

    // ── Jackpot (renforcement variable) ─────────────────────────────────────

    @Test
    void jackpotTriggersBelowChanceThresholdOnly() {
        assertTrue(DungeonRush.isJackpot(0.0));
        assertTrue(DungeonRush.isJackpot(DungeonRush.JACKPOT_CHANCE - 1e-9));
        assertFalse(DungeonRush.isJackpot(DungeonRush.JACKPOT_CHANCE));
        assertFalse(DungeonRush.isJackpot(0.99));
    }

    // ── Sans-faute ───────────────────────────────────────────────────────────

    @Test
    void floorStartsFlawlessAndAHitSpoilsIt() {
        DungeonRush.beginFloor(player);
        assertTrue(DungeonRush.isFlawless(player));
        DungeonRush.onHit(player);
        assertFalse(DungeonRush.isFlawless(player));
    }

    @Test
    void reEnteringAFloorResetsFlawless() {
        DungeonRush.beginFloor(player);
        DungeonRush.onHit(player);
        DungeonRush.beginFloor(player);
        assertTrue(DungeonRush.isFlawless(player), "chaque étage repart sans-faute");
    }

    @Test
    void unknownPlayerIsNotFlawless() {
        assertFalse(DungeonRush.isFlawless(UUID.randomUUID()),
                "sans beginFloor, pas de bonus sans-faute (évite un ×2 gratuit au premier étage tracké)");
    }

    @Test
    void clearForgetsEverything() {
        DungeonRush.beginFloor(player);
        DungeonRush.onKill(player, 1000L);
        DungeonRush.clear(player);
        assertFalse(DungeonRush.isFlawless(player));
        assertEquals(1, DungeonRush.onKill(player, 1001L));
    }
}
