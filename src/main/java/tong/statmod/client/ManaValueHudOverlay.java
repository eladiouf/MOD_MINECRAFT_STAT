package tong.statmod.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@OnlyIn(Dist.CLIENT)
public final class ManaValueHudOverlay {

    private ManaValueHudOverlay() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath("statmod", "mana_numbers_bottom_right"),
                ManaValueHudOverlay::renderBottomRight);
    }

    private static void renderBottomRight(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float mana = ClientManaCache.getCurrentMana();
        float maxMana = ClientManaCache.getMaxMana();
        if (maxMana <= 0) return;
        Font font = mc.font;
        String text = Math.round(mana) + " / " + Math.round(maxMana);
        int x = graphics.guiWidth() - font.width(text) - 8;
        int y = graphics.guiHeight() - 42;
        graphics.fill(x - 3, y - 1, x + font.width(text) + 3, y + font.lineHeight + 1, 0x80000000);
        graphics.drawString(font, text, x, y, 0xFF55FFFF, false);
    }
}
