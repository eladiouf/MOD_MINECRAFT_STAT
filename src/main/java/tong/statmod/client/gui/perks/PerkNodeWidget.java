package tong.statmod.client.gui.perks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.client.texture.TextureCache;
import tong.statmod.perks.Perk;

public class PerkNodeWidget extends AbstractWidget {
    private static final int NODE_SIZE = 24;
    private static final int LOCKED_BORDER = 0xFF555555;
    private static final int AVAILABLE_BORDER = 0xFFC49A3C;
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

        boolean showGlow = false;

        switch (state) {
            case UNLOCKED:
                showGlow = true;
                break;
            case AVAILABLE:
                showGlow = true;
                break;
            case LOCKED:
            default:
                showGlow = false;
                break;
        }

        // Glow effect
        if (showGlow && state == PerkNodeState.AVAILABLE) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() / 300.0);
            int glowAlpha = (int)(60 * pulse);
            int glowColor = (glowAlpha << 24) | (0xC49A3C & 0x00FFFFFF);
            graphics.fill(cx - r - 3, cy - r - 3, cx + r + 3, cy - r - 1, glowColor);
            graphics.fill(cx - r - 3, cy + r + 1, cx + r + 3, cy + r + 3, glowColor);
            graphics.fill(cx - r - 3, cy - r - 1, cx - r - 1, cy + r + 1, glowColor);
            graphics.fill(cx + r + 1, cy - r - 1, cx + r + 3, cy + r + 1, glowColor);
        } else if (showGlow && state == PerkNodeState.UNLOCKED) {
            int glowColor = 0x40FFD700;
            graphics.fill(cx - r - 3, cy - r - 3, cx + r + 3, cy - r - 1, glowColor);
            graphics.fill(cx - r - 3, cy + r + 1, cx + r + 3, cy + r + 3, glowColor);
            graphics.fill(cx - r - 3, cy - r - 1, cx - r - 1, cy + r + 1, glowColor);
            graphics.fill(cx + r + 1, cy - r - 1, cx + r + 3, cy + r + 1, glowColor);
        }

        // Node background texture
        ResourceLocation bgTex = TextureCache.get("perk_node_bg.png");
        graphics.blit(bgTex, cx - r, cy - r, NODE_SIZE, NODE_SIZE, 0, 0, 30, 32, 30, 32);

        // Border overlay based on state
        int borderColor;
        switch (state) {
            case UNLOCKED:
                borderColor = UNLOCKED_BORDER;
                break;
            case AVAILABLE:
                borderColor = AVAILABLE_BORDER;
                break;
            default:
                borderColor = LOCKED_BORDER;
                break;
        }
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
            ResourceLocation unlockedTex = TextureCache.get("perk_node_unlocked.png");
            graphics.blit(unlockedTex, cx - r, cy - r, NODE_SIZE, NODE_SIZE, 0, 0, 33, 32, 33, 32);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    public Perk getPerk() { return perk; }
    public PerkNodeState getState() { return state; }
}
