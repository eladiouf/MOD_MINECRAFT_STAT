package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PerkToSkillMapperTest {
    @Test
    void resolvesTranscendencePerksToTensuraSkills() {
        assertEquals("tensura:giant_strength", PerkToSkillMapper.resolveSkillId(Perk.BRUTE_TRANSCENDENCE));
        assertEquals("tensura:infinite_regeneration", PerkToSkillMapper.resolveSkillId(Perk.RESIST_TRANSCENDENCE));
        assertEquals("tensura:godly_craftsman", PerkToSkillMapper.resolveSkillId(Perk.FORGE_TRANSCENDENCE));
    }

    @Test
    void returnsNullForNonTranscendencePerks() {
        assertNull(PerkToSkillMapper.resolveSkillId(Perk.BRUTE_CORE));
    }
}
