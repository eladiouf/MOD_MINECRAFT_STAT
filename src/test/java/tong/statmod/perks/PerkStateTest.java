package tong.statmod.perks;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkStateTest {

    @Test
    void clearPlayer_removesAllTrackedState() {
        UUID uuid = UUID.randomUUID();
        PerkState.setCooldown(uuid, 13);
        PerkState.recordComboHit(uuid, 1000L, 3000L);
        PerkState.noteTrackedHit(uuid, 42);
        assertTrue(PerkState.tryBeginEffectProcessing(uuid));

        PerkState.clearPlayer(uuid);

        assertFalse(PerkState.isOnCooldown(uuid, 13, 10_000));
        assertEquals(0, PerkState.getComboCount(uuid));
        assertFalse(PerkState.hasTrackedHit(uuid, 42));
        assertFalse(PerkState.isEffectProcessing(uuid));
    }

    @Test
    void recordComboHit_resetsAfterWindow() {
        UUID uuid = UUID.randomUUID();

        assertEquals(1, PerkState.recordComboHit(uuid, 1_000L, 3_000L));
        assertEquals(2, PerkState.recordComboHit(uuid, 2_000L, 3_000L));
        assertEquals(1, PerkState.recordComboHit(uuid, 6_500L, 3_000L));
        assertEquals(1, PerkState.getComboCount(uuid));
    }

    @Test
    void trackHitAndEffectProcessing_roundTrip() {
        UUID uuid = UUID.randomUUID();

        PerkState.noteTrackedHit(uuid, 7);
        assertTrue(PerkState.hasTrackedHit(uuid, 7));
        assertFalse(PerkState.hasTrackedHit(uuid, 8));

        assertTrue(PerkState.tryBeginEffectProcessing(uuid));
        assertFalse(PerkState.tryBeginEffectProcessing(uuid));
        assertTrue(PerkState.isEffectProcessing(uuid));

        PerkState.finishEffectProcessing(uuid);
        assertFalse(PerkState.isEffectProcessing(uuid));
    }
}
