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

import static tong.statmod.client.texture.TextureCache.drawInkText;

public class StatWidget extends AbstractWidget {
    private static final int WIDGET_HEIGHT = 28;
    private static final int CARD_COLOR = 0xFFD4C494;
    private static final int BORDER_COLOR = 0xFFA0724A;
    private static final int TEXT_COLOR = 0xFF3A1A00;
    private static final int LEVEL_COLOR = 0xFF8B4513;
    private static final int XP_BG_COLOR = 0xFFB8965A;
    private static final int XP_TEXT_COLOR = 0xFF6B4C1E;

    private final StatType stat;

    public StatWidget(StatType stat, int x, int y) {
        super(x, y, 220, WIDGET_HEIGHT, Component.literal(stat.displayName));
        this.stat = stat;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int level = ClientStatsCache.getLevel(stat);
        int xp = ClientStatsCache.getXp(stat);
        int needed = (level + 1) * (level + 1) * 10;

        int right = getX() + 220;
        int bottom = getY() + WIDGET_HEIGHT;

        // Card background
        graphics.fill(getX(), getY(), right, bottom, CARD_COLOR);
        // Card border
        graphics.fill(getX(), getY(), right, getY() + 1, BORDER_COLOR);
        graphics.fill(getX(), bottom - 1, right, bottom, BORDER_COLOR);
        graphics.fill(getX(), getY(), getX() + 1, bottom, BORDER_COLOR);
        graphics.fill(right - 1, getY(), right, bottom, BORDER_COLOR);

        var font = Minecraft.getInstance().font;

        // Icon
        ResourceLocation iconTex = TextureCache.get("stat_icon_" + stat.index + ".png");
        graphics.blit(iconTex, getX() + 4, getY() + 5, 16, 16, 0, 0, 32, 32, 32, 32);

        // Name + Level
        drawInkText(graphics, font, stat.displayName, getX() + 24, getY() + 4, TEXT_COLOR);
        drawInkText(graphics, font, "Niv. " + level, getX() + 160, getY() + 4, LEVEL_COLOR);

        // XP bar
        int barX = getX() + 24;
        int barY = getY() + 18;
        int barWidth = 140;
        int barHeight = 4;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, XP_BG_COLOR);

        ResourceLocation xpFillTex = TextureCache.get("xp_bar_fill.png");
        if (level >= 100) {
            graphics.blit(xpFillTex, barX, barY, barWidth, barHeight, 0, 0, 64, 17, 64, 17);
            String maxText = "MAX";
            drawInkText(graphics, font, maxText, barX + barWidth - font.width(maxText), barY - 1, XP_TEXT_COLOR);
        } else {
            int filled = (int) ((float) xp / needed * barWidth);
            if (filled > 0) {
                graphics.blit(xpFillTex, barX, barY, filled, barHeight, 0, 0, 64, 17, 64, 17);
            }
        }

        // XP text right-aligned below bar
        String xpText = level < 100 ? xp + " / " + needed + " XP" : "";
        if (!xpText.isEmpty()) {
            drawInkText(graphics, font, xpText, barX + barWidth - font.width(xpText), barY + 5, XP_TEXT_COLOR);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
