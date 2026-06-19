package tong.statmod.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import tong.statmod.client.ClientStatCache;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class TalentTreePanel {
    private static final int NODE_WIDTH = 90;
    private static final int NODE_HEIGHT = 26;
    private static final int NODE_GAP = 12;
    private static final int PANEL_PADDING = 16;

    private final StatType stat;
    private final int x;
    private final int y;
    private final int panelWidth;
    private final int panelHeight;
    private final List<PerkNodeWidget> nodes = new ArrayList<>();
    private int scrollOffset;

    public TalentTreePanel(StatType stat, int x, int y, int panelWidth, int panelHeight) {
        this.stat = stat;
        this.x = x;
        this.y = y;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;

        int centerX = x + panelWidth / 2;
        int startY = y + PANEL_PADDING + 20;

        for (PerkTier tier : PerkTier.values()) {
            Perk perk = Perk.byStatAndTier(stat, tier);
            if (perk != null) {
                nodes.add(new PerkNodeWidget(perk, centerX - NODE_WIDTH / 2, startY + nodes.size() * (NODE_HEIGHT + NODE_GAP)));
            }
        }

        this.scrollOffset = 0;
    }

    public StatType getStat() { return stat; }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, Font font) {
        int borderColor = statColor(stat);

        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xCC111111);
        graphics.fill(x, y, x + panelWidth, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + panelHeight, borderColor);
        graphics.fill(x + panelWidth - 1, y, x + panelWidth, y + panelHeight, borderColor);
        graphics.fill(x, y + panelHeight - 1, x + panelWidth, y + panelHeight, borderColor);

        int centerX = x + panelWidth / 2;
        graphics.drawString(font, Component.literal("\u00a7l" + stat.displayName + "\u00a7r"), centerX - font.width(stat.displayName) / 2, y + 6, 0xFFFFFFFF);

        var player = Minecraft.getInstance().player;
        int statLevel = player != null
                ? RaceEffectApplier.getEffectiveLevel(player, stat.index)
                : ClientStatCache.getLevel(stat.index);
        int cap = ClientStatCache.getSoulLevel() > 0 ? Math.min(ClientStatCache.getSoulLevel(), 100) : 100;
        String levelText = "Lv." + statLevel + "/" + cap;
        graphics.drawString(font, Component.literal("\u00a77" + levelText + "\u00a7r"), centerX - font.width(levelText) / 2, y + 16, 0xFFAAAAAA);

        graphics.enableScissor(x + 1, y + 32, x + panelWidth - 1, y + panelHeight - 1);

        int contentStartY = y + 32 + scrollOffset;

        int totalH = nodes.size() * (NODE_HEIGHT + NODE_GAP);
        int visibleH = panelHeight - 32 - PANEL_PADDING;
        int maxScroll = Math.max(0, totalH - visibleH);
        scrollOffset = Math.max(-maxScroll, Math.min(0, scrollOffset));

        for (int i = 0; i < nodes.size(); i++) {
            PerkNodeWidget node = nodes.get(i);
            int nodeY = contentStartY + i * (NODE_HEIGHT + NODE_GAP);
            int nodeX = node.getX();

            if (i > 0) {
                int aboveNodeY = contentStartY + (i - 1) * (NODE_HEIGHT + NODE_GAP) + NODE_HEIGHT;
                int lineX = nodeX + NODE_WIDTH / 2;

                int visibleLineStartY = Math.max(aboveNodeY, y + 32);
                int visibleLineEndY = Math.min(nodeY, y + panelHeight - 1);
                if (visibleLineStartY < visibleLineEndY) {
                    graphics.fill(lineX, visibleLineStartY, lineX + 1, visibleLineEndY, 0x66666666);
                }
            }

            if (nodeY + NODE_HEIGHT > y + 32 && nodeY < y + panelHeight - 1) {
                node.render(graphics, mouseX, mouseY, font);
            }
        }

        graphics.disableScissor();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + panelHeight) return false;

        for (PerkNodeWidget node : nodes) {
            if (node.isMouseOver((int) mouseX, (int) mouseY)) {
                node.tryClick();
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + panelHeight) return false;
        scrollOffset += (int) scrollY * 8;
        return true;
    }

    private static int statColor(StatType stat) {
        int idx = stat.index;
        if (idx == 0) return 0xFFFF4444;
        if (idx == 1) return 0xFF4488FF;
        if (idx == 2) return 0xFFFFAA00;
        if (idx == 3) return 0xFF44FF44;
        if (idx == 4) return 0xFF888888;
        if (idx == 5) return 0xFF00AA00;
        if (idx == 6) return 0xFFFF8844;
        if (idx == 16) return 0xFF44AAAA;
        if (idx == 17) return 0xFFFF88FF;
        if (idx == 18) return 0xFFAA5500;
        if (idx == 19) return 0xFFFF5555;
        if (idx == 20) return 0xFF55FFAA;
        if (idx == 21) return 0xFFAA0000;
        if (idx == 22) return 0xFFAA88FF;
        return 0xFFFFFFFF;
    }
}
