package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.client.texture.TextureCache;
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
    private static final int BG_COLOR = 0xC0E8D5A3;
    private static final int TEXT_COLOR = 0xFF3A1A00;
    private static final int SEPARATOR_COLOR = 0xFF8B4513;

    private final List<StatWidget> widgets = new ArrayList<>();
    private int scrollOffset = 0;

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
        int y = getTabAreaEnd();

        for (StatType stat : StatType.values()) {
            if (stat.category == category) {
                StatWidget widget = new StatWidget(stat, this.width / 2 - 110, y);
                addRenderableWidget(widget);
                widgets.add(widget);
                y += 34;
            }
        }
    }

    private int getTabAreaEnd() {
        return 55;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw parchment background
        ResourceLocation bgTex = TextureCache.get("bg_parchment.png");
        for (int x = 0; x < this.width; x += 256) {
            for (int y = 0; y < this.height; y += 256) {
                graphics.blit(bgTex, x, y, 0, 0, 256, 256, 256, 256);
            }
        }
        // Darken overlay
        graphics.fill(0, 0, this.width, this.height, BG_COLOR);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Title
        String title = "\u2764 STATISTIQUES \u2764";
        int titleX = this.width / 2 - this.font.width(title) / 2;
        graphics.drawString(this.font, title, titleX, 10, TEXT_COLOR);

        // Flanking lines
        int lineStartX = 20;
        int lineEndX = titleX - 10;
        if (lineEndX > lineStartX) {
            graphics.fill(lineStartX, 14, lineEndX, 15, SEPARATOR_COLOR);
        }
        lineStartX = titleX + this.font.width(title) + 10;
        lineEndX = this.width - 20;
        if (lineEndX > lineStartX) {
            graphics.fill(lineStartX, 14, lineEndX, 15, SEPARATOR_COLOR);
        }

        // Tabs
        int tabX = this.width / 2 - (TAB_COUNT * 60) / 2;
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean active = i == selectedTab;
            boolean isMagicTab = i == 1;

            int bg = active ? 0xFFD4C494 : 0xFFC4A86A;
            int border = active ? 0xFFC49A3C : 0xFF8B4513;

            graphics.fill(tabX, 25, tabX + 55, 42, bg);
            graphics.fill(tabX, 25, tabX + 55, 26, border);
            graphics.fill(tabX, 41, tabX + 55, 42, border);
            graphics.fill(tabX, 25, tabX + 1, 42, border);
            graphics.fill(tabX + 54, 25, tabX + 55, 42, border);

            int textColor = active ? 0xFF3A1A00 : 0xFF5A3A10;
            String label = isMagicTab ? tabNames[i] + " \u269C" : tabNames[i];
            graphics.drawString(this.font, label,
                tabX + 27 - this.font.width(label) / 2,
                30, textColor);

            tabX += 60;
        }

        // Separator below tabs
        graphics.fill(20, 44, this.width - 20, 45, SEPARATOR_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tabX = this.width / 2 - (TAB_COUNT * 60) / 2;
        for (int i = 0; i < TAB_COUNT; i++) {
            if (mouseX >= tabX && mouseX <= tabX + 55 && mouseY >= 25 && mouseY <= 42) {
                if (selectedTab != i) {
                    selectedTab = i;
                    scrollOffset = 0;
                    updateWidgets();
                }
                return true;
            }
            tabX += 60;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        scrollOffset = Math.max(0, scrollOffset - (int)(scrollDelta * 20));
        updateWidgetPositions();
        return true;
    }

    private void updateWidgetPositions() {
        int baseY = getTabAreaEnd();
        int i = 0;
        StatCategory category = tabCategories[selectedTab];
        for (StatType stat : StatType.values()) {
            if (stat.category == category) {
                if (i < widgets.size()) {
                    widgets.get(i).setY(baseY + i * 34 - scrollOffset);
                }
                i++;
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
