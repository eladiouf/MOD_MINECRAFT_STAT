package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class TensuraSpellTaxonomyTest {
    @Test
    void classifiesRealCuratedTensuraSpells() {
        assertEquals(StatType.FIRE_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:fire_bolt"));
        assertEquals(StatType.WATER_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:healing_rain"));
        assertEquals(StatType.EARTH_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:earth_wall"));
        assertEquals(StatType.AIR_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:lightning_lance"));
        assertEquals(StatType.MAGIC_RESISTANCE, TensuraSpellTaxonomy.primaryStat("tensura:anti_magic_area"));
        assertEquals(StatType.CASTING_SPEED, TensuraSpellTaxonomy.primaryStat("tensura:teleport"));
        assertEquals(StatType.ERUDITION, TensuraSpellTaxonomy.primaryStat("tensura:analyze"));
        assertEquals(StatType.ARCANE_POWER, TensuraSpellTaxonomy.primaryStat("tensura:true_darkness"));
        assertEquals(StatType.MANA_POOL, TensuraSpellTaxonomy.primaryStat("tensura:multilayer_barrier"));
        assertEquals(StatType.MANA_POOL, TensuraSpellTaxonomy.primaryStat("tensura:maximum_magic_bullet"));
    }

    @Test
    void exposesSecondaryStatsForRealMagicProfiles() {
        assertIterableEquals(
                java.util.List.of(StatType.ARCANE_POWER),
                TensuraSpellTaxonomy.secondaryStats("tensura:fire_bolt"));
        assertIterableEquals(
                java.util.List.of(StatType.MANA_POOL, StatType.ERUDITION),
                TensuraSpellTaxonomy.secondaryStats("tensura:healing_rain"));
        assertIterableEquals(
                java.util.List.of(StatType.MAGIC_RESISTANCE),
                TensuraSpellTaxonomy.secondaryStats("tensura:earth_jail"));
        assertIterableEquals(
                java.util.List.of(StatType.WILLPOWER),
                TensuraSpellTaxonomy.secondaryStats("tensura:anti_magic_area"));
        assertIterableEquals(
                java.util.List.of(StatType.AIR_AFFINITY),
                TensuraSpellTaxonomy.secondaryStats("tensura:teleport"));
        assertIterableEquals(
                java.util.List.of(StatType.WILLPOWER),
                TensuraSpellTaxonomy.secondaryStats("tensura:analyze"));
        assertIterableEquals(
                java.util.List.of(StatType.MAGIC_RESISTANCE, StatType.WILLPOWER),
                TensuraSpellTaxonomy.secondaryStats("tensura:multilayer_barrier"));
        assertIterableEquals(
                java.util.List.of(StatType.ARCANE_POWER, StatType.ERUDITION),
                TensuraSpellTaxonomy.secondaryStats("tensura:maximum_magic_bullet"));
    }

    @Test
    void givesManaPoolItsOwnRepresentativeTensuraLine() {
        assertEquals("tensura:magic_aura", TensuraSpellTaxonomy.skillForTier(StatType.MANA_POOL, tong.statmod.perks.PerkTier.CORE));
        assertEquals("tensura:multilayer_barrier", TensuraSpellTaxonomy.skillForTier(StatType.MANA_POOL, tong.statmod.perks.PerkTier.MASTERY));
        assertEquals("tensura:maximum_magic_bullet", TensuraSpellTaxonomy.skillForTier(StatType.MANA_POOL, tong.statmod.perks.PerkTier.TRANSCENDENCE));
    }
}
