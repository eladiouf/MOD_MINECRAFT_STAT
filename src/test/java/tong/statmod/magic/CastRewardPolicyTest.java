package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CastRewardPolicyTest {
    @Test
    void qualifying_void_cast_yields_practice_only() {
        CastContext ctx = new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE,
                0.25, false, false, 1);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertEquals(1, r.practiceMasteryDelta());
        assertEquals(0, r.progressionMasteryDelta());
        assertEquals(0, r.magicPointsDelta());
        assertEquals(0, r.masteryDelta(), "compatibility mastery must remain bankable only");
    }

    @Test
    void qualifying_impact_cast_preserves_bankable_mastery_and_magic_points() {
        CastContext ctx = new CastContext("irons_spellbooks:fireball", MagicBranch.FIRE,
                1.0, true, false, 2);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertEquals(0, r.practiceMasteryDelta());
        assertEquals(6, r.progressionMasteryDelta());
        assertEquals(1, r.magicPointsDelta());
        assertEquals(r.progressionMasteryDelta(), r.masteryDelta());
        assertEquals(1, r.arcaneDelta());
    }

    @Test
    void casts_below_five_percent_mana_yield_no_reward() {
        CastContext ctx = new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE,
                0.01, false, false, 1);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertEquals(0, r.practiceMasteryDelta());
        assertEquals(0, r.progressionMasteryDelta());
        assertEquals(0, r.magicPointsDelta());
        assertEquals(0, r.masteryDelta());
        assertEquals(0, r.arcaneDelta());
    }

    @Test
    void free_cast_yields_no_reward() {
        CastContext ctx = new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE,
                1.0, true, true, 2);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertEquals(0, r.practiceMasteryDelta());
        assertEquals(0, r.progressionMasteryDelta());
        assertEquals(0, r.magicPointsDelta());
        assertEquals(0, r.masteryDelta());
        assertEquals(0, r.arcaneDelta());
    }

    @Test
    void higher_spell_levels_scale_meaningful_reward() {
        CastContext low = new CastContext("irons_spellbooks:fireball", MagicBranch.FIRE,
                1.0, true, false, 1);
        CastContext high = new CastContext("irons_spellbooks:fireball", MagicBranch.FIRE,
                1.0, true, false, 5);
        assertTrue(CastRewardPolicy.evaluate(high).progressionMasteryDelta()
                > CastRewardPolicy.evaluate(low).progressionMasteryDelta());
    }
}
