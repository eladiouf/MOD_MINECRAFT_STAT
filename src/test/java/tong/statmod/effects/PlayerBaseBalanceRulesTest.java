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
    void scalesManaCapacityToAnAbsoluteFifteenHundredCapIncludingMilestones() {
        assertEquals(500.0D, PlayerBaseBalanceRules.maxMana(0, 0), 1.0e-9);
        assertEquals(753.75D, PlayerBaseBalanceRules.maxMana(25, 1), 1.0e-9);
        assertEquals(1007.5D, PlayerBaseBalanceRules.maxMana(50, 2), 1.0e-9);
        assertEquals(1261.25D, PlayerBaseBalanceRules.maxMana(75, 3), 1.0e-9);
        assertEquals(1500.0D, PlayerBaseBalanceRules.maxMana(100, 3), 1.0e-9);
        assertEquals(1500.0D, PlayerBaseBalanceRules.maxMana(500, 50), 1.0e-9);
        assertEquals(500.0D, PlayerBaseBalanceRules.maxMana(-1, -1), 1.0e-9);
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
