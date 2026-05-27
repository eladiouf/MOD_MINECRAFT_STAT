package tong.statmod.client.gui.perks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import tong.statmod.perks.Perk;

public class PerkNodeWidget extends AbstractWidget {
    private static final int NODE_SIZE = 24;
    private static final int LOCKED_BG = 0xFF666666;
    private static final int LOCKED_BORDER = 0xFF555555;
    private static final int AVAILABLE_BG = 0xFFD4C494;
    private static final int AVAILABLE_BORDER = 0xFFC49A3C;
    private static final int UNLOCKED_BG = 0xFFD4C494;
    private static final int UNLOCKED_BORDER = 0xFFFFD700;

    private final Perk perk;
    private final PerkNodeState state;

    public enum PerkNodeState { LOCKED, AVAILABLE, UNLOCKED }

    public PerkNodeWidget(Perk perk, PerkNodeState state, int x, int y) {
        super(x, y, NODE_SIZE, NODE_SIZE, Component.literal(perk.name));
        this.perk = perk;
        this.state = state;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cx = getX() + NODE_SIZE / 2;
        int cy = getY() + NODE_SIZE / 2;
        int r = NODE_SIZE / 2;

        int bgColor, borderColor;
        boolean showGlow = false;

        switch (state) {
            case UNLOCKED:
                bgColor = UNLOCKED_BG;
                borderColor = UNLOCKED_BORDER;
                showGlow = true;
                break;
            case AVAILABLE:
                bgColor = AVAILABLE_BG;
                borderColor = AVAILABLE_BORDER;
                showGlow = true;
                break;
            case LOCKED:
            default:
                bgColor = LOCKED_BG;
                borderColor = LOCKED_BORDER;
                showGlow = false;
                break;
        }

        // Glow effect
        if (showGlow && state == PerkNodeState.AVAILABLE) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() / 300.0);
            int glowAlpha = (int)(60 * pulse);
            int glowColor = (glowAlpha << 24) | (0xC49A3C & 0x00FFFFFF);
            graphics.fill(cx - r - 2, cy - r - 2, cx + r + 2, cy - r, glowColor);
            graphics.fill(cx - r - 2, cy + r, cx + r + 2, cy + r + 2, glowColor);
            graphics.fill(cx - r - 2, cy - r, cx - r, cy + r, glowColor);
            graphics.fill(cx + r, cy - r, cx + r + 2, cy + r, glowColor);
        } else if (showGlow && state == PerkNodeState.UNLOCKED) {
            int glowColor = 0x40FFD700;
            graphics.fill(cx - r - 2, cy - r - 2, cx + r + 2, cy - r, glowColor);
            graphics.fill(cx - r - 2, cy + r, cx + r + 2, cy + r + 2, glowColor);
            graphics.fill(cx - r - 2, cy - r, cx - r, cy + r, glowColor);
            graphics.fill(cx + r, cy - r, cx + r + 2, cy + r, glowColor);
        }

        // Square node (circle approximation)
        graphics.fill(cx - r, cy - r, cx + r, cy + r, bgColor);
        graphics.fill(cx - r, cy - r, cx + r, cy - r + 1, borderColor);
        graphics.fill(cx - r, cy + r - 1, cx + r, cy + r, borderColor);
        graphics.fill(cx - r, cy - r, cx - r + 1, cy + r, borderColor);
        graphics.fill(cx + r - 1, cy - r, cx + r, cy + r, borderColor);

        // Level text inside node
        var font = Minecraft.getInstance().font;
        String lvlText = String.valueOf(perk.levelRequired);
        int textColor = state == PerkNodeState.LOCKED ? 0xFFAAAAAA : 0xFF3A1A00;
        graphics.drawString(font, lvlText, cx - font.width(lvlText) / 2, cy - 3, textColor);

        // Checkmark for unlocked
        if (state == PerkNodeState.UNLOCKED) {
            graphics.drawString(font, "\u2713", cx + 5, cy - 8, 0xFFFFD700);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    public Perk getPerk() { return perk; }
    public PerkNodeState getState() { return state; }
}
