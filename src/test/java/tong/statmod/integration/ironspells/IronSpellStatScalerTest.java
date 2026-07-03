package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IronSpellStatScalerTest {
    private static final double EPSILON = 1.0e-9d;

    @Test
    void maxManaBonus_scalesOneForOneWithManaPool() {
        assertEquals(0.0d, IronSpellStatScaler.maxManaBonus(0), EPSILON);
        assertEquals(25.0d, IronSpellStatScaler.maxManaBonus(25), EPSILON);
        assertEquals(100.0d, IronSpellStatScaler.maxManaBonus(100), EPSILON);
    }

    @Test
    void manaRegenBonus_combinesReserveAndErudition() {
        assertEquals(0.0d, IronSpellStatScaler.manaRegenBonus(0, 0), EPSILON);
        assertEquals(0.10d, IronSpellStatScaler.manaRegenBonus(20, 0), EPSILON);
        assertEquals(0.30d, IronSpellStatScaler.manaRegenBonus(20, 40), EPSILON);
    }

    @Test
    void spellPowerAndResistBonuses_followMagicCoreLevels() {
        assertEquals(0.0d, IronSpellStatScaler.spellPowerBonus(0), EPSILON);
        assertEquals(0.30d, IronSpellStatScaler.spellPowerBonus(100), EPSILON);
        assertEquals(0.15d, IronSpellStatScaler.spellResistBonus(50), EPSILON);
    }

    @Test
    void castBonuses_splitBetweenCastingSpeedAndErudition() {
        assertEquals(0.0d, IronSpellStatScaler.castTimeReductionBonus(0), EPSILON);
        assertEquals(0.30d, IronSpellStatScaler.castTimeReductionBonus(100), EPSILON);
        assertEquals(0.20d, IronSpellStatScaler.cooldownReductionBonus(50), EPSILON);
    }
}
