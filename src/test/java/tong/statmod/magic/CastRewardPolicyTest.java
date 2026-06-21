package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CastRewardPolicyTest {
    @Test
    void valid_baseline_cast_gives_small_mastery_no_arcane() {
        CastContext ctx = new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE,
                1.0, false, false, 1);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertTrue(r.masteryDelta() > 0);
        assertTrue(r.masteryDelta() < 5);
        assertEquals(0, r.arcaneDelta());
    }

    @Test
    void meaningful_impact_cast_gives_larger_mastery_and_arcane() {
        CastContext ctx = new CastContext("irons_spellbooks:fireball", MagicBranch.FIRE,
                1.0, true, false, 2);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertTrue(r.masteryDelta() >= 5);
        assertEquals(1, r.arcaneDelta());
    }

    @Test
    void empty_spam_protection_for_low_mana_fraction() {
        CastContext ctx = new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE,
                0.01, false, false, 1);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertEquals(0, r.masteryDelta());
        assertEquals(0, r.arcaneDelta());
    }

    @Test
    void free_cast_yields_no_reward() {
        CastContext ctx = new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE,
                1.0, true, true, 2);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertEquals(0, r.masteryDelta());
        assertEquals(0, r.arcaneDelta());
    }

    @Test
    void higher_spell_levels_scale_meaningful_reward() {
        CastContext low = new CastContext("irons_spellbooks:fireball", MagicBranch.FIRE,
                1.0, true, false, 1);
        CastContext high = new CastContext("irons_spellbooks:fireball", MagicBranch.FIRE,
                1.0, true, false, 5);
        assertTrue(CastRewardPolicy.evaluate(high).masteryDelta()
                > CastRewardPolicy.evaluate(low).masteryDelta());
    }
}
