package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TensuraSpellTaxonomyTest {
    @Test
    void classifiesCuratedSignatureSpells() {
        assertEquals(StatType.FIRE_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:fire_bolt"));
        assertEquals(StatType.WATER_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:healing_rain"));
        assertEquals(StatType.EARTH_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:earth_barrier"));
        assertEquals(StatType.AIR_AFFINITY, TensuraSpellTaxonomy.primaryStat("tensura:wind_cutter"));
        assertEquals(StatType.ERUDITION, TensuraSpellTaxonomy.primaryStat("tensura:spatial_movement"));
    }
}
