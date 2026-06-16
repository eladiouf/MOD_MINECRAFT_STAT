package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatLevelSkillRewardsTest {

    @Test
    void resolvesThresholdRewardsForCoreStats() {
        assertEquals("tensura:berserk", StatLevelSkillRewards.resolveSkillId(StatType.BRUTE_FORCE.index, 10));
        assertEquals("tensura:sword_meister", StatLevelSkillRewards.resolveSkillId(StatType.BLADE_TECHNIQUE.index, 10));
        assertEquals("tensura:mana_manipulation", StatLevelSkillRewards.resolveSkillId(StatType.MANA_POOL.index, 10));
    }

    @Test
    void returnsNullForUnmappedThresholds() {
        assertNull(StatLevelSkillRewards.resolveSkillId(StatType.BRUTE_FORCE.index, 9));
        assertNull(StatLevelSkillRewards.resolveSkillId(99, 10));
    }
}
