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
        assertEquals("mahoutsukai:gandr_spell_scroll", TensuraSpellGate.resolveMahouScrollId("gandr"));
    }

    @Test
    void returnsNullForUnknownMappings() {
        assertNull(TensuraSpellGate.resolveTensuraSpellId("unknown"));
        assertNull(TensuraSpellGate.resolveMahouScrollId("unknown"));
    }

    @Test
    void fallsBackToStatBasedRewardsForActualPerks() {
        assertEquals("tensura:berserk", TensuraSpellGate.resolveForPerk(Perk.BRUTE_CORE));
        assertEquals("tensura:sword_meister", TensuraSpellGate.resolveForPerk(Perk.BLADE_CORE));
        assertEquals("tensura:alchemy", TensuraSpellGate.resolveForPerk(Perk.ALCHEM_CORE));
        assertEquals("tensura:darkness", TensuraSpellGate.resolveForPerk(Perk.ARCANE_CORE));
    }

    @Test
    void exposesPrimaryStatForResolvedSpell() {
        assertEquals(StatType.FIRE_AFFINITY, TensuraSpellGate.primaryStatForResolvedSkill("tensura:fire_bolt"));
        assertEquals(StatType.ERUDITION, TensuraSpellGate.primaryStatForResolvedSkill("tensura:spatial_movement"));
    }
}
