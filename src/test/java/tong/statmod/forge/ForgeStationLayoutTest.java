package tong.statmod.forge;

import org.junit.jupiter.api.Test;
import tong.statmod.menu.EnchantmentAnvilMenu;
import tong.statmod.menu.InfusionForgeMenu;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForgeStationLayoutTest {

    @Test
    void infusionForge_menuUsesThreeInputsPlusOneOutput() {
        assertEquals(3, InfusionForgeMenu.INPUT_SLOT_COUNT);
        assertEquals(4, InfusionForgeMenu.STATION_SLOT_COUNT);
    }

    @Test
    void enchantmentAnvil_menuUsesFourInputsPlusOneOutput() {
        assertEquals(4, EnchantmentAnvilMenu.INPUT_SLOT_COUNT);
        assertEquals(5, EnchantmentAnvilMenu.STATION_SLOT_COUNT);
    }
}
