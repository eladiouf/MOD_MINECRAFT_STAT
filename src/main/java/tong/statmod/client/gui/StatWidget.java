package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.texture.TextureCache;
import tong.statmod.stats.StatType;

public class StatWidget extends AbstractWidget {
    private static final int CARD_COLOR = 0xFFD4C494;
    private static final int BORDER_COLOR = 0xFFA0724A;
    private static final int TEXT_COLOR = 0xFF3A1A00;
    private static final int LEVEL_COLOR = 0xFF8B4513;
    private static final int XP_BG_COLOR = 0xFFB8965A;
    private static final int XP_GRADIENT_START = 0xFF8B4513;
    private static final int XP_GRADIENT_END = 0xFFD2691E;
    private static final int XP_TEXT_COLOR = 0xFF6B4C1E;

    private final StatType stat;

    public StatWidget(StatType stat, int x, int y) {
        super(x, y, 220, 30, Component.literal(stat.displayName));
        this.stat = stat;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int level = ClientStatsCache.getLevel(stat);
        int xp = ClientStatsCache.getXp(stat);
        int needed = (level + 1) * (level + 1) * 10;

        // Card background
        graphics.fill(getX(), getY(), getX() + 220, getY() + 30, CARD_COLOR);
        // Card border
        graphics.fill(getX(), getY(), getX() + 220, getY() + 1, BORDER_COLOR);
        graphics.fill(getX(), getY() + 29, getX() + 220, getY() + 30, BORDER_COLOR);
        graphics.fill(getX(), getY(), getX() + 1, getY() + 30, BORDER_COLOR);
        graphics.fill(getX() + 219, getY(), getX() + 220, getY() + 30, BORDER_COLOR);

        var font = Minecraft.getInstance().font;

        // Icon
        ResourceLocation iconTex = TextureCache.get("stat_icon_" + stat.index + ".png");
        graphics.blit(iconTex, getX() + 4, getY() + 5, 0, 0, 16, 16, 16, 16);

        // Name + Level
        graphics.drawString(font, stat.displayName, getX() + 24, getY() + 4, TEXT_COLOR);
        graphics.drawString(font, "Niv. " + level, getX() + 160, getY() + 4, LEVEL_COLOR);

        // XP bar
        int barX = getX() + 24;
        int barY = getY() + 18;
        int barWidth = 140;
        int barHeight = 4;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, XP_BG_COLOR);

        if (level < 100) {
            int filled = (int) ((float) xp / needed * barWidth);
            if (filled > 0) {
                int slices = 10;
                int sliceW = Math.max(1, filled / slices);
                for (int i = 0; i < slices && i * sliceW < filled; i++) {
                    float t = (float) i / slices;
                    int r = (int) (0x8B + (0xD2 - 0x8B) * t);
                    int g = (int) (0x45 + (0x69 - 0x45) * t);
                    int bVal = (int) (0x13 + (0x1E - 0x13) * t);
                    int color = 0xFF000000 | (r << 16) | (g << 8) | bVal;
                    graphics.fill(barX + i * sliceW, barY, Math.min(barX + i * sliceW + sliceW, barX + filled), barY + barHeight, color);
                }
            }
        } else {
            String maxText = "MAX";
            graphics.drawString(font, maxText, barX + barWidth - font.width(maxText), barY - 1, XP_TEXT_COLOR);
        }

        // XP text right-aligned below bar
        String xpText = level < 100 ? xp + " / " + needed + " XP" : "";
        if (!xpText.isEmpty()) {
            graphics.drawString(font, xpText, barX + barWidth - font.width(xpText), barY + 5, XP_TEXT_COLOR);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
