package tong.statmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PerkScreen extends Screen {
    private static final int TAB_WIDTH = 56;
    private static final int TAB_HEIGHT = 16;
    private static final int TAB_GAP = 2;

    private final List<StatType> perkStats = new ArrayList<>();
    private int selectedStatIndex;
    private TalentTreePanel treePanel;

    public PerkScreen() {
        super(Component.translatable("statmod.screen.perk_tree"));
    }

    @Override
    protected void init() {
        super.init();
        perkStats.clear();
        for (StatType stat : StatType.values()) {
            if (stat.hasPerks()) {
                perkStats.add(stat);
            }
        }
        if (selectedStatIndex >= perkStats.size()) {
            selectedStatIndex = 0;
        }
        rebuildPanel();
    }

    private void rebuildPanel() {
        if (perkStats.isEmpty()) return;
        StatType stat = perkStats.get(selectedStatIndex);
        int panelX = 20;
        int panelY = 60;
        int panelW = width - 40;
        int panelH = height - 80;
        treePanel = new TalentTreePanel(stat, panelX, panelY, panelW, panelH);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(partialTick);
        graphics.fill(0, 0, width, height, 0xCC0A0A1A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (perkStats.isEmpty()) {
            graphics.drawString(font, Component.literal("\u00a7cNo perk stats available\u00a7r"), width / 2 - 60, height / 2, 0xFFFF5555);
            return;
        }

        String title = "\u00a7l\u00a7ePerk Tree\u00a7r";
        graphics.drawString(font, Component.literal(title), (width - font.width(title)) / 2, 10, 0xFFFFAA00);

        int totalPoints = 0;
        for (Perk perk : Perk.values()) {
            if (tong.statmod.client.ClientPerkCache.isUnlocked(perk)) totalPoints++;
        }
        String summary = "\u00a77Perks unlocked: \u00a7f" + totalPoints + "\u00a77 / \u00a7f" + Perk.values().length +
                "  \u00a77|  Perk Points: \u00a7f" + tong.statmod.client.ClientPerkCache.getPointsForStat(perkStats.get(selectedStatIndex).index);
        graphics.drawString(font, Component.literal(summary), (width - font.width(summary)) / 2, 22, 0xFF888888);

        renderTabs(graphics, mouseX, mouseY);

        if (treePanel != null) {
            treePanel.render(graphics, mouseX, mouseY, font);
        }
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int totalTabW = perkStats.size() * (TAB_WIDTH + TAB_GAP) - TAB_GAP;
        int startX = (width - totalTabW) / 2;

        for (int i = 0; i < perkStats.size(); i++) {
            StatType stat = perkStats.get(i);
            int tx = startX + i * (TAB_WIDTH + TAB_GAP);
            int ty = 38;
            boolean selected = i == selectedStatIndex;
            boolean hovered = mouseX >= tx && mouseX < tx + TAB_WIDTH && mouseY >= ty && mouseY < ty + TAB_HEIGHT;

            int bgColor = selected ? 0xFF333355 : hovered ? 0xFF222244 : 0xFF111122;
            int borderColor = selected ? 0xFFFFAA00 : 0xFF444466;

            graphics.fill(tx, ty, tx + TAB_WIDTH, ty + TAB_HEIGHT, bgColor);
            graphics.fill(tx, ty, tx + TAB_WIDTH, ty + 1, borderColor);
            graphics.fill(tx, ty, tx + 1, ty + TAB_HEIGHT, borderColor);
            graphics.fill(tx + TAB_WIDTH - 1, ty, tx + TAB_WIDTH, ty + TAB_HEIGHT, borderColor);
            graphics.fill(tx, ty + TAB_HEIGHT - 1, tx + TAB_WIDTH, ty + TAB_HEIGHT, selected ? 0xFFFFAA00 : borderColor);

            String label = stat.displayName;
            while (!label.isEmpty() && font.width(label) > TAB_WIDTH - 4) {
                label = label.substring(0, label.length() - 1);
            }
            int textColor = selected ? 0xFFFFAA00 : hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
            graphics.drawString(font, label, tx + (TAB_WIDTH - font.width(label)) / 2, ty + (TAB_HEIGHT - 9) / 2, textColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int totalTabW = perkStats.size() * (TAB_WIDTH + TAB_GAP) - TAB_GAP;
        int startX = (width - totalTabW) / 2;

        for (int i = 0; i < perkStats.size(); i++) {
            int tx = startX + i * (TAB_WIDTH + TAB_GAP);
            int ty = 38;
            if (mouseX >= tx && mouseX < tx + TAB_WIDTH && mouseY >= ty && mouseY < ty + TAB_HEIGHT) {
                if (selectedStatIndex != i) {
                    selectedStatIndex = i;
                    rebuildPanel();
                }
                return true;
            }
        }

        if (treePanel != null && treePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (treePanel != null && treePanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 || key == 66) { onClose(); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
