package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraSkillGateTest {
    @Test
    void gatesHighTierMagicPerksToSignatureTensuraSkills() {
        assertEquals(java.util.List.of("tensura:darkness"), TensuraSkillGate.requiredSkills(Perk.ARCANE_MASTERY.id));
        assertEquals(java.util.List.of("tensura:fire_bolt"), TensuraSkillGate.requiredSkills(Perk.FIRE_MASTERY.id));
        assertEquals(java.util.List.of("tensura:healing_rain"), TensuraSkillGate.requiredSkills(Perk.WATER_MASTERY.id));
        assertEquals(java.util.List.of("tensura:earth_barrier"), TensuraSkillGate.requiredSkills(Perk.EARTH_MASTERY.id));
        assertEquals(java.util.List.of("tensura:wind_cutter"), TensuraSkillGate.requiredSkills(Perk.AIR_MASTERY.id));
        assertEquals(java.util.List.of("tensura:light_binding"), TensuraSkillGate.requiredSkills(Perk.MAGIC_RESIST_MASTERY.id));
        assertEquals(java.util.List.of("tensura:spatial_movement"), TensuraSkillGate.requiredSkills(Perk.CASTING_SPEED_MASTERY.id));
        assertEquals(java.util.List.of("tensura:spatial_movement"), TensuraSkillGate.requiredSkills(Perk.ERUDITION_MASTERY.id));
    }

    @Test
    void marksHighTierMagicPerksAsExternallyGated() {
        assertTrue(TensuraSkillGate.isGated(Perk.ARCANE_MASTERY.id));
        assertTrue(TensuraSkillGate.isGated(Perk.FIRE_TRANSCENDENCE.id));
        assertTrue(TensuraSkillGate.isGated(Perk.WATER_TRANSCENDENCE.id));
        assertTrue(TensuraSkillGate.isGated(Perk.EARTH_TRANSCENDENCE.id));
        assertTrue(TensuraSkillGate.isGated(Perk.AIR_TRANSCENDENCE.id));
        assertTrue(TensuraSkillGate.isGated(Perk.MAGIC_RESIST_TRANSCENDENCE.id));
        assertTrue(TensuraSkillGate.isGated(Perk.CASTING_SPEED_TRANSCENDENCE.id));
        assertTrue(TensuraSkillGate.isGated(Perk.ERUDITION_TRANSCENDENCE.id));
    }
}
