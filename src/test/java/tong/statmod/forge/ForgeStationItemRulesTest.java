package tong.statmod.forge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeStationItemRulesTest {

    @Test
    void rules_identifyKnownInfusionFamiliesByItemId() {
        assertTrue(ForgeStationItemRules.isRoughIntermediateId("statmod:rough_blade_arcane"));
        assertTrue(ForgeStationItemRules.isRoughIntermediateId("statmod:rough_dagger_blade_arcane"));
        assertTrue(ForgeStationItemRules.isRuneEssenceId("statmod:rune_essence_arcane"));
        assertTrue(ForgeStationItemRules.isGripId("statmod:runic_grip"));

        assertFalse(ForgeStationItemRules.isRoughIntermediateId("minecraft:diamond_sword"));
        assertFalse(ForgeStationItemRules.isRuneEssenceId("slu:flame_shard"));
        assertFalse(ForgeStationItemRules.isGripId("minecraft:stick"));
    }
}
