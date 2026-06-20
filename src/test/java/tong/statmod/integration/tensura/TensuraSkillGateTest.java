package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraSkillGateTest {
    @Test
    void gatesHighTierMagicPerksToSignatureTensuraSkills() {
        assertEquals(java.util.List.of("tensura:darkness_cannon"), TensuraSkillGate.requiredSkills(Perk.ARCANE_MASTERY.id));
        assertEquals(java.util.List.of("tensura:darkness_cannon", "tensura:true_darkness"), TensuraSkillGate.requiredSkills(Perk.ARCANE_TRANSCENDENCE.id));
        assertEquals(java.util.List.of("tensura:fire_storm", "tensura:hellfire"), TensuraSkillGate.requiredSkills(Perk.FIRE_TRANSCENDENCE.id));
        assertEquals(java.util.List.of("tensura:healing_rain", "tensura:full_recovery"), TensuraSkillGate.requiredSkills(Perk.WATER_TRANSCENDENCE.id));
        assertEquals(java.util.List.of("tensura:earth_jail", "tensura:earth_storm"), TensuraSkillGate.requiredSkills(Perk.EARTH_TRANSCENDENCE.id));
        assertEquals(java.util.List.of("tensura:lightning_lance", "tensura:aerial_blade"), TensuraSkillGate.requiredSkills(Perk.AIR_TRANSCENDENCE.id));
        assertEquals(java.util.List.of("tensura:magic_barrier"), TensuraSkillGate.requiredSkills(Perk.MAGIC_RESIST_MASTERY.id));
        assertEquals(java.util.List.of("tensura:magic_barrier", "tensura:anti_magic_area"), TensuraSkillGate.requiredSkills(Perk.MAGIC_RESIST_TRANSCENDENCE.id));
        assertEquals(java.util.List.of("tensura:teleport"), TensuraSkillGate.requiredSkills(Perk.CASTING_SPEED_MASTERY.id));
        assertEquals(java.util.List.of("tensura:teleport", "tensura:gate"), TensuraSkillGate.requiredSkills(Perk.CASTING_SPEED_TRANSCENDENCE.id));
        assertEquals(java.util.List.of("tensura:multilayer_barrier"), TensuraSkillGate.requiredSkills(Perk.MANA_POOL_MASTERY.id));
        assertEquals(java.util.List.of("tensura:multilayer_barrier", "tensura:maximum_magic_bullet"), TensuraSkillGate.requiredSkills(Perk.MANA_POOL_TRANSCENDENCE.id));
        assertEquals(java.util.List.of("tensura:analyze", "tensura:clairvoyance"), TensuraSkillGate.requiredSkills(Perk.ERUDITION_TRANSCENDENCE.id));
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
        assertTrue(TensuraSkillGate.isGated(Perk.MANA_POOL_TRANSCENDENCE.id));
        assertTrue(TensuraSkillGate.isGated(Perk.ERUDITION_TRANSCENDENCE.id));
    }
}
