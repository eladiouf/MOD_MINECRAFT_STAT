package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicRace;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblyForgingPolicyTest {

    @Test
    void nonGatedItem_isAlwaysAllowed() {
        PlayerStatData data = new PlayerStatData();
        ResourceLocation resultId = ResourceLocation.parse("minecraft:stick");

        assertTrue(AssemblyForgingPolicy.canAssemble(resultId, data));
        assertNull(AssemblyForgingPolicy.missingRequirements(resultId, data));
    }

    @Test
    void topTierWeapon_reportsAllMissingRequirements() {
        PlayerStatData data = new PlayerStatData();
        ResourceLocation resultId = ResourceLocation.parse("tensura:hihiirokane_katana");

        assertEquals("FORGING 0/70, ERUDITION 0/30, ARCANE_POWER 0/22",
                AssemblyForgingPolicy.missingRequirements(resultId, data));
    }

    @Test
    void raceAndPerkDiscounts_applyBeforeCheckingForgingRequirement() {
        PlayerStatData data = new PlayerStatData();
        data.setMagicRace(MagicRace.DWARF);
        data.addUnlockedPerk(Perk.FORGE_CORE.id);
        data.setLevel(StatType.FORGING.index, 11);

        ResourceLocation resultId = ResourceLocation.parse("block_factorys_bosses:knight_sword");
        assertTrue(AssemblyForgingPolicy.canAssemble(resultId, data),
                "steel gate 15 should become 11 with DWARF + FORGE_CORE");
    }
}
