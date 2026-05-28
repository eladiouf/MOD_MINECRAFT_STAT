package tong.statmod.client.texture;

import net.minecraft.client.gui.Font;
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
        graphics.blit(tex, x, y, 0, 0, b, b, texWidth, texHeight);
        graphics.blit(tex, x + w - b, y, texWidth - b, 0, b, b, texWidth, texHeight);
        graphics.blit(tex, x, y + h - b, 0, texHeight - b, b, b, texWidth, texHeight);
        graphics.blit(tex, x + w - b, y + h - b, texWidth - b, texHeight - b, b, b, texWidth, texHeight);

        // Edges
        graphics.blit(tex, x + b, y, innerW, b, b, 0, srcInnerW, b, texWidth, texHeight);
        graphics.blit(tex, x + b, y + h - b, innerW, b, b, texHeight - b, srcInnerW, b, texWidth, texHeight);
        graphics.blit(tex, x, y + b, b, innerH, 0, b, b, srcInnerH, texWidth, texHeight);
        graphics.blit(tex, x + w - b, y + b, b, innerH, texWidth - b, b, b, srcInnerH, texWidth, texHeight);

        // Center
        graphics.blit(tex, x + b, y + b, innerW, innerH, b, b, srcInnerW, srcInnerH, texWidth, texHeight);
    }

    public static void drawInkText(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        int shadowColor = (color & 0x00FFFFFF) | 0x40000000;
        graphics.drawString(font, text, x + 1, y, shadowColor, false);
        graphics.drawString(font, text, x, y + 1, shadowColor, false);
        graphics.drawString(font, text, x, y, color, false);
    }
}
