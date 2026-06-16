package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpicFightSkillRequirementResolverTest {
    @Test
    void definesExactlyFiveMvpSkills() {
        assertEquals(5, EpicFightSkillRequirementResolver.mvpRequirements().size());
    }

    @Test
    void rushingTempoUsesAgilityAndRapidite() {
        Map<Integer, Integer> requirements = EpicFightSkillRequirementResolver.requirementsFor("rushing_tempo");
        assertEquals(35, requirements.get(StatType.AGILITY.index));
        assertEquals(30, requirements.get(StatType.RAPIDITE.index));
    }

    @Test
    void liechtenauerUsesBladeTechniqueAndPrecision() {
        Map<Integer, Integer> requirements = EpicFightSkillRequirementResolver.requirementsFor("liechtenauer");
        assertEquals(40, requirements.get(StatType.BLADE_TECHNIQUE.index));
        assertEquals(25, requirements.get(StatType.PRECISION.index));
    }
}
