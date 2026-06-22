package tong.statmod.integration.ironspells.bridge;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraSchoolMappingTest {
    @Test
    void primaryStatMapsToIronsSchoolPath() {
        assertEquals("fire", TensuraSchoolMapping.schoolPathFor(StatType.FIRE_AFFINITY));
        assertEquals("ice", TensuraSchoolMapping.schoolPathFor(StatType.WATER_AFFINITY));
        assertEquals("nature", TensuraSchoolMapping.schoolPathFor(StatType.EARTH_AFFINITY));
        assertEquals("lightning", TensuraSchoolMapping.schoolPathFor(StatType.AIR_AFFINITY));
        assertEquals("evocation", TensuraSchoolMapping.schoolPathFor(StatType.ARCANE_POWER));
        assertEquals("evocation", TensuraSchoolMapping.schoolPathFor(StatType.MANA_POOL));
        assertEquals("evocation", TensuraSchoolMapping.schoolPathFor(StatType.CASTING_SPEED));
        assertEquals("holy", TensuraSchoolMapping.schoolPathFor(StatType.ERUDITION));
        assertEquals("holy", TensuraSchoolMapping.schoolPathFor(StatType.MAGIC_RESISTANCE));
    }

    @Test
    void unknownAndNullDefaultToEvocation() {
        assertEquals("evocation", TensuraSchoolMapping.schoolPathFor(null));
        assertEquals("evocation", TensuraSchoolMapping.schoolPathFor(StatType.BRUTE_FORCE));
        assertEquals("evocation", TensuraSchoolMapping.schoolPathFor(StatType.AGILITY));
    }

    @Test
    void everyTaxonomyProfileResolvesToABuiltInIronsSchool() {
        java.util.Set<String> allowed = java.util.Set.of(
                "fire", "ice", "nature", "lightning", "evocation", "holy");
        for (tong.statmod.integration.tensura.TensuraSpellProfile p :
                tong.statmod.integration.tensura.TensuraSpellTaxonomy.allProfiles()) {
            String school = TensuraSchoolMapping.schoolPathFor(p.primaryStat());
            assertTrue(allowed.contains(school),
                    p.skillId() + " mapped to unknown school '" + school + "'");
        }
    }
}
