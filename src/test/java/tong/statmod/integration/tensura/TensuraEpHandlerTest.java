package tong.statmod.integration.tensura;

import io.github.manasmods.tensura.util.EnergyHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraEpHandlerTest {

    @Test
    void epDrainTypesAreDetected() {
        assertTrue(TensuraEpHandler.isEpDrainType(EnergyHelper.DrainType.EP));
        assertTrue(TensuraEpHandler.isEpDrainType(EnergyHelper.DrainType.MAX_EP));
        assertFalse(TensuraEpHandler.isEpDrainType(EnergyHelper.DrainType.AURA));
    }

    @Test
    void actionTypesAreResolvedFromWeaponShape() {
        assertEquals("melee_sword", TensuraEpHandler.resolveActionTypeId("item.minecraft.diamond_sword"));
        assertEquals("melee_axe", TensuraEpHandler.resolveActionTypeId("item.minecraft.diamond_axe"));
        assertEquals("ranged", TensuraEpHandler.resolveActionTypeId("item.minecraft.bow"));
    }
}
