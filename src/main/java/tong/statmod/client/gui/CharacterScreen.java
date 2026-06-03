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

import static tong.statmod.client.texture.TextureCache.drawInkText;

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
    private static final int WIDGET_SPACING = 32;
    private static final int WIDGET_HEIGHT = 28;

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
                y += WIDGET_SPACING;
            }
        }
    }

    private int getTabAreaEnd() {
        return 56;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Prevent vanilla dirt overlay from overwriting our parchment
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
        drawInkText(graphics, this.font, title, titleX, 10, TEXT_COLOR);

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
        int tabX = this.width / 2 - (TAB_COUNT * 52) / 2;
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean active = i == selectedTab;

            ResourceLocation tabTex = TextureCache.get(active ? "tab_active.png" : "tab_inactive.png");
            graphics.blit(tabTex, tabX, 25, 0, 0, 48, 24, 48, 24);

            int textColor = active ? 0xFF3A1A00 : 0xFF5A3A10;
            drawInkText(graphics, this.font, tabNames[i],
                tabX + 24 - this.font.width(tabNames[i]) / 2,
                31, textColor);

            tabX += 52;
        }

        // Separator below tabs
        graphics.fill(20, 51, this.width - 20, 52, SEPARATOR_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tabX = this.width / 2 - (TAB_COUNT * 52) / 2;
        for (int i = 0; i < TAB_COUNT; i++) {
            if (mouseX >= tabX && mouseX <= tabX + 48 && mouseY >= 25 && mouseY <= 49) {
                if (selectedTab != i) {
                    selectedTab = i;
                    scrollOffset = 0;
                    updateWidgets();
                }
                return true;
            }
            tabX += 52;
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
                    widgets.get(i).setY(baseY + i * WIDGET_SPACING - scrollOffset);
                }
                i++;
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
