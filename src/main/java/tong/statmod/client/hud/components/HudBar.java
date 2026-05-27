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
