# GUI Redesign Parchemin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Overhaul CharacterScreen, PerkScreen, and HUD overlays with a medieval parchment RPG visual style.

**Architecture:** Three layers — (1) texture system (TextureCache + NinePatchRenderer) for loading/styling AI-generated PNGs, (2) reusable parchment components (ParchmentPanel, StatWidget rewrite, PerkNodeWidget, TalentTreePanel), (3) screen overhauls binding it all together. HUD reuses existing HudBar but adds gradient support and parchment-style colors.

**Tech Stack:** Minecraft Forge 1.20.1, vanilla Screen/AbstractWidget/GuiGraphics, Epic Fight integration.

---

## File Structure

```
src/main/java/tong/statmod/client/
├── texture/
│   ├── TextureCache.java           [NEW] — loads & caches GUI textures, drawNinePatch helper
├── gui/
│   ├── CharacterScreen.java        [MODIFY] — parchment background, styled tabs, scroll
│   ├── StatWidget.java             [REWRITE] — card-style stat entry with icon + gradient XP bar
│   ├── PerkScreen.java            [MODIFY] — tabs + TalentTreePanel, tooltips, point display
│   ├── perks/
│   │   ├── PerkNodeWidget.java    [NEW] — clickable circular perk node with states
│   │   └── TalentTreePanel.java   [NEW] — grid layout: stat rows + 3 perk nodes each
├── hud/
│   ├── HUDManager.java            [MODIFY] — move overlays to top-left
│   ├── components/
│   │   └── HudBar.java            [MODIFY] — add renderGradient() method
│   └── overlays/
│       ├── SurvivalOverlay.java   [MODIFY] — parchment colors, sepia borders, thirst bar (already exists)
│       └── GlobalLevelOverlay.java [MODIFY] — parchment panel, gold-brown XP bar
```

---

### Task 1: TextureCache — Texture Loading System

**Files:**
- Create: `src/main/java/tong/statmod/client/texture/TextureCache.java`

- [ ] **Step 1: Create TextureCache**

```java
package tong.statmod.client.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

import java.util.HashMap;
import java.util.Map;

public class TextureCache {
    private static final Map<String, ResourceLocation> textures = new HashMap<>();
    private static final String GUI_PATH = "textures/gui/";

    public static ResourceLocation get(String path) {
        return textures.computeIfAbsent(path, p ->
            new ResourceLocation(STATMod.MODID, GUI_PATH + p));
    }

    public static void drawNinePatch(GuiGraphics graphics, ResourceLocation tex,
                                      int x, int y, int w, int h, int border, int texWidth, int texHeight) {
        int b = border;
        int innerW = w - 2 * b;
        int innerH = h - 2 * b;
        int srcInnerW = texWidth - 2 * b;
        int srcInnerH = texHeight - 2 * b;

        // Corners
        graphics.blit(tex, x, y, 0, 0, b, b, texWidth, texHeight);                           // top-left
        graphics.blit(tex, x + w - b, y, texWidth - b, 0, b, b, texWidth, texHeight);          // top-right
        graphics.blit(tex, x, y + h - b, 0, texHeight - b, b, b, texWidth, texHeight);         // bottom-left
        graphics.blit(tex, x + w - b, y + h - b, texWidth - b, texHeight - b, b, b, texWidth, texHeight); // bottom-right

        // Edges
        graphics.blit(tex, x + b, y, innerW, b, b, 0, srcInnerW, b, texWidth, texHeight);      // top
        graphics.blit(tex, x + b, y + h - b, innerW, b, b, texHeight - b, srcInnerW, b, texWidth, texHeight); // bottom
        graphics.blit(tex, x, y + b, b, innerH, 0, b, b, srcInnerH, texWidth, texHeight);      // left
        graphics.blit(tex, x + w - b, y + b, b, innerH, texWidth - b, b, b, srcInnerH, texWidth, texHeight); // right

        // Center
        graphics.blit(tex, x + b, y + b, innerW, innerH, b, b, srcInnerW, srcInnerH, texWidth, texHeight);
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `.\gradlew build 2>&1 | findstr /C:"error"`
Expected: no errors referencing TextureCache

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/client/texture/TextureCache.java
git commit -m "feat(gui): add TextureCache with nine-patch rendering"
```

---

### Task 2: HudBar Gradient Support + HUD Position

**Files:**
- Modify: `src/main/java/tong/statmod/client/hud/components/HudBar.java`
- Modify: `src/main/java/tong/statmod/client/hud/HUDManager.java`

- [ ] **Step 1: Add renderGradient to HudBar**

Replace render method and add gradient method:

```java
package tong.statmod.client.hud.components;

import net.minecraft.client.gui.GuiGraphics;
import tong.statmod.client.hud.animation.LerpedValue;

public class HudBar {
    private final LerpedValue lerp;
    private final int barWidth;
    private final int barHeight;

    public HudBar(int barWidth, int barHeight, float speed) {
        this.lerp = new LerpedValue(0, speed);
        this.barWidth = barWidth;
        this.barHeight = barHeight;
    }

    public HudBar(int barWidth, int barHeight) {
        this(barWidth, barHeight, 0.15f);
    }

    public void setFill(float percent) {
        lerp.chase(Math.max(0, Math.min(1, percent)));
    }

    public void tick() {
        lerp.tick();
    }

    public void render(GuiGraphics graphics, int x, int y, int color) {
        float fill = lerp.getValue(0);
        int filledWidth = (int)(fill * barWidth);

        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF000000 | (0xB8965A & 0x00FFFFFF));
        if (filledWidth > 0) {
            graphics.fill(x, y, x + Math.min(filledWidth, barWidth), y + barHeight, color);
        }
    }

    public void renderGradient(GuiGraphics graphics, int x, int y, int colorStart, int colorEnd) {
        float fill = lerp.getValue(0);
        int filledWidth = (int)(fill * barWidth);

        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF000000 | (0xB8965A & 0x00FFFFFF));

        if (filledWidth > 0) {
            int slices = 10;
            int sliceW = Math.max(1, filledWidth / slices);
            for (int i = 0; i < slices && i * sliceW < filledWidth; i++) {
                float t = (float) i / slices;
                int r = lerpColor((colorStart >> 16) & 0xFF, (colorEnd >> 16) & 0xFF, t);
                int g = lerpColor((colorStart >> 8) & 0xFF, (colorEnd >> 8) & 0xFF, t);
                int bVal = lerpColor(colorStart & 0xFF, colorEnd & 0xFF, t);
                int sliceColor = 0xFF000000 | (r << 16) | (g << 8) | bVal;
                int sx = x + i * sliceW;
                int ex = Math.min(sx + sliceW, x + filledWidth);
                graphics.fill(sx, y, ex, y + barHeight, sliceColor);
            }
        }
    }

    private static int lerpColor(int a, int b, float t) {
        return (int)(a + (b - a) * t);
    }

    public void renderWithBorder(GuiGraphics graphics, int x, int y, int color, int borderColor) {
        render(graphics, x, y, color);
        graphics.fill(x - 1, y - 1, x + barWidth + 1, y, borderColor);
        graphics.fill(x - 1, y + barHeight, x + barWidth + 1, y + barHeight + 1, borderColor);
        graphics.fill(x - 1, y, x, y + barHeight, borderColor);
        graphics.fill(x + barWidth, y, x + barWidth + 1, y + barHeight, borderColor);
    }

    public float getCurrentFill() {
        return lerp.getCurrent();
    }
}
```

- [ ] **Step 2: Move HUD to top-left in HUDManager**

Change overlay registration order. The overlays themselves already use x/y coordinates. We just need SurvivalOverlay to render at top-left instead of bottom-left.

In `HUDManager.java`, change registration to place global level above survival:

No code change needed — the registration order doesn't affect position. The position is determined by render coordinates in each overlay. We'll fix coordinates in Task 4.

- [ ] **Step 3: Verify compile**

Run: `.\gradlew build 2>&1 | findstr /C:"error"`
Expected: no errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/client/hud/components/HudBar.java
git commit -m "feat(hud): add gradient and border rendering to HudBar"
```

---

### Task 3: Rewrite StatWidget — Parchment Card Style

**Files:**
- Rewrite: `src/main/java/tong/statmod/client/gui/StatWidget.java`

- [ ] **Step 1: Rewrite StatWidget**

Replace the entire file:

```java
package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.texture.TextureCache;
import tong.statmod.stats.StatType;

public class StatWidget extends AbstractWidget {
    private static final int CARD_COLOR = 0xFFD4C494;
    private static final int BORDER_COLOR = 0xFFA0724A;
    private static final int TEXT_COLOR = 0xFF3A1A00;
    private static final int LEVEL_COLOR = 0xFF8B4513;
    private static final int XP_BG_COLOR = 0xFFB8965A;
    private static final int XP_GRADIENT_START = 0xFF8B4513;
    private static final int XP_GRADIENT_END = 0xFFD2691E;
    private static final int XP_TEXT_COLOR = 0xFF6B4C1E;

    private final StatType stat;

    public StatWidget(StatType stat, int x, int y) {
        super(x, y, 220, 30, Component.literal(stat.displayName));
        this.stat = stat;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int level = ClientStatsCache.getLevel(stat);
        int xp = ClientStatsCache.getXp(stat);
        int needed = (level + 1) * (level + 1) * 10;

        // Card background
        graphics.fill(getX(), getY(), getX() + 220, getY() + 30, CARD_COLOR);
        // Card border
        graphics.fill(getX(), getY(), getX() + 220, getY() + 1, BORDER_COLOR);
        graphics.fill(getX(), getY() + 29, getX() + 220, getY() + 30, BORDER_COLOR);
        graphics.fill(getX(), getY(), getX() + 1, getY() + 30, BORDER_COLOR);
        graphics.fill(getX() + 219, getY(), getX() + 220, getY() + 30, BORDER_COLOR);

        var font = Minecraft.getInstance().font;

        // Icon
        ResourceLocation iconTex = TextureCache.get("stat_icon_" + stat.index + ".png");
        graphics.blit(iconTex, getX() + 4, getY() + 5, 0, 0, 16, 16, 16, 16);

        // Name + Level
        graphics.drawString(font, stat.displayName, getX() + 24, getY() + 4, TEXT_COLOR);
        graphics.drawString(font, "Niv. " + level, getX() + 160, getY() + 4, LEVEL_COLOR);

        // XP bar
        int barX = getX() + 24;
        int barY = getY() + 18;
        int barWidth = 140;
        int barHeight = 4;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, XP_BG_COLOR);

        if (level < 100) {
            int filled = (int) ((float) xp / needed * barWidth);
            if (filled > 0) {
                int slices = 10;
                int sliceW = Math.max(1, filled / slices);
                for (int i = 0; i < slices && i * sliceW < filled; i++) {
                    float t = (float) i / slices;
                    int r = (int) (0x8B + (0xD2 - 0x8B) * t);
                    int g = (int) (0x45 + (0x69 - 0x45) * t);
                    int bVal = (int) (0x13 + (0x1E - 0x13) * t);
                    int color = 0xFF000000 | (r << 16) | (g << 8) | bVal;
                    graphics.fill(barX + i * sliceW, barY, Math.min(barX + i * sliceW + sliceW, barX + filled), barY + barHeight, color);
                }
            }
        } else {
            String maxText = "MAX";
            graphics.drawString(font, maxText, barX + barWidth - font.width(maxText), barY - 1, XP_TEXT_COLOR);
        }

        // XP text right-aligned below bar
        String xpText = level < 100 ? xp + " / " + needed + " XP" : "";
        if (!xpText.isEmpty()) {
            graphics.drawString(font, xpText, barX + barWidth - font.width(xpText), barY + 5, XP_TEXT_COLOR);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
```

- [ ] **Step 2: Verify compile**

Run: `.\gradlew build 2>&1 | findstr /C:"error"`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/StatWidget.java
git commit -m "feat(gui): parchment-style stat widget with icon and gradient bar"
```

---

### Task 4: CharacterScreen Overhaul

**Files:**
- Modify: `src/main/java/tong/statmod/client/gui/CharacterScreen.java`

- [ ] **Step 1: Rewrite CharacterScreen**

Replace the file:

```java
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
    private static final int[] TAB_COLORS_ACTIVE = {0xFFD4C494, 0xFFD4C494, 0xFFD4C494, 0xFFD4C494, 0xFFD4C494};
    private static final int[] TAB_COLORS_INACTIVE = {0xFFC4A86A, 0xFFC4A86A, 0xFFC4A86A, 0xFFC4A86A, 0xFFC4A86A};
    private static final int TAB_BORDER = 0xFF8B4513;
    private static final int TAB_BORDER_ACTIVE = 0xFFC49A3C;
    private static final int BG_COLOR = 0xC0E8D5A3;
    private static final int TEXT_COLOR = 0xFF3A1A00;
    private static final int SEPARATOR_COLOR = 0xFF8B4513;

    private final List<StatWidget> widgets = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean initialized = false;

    public CharacterScreen() {
        super(Component.translatable("screen.statmod.character"));
    }

    @Override
    protected void init() {
        super.init();
        initialized = false;
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
        initialized = true;
    }

    private int getTabAreaEnd() {
        return 50;
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

            int bg = active ? TAB_COLORS_ACTIVE[i] : TAB_COLORS_INACTIVE[i];
            int border = active ? TAB_BORDER_ACTIVE : TAB_BORDER;

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
        int sepY = 44;
        graphics.fill(20, sepY, this.width - 20, sepY + 1, SEPARATOR_COLOR);

        // Scrollable stats area (handled by Widget clipping via Screen)
    }

    private int getTabStartX() {
        return this.width / 2 - (TAB_COUNT * 60) / 2;
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
```

- [ ] **Step 2: Verify compile**

Run: `.\gradlew build 2>&1 | findstr /C:"error"`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/CharacterScreen.java
git commit -m "feat(gui): parchment-styled CharacterScreen with centered tabs"
```

---

### Task 5: HUD Overlays — Parchment Style + Top-Left Position

**Files:**
- Modify: `src/main/java/tong/statmod/client/hud/overlays/GlobalLevelOverlay.java`
- Modify: `src/main/java/tong/statmod/client/hud/overlays/SurvivalOverlay.java`

- [ ] **Step 1: Rewrite GlobalLevelOverlay**

```java
package tong.statmod.client.hud.overlays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.hud.components.HudBar;
import tong.statmod.client.hud.animation.LerpedValue;

@Mod.EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlobalLevelOverlay implements IGuiOverlay {
    public static final GlobalLevelOverlay INSTANCE = new GlobalLevelOverlay();

    private static final int PANEL_BG = 0x66D4C494;
    private static final int PANEL_BORDER = 0xFF8B4513;
    private static final int XP_BG_COLOR = 0xFFB8965A;
    private static final int XP_GRADIENT_START = 0xFF8B4513;
    private static final int XP_GRADIENT_END = 0xFFD2691E;
    private static final int LEVEL_COLOR = 0xFF3A1A00;

    private static final int XP_BAR_WIDTH = 100;
    private static final int XP_BAR_HEIGHT = 4;

    private final HudBar xpBar = new HudBar(XP_BAR_WIDTH, XP_BAR_HEIGHT, 0.08f);
    private final LerpedValue levelLerp = new LerpedValue(0, 0.1f);
    private int lastLevel = -1;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        int globalLevel = ClientStatsCache.getGlobalLevel();
        float xpProgress = ClientStatsCache.getGlobalXpProgress();

        levelLerp.chase(globalLevel);
        xpBar.setFill(xpProgress);

        int x = 4;
        int y = 4;

        // Panel background
        graphics.fill(x - 2, y - 2, x + XP_BAR_WIDTH + 6, y + 26, PANEL_BG);
        // Panel border
        graphics.fill(x - 2, y - 2, x + XP_BAR_WIDTH + 6, y - 1, PANEL_BORDER);
        graphics.fill(x - 2, y + 25, x + XP_BAR_WIDTH + 6, y + 26, PANEL_BORDER);
        graphics.fill(x - 2, y - 2, x - 1, y + 26, PANEL_BORDER);
        graphics.fill(x + XP_BAR_WIDTH + 5, y - 2, x + XP_BAR_WIDTH + 6, y + 26, PANEL_BORDER);

        var font = Minecraft.getInstance().font;

        // Level text
        String levelText = "\u2726 Niveau " + globalLevel;
        graphics.drawString(font, levelText, x, y, LEVEL_COLOR);

        // XP bar with gradient
        graphics.fill(x, y + 12, x + XP_BAR_WIDTH, y + 12 + XP_BAR_HEIGHT, XP_BG_COLOR);
        xpBar.renderGradient(graphics, x, y + 12, XP_GRADIENT_START, XP_GRADIENT_END);

        // XP percentage
        String pctText = Math.round(xpProgress * 100) + "%";
        graphics.drawString(font, pctText, x + XP_BAR_WIDTH + 4, y + 10, 0xFF6B4C1E);

        if (globalLevel > lastLevel && lastLevel >= 0) {
            graphics.fill(0, 0, screenWidth, screenHeight, 0x60FFFFFF);
        }
        lastLevel = globalLevel;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        INSTANCE.levelLerp.tick();
        INSTANCE.xpBar.tick();
    }
}
```

- [ ] **Step 2: Rewrite SurvivalOverlay**

```java
package tong.statmod.client.hud.overlays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.hud.components.HudBar;

@Mod.EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SurvivalOverlay implements IGuiOverlay {
    public static final SurvivalOverlay INSTANCE = new SurvivalOverlay();

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 6;
    private static final int BAR_BORDER_COLOR = 0xFF8B4513;
    private static final int LABEL_COLOR = 0xFF3A1A00;

    private final HudBar healthBar = new HudBar(BAR_WIDTH, BAR_HEIGHT, 0.12f);
    private final HudBar foodBar = new HudBar(BAR_WIDTH, BAR_HEIGHT, 0.10f);
    private final HudBar fatigueBar = new HudBar(BAR_WIDTH, BAR_HEIGHT, 0.15f);
    private final HudBar thirstBar = new HudBar(BAR_WIDTH, BAR_HEIGHT, 0.15f);

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        float health = mc.player.getHealth() / mc.player.getMaxHealth();
        float food = mc.player.getFoodData().getFoodLevel() / 20.0f;
        float fatigue = ClientStatsCache.getFatigue() / (float) ClientStatsCache.getMaxFatigue();
        float thirst = ClientStatsCache.getThirst() / 100.0f;

        healthBar.setFill(health);
        foodBar.setFill(food);
        fatigueBar.setFill(fatigue);
        thirstBar.setFill(thirst);

        var font = Minecraft.getInstance().font;

        int x = 4;
        int y = 32;

        renderBar(graphics, font, x, y, healthBar, "\u2665", health, 0xFF4444, 0xFF4444, (int)(health * 100) + "%");
        y += 10;
        renderBar(graphics, font, x, y, foodBar, "\u2615", food, 0xFFA500, 0xFFA500, mc.player.getFoodData().getFoodLevel() + "/20");
        y += 10;
        renderBar(graphics, font, x, y, fatigueBar, "\u26A1", fatigue, getFatigueColor(fatigue), getFatigueColor(fatigue), (int)(fatigue * 100) + "%");
        y += 10;
        renderBar(graphics, font, x, y, thirstBar, "\uD83D\uDCA7", thirst, 0x3399FF, 0x3399FF, (int) ClientStatsCache.getThirst() + "/100");
    }

    private void renderBar(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                           int x, int y, HudBar bar, String icon, float value,
                           int fillColor, int gradientEnd, String label) {
        graphics.drawString(font, icon, x, y, LABEL_COLOR);
        bar.renderWithBorder(graphics, x + 10, y, fillColor, BAR_BORDER_COLOR);
        String valStr = label;
        graphics.drawString(font, valStr, x + 10 + BAR_WIDTH + 4, y, 0xFF6B4C1E);
    }

    private int getFatigueColor(float fatiguePercent) {
        if (fatiguePercent < 0.25f) return 0x00AA00;
        if (fatiguePercent < 0.50f) return 0xFFD700;
        if (fatiguePercent < 0.75f) return 0xFF8C00;
        return 0xFF0000;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        INSTANCE.healthBar.tick();
        INSTANCE.foodBar.tick();
        INSTANCE.fatigueBar.tick();
        INSTANCE.thirstBar.tick();
    }
}
```

- [ ] **Step 3: Verify compile**

Run: `.\gradlew build 2>&1 | findstr /C:"error"`
Expected: no errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/client/hud/overlays/
git commit -m "feat(hud): parchment style HUD at top-left with borders and new colors"
```

---

### Task 6: PerkNodeWidget

**Files:**
- Create: `src/main/java/tong/statmod/client/gui/perks/PerkNodeWidget.java`

- [ ] **Step 1: Create PerkNodeWidget**

```java
package tong.statmod.client.gui.perks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.perks.Perk;

public class PerkNodeWidget extends AbstractWidget {
    private static final int NODE_SIZE = 24;
    private static final int LOCKED_BG = 0xFF666666;
    private static final int LOCKED_BORDER = 0xFF555555;
    private static final int AVAILABLE_BG = 0xFFD4C494;
    private static final int AVAILABLE_BORDER = 0xFFC49A3C;
    private static final int UNLOCKED_BG = 0xFFD4C494;
    private static final int UNLOCKED_BORDER = 0xFFFFD700;

    private final Perk perk;
    private final PerkNodeState state;

    public enum PerkNodeState { LOCKED, AVAILABLE, UNLOCKED }

    public PerkNodeWidget(Perk perk, PerkNodeState state, int x, int y) {
        super(x, y, NODE_SIZE, NODE_SIZE, Component.literal(perk.name));
        this.perk = perk;
        this.state = state;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cx = getX() + NODE_SIZE / 2;
        int cy = getY() + NODE_SIZE / 2;
        int r = NODE_SIZE / 2;

        int bgColor, borderColor;
        boolean showGlow = false;

        switch (state) {
            case UNLOCKED:
                bgColor = UNLOCKED_BG;
                borderColor = UNLOCKED_BORDER;
                showGlow = true;
                break;
            case AVAILABLE:
                bgColor = AVAILABLE_BG;
                borderColor = AVAILABLE_BORDER;
                showGlow = true;
                break;
            case LOCKED:
            default:
                bgColor = LOCKED_BG;
                borderColor = LOCKED_BORDER;
                showGlow = false;
                break;
        }

        // Glow
        if (showGlow && state == PerkNodeState.AVAILABLE) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() / 300.0);
            int glowAlpha = (int)(60 * pulse);
            int glowColor = (glowAlpha << 24) | (0xC49A3C & 0x00FFFFFF);
            graphics.fill(cx - r - 2, cy - r - 2, cx + r + 2, cy - r, glowColor);
            graphics.fill(cx - r - 2, cy + r, cx + r + 2, cy + r + 2, glowColor);
            graphics.fill(cx - r - 2, cy - r, cx - r, cy + r, glowColor);
            graphics.fill(cx + r, cy - r, cx + r + 2, cy + r, glowColor);
        } else if (showGlow && state == PerkNodeState.UNLOCKED) {
            int glowColor = 0x40FFD700;
            graphics.fill(cx - r - 2, cy - r - 2, cx + r + 2, cy - r, glowColor);
            graphics.fill(cx - r - 2, cy + r, cx + r + 2, cy + r + 2, glowColor);
            graphics.fill(cx - r - 2, cy - r, cx - r, cy + r, glowColor);
            graphics.fill(cx + r, cy - r, cx + r + 2, cy + r, glowColor);
        }

        // Circle approximation via filled squares
        graphics.fill(cx - r, cy - r, cx + r, cy + r, bgColor);
        graphics.fill(cx - r, cy - r, cx + r, cy - r + 1, borderColor);
        graphics.fill(cx - r, cy + r - 1, cx + r, cy + r, borderColor);
        graphics.fill(cx - r, cy - r, cx - r + 1, cy + r, borderColor);
        graphics.fill(cx + r - 1, cy - r, cx + r, cy + r, borderColor);

        // Level requirement text inside
        var font = Minecraft.getInstance().font;
        String lvlText = String.valueOf(perk.levelRequired);
        int textColor = state == PerkNodeState.LOCKED ? 0xFFAAAAAA : 0xFF3A1A00;
        graphics.drawString(font, lvlText, cx - font.width(lvlText) / 2, cy - 3, textColor);

        // Checkmark for unlocked
        if (state == PerkNodeState.UNLOCKED) {
            graphics.drawString(font, "\u2713", cx + 5, cy - 8, 0xFFFFD700);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    public Perk getPerk() { return perk; }
    public PerkNodeState getState() { return state; }
}
```

- [ ] **Step 2: Verify compile**

Run: `.\gradlew build 2>&1 | findstr /C:"error"`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/perks/PerkNodeWidget.java
git commit -m "feat(gui): PerkNodeWidget with circular node rendering and glow states"
```

---

### Task 7: TalentTreePanel

**Files:**
- Create: `src/main/java/tong/statmod/client/gui/perks/TalentTreePanel.java`

- [ ] **Step 1: Create TalentTreePanel**

```java
package tong.statmod.client.gui.perks;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

public class TalentTreePanel extends AbstractWidget {
    private static final int PANEL_BORDER_COLOR = 0xFF8B4513;
    private static final int STAT_NAME_COLOR = 0xFF3A1A00;
    private static final int CONNECTOR_COLOR = 0xFFC49A3C;
    private static final int CONNECTOR_LOCKED_COLOR = 0xFF666666;
    private static final int NODE_GAP = 40;
    private static final int STAT_GAP = 55;

    private final StatCategory category;
    private final List<PerkNodeWidget> nodeWidgets = new ArrayList<>();
    private int scrollOffset = 0;

    public TalentTreePanel(StatCategory category, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Talent Tree"));
        this.category = category;
        buildTree();
    }

    private void buildTree() {
        nodeWidgets.clear();
        int rowY = getY() + 10;

        for (StatType stat : StatType.values()) {
            if (stat.category != category) continue;

            int statLevel = ClientStatsCache.getLevel(stat);
            int centerX = getX() + getWidth() / 2;
            int statLabelX = centerX - Minecraft.getInstance().font.width(stat.displayName) / 2;

            // Perk nodes for this stat
            List<Perk> statPerks = new ArrayList<>();
            for (Perk perk : Perk.values()) {
                if (perk.stat == stat) statPerks.add(perk);
            }

            // Sort by levelRequired (already in order: 20, 50, 80)
            int nodeStartX = centerX - (statPerks.size() * NODE_GAP) / 2;
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
                nodeWidgets.add(widget);
            }

            rowY += STAT_GAP;
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Panel border
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, PANEL_BORDER_COLOR);
        graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), PANEL_BORDER_COLOR);
        graphics.fill(getX(), getY(), getX() + 1, getY() + getHeight(), PANEL_BORDER_COLOR);
        graphics.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), PANEL_BORDER_COLOR);

        var font = Minecraft.getInstance().font;

        int rowY = getY() + 10 - scrollOffset;
        int centerX = getX() + getWidth() / 2;

        for (StatType stat : StatType.values()) {
            if (stat.category != category) continue;

            // Stat name
            String statName = stat.displayName;
            int statLevel = ClientStatsCache.getLevel(stat);
            String header = statName + "  \u2605" + statLevel;
            graphics.drawString(font, header, centerX - font.width(header) / 2, rowY, STAT_NAME_COLOR);

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
                int color = (leftUnlocked || rightUnlocked) ? CONNECTOR_COLOR : CONNECTOR_LOCKED_COLOR;

                int lx = nodeStartX + i * NODE_GAP + 24;
                int rx = nodeStartX + (i + 1) * NODE_GAP;
                int cy = rowY + 32;
                graphics.fill(lx, cy, rx, cy + 1, color);
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
```

- [ ] **Step 2: Verify compile**

Run: `.\gradlew build 2>&1 | findstr /C:"error"`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/perks/TalentTreePanel.java
git commit -m "feat(gui): TalentTreePanel with stat rows, connector lines, and perk nodes"
```

---

### Task 8: PerkScreen Overhaul — Tree View

**Files:**
- Modify: `src/main/java/tong/statmod/client/gui/PerkScreen.java`

- [ ] **Step 1: Rewrite PerkScreen**

```java
package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.client.texture.TextureCache;
import tong.statmod.client.gui.perks.TalentTreePanel;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatCategory;
import tong.statmod.stats.StatType;

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
    private static final int PANEL_X = 20;
    private static final int PANEL_Y = 55;
    private static final int PANEL_PADDING = 4;

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
        clearWidgets();
        StatCategory category = tabCategories[selectedTab];
        int panelW = this.width - 40;
        int panelH = this.height - PANEL_Y - 20;
        treePanel = new TalentTreePanel(category, PANEL_X, PANEL_Y, panelW, panelH);
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
        graphics.drawString(font, title, titleX, 8, TEXT_COLOR);

        // Flanking lines
        int lineEnd = titleX - 10;
        if (lineEnd > PANEL_X) graphics.fill(PANEL_X, 12, lineEnd, 13, SEPARATOR_COLOR);
        int lineStart = titleX + font.width(title) + 10;
        if (lineStart < this.width - PANEL_X) graphics.fill(lineStart, 12, this.width - PANEL_X, 13, SEPARATOR_COLOR);

        // Points display
        String pointsText = "Points: " + ClientPerkCache.getAvailablePoints();
        graphics.drawString(font, pointsText, this.width - PANEL_X - font.width(pointsText), 8, POINTS_COLOR);

        // Tabs
        int tabX = this.width / 2 - (TAB_COUNT * 60) / 2;
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean active = i == selectedTab;
            boolean isMagicTab = i == 1;

            int bg = active ? 0xFFD4C494 : 0xFFC4A86A;
            int border = active ? 0xFFC49A3C : 0xFF8B4513;

            graphics.fill(tabX, 23, tabX + 55, 40, bg);
            graphics.fill(tabX, 23, tabX + 55, 24, border);
            graphics.fill(tabX, 39, tabX + 55, 40, border);
            graphics.fill(tabX, 23, tabX + 1, 40, border);
            graphics.fill(tabX + 54, 23, tabX + 55, 40, border);

            int textColor = active ? 0xFF3A1A00 : 0xFF5A3A10;
            String label = isMagicTab ? tabNames[i] + " \u269C" : tabNames[i];
            graphics.drawString(font, label, tabX + 27 - font.width(label) / 2, 28, textColor);
            tabX += 60;
        }

        // Separator below tabs
        graphics.fill(PANEL_X, 42, this.width - PANEL_X, 43, SEPARATOR_COLOR);

        // Render tree panel
        treePanel.setScrollOffset(scrollOffset);
        treePanel.renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tabX = this.width / 2 - (TAB_COUNT * 60) / 2;
        for (int i = 0; i < TAB_COUNT; i++) {
            if (mouseX >= tabX && mouseX <= tabX + 55 && mouseY >= 23 && mouseY <= 40) {
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
```

- [ ] **Step 2: Verify compile**

Run: `.\gradlew build 2>&1 | findstr /C:"error"`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/PerkScreen.java
git commit -m "feat(gui): PerkScreen with talent tree panel and parchment styling"
```

---

### Task 9: Build Verification

- [ ] **Step 1: Full build**

Run: `.\gradlew build 2>&1 | findstr /C:"error" /C:"BUILD"`
Expected: "BUILD SUCCESSFUL" with no errors

- [ ] **Step 2: Verify all new files exist**

```bash
dir /s /b src\main\java\tong\statmod\client\texture\*.java
dir /s /b src\main\java\tong\statmod\client\gui\perks\*.java
```

Expected: both files exist

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat(gui): complete parchment theme GUI overhaul"
```
