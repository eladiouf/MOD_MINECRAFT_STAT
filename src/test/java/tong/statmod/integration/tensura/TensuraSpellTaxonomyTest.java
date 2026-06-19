package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class TensuraSpellTaxonomyTest {
    @Test
    void classifiesCuratedSignatureSpells() {
        assertEquals(StatType.FIRE_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:fire_bolt"));
        assertEquals(StatType.WATER_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:healing_rain"));
        assertEquals(StatType.EARTH_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:earth_barrier"));
        assertEquals(StatType.AIR_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:wind_cutter"));
        assertEquals(StatType.ERUDITION, TensuraSpellTaxonomy.primaryStat("tensura:spatial_movement"));
    }

    @Test
    void exposesSecondaryStatsForCuratedMagicProfiles() {
        assertIterableEquals(
                java.util.List.of(StatType.ARCANE_POWER),
                TensuraSpellTaxonomy.secondaryStats("tensura:fire_bolt"));
        assertIterableEquals(
                java.util.List.of(StatType.MANA_POOL, StatType.ERUDITION),
                TensuraSpellTaxonomy.secondaryStats("tensura:healing_rain"));
        assertIterableEquals(
                java.util.List.of(StatType.CASTING_SPEED, StatType.AIR_AFFINITY),
                TensuraSpellTaxonomy.secondaryStats("tensura:spatial_movement"));
    }
}
