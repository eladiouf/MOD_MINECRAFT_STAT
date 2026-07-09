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

    private static final int BAR_W = 110;
    private static final int BAR_H = 8;
    private static final int BAR_X = 4;
    private static final int BAR_Y = 4;

    private ManaValueHudOverlay() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH,
                ResourceLocation.fromNamespaceAndPath("statmod", "mana_bar_top_left"),
                ManaValueHudOverlay::renderTopLeft);

        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath("statmod", "mana_numbers_bottom_right"),
                ManaValueHudOverlay::renderBottomRight);
    }

    private static void renderTopLeft(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float mana = ClientManaCache.getCurrentMana();
        float maxMana = ClientManaCache.getMaxMana();
        if (maxMana <= 0) return;
        Font font = mc.font;

        int pct = (int) Math.round(mana / maxMana * 100);

        int barBg = 0xCC1A1A2E;
        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + BAR_H, barBg);

        if (pct > 0) {
            int fillW = Math.max(1, (BAR_W - 2) * pct / 100);
            int color = pct > 66 ? 0xFF4080FF : pct > 33 ? 0xFFFFA040 : 0xFFFF4040;
            graphics.fill(BAR_X + 1, BAR_Y + 1, BAR_X + 1 + fillW, BAR_Y + BAR_H - 1, color);
        }

        String pctText = pct + "%";
        graphics.drawString(font, pctText,
                BAR_X + BAR_W / 2 - font.width(pctText) / 2,
                BAR_Y + 1, 0xFFFFFFFF, true);
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
