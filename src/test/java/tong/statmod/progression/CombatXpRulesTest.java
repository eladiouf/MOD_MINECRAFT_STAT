package tong.statmod.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatXpRulesTest {

    @Test
    void lowHealthChecksUseRemainingHealth() {
        assertFalse(CombatXpRules.isLowHealthAfterHit(10f, 2f, 20f, 0.2f));
        assertTrue(CombatXpRules.isLowHealthAfterHit(5f, 2f, 20f, 0.2f));
        assertTrue(CombatXpRules.isLowHealthAfterHit(4.5f, 4.4f, 20f, 0.2f));
        assertFalse(CombatXpRules.isLowHealthAfterHit(5f, 0.1f, 20f, 0.2f));
    }

    @Test
    void halfHeartSurvivalRequiresPositivePreHitHealth() {
        assertTrue(CombatXpRules.survivesAtHalfHeart(3f, 2.2f));
        assertTrue(CombatXpRules.survivesAtHalfHeart(1.1f, 0.2f));
        assertFalse(CombatXpRules.survivesAtHalfHeart(0f, 0.1f));
    }
}
