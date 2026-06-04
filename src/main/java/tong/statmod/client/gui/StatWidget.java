package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.texture.StatIconRenderer;
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
        int needed = tong.statmod.stats.StatCalculator.getXpForNextLevel(level);

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

        // Icon (sprite atlas)
        StatIconRenderer.renderIcon(graphics, stat.name(), getX() + 4, getY() + 5);

        // Name + Level
        int levelColor;
        if (level >= 100) levelColor = 0xFF55FF55;
        else if (level >= 50) levelColor = 0xFFFFAA00;
        else levelColor = TEXT_COLOR;
        drawInkText(graphics, font, stat.displayName, getX() + 24, getY() + 4, TEXT_COLOR);
        drawInkText(graphics, font, "Niv. " + level, getX() + 160, getY() + 4, levelColor);

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
            // XP percentage next to bar
            String pctText = (int)(((float) xp / needed) * 100) + "%";
            drawInkText(graphics, font, pctText, barX + barWidth + 2, barY - 2, XP_TEXT_COLOR);
        }

        // XP text right-aligned below bar
        String xpText = level < 100 ? xp + " / " + needed + " XP" : "";
        if (!xpText.isEmpty()) {
            drawInkText(graphics, font, xpText, barX + barWidth - font.width(xpText), barY + 5, XP_TEXT_COLOR);
        }

        // Tooltip on hover
        if (mouseX >= getX() && mouseX <= getX() + 220 && mouseY >= getY() && mouseY <= getY() + WIDGET_HEIGHT) {
            String effect = getStatEffectDescription(stat);
            if (level >= 100) {
                effect += " \u00a7a(MAXED)";
            }
            graphics.renderTooltip(font, Component.literal(effect), mouseX, mouseY);
        }
    }

    private static String getStatEffectDescription(StatType stat) {
        return switch (stat) {
            case BRUTE_FORCE -> "+0.2% melee damage per level";
            case BLADE_TECHNIQUE -> "+0.15% damage, +attack speed per level";
            case RAPIDITE -> "+0.3% attack speed per level";
            case AGILITY -> "+0.2% move speed per level";
            case PHYSICAL_RESISTANCE -> "-0.3% damage taken per level";
            case PHYSICAL_ENDURANCE -> "+0.2 hearts per level";
            case PRECISION -> "+0.3% crit chance per level";
            case ARCANE_POWER -> "+0.3% magic damage per level";
            case WATER_AFFINITY -> "+0.5% swim speed per level";
            case EARTH_AFFINITY -> "+0.3% mining speed per level";
            case FIRE_AFFINITY -> "+0.5% fire damage per level";
            case AIR_AFFINITY -> "+0.3% jump height per level";
            case MAGIC_RESISTANCE -> "-0.3% magic damage taken per level";
            case CASTING_SPEED -> "+0.3% item use speed per level";
            case MANA_POOL -> "+1 max mana per level";
            case ERUDITION -> "+0.5% XP bonus per level";
            case TRACKING -> "+0.3% luck per level";
            case KEEN_SENSES -> "+0.3 blocks detection per level";
            case FORGING -> "+0.3% tool durability per level";
            case COOKING -> "+0.3% food saturation per level";
            case ALCHEMY -> "+0.3% potion duration per level";
            case INTIMIDATION -> "+0.5 blocks fear range per level";
            case WILLPOWER -> "-0.5% status duration per level";
        };
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
