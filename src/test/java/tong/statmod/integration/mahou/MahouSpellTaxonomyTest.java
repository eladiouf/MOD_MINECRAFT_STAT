package tong.statmod.integration.mahou;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class MahouSpellTaxonomyTest {
    @Test
    void classifiesCuratedMahouSignatureSpells() {
        assertEquals(StatType.ARCANE_POWER, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_gandr"));
        assertEquals(StatType.EARTH_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_rho_aias"));
        assertEquals(StatType.FIRE_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_fallen_down"));
        assertEquals(StatType.ERUDITION, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_mystic_staff"));
        assertEquals(StatType.ARCANE_POWER, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_boundary_drain_life"));
        assertEquals(StatType.CASTING_SPEED, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_mental_displacement"));
        assertEquals(StatType.EARTH_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_boundary_gravity"));
        assertEquals(StatType.ERUDITION, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_prediction"));
        assertEquals(StatType.FIRE_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_black_flame"));
        assertEquals(StatType.ERUDITION, MahouSpellTaxonomy.primaryStat("mahoutsukai:scroll_treasury_projection"));
    }

    @Test
    void exposesSecondaryStatsForCuratedMahouProfiles() {
        assertIterableEquals(
                java.util.List.of(StatType.CASTING_SPEED),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:scroll_gandr"));
        assertIterableEquals(
                java.util.List.of(StatType.MAGIC_RESISTANCE, StatType.WILLPOWER),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:scroll_rho_aias"));
        assertIterableEquals(
                java.util.List.of(StatType.MANA_POOL, StatType.WILLPOWER),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:scroll_boundary_drain_life"));
        assertIterableEquals(
                java.util.List.of(StatType.AIR_AFFINITY),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:scroll_mental_displacement"));
        assertIterableEquals(
                java.util.List.of(StatType.MAGIC_RESISTANCE),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:scroll_boundary_gravity"));
        assertIterableEquals(
                java.util.List.of(StatType.WILLPOWER),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:scroll_prediction"));
        assertIterableEquals(
                java.util.List.of(StatType.ARCANE_POWER),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:scroll_black_flame"));
        assertIterableEquals(
                java.util.List.of(StatType.MANA_POOL),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:scroll_treasury_projection"));
    }

    @Test
    void exposesFamiliesForNewRealMahouProfiles() {
        assertEquals("displacement", MahouSpellTaxonomy.profile("mahoutsukai:scroll_mental_displacement").family());
        assertEquals("boundary", MahouSpellTaxonomy.profile("mahoutsukai:scroll_boundary_gravity").family());
        assertEquals("eyes", MahouSpellTaxonomy.profile("mahoutsukai:scroll_prediction").family());
        assertEquals("eyes", MahouSpellTaxonomy.profile("mahoutsukai:scroll_black_flame").family());
        assertEquals("projection", MahouSpellTaxonomy.profile("mahoutsukai:scroll_treasury_projection").family());
    }
}
