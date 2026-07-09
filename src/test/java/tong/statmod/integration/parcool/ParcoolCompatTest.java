package tong.statmod.integration.parcool;

import org.junit.jupiter.api.Test;
import tong.statmod.stamina.StaminaRules;
import tong.statmod.stamina.StaminaThreshold;
import tong.statmod.stats.StatType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcoolCompatTest {
    @Test
    void actionRewardsAreGranularPerMovementType() {
        assertEquals(Map.of(StatType.AGILITY, 2), ParcoolCompat.xpRewardsForAction("vault"));
        assertEquals(Map.of(StatType.AGILITY, 2, StatType.BRUTE_FORCE, 1), ParcoolCompat.xpRewardsForAction("walljump"));
        assertEquals(Map.of(StatType.AGILITY, 1, StatType.RAPIDITE, 1), ParcoolCompat.xpRewardsForAction("dodge"));
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

    @Test
    void staminaCostsCoverBurstAndSustainedParkourActions() {
        assertEquals(4.0f, ParcoolCompat.staminaCostForAction("vault"), 0.0001f);
        assertEquals(8.0f, ParcoolCompat.staminaCostForAction("roll"), 0.0001f);
        assertEquals(0.5f, ParcoolCompat.staminaTickDrainForAction("wallrun"), 0.0001f);
        assertEquals(0.0f, ParcoolCompat.staminaTickDrainForAction("vault"), 0.0001f);
    }

    @Test
    void sustainedParkourDrainSupportsLongCombatMovementWithoutPassiveRegen() {
        float twoMinutesWallrunCost = ParcoolCompat.staminaTickDrainForAction("wallrun") * 120.0f;
        assertTrue(twoMinutesWallrunCost <= StaminaRules.BASE_MAX_STAMINA * 0.25f);
    }

    @Test
    void criticalOrInsufficientStaminaBlocksParkourActions() {
        assertFalse(ParcoolCompat.canUseAction(StaminaThreshold.CRITICAL, 20.0f, "roll"));
        assertFalse(ParcoolCompat.canUseAction(StaminaThreshold.NORMAL, 3.0f, "vault"));
        assertTrue(ParcoolCompat.canUseAction(StaminaThreshold.NORMAL, 8.0f, "vault"));
    }
}
