package tong.statmod.client.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

import java.util.*;

public class StatIconRenderer {
    public static final ResourceLocation ATLAS = new ResourceLocation(STATMod.MODID, "gui/stat_icons.png");
    public static final int ICON_SIZE = 128;
    public static final int COLS = 8;
    public static final int ROWS = 3;

    private static final Map<String, int[]> CACHE = new HashMap<>();

    static {
        CACHE.put("BRUTE_FORCE", new int[]{0, 0});
        CACHE.put("BLADE_TECHNIQUE", new int[]{0, 1});
        CACHE.put("RAPIDITE", new int[]{0, 2});
        CACHE.put("AGILITY", new int[]{0, 3});
        CACHE.put("PHYSICAL_RESISTANCE", new int[]{0, 4});
        CACHE.put("PHYSICAL_ENDURANCE", new int[]{0, 5});
        CACHE.put("PRECISION", new int[]{0, 6});
        CACHE.put("ARCANE_POWER", new int[]{0, 7});
        CACHE.put("WATER_AFFINITY", new int[]{1, 0});
        CACHE.put("EARTH_AFFINITY", new int[]{1, 1});
        CACHE.put("FIRE_AFFINITY", new int[]{1, 2});
        CACHE.put("AIR_AFFINITY", new int[]{1, 3});
        CACHE.put("MAGIC_RESISTANCE", new int[]{1, 4});
        CACHE.put("CASTING_SPEED", new int[]{1, 5});
        CACHE.put("MANA_POOL", new int[]{1, 6});
        CACHE.put("ERUDITION", new int[]{1, 7});
        CACHE.put("TRACKING", new int[]{2, 0});
        CACHE.put("KEEN_SENSES", new int[]{2, 1});
        CACHE.put("FORGING", new int[]{2, 2});
        CACHE.put("COOKING", new int[]{2, 3});
        CACHE.put("ALCHEMY", new int[]{2, 4});
        CACHE.put("INTIMIDATION", new int[]{2, 5});
        CACHE.put("WILLPOWER", new int[]{2, 6});
    }

    public static void renderIcon(GuiGraphics graphics, String statName, int x, int y) {
        renderIcon(graphics, statName, x, y, 16);
    }

    public static void renderIcon(GuiGraphics graphics, String statName, int x, int y, int size) {
        int[] pos = CACHE.get(statName);
        if (pos == null) return;
        int row = pos[0], col = pos[1];
        int u = col * ICON_SIZE;
        int v = row * ICON_SIZE;
        graphics.blit(ATLAS, x, y, size, size, u, v, ICON_SIZE, ICON_SIZE, COLS * ICON_SIZE, ROWS * ICON_SIZE);
    }
}
