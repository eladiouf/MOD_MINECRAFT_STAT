package tong.statmod.client.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeStationScreenSlotOverlaySourceTest {
    @Test
    void forgeStationDecorUsesVanillaSizedSlotFrames() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "gui", "ForgeStationScreenDecor.java"));

        assertTrue(source.contains("private static final int SLOT_PADDING = 1;"));
        assertTrue(source.contains("private static final int SLOT_OUTER_SIZE = 18;"));
    }

    @Test
    void infusionForgeScreenDrawsFourAlignedSlotFrames() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "gui", "InfusionForgeScreen.java"));

        assertTrue(source.contains("ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 26, 38"));
        assertTrue(source.contains("ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 62, 38"));
        assertTrue(source.contains("ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 98, 38"));
        assertTrue(source.contains("ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 134, 38"));
        assertTrue(source.contains("ForgeStationScreenDecor.renderPlayerInventorySlots(graphics, leftPos, topPos);"));
    }

    @Test
    void enchantmentAnvilScreenDrawsFiveAlignedSlotFrames() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "tong", "statmod", "client", "gui", "EnchantmentAnvilScreen.java"));

        assertTrue(source.contains("ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 17, 38"));
        assertTrue(source.contains("ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 44, 38"));
        assertTrue(source.contains("ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 71, 38"));
        assertTrue(source.contains("ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 98, 38"));
        assertTrue(source.contains("ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 134, 38"));
        assertTrue(source.contains("ForgeStationScreenDecor.renderPlayerInventorySlots(graphics, leftPos, topPos);"));
    }
}
