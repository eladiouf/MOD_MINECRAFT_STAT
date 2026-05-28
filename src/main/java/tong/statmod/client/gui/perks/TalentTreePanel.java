package tong.statmod.client.gui.perks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.texture.TextureCache;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatCategory;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

import static tong.statmod.client.texture.TextureCache.drawInkText;

public class TalentTreePanel extends AbstractWidget {
    private static final int STAT_NAME_COLOR = 0xFF3A1A00;
    private static final int CONNECTOR_COLOR = 0xFFC49A3C;
    private static final int CONNECTOR_LOCKED_COLOR = 0xFF666666;
    private static final int NODE_GAP = 40;
    private static final int STAT_GAP = 55;

    private final StatCategory category;
    private int scrollOffset = 0;

    public TalentTreePanel(StatCategory category, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Talent Tree"));
        this.category = category;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Panel border using nine-patch
        ResourceLocation panelBorderTex = TextureCache.get("panel_border.png");
        TextureCache.drawNinePatch(graphics, panelBorderTex, getX(), getY(), getWidth(), getHeight(), 8, 32, 31);

        var font = Minecraft.getInstance().font;
        ResourceLocation connectorTex = TextureCache.get("perk_connector.png");

        int rowY = getY() + 10 - scrollOffset;
        int centerX = getX() + getWidth() / 2;

        for (StatType stat : StatType.values()) {
            if (stat.category != category) continue;

            int statLevel = ClientStatsCache.getLevel(stat);

            // Stat name header
            String header = stat.displayName + "  \u2605" + statLevel;
            drawInkText(graphics, font, header, centerX - font.width(header) / 2, rowY, STAT_NAME_COLOR);

            // Get stat perks
            List<Perk> statPerks = new ArrayList<>();
            for (Perk perk : Perk.values()) {
                if (perk.stat == stat) statPerks.add(perk);
            }

            // Draw connector lines between nodes
            int nodeStartX = centerX - (statPerks.size() * NODE_GAP) / 2;
            for (int i = 0; i < statPerks.size() - 1; i++) {
                boolean leftUnlocked = ClientPerkCache.isUnlocked(statPerks.get(i));
                boolean rightUnlocked = ClientPerkCache.isUnlocked(statPerks.get(i + 1));
                boolean anyUnlocked = leftUnlocked || rightUnlocked;
                int endpointColor = anyUnlocked ? CONNECTOR_COLOR : CONNECTOR_LOCKED_COLOR;

                int lx = nodeStartX + i * NODE_GAP + 24;
                int rx = nodeStartX + (i + 1) * NODE_GAP;
                int cy = rowY + 30;
                int cw = rx - lx;
                // Blit connector texture stretched, color-tinted via the fill endpoints
                graphics.blit(connectorTex, lx, cy, cw, 7, 0, 0, 64, 7, 64, 7);
                // Overlay color strips at ends for unlock state indicator
                graphics.fill(lx, cy + 3, lx + 2, cy + 4, endpointColor);
                graphics.fill(rx - 2, cy + 3, rx, cy + 4, endpointColor);
            }

            // Render node widgets for this stat
            for (int i = 0; i < statPerks.size(); i++) {
                Perk perk = statPerks.get(i);
                PerkNodeWidget.PerkNodeState state;
                if (ClientPerkCache.isUnlocked(perk)) {
                    state = PerkNodeWidget.PerkNodeState.UNLOCKED;
                } else if (statLevel >= perk.levelRequired && ClientPerkCache.getAvailablePoints() > 0) {
                    state = PerkNodeWidget.PerkNodeState.AVAILABLE;
                } else {
                    state = PerkNodeWidget.PerkNodeState.LOCKED;
                }

                int nx = nodeStartX + i * NODE_GAP;
                int ny = rowY + 20;

                PerkNodeWidget widget = new PerkNodeWidget(perk, state, nx, ny);
                widget.renderWidget(graphics, mouseX, mouseY, partialTick);
            }

            rowY += STAT_GAP;
        }
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, offset);
    }

    public int getTotalHeight() {
        int count = 0;
        for (StatType stat : StatType.values()) {
            if (stat.category == category) count++;
        }
        return 20 + count * STAT_GAP;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
