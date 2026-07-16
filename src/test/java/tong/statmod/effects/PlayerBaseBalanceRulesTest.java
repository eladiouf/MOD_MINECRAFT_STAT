package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerBaseBalanceRulesTest {
    @Test
    void definesExactPhysicalAndManaBases() {
        assertEquals(100.0D, PlayerBaseBalanceRules.BASE_MAX_HEALTH, 1.0e-9);
        assertEquals(5.0D, PlayerBaseBalanceRules.BASE_ATTACK_DAMAGE, 1.0e-9);
        assertEquals(500.0D, PlayerBaseBalanceRules.maxMana(0, 0), 1.0e-9);
        assertEquals(1.0D, PlayerBaseBalanceRules.manaRegenPerSecond(0, 0), 1.0e-9);
    }

    @Test
    void scalesManaCapacityWithLevelAndMilestones() {
        assertEquals(765.0D, PlayerBaseBalanceRules.maxMana(25, 1), 1.0e-9);
        assertEquals(1030.0D, PlayerBaseBalanceRules.maxMana(50, 2), 1.0e-9);
        assertEquals(1545.0D, PlayerBaseBalanceRules.maxMana(100, 3), 1.0e-9);
        assertEquals(1545.0D, PlayerBaseBalanceRules.maxMana(500, 50), 1.0e-9);
    }

    @Test
    void scalesManaRegenFromOneToStrictlyCappedSeventeenPerSecond() {
        assertEquals(5.125D, PlayerBaseBalanceRules.manaRegenPerSecond(25, 1), 1.0e-9);
        assertEquals(9.25D, PlayerBaseBalanceRules.manaRegenPerSecond(50, 2), 1.0e-9);
        assertEquals(13.375D, PlayerBaseBalanceRules.manaRegenPerSecond(75, 3), 1.0e-9);
        assertEquals(17.0D, PlayerBaseBalanceRules.manaRegenPerSecond(100, 3), 1.0e-9);
        assertEquals(17.0D, PlayerBaseBalanceRules.manaRegenPerSecond(500, 50), 1.0e-9);
        assertEquals(1.0D, PlayerBaseBalanceRules.manaRegenPerSecond(-1, -1), 1.0e-9);
    }
}
