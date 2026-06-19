package tong.statmod.integration.mahou;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MahouMappingTest {
    @Test
    void mapsPathFragmentsToElements() {
        assertEquals("earth", MahouElementMapper.elementForPath("mahoutsukai:rho_aias_spell_scroll"));
        assertEquals("fire", MahouElementMapper.elementForPath("mahoutsukai:fallen_down_spell_scroll"));
        assertEquals("fire", MahouElementMapper.elementForPath("fire_scroll"));
        assertEquals("water", MahouElementMapper.elementForPath("ice_tome"));
        assertEquals("arcane", MahouElementMapper.elementForPath("mystery_scroll"));
    }

    @Test
    void mapsElementsToStats() {
        assertArrayEquals(new StatType[]{StatType.FIRE_AFFINITY, StatType.ARCANE_POWER},
                MahouPerkMap.statsForElement("fire"));
        assertArrayEquals(new StatType[]{StatType.ARCANE_POWER, StatType.ERUDITION},
                MahouPerkMap.statsForElement("arcane"));
        assertArrayEquals(new StatType[]{StatType.EARTH_AFFINITY, StatType.MAGIC_RESISTANCE},
                MahouPerkMap.statsForSpellId("mahoutsukai:rho_aias_spell_scroll"));
        assertArrayEquals(new StatType[]{StatType.ERUDITION, StatType.MANA_POOL},
                MahouPerkMap.statsForSpellId("mahoutsukai:mystic_staff_spell_scroll"));
        assertArrayEquals(new StatType[]{StatType.ARCANE_POWER, StatType.MANA_POOL},
                MahouPerkMap.statsForSpellId("mahoutsukai:scroll_boundary_drain_life"));
    }

    @Test
    void scalesTierBonusByScrollName() {
        assertEquals(1, MahouSpellTier.xpMultiplierForPath("basic_scroll"));
        assertEquals(2, MahouSpellTier.xpMultiplierForPath("advanced_scroll"));
        assertEquals(3, MahouSpellTier.xpMultiplierForPath("ultimate_scroll"));
    }

    @Test
    void gatesSpellTiersByArcanePower() {
        assertEquals(0, MahouSpellTier.requiredArcanePowerForPath("basic_scroll"));
        assertEquals(20, MahouSpellTier.requiredArcanePowerForPath("advanced_scroll"));
        assertEquals(40, MahouSpellTier.requiredArcanePowerForPath("ultimate_scroll"));
        assertEquals(40, MahouSpellTier.requiredArcanePowerForPath("master_grimoire"));

        assertEquals(false, MahouSpellTier.canCast(19, "advanced_scroll"));
        assertEquals(true, MahouSpellTier.canCast(20, "advanced_scroll"));
        assertEquals(false, MahouSpellTier.canCast(39, "ultimate_scroll"));
        assertEquals(true, MahouSpellTier.canCast(40, "ultimate_scroll"));
    }

    @Test
    void gatesCuratedProfilesByTheirPrimaryMagicStat() {
        assertEquals(10, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:gandr_spell_scroll"));
        assertEquals(15, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:rho_aias_spell_scroll"));
        assertEquals(30, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:fallen_down_spell_scroll"));
        assertEquals(20, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:mystic_staff_spell_scroll"));
        assertEquals(20, MahouSpellTier.requiredPrimaryStatLevel("mahoutsukai:scroll_boundary_drain_life"));

        assertEquals(10, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:gandr_spell_scroll"));
        assertEquals(20, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:rho_aias_spell_scroll"));
        assertEquals(25, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:mystic_staff_spell_scroll"));
        assertEquals(25, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:scroll_boundary_drain_life"));
        assertEquals(40, MahouSpellTier.requiredArcanePowerForItemId("mahoutsukai:fallen_down_spell_scroll"));

        assertEquals(false, MahouSpellTier.canCast(39, 30, "mahoutsukai:fallen_down_spell_scroll"));
        assertEquals(false, MahouSpellTier.canCast(40, 29, "mahoutsukai:fallen_down_spell_scroll"));
        assertEquals(true, MahouSpellTier.canCast(40, 30, "mahoutsukai:fallen_down_spell_scroll"));
        assertEquals(false, MahouSpellTier.canCast(24, 20, "mahoutsukai:scroll_boundary_drain_life"));
        assertEquals(true, MahouSpellTier.canCast(25, 20, "mahoutsukai:scroll_boundary_drain_life"));
        assertEquals(false, MahouSpellTier.canCast(40, 14, "mahoutsukai:rho_aias_spell_scroll"));
        assertEquals(true, MahouSpellTier.canCast(40, 15, "mahoutsukai:rho_aias_spell_scroll"));
    }

    @Test
    void computesCombatBonusesFromRecentSpellAndStats() {
        assertEquals(1.2f, MahouCompat.earthDamageMultiplier(20, "earth", 200, 210), 0.0001f);
        assertEquals(1.0f, MahouCompat.earthDamageMultiplier(20, "fire", 200, 210), 0.0001f);
        assertEquals(0.3f, MahouCompat.magicReflectionChance(60, 60), 0.0001f);
    }
}
