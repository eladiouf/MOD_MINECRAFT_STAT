package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastImpactTrackerTest {
    private static final UUID PLAYER = UUID.fromString("3b89f7c0-139c-47f1-8245-c145e0e3d70a");
    private static final String FIREBOLT = "irons_spellbooks:firebolt";
    private static final String ICESPIKE = "irons_spellbooks:ice_spike";

    @Test
    void marked_cast_consumes_as_verified_impact() {
        CastImpactTracker tracker = new CastImpactTracker();

        tracker.begin(PLAYER, FIREBOLT, 100L);
        tracker.markImpact(PLAYER, FIREBOLT);

        assertTrue(tracker.consume(PLAYER, FIREBOLT, 100L));
    }

    @Test
    void untouched_cast_consumes_without_impact() {
        CastImpactTracker tracker = new CastImpactTracker();

        tracker.begin(PLAYER, FIREBOLT, 100L);

        assertFalse(tracker.consume(PLAYER, FIREBOLT, 100L));
    }

    @Test
    void mismatched_spell_does_not_steal_impact_evidence() {
        CastImpactTracker tracker = new CastImpactTracker();

        tracker.begin(PLAYER, FIREBOLT, 100L);
        tracker.begin(PLAYER, ICESPIKE, 100L);
        tracker.markImpact(PLAYER, ICESPIKE);

        assertFalse(tracker.consume(PLAYER, FIREBOLT, 100L));
        assertTrue(tracker.consume(PLAYER, ICESPIKE, 100L));
    }

    @Test
    void clear_removes_pending_impact_evidence() {
        CastImpactTracker tracker = new CastImpactTracker();

        tracker.begin(PLAYER, FIREBOLT, 100L);
        tracker.markImpact(PLAYER, FIREBOLT);
        tracker.clear(PLAYER);

        assertFalse(tracker.consume(PLAYER, FIREBOLT, 100L));
    }

    @Test
    void expired_cast_cannot_supply_impact_evidence() {
        CastImpactTracker tracker = new CastImpactTracker();

        tracker.begin(PLAYER, FIREBOLT, 100L);
        tracker.markImpact(PLAYER, FIREBOLT);

        assertFalse(tracker.consume(PLAYER, FIREBOLT,
                100L + CastImpactTracker.PENDING_TTL_TICKS + 1L));
    }

    @Test
    void pending_casts_are_bounded_per_player_and_spell() {
        CastImpactTracker tracker = new CastImpactTracker();

        for (int i = 0; i <= CastImpactTracker.MAX_PENDING_PER_SPELL; i++) {
            tracker.begin(PLAYER, FIREBOLT, 100L);
        }
        tracker.markImpact(PLAYER, FIREBOLT);

        for (int i = 0; i < CastImpactTracker.MAX_PENDING_PER_SPELL; i++) {
            assertEquals(i == CastImpactTracker.MAX_PENDING_PER_SPELL - 1,
                    tracker.consume(PLAYER, FIREBOLT, 100L));
        }
        assertFalse(tracker.consume(PLAYER, FIREBOLT, 100L));
    }
}
