package tong.statmod.perks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EpicFightPerkGateTest {

    @Test
    void resolvesMappedCombatPerksToEpicFightSkills() {
        assertNotNull(EpicFightPerkGate.resolveSkillId(Perk.BLADE_CORE));
        assertNotNull(EpicFightPerkGate.resolveSkillId(Perk.BRUTE_CORE));
        assertNotNull(EpicFightPerkGate.resolveSkillId(Perk.AGIL_CORE));
    }

    @Test
    void returnsNullForUnmappedPerks() {
        assertNull(EpicFightPerkGate.resolveSkillId(Perk.COOK_CORE));
    }
}
