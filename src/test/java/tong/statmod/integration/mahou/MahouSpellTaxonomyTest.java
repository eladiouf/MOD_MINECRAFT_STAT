package tong.statmod.integration.mahou;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class MahouSpellTaxonomyTest {
    @Test
    void classifiesCuratedMahouSignatureSpells() {
        assertEquals(StatType.ARCANE_POWER, MahouSpellTaxonomy.primaryStat("mahoutsukai:gandr_spell_scroll"));
        assertEquals(StatType.EARTH_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:rho_aias_spell_scroll"));
        assertEquals(StatType.FIRE_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:fallen_down_spell_scroll"));
        assertEquals(StatType.ERUDITION, MahouSpellTaxonomy.primaryStat("mahoutsukai:mystic_staff_spell_scroll"));
    }

    @Test
    void exposesSecondaryStatsForCuratedMahouProfiles() {
        assertIterableEquals(
                java.util.List.of(StatType.CASTING_SPEED),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:gandr_spell_scroll"));
        assertIterableEquals(
                java.util.List.of(StatType.MAGIC_RESISTANCE, StatType.WILLPOWER),
                MahouSpellTaxonomy.secondaryStats("mahoutsukai:rho_aias_spell_scroll"));
    }
}
