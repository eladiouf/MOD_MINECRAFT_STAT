package tong.statmod.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.awt.Color;

public class ComboOverlay implements IGuiOverlay {
    public static final ComboOverlay INSTANCE = new ComboOverlay();

    private int comboCount = 0;
    private long lastHitTime = 0;
    private static final long FADE_OUT = 3000;

    public void registerHit() {
        this.comboCount++;
        this.lastHitTime = System.currentTimeMillis();
    }

    public void reset() {
        this.comboCount = 0;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        long elapsed = System.currentTimeMillis() - lastHitTime;
        if (elapsed > FADE_OUT || comboCount == 0) {
            comboCount = 0;
            return;
        }

        String text = comboCount + " hits";
        float alpha = Math.max(0, 1.0f - (elapsed / (float) FADE_OUT));
        int color = switch ((comboCount / 10) % 4) {
            case 0 -> Color.WHITE.getRGB();
            case 1 -> Color.YELLOW.getRGB();
            case 2 -> Color.ORANGE.getRGB();
            default -> Color.RED.getRGB();
        };

        int x = screenWidth / 2 - 20;
        int y = screenHeight / 4 - 10;
        graphics.setColor(1, 1, 1, alpha);
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color);
        graphics.setColor(1, 1, 1, 1);
    }
}
