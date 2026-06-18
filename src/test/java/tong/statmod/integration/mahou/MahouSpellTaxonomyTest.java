package tong.statmod.integration.mahou;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MahouSpellTaxonomyTest {
    @Test
    void classifiesCuratedMahouSignatureSpells() {
        assertEquals(StatType.ARCANE_POWER, MahouSpellTaxonomy.primaryStat("mahoutsukai:gandr_spell_scroll"));
        assertEquals(StatType.EARTH_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:rho_aias_spell_scroll"));
        assertEquals(StatType.FIRE_AFFINITY, MahouSpellTaxonomy.primaryStat("mahoutsukai:fallen_down_spell_scroll"));
        assertEquals(StatType.ERUDITION, MahouSpellTaxonomy.primaryStat("mahoutsukai:mystic_staff_spell_scroll"));
    }
}
