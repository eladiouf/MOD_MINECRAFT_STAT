package tong.statmod.client.gui.perks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.client.texture.TextureCache;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;

public class PerkNodeWidget extends AbstractWidget {
    private static final int NODE_SIZE = 24;
    private static final int TRANS_NODE_SIZE = 30;

    // Tier border colors
    private static final int CORE_COLOR = 0xFF9E9E9E;
    private static final int ACTIVE_COLOR = 0xFF4CAF50;
    private static final int SYNERGY_COLOR = 0xFF2196F3;
    private static final int SITUATIONAL_COLOR = 0xFFFF9800;
    private static final int MASTERY_COLOR = 0xFF9C27B0;
    private static final int TRANSCENDENCE_COLOR = 0xFFD4FF00;

    private static final int LOCKED_BORDER = 0xFF555555;
    private static final int AVAILABLE_BORDER = 0xFFC49A3C;
    private static final int UNLOCKED_BORDER = 0xFFFFD700;

    private final Perk perk;
    private final PerkNodeState state;

    public enum PerkNodeState { LOCKED, AVAILABLE, UNLOCKED }

    public PerkNodeWidget(Perk perk, PerkNodeState state, int x, int y) {
        super(x, y, perk.tier == PerkTier.TRANSCENDENCE ? TRANS_NODE_SIZE : NODE_SIZE,
            perk.tier == PerkTier.TRANSCENDENCE ? TRANS_NODE_SIZE : NODE_SIZE,
            Component.literal(perk.name));
        this.perk = perk;
        this.state = state;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int size = perk.tier == PerkTier.TRANSCENDENCE ? TRANS_NODE_SIZE : NODE_SIZE;
        int cx = getX() + size / 2;
        int cy = getY() + size / 2;
        int r = size / 2;

        boolean showGlow = state == PerkNodeState.AVAILABLE || state == PerkNodeState.UNLOCKED;

        // Glow effect
        if (state == PerkNodeState.AVAILABLE) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() / 300.0);
            int glowAlpha = (int)(60 * pulse);
            int glowColor = (glowAlpha << 24) | (0xC49A3C & 0x00FFFFFF);
            graphics.fill(cx - r - 3, cy - r - 3, cx + r + 3, cy - r - 1, glowColor);
            graphics.fill(cx - r - 3, cy + r + 1, cx + r + 3, cy + r + 3, glowColor);
            graphics.fill(cx - r - 3, cy - r - 1, cx - r - 1, cy + r + 1, glowColor);
            graphics.fill(cx + r + 1, cy - r - 1, cx + r + 3, cy + r + 1, glowColor);
        } else if (state == PerkNodeState.UNLOCKED) {
            int glowColor = 0x40FFD700;
            graphics.fill(cx - r - 3, cy - r - 3, cx + r + 3, cy - r - 1, glowColor);
            graphics.fill(cx - r - 3, cy + r + 1, cx + r + 3, cy + r + 3, glowColor);
            graphics.fill(cx - r - 3, cy - r - 1, cx - r - 1, cy + r + 1, glowColor);
            graphics.fill(cx + r + 1, cy - r - 1, cx + r + 3, cy + r + 1, glowColor);
        }

        // Node background
        ResourceLocation bgTex = TextureCache.get("perk_node_bg.png");
        graphics.blit(bgTex, cx - r, cy - r, size, size, 0, 0, 30, 32, 30, 32);

        // Tier border color
        int tierColor;
        switch (perk.tier) {
            case CORE: tierColor = CORE_COLOR; break;
            case ACTIVE: tierColor = ACTIVE_COLOR; break;
            case SYNERGY: tierColor = SYNERGY_COLOR; break;
            case SITUATIONAL: tierColor = SITUATIONAL_COLOR; break;
            case MASTERY: tierColor = MASTERY_COLOR; break;
            case TRANSCENDENCE: tierColor = TRANSCENDENCE_COLOR; break;
            default: tierColor = LOCKED_BORDER;
        }

        // Border — use tier color when unlocked/available, gray when locked
        int borderColor;
        switch (state) {
            case UNLOCKED: borderColor = UNLOCKED_BORDER; break;
            case AVAILABLE: borderColor = tierColor; break;
            default: borderColor = LOCKED_BORDER; break;
        }
        graphics.fill(cx - r, cy - r, cx + r, cy - r + 1, borderColor);
        graphics.fill(cx - r, cy + r - 1, cx + r, cy + r, borderColor);
        graphics.fill(cx - r, cy - r, cx - r + 1, cy + r, borderColor);
        graphics.fill(cx + r - 1, cy - r, cx + r, cy + r, borderColor);

        // Show tier indicator letter in node
        var font = Minecraft.getInstance().font;
        String tierChar = switch (perk.tier) {
            case CORE -> "C";
            case ACTIVE -> "A";
            case SYNERGY -> "S";
            case SITUATIONAL -> "U";
            case MASTERY -> "M";
            case TRANSCENDENCE -> "T";
        };
        int textColor = state == PerkNodeState.LOCKED ? 0xFFAAAAAA :
            perk.tier == PerkTier.TRANSCENDENCE ? 0xFFD4FF00 : 0xFF3A1A00;
        graphics.drawString(font, tierChar, cx - font.width(tierChar) / 2, cy - 3, textColor);

        // Cost indicator for synergy+
        if (perk.tier.cost > 1) {
            String costText = "*" + perk.tier.cost;
            graphics.drawString(font, costText, cx - font.width(costText) / 2, cy + 5, state == PerkNodeState.LOCKED ? 0xFF888888 : 0xFFD4FF00);
        }

        // Checkmark for unlocked
        if (state == PerkNodeState.UNLOCKED) {
            ResourceLocation unlockedTex = TextureCache.get("perk_node_unlocked.png");
            graphics.blit(unlockedTex, cx - r, cy - r, size, size, 0, 0, 33, 32, 33, 32);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    public Perk getPerk() { return perk; }
    public PerkNodeState getState() { return state; }
}
