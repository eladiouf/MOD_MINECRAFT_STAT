package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TensuraSpellGateTest {

    @Test
    void resolvesKnownTensuraAndMahouMappings() {
        assertEquals("tensura:fire_bolt", TensuraSpellGate.resolveTensuraSpellId("fire_bolt"));
        assertEquals("tensura:magic_barrier", TensuraSpellGate.resolveTensuraSpellId("light_bind"));
        assertEquals("tensura:teleport", TensuraSpellGate.resolveTensuraSpellId("space_shift"));
        assertEquals("mahoutsukai:gandr_spell_scroll", TensuraSpellGate.resolveMahouScrollId("gandr"));
    }

    @Test
    void returnsNullForUnknownMappings() {
        assertNull(TensuraSpellGate.resolveTensuraSpellId("unknown"));
        assertNull(TensuraSpellGate.resolveMahouScrollId("unknown"));
    }

    @Test
    void resolvesTieredMagicPerksToRealTensuraSkills() {
        assertEquals("tensura:darkness", TensuraSpellGate.resolveForPerk(Perk.ARCANE_CORE));
        assertEquals("tensura:darkness_cannon", TensuraSpellGate.resolveForPerk(Perk.ARCANE_MASTERY));
        assertEquals("tensura:true_darkness", TensuraSpellGate.resolveForPerk(Perk.ARCANE_TRANSCENDENCE));
        assertEquals("tensura:earth_wall", TensuraSpellGate.resolveForPerk(Perk.EARTH_CORE));
        assertEquals("tensura:magma_surge", TensuraSpellGate.resolveForPerk(Perk.EARTH_TRANSCENDENCE));
        assertEquals("tensura:hellfire", TensuraSpellGate.resolveForPerk(Perk.FIRE_TRANSCENDENCE));
        assertEquals("tensura:anti_magic_area", TensuraSpellGate.resolveForPerk(Perk.MAGIC_RESIST_TRANSCENDENCE));
        assertEquals("tensura:teleport", TensuraSpellGate.resolveForPerk(Perk.CASTING_SPEED_MASTERY));
        assertEquals("tensura:dimension_cutter", TensuraSpellGate.resolveForPerk(Perk.ERUDITION_TRANSCENDENCE));
    }

    @Test
    void keepsStatFallbackRewardsForPhysicalAndCraftPerks() {
        assertEquals("tensura:berserk", TensuraSpellGate.resolveForPerk(Perk.BRUTE_CORE));
        assertEquals("tensura:sword_meister", TensuraSpellGate.resolveForPerk(Perk.BLADE_CORE));
        assertEquals("tensura:alchemy", TensuraSpellGate.resolveForPerk(Perk.ALCHEM_CORE));
    }

    @Test
    void exposesPrimaryStatForResolvedSpell() {
        assertEquals(StatType.FIRE_AFFINITY, TensuraSpellGate.primaryStatForResolvedSkill("tensura:fire_bolt"));
        assertEquals(StatType.CASTING_SPEED, TensuraSpellGate.primaryStatForResolvedSkill("tensura:teleport"));
        assertEquals(StatType.ERUDITION, TensuraSpellGate.primaryStatForResolvedSkill("tensura:analyze"));
    }
}
