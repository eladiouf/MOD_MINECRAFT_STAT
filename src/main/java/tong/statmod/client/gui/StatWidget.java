package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.stats.StatType;

import java.awt.Color;

public class StatWidget extends AbstractWidget {
    private final StatType stat;

    public StatWidget(StatType stat, int x, int y) {
        super(x, y, 200, 20, Component.literal(stat.displayName));
        this.stat = stat;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        int level = ClientStatsCache.getLevel(stat);
        int xp = ClientStatsCache.getXp(stat);
        int needed = (level + 1) * (level + 1) * 10;

        graphics.drawString(font, stat.displayName + "  Lv." + level, getX(), getY(), 0xFFFFFF);

        int barWidth = 120;
        int barHeight = 4;
        int barX = getX() + 80;
        int barY = getY() + 12;
        int filled = needed > 0 ? (int) ((float) xp / needed * barWidth) : 0;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, Color.DARK_GRAY.getRGB());
        if (level < 100) {
            graphics.fill(barX, barY, barX + Math.min(filled, barWidth), barY + barHeight, Color.CYAN.getRGB());
        }

        String xpText = level < 100 ? xp + "/" + needed + " XP" : "MAX";
        graphics.drawString(font, xpText, getX() + 80 + barWidth + 5, barY - 2, Color.GRAY.getRGB());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
