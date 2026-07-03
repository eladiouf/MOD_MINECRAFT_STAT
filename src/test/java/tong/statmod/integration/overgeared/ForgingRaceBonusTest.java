package tong.statmod.integration.overgeared;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.overgeared.OvergearedRecipeGate.MaterialGate;
import tong.statmod.magic.MagicRace;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Mission M5 — Phase ζ tests.
 *
 * <p>Vérifie les discounts de niveau FORGING appliqués par race + perks, et le multiplier
 * de durability. On utilise un vrai {@link PlayerStatData} — pas de Minecraft runtime
 * requis puisque les setters se contentent de manipuler des primitives.
 */
class ForgingRaceBonusTest {

    private PlayerStatData freshData() {
        return new PlayerStatData();
    }

    @Test
    void noRaceNoPerk_noDiscount() {
        PlayerStatData data = freshData();
        assertEquals(0, ForgingRaceBonus.forgingLevelDiscount(data));
        assertEquals(1.0f, ForgingRaceBonus.durabilityMultiplier(data), 0.001f);
    }

    @Test
    void nullData_returnsZeroAndOne() {
        assertEquals(0, ForgingRaceBonus.forgingLevelDiscount(null));
        assertEquals(1.0f, ForgingRaceBonus.durabilityMultiplier(null), 0.001f);
    }

    @Test
    void dwarfGetsThreeDiscount() {
        PlayerStatData data = freshData();
        data.setMagicRace(MagicRace.DWARF);
        assertEquals(3, ForgingRaceBonus.forgingLevelDiscount(data));
    }

    @Test
    void dwarfAndForgeCore_stackToFour() {
        PlayerStatData data = freshData();
        data.setMagicRace(MagicRace.DWARF);
        data.addUnlockedPerk(Perk.FORGE_CORE.id);
        assertEquals(4, ForgingRaceBonus.forgingLevelDiscount(data));
    }

    @Test
    void dwarfAndForgeTranscendence_stackToSix() {
        PlayerStatData data = freshData();
        data.setMagicRace(MagicRace.DWARF);
        data.addUnlockedPerk(Perk.FORGE_TRANSCENDENCE.id);
        assertEquals(6, ForgingRaceBonus.forgingLevelDiscount(data));
    }

    @Test
    void allForgePerksAndDwarf_stackToSeven() {
        PlayerStatData data = freshData();
        data.setMagicRace(MagicRace.DWARF);
        data.addUnlockedPerk(Perk.FORGE_CORE.id);
        data.addUnlockedPerk(Perk.FORGE_TRANSCENDENCE.id);
        assertEquals(7, ForgingRaceBonus.forgingLevelDiscount(data));
    }

    @Test
    void elfGetsDurabilityBonus() {
        PlayerStatData data = freshData();
        data.setMagicRace(MagicRace.ELF);
        assertEquals(1.25f, ForgingRaceBonus.durabilityMultiplier(data), 0.001f);
    }

    @Test
    void elfAndForgeMastery_durabilityStacksTo15() {
        PlayerStatData data = freshData();
        data.setMagicRace(MagicRace.ELF);
        data.addUnlockedPerk(Perk.FORGE_MASTERY.id);
        assertEquals(1.50f, ForgingRaceBonus.durabilityMultiplier(data), 0.001f);
    }

    @Test
    void humanAndBeast_noEffectInPhaseZeta() {
        // HUMAN et BEAST ont des bonus qui touchent la quality (Phase δ), pas les gates.
        // Aucun impact sur discount ou durability multiplier.
        PlayerStatData human = freshData();
        human.setMagicRace(MagicRace.HUMAN);
        assertEquals(0, ForgingRaceBonus.forgingLevelDiscount(human));
        assertEquals(1.0f, ForgingRaceBonus.durabilityMultiplier(human), 0.001f);

        PlayerStatData beast = freshData();
        beast.setMagicRace(MagicRace.BEAST);
        assertEquals(0, ForgingRaceBonus.forgingLevelDiscount(beast));
        assertEquals(1.0f, ForgingRaceBonus.durabilityMultiplier(beast), 0.001f);
    }

    @Test
    void applyBonuses_dwarfReducesForgingOnly() {
        PlayerStatData data = freshData();
        data.setMagicRace(MagicRace.DWARF);
        MaterialGate base = new MaterialGate(70, 30, 22);  // hihiirokane
        MaterialGate boosted = ForgingRaceBonus.applyBonuses(base, data);
        assertEquals(67, boosted.forging(), "DWARF -3 FORGING");
        assertEquals(30, boosted.erudition(), "ERUDITION untouched");
        assertEquals(22, boosted.arcanePower(), "ARCANE_POWER untouched");
    }

    @Test
    void applyBonuses_neverGoesBelowZero() {
        PlayerStatData data = freshData();
        data.setMagicRace(MagicRace.DWARF);
        data.addUnlockedPerk(Perk.FORGE_CORE.id);
        data.addUnlockedPerk(Perk.FORGE_TRANSCENDENCE.id);
        MaterialGate lowTier = new MaterialGate(3, 0, 0);  // tin
        MaterialGate boosted = ForgingRaceBonus.applyBonuses(lowTier, data);
        assertEquals(0, boosted.forging(), "clamped to 0");
    }

    @Test
    void applyBonuses_noDiscount_returnsSameInstance() {
        PlayerStatData data = freshData();  // pas de race, pas de perks
        MaterialGate base = new MaterialGate(10, 5, 3);
        MaterialGate result = ForgingRaceBonus.applyBonuses(base, data);
        assertEquals(base, result, "no-op returns original");
    }

    @Test
    void applyBonuses_nullGate_returnsNull() {
        assertNull(ForgingRaceBonus.applyBonuses(null, freshData()));
    }
}
