package tong.statmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tong.statmod.stats.StatCategory;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

public class CharacterScreen extends Screen {
    private static final int TAB_COUNT = 5;
    private int selectedTab = 0;
    private final String[] tabNames = {"Combat", "Magie", "Survie", "Artisanat", "Mental"};
    private final StatCategory[] tabCategories = {
        StatCategory.COMBAT, StatCategory.MAGIC, StatCategory.SURVIVAL,
        StatCategory.CRAFTING, StatCategory.MENTAL};
    private List<StatWidget> widgets = new ArrayList<>();

    public CharacterScreen() {
        super(Component.translatable("screen.statmod.character"));
    }

    @Override
    protected void init() {
        super.init();
        updateWidgets();
    }

    private void updateWidgets() {
        this.clearWidgets();
        widgets.clear();

        StatCategory category = tabCategories[selectedTab];
        int y = 40;
        for (StatType stat : StatType.values()) {
            if (stat.category == category) {
                StatWidget widget = new StatWidget(stat, 20, y);
                addRenderableWidget(widget);
                widgets.add(widget);
                y += 30;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int tabX = 20;
        for (int i = 0; i < TAB_COUNT; i++) {
            int color = i == selectedTab ? 0xFFFFFF : 0x888888;
            graphics.drawString(this.font, tabNames[i], tabX, 20, color);
            tabX += this.font.width(tabNames[i]) + 15;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tabX = 20;
        for (int i = 0; i < TAB_COUNT; i++) {
            int tabWidth = this.font.width(tabNames[i]) + 15;
            if (mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= 20 && mouseY <= 35) {
                if (selectedTab != i) {
                    selectedTab = i;
                    updateWidgets();
                }
                return true;
            }
            tabX += tabWidth;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
