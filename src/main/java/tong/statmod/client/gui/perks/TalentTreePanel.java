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
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatCategory;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

import static tong.statmod.client.texture.TextureCache.drawInkText;

public class TalentTreePanel extends AbstractWidget {
    private static final int STAT_NAME_COLOR = 0xFF3A1A00;
    private static final int CONNECTOR_COLOR = 0xFFC49A3C;
    private static final int CONNECTOR_LOCKED_COLOR = 0xFF666666;
    private static final int NODE_GAP = 38;
    private static final int STAT_GAP = 65;

    private static final int CORE_COLOR = 0xFF9E9E9E;
    private static final int ACTIVE_COLOR = 0xFF4CAF50;
    private static final int SYNERGY_COLOR = 0xFF2196F3;
    private static final int SITUATIONAL_COLOR = 0xFFFF9800;
    private static final int MASTERY_COLOR = 0xFF9C27B0;
    private static final int TRANSCENDENCE_COLOR = 0xFFD4FF00;

    private final StatCategory category;
    private int scrollOffset = 0;

    public TalentTreePanel(StatCategory category, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Talent Tree"));
        this.category = category;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation panelBorderTex = TextureCache.get("panel_border.png");
        TextureCache.drawNinePatch(graphics, panelBorderTex, getX(), getY(), getWidth(), getHeight(), 8, 32, 31);

        var font = Minecraft.getInstance().font;
        ResourceLocation connectorTex = TextureCache.get("perk_connector.png");

        int rowY = getY() + 10 - scrollOffset;
        int centerX = getX() + getWidth() / 2;

        for (StatType stat : StatType.values()) {
            if (stat.category != category) continue;

            int statLevel = ClientStatsCache.getLevel(stat);
            int available = ClientPerkCache.getAvailablePointsForStat(stat.index);

            // Stat name header with points
            String header = stat.displayName + "  \u2605" + statLevel + "  [\u25a0" + available + "]";
            drawInkText(graphics, font, header, centerX - font.width(header) / 2, rowY, STAT_NAME_COLOR);

            // Get stat perks sorted by tier order
            List<Perk> statPerks = new ArrayList<>();
            for (Perk perk : Perk.values()) {
                if (perk.stat == stat) statPerks.add(perk);
            }
            statPerks.sort((a, b) -> a.tier.ordinal() - b.tier.ordinal());

            // Draw tier labels above nodes
            int nodeStartX = centerX - (statPerks.size() * NODE_GAP) / 2;
            for (int i = 0; i < statPerks.size(); i++) {
                Perk perk = statPerks.get(i);
                int tierColor = switch (perk.tier) {
                    case CORE -> CORE_COLOR;
                    case ACTIVE -> ACTIVE_COLOR;
                    case SYNERGY -> SYNERGY_COLOR;
                    case SITUATIONAL -> SITUATIONAL_COLOR;
                    case MASTERY -> MASTERY_COLOR;
                    case TRANSCENDENCE -> TRANSCENDENCE_COLOR;
                };
                String tierName = perk.tier.name().charAt(0) + perk.tier.name().substring(1).toLowerCase();
                int tx = nodeStartX + i * NODE_GAP + 12 - font.width(tierName) / 2;
                int ty = rowY + 13;
                graphics.drawString(font, tierName, tx, ty, tierColor);
            }

            // Draw connector lines between nodes
            for (int i = 0; i < statPerks.size() - 1; i++) {
                boolean leftUnlocked = ClientPerkCache.isUnlocked(statPerks.get(i));
                boolean rightUnlocked = ClientPerkCache.isUnlocked(statPerks.get(i + 1));
                boolean anyUnlocked = leftUnlocked || rightUnlocked;
                int endpointColor = anyUnlocked ? CONNECTOR_COLOR : CONNECTOR_LOCKED_COLOR;

                int lx = nodeStartX + i * NODE_GAP + 24;
                int rx = nodeStartX + (i + 1) * NODE_GAP;
                int cy = rowY + 32;
                int cw = rx - lx;
                graphics.blit(connectorTex, lx, cy, cw, 7, 0, 0, 64, 7, 64, 7);
                graphics.fill(lx, cy + 3, lx + 2, cy + 4, endpointColor);
                graphics.fill(rx - 2, cy + 3, rx, cy + 4, endpointColor);
            }

            // Render node widgets
            for (int i = 0; i < statPerks.size(); i++) {
                Perk perk = statPerks.get(i);
                PerkNodeWidget.PerkNodeState state;
                if (ClientPerkCache.isUnlocked(perk)) {
                    state = PerkNodeWidget.PerkNodeState.UNLOCKED;
                } else if (statLevel >= perk.tier.levelRequired && available >= perk.tier.cost) {
                    state = PerkNodeWidget.PerkNodeState.AVAILABLE;
                } else {
                    state = PerkNodeWidget.PerkNodeState.LOCKED;
                }

                int size = perk.tier == PerkTier.TRANSCENDENCE ? 30 : 24;
                int nx = nodeStartX + i * NODE_GAP;
                int ny = rowY + 22;

                PerkNodeWidget widget = new PerkNodeWidget(perk, state, nx, ny);
                widget.renderWidget(graphics, mouseX, mouseY, partialTick);

                // Cost text for synergy+
                if (perk.tier.cost > 1) {
                    String costStr = perk.tier.cost + " pts";
                    graphics.drawString(font, costStr, nx + size + 2, ny + size / 2 - 3, 0xFFD4FF00);
                }

                // Synergy requirement indicator
                if (perk.synergyStat != null) {
                    int synergyLevel = ClientStatsCache.getLevel(perk.synergyStat);
                    String synStr = perk.synergyStat.displayName + " 40+";
                    int synColor = synergyLevel >= 40 ? 0xFF4CAF50 : 0xFF888888;
                    graphics.drawString(font, synStr, nx + size + 2, ny + size / 2 + 6, synColor);
                }
            }

            rowY += STAT_GAP;
        }
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, offset);
    }

    public Perk getPerkAt(double mouseX, double mouseY) {
        int rowY = getY() + 10 - scrollOffset;
        int centerX = getX() + getWidth() / 2;

        for (StatType stat : StatType.values()) {
            if (stat.category != category) continue;

            List<Perk> statPerks = new ArrayList<>();
            for (Perk perk : Perk.values()) {
                if (perk.stat == stat) statPerks.add(perk);
            }
            statPerks.sort((a, b) -> a.tier.ordinal() - b.tier.ordinal());

            int nodeStartX = centerX - (statPerks.size() * NODE_GAP) / 2;
            for (int i = 0; i < statPerks.size(); i++) {
                Perk perk = statPerks.get(i);
                int size = perk.tier == PerkTier.TRANSCENDENCE ? 30 : 24;
                int nx = nodeStartX + i * NODE_GAP;
                int ny = rowY + 22;
                if (mouseX >= nx && mouseX <= nx + size && mouseY >= ny && mouseY <= ny + size) {
                    return statPerks.get(i);
                }
            }
            rowY += STAT_GAP;
        }
        return null;
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
