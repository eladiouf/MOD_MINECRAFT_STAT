package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IronSpellCatalogTest {
    @Test void everyBaseProfileCoversEveryIntent() {
        for (IronSpellProfile profile : IronSpellProfile.values()) {
            for (IronSpellIntent intent : IronSpellIntent.values()) {
                assertFalse(IronSpellCatalog.spellIds(profile, intent).isEmpty(),
                        () -> profile + " has no " + intent + " spell");
            }
        }
    }

    @Test void schoolProfilesContainTheirSignatureSpells() {
        assertContains(IronSpellProfile.FIRE_ARTILLERY, IronSpellIntent.AREA_DAMAGE,
                "irons_spellbooks:magma_bomb");
        assertContains(IronSpellProfile.FROST_CONTROLLER, IronSpellIntent.CONTROL,
                "irons_spellbooks:ice_tomb");
        assertContains(IronSpellProfile.STORM_HUNTER, IronSpellIntent.DIRECT_DAMAGE,
                "irons_spellbooks:lightning_lance");
        assertContains(IronSpellProfile.ARCANE_DUELIST, IronSpellIntent.MOBILITY,
                "irons_spellbooks:teleport");
        assertContains(IronSpellProfile.NECROMANTIC_PRESSURE, IronSpellIntent.SUMMON,
                "irons_spellbooks:raise_dead");
        assertContains(IronSpellProfile.HOLY_SUPPORT, IronSpellIntent.ALLY_SUPPORT,
                "irons_spellbooks:healing_circle");
    }

    private static void assertContains(IronSpellProfile profile, IronSpellIntent intent,
                                       String spellId) {
        assertTrue(IronSpellCatalog.spellIds(profile, intent).contains(spellId));
    }
}
