package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.client.texture.TextureCache;
import tong.statmod.client.gui.perks.TalentTreePanel;
import tong.statmod.stats.StatCategory;

import static tong.statmod.client.texture.TextureCache.drawInkText;

public class PerkScreen extends Screen {
    private static final int TAB_COUNT = 5;
    private int selectedTab = 0;
    private final String[] tabNames = {"Combat", "Magie", "Survie", "Artisanat", "Mental"};
    private final StatCategory[] tabCategories = {
        StatCategory.COMBAT, StatCategory.MAGIC, StatCategory.SURVIVAL,
        StatCategory.CRAFTING, StatCategory.MENTAL};
    private static final int BG_COLOR = 0xC0E8D5A3;
    private static final int TEXT_COLOR = 0xFF3A1A00;
    private static final int POINTS_COLOR = 0xFFC49A3C;
    private static final int SEPARATOR_COLOR = 0xFF8B4513;
    private static final int PANEL_PADDING = 20;

    private TalentTreePanel treePanel;
    private int scrollOffset = 0;

    public PerkScreen() {
        super(Component.translatable("screen.statmod.perks"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildTree();
    }

    private void rebuildTree() {
        StatCategory category = tabCategories[selectedTab];
        int panelX = PANEL_PADDING;
        int panelY = 53;
        int panelW = this.width - 40;
        int panelH = this.height - panelY - 20;
        treePanel = new TalentTreePanel(category, panelX, panelY, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Parchment background
        ResourceLocation bgTex = TextureCache.get("bg_parchment.png");
        for (int x = 0; x < this.width; x += 256) {
            for (int y = 0; y < this.height; y += 256) {
                graphics.blit(bgTex, x, y, 0, 0, 256, 256, 256, 256);
            }
        }
        graphics.fill(0, 0, this.width, this.height, BG_COLOR);

        super.render(graphics, mouseX, mouseY, partialTick);

        var font = Minecraft.getInstance().font;

        // Title
        String title = "\u2764 ARBRE DE TALENTS \u2764";
        int titleX = this.width / 2 - font.width(title) / 2;
        drawInkText(graphics, font, title, titleX, 8, TEXT_COLOR);

        // Flanking lines
        int lineEnd = titleX - 10;
        if (lineEnd > PANEL_PADDING) graphics.fill(PANEL_PADDING, 12, lineEnd, 13, SEPARATOR_COLOR);
        int lineStart = titleX + font.width(title) + 10;
        if (lineStart < this.width - PANEL_PADDING) graphics.fill(lineStart, 12, this.width - PANEL_PADDING, 13, SEPARATOR_COLOR);

        // Points display
        String pointsText = "Points: " + ClientPerkCache.getAvailablePoints();
        drawInkText(graphics, font, pointsText, this.width - PANEL_PADDING - font.width(pointsText), 8, POINTS_COLOR);

        // Tabs
        int tabX = this.width / 2 - (TAB_COUNT * 52) / 2;
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean active = i == selectedTab;

            ResourceLocation tabTex = TextureCache.get(active ? "tab_active.png" : "tab_inactive.png");
            graphics.blit(tabTex, tabX, 22, 0, 0, 48, 24, 48, 24);

            int textColor = active ? 0xFF3A1A00 : 0xFF5A3A10;
            drawInkText(graphics, font, tabNames[i],
                tabX + 24 - font.width(tabNames[i]) / 2,
                27, textColor);
            tabX += 52;
        }

        // Separator below tabs
        graphics.fill(PANEL_PADDING, 48, this.width - PANEL_PADDING, 49, SEPARATOR_COLOR);

        // Render tree panel
        treePanel.setScrollOffset(scrollOffset);
        treePanel.renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tabX = this.width / 2 - (TAB_COUNT * 52) / 2;
        for (int i = 0; i < TAB_COUNT; i++) {
            if (mouseX >= tabX && mouseX <= tabX + 48 && mouseY >= 22 && mouseY <= 46) {
                if (selectedTab != i) {
                    selectedTab = i;
                    scrollOffset = 0;
                    rebuildTree();
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
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
