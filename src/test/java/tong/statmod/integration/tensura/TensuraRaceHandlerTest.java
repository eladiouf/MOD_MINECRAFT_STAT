package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.RaceData;
import tong.statmod.integration.RaceModifierRegistry;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraRaceHandlerTest {

    @Test
    void normalizesNullAndBlankRaceIdsToHuman() {
        assertEquals("tensura:human", TensuraRaceHandler.normalizeRaceId(null));
        assertEquals("tensura:human", TensuraRaceHandler.normalizeRaceId("   "));
    }

    @Test
    void normalizesNamespaceAndCase() {
        assertEquals("tensura:elf", TensuraRaceHandler.normalizeRaceId("Tensura:Elf"));
        assertEquals("custom:beastfolk", TensuraRaceHandler.normalizeRaceId("custom:BeastFolk"));
    }

    @Test
    void exposesRegistryBackedRaceBonuses() {
        RaceData dwarf = RaceModifierRegistry.get("tensura:dwarf");
        assertTrue(dwarf.modifiers().stream().anyMatch(mod -> mod.statIndex() == 18 && mod.flatBonus() == 2));
        assertTrue(dwarf.modifiers().stream().anyMatch(mod -> mod.statIndex() == 4 && mod.flatBonus() == 1));
    }

    @Test
    void autoRespecRefundsPerksThatNoLongerMatchRace() {
        PlayerStatData data = new PlayerStatData();
        data.addUnlockedPerk(Perk.BLADE_TRANSCENDENCE.id);
        data.addUnlockedPerk(Perk.BRUTE_CORE.id);

        int refunded = TensuraRaceHandler.autoRespecRacePerks(data, "tensura:human");

        assertEquals(2, refunded);
        assertFalse(data.isPerkUnlocked(Perk.BLADE_TRANSCENDENCE.id));
        assertTrue(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
        assertEquals(2, data.getPerkPointsForStat(Perk.BLADE_TRANSCENDENCE.stat.index));
    }

    @Test
    void autoRespecKeepsPerksValidForNewRace() {
        PlayerStatData data = new PlayerStatData();
        data.addUnlockedPerk(Perk.BLADE_TRANSCENDENCE.id);

        int refunded = TensuraRaceHandler.autoRespecRacePerks(data, "tensura:kijin");

        assertEquals(0, refunded);
        assertTrue(data.isPerkUnlocked(Perk.BLADE_TRANSCENDENCE.id));
    }
}
