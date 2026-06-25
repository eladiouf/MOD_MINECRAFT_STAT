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
    private TalentTreePanel treePanel;

    public PerkScreen() {
        super(Component.translatable("statmod.screen.perk_tree"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildPanel();
    }

    private void rebuildPanel() {
        List<StatType> allStats = new ArrayList<>();
        for (StatType stat : StatType.values()) {
            if (stat.hasPerks()) {
                allStats.add(stat);
            }
        }
        int panelX = 12;
        int panelY = 36;
        int panelW = width - 24;
        int panelH = height - 48;
        treePanel = new TalentTreePanel(allStats, panelX, panelY, panelW, panelH);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(partialTick);
        graphics.fill(0, 0, width, height, 0xCC0A0A1A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        String title = "\u00a7l\u00a7ePerk Tree\u00a7r";
        graphics.drawString(font, Component.literal(title), (width - font.width(title)) / 2, 8, 0xFFFFAA00);

        int totalPoints = 0;
        for (Perk perk : Perk.values()) {
            if (tong.statmod.client.ClientPerkCache.isUnlocked(perk)) totalPoints++;
        }
        String summary = "\u00a77Perks: \u00a7f" + totalPoints + "\u00a77 / \u00a7f" + Perk.values().length;
        graphics.drawString(font, Component.literal(summary), (width - font.width(summary)) / 2, 20, 0xFF888888);

        if (treePanel != null) {
            treePanel.render(graphics, mouseX, mouseY, font);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
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
