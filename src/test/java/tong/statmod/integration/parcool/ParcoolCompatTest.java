package tong.statmod.integration.parcool;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParcoolCompatTest {
    @Test
    void actionRewardsAreGranularPerMovementType() {
        assertEquals(Map.of(StatType.AGILITY, 3), ParcoolCompat.xpRewardsForAction("vault"));
        assertEquals(Map.of(StatType.AGILITY, 3, StatType.BRUTE_FORCE, 2), ParcoolCompat.xpRewardsForAction("walljump"));
        assertEquals(Map.of(StatType.AGILITY, 2, StatType.RAPIDITE, 2), ParcoolCompat.xpRewardsForAction("dodge"));
    }

    @Test
    void comboBonusAddsFiftyPercentToEachRewardedStat() {
        Map<StatType, Integer> rewards = Map.of(
                StatType.AGILITY, 3,
                StatType.BRUTE_FORCE, 2
        );

        assertEquals(
                Map.of(StatType.AGILITY, 5, StatType.BRUTE_FORCE, 3),
                ParcoolCompat.withComboBonus(rewards)
        );
    }
}
