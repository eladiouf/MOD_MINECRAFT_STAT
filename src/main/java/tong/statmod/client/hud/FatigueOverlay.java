package tong.statmod.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import tong.statmod.client.ClientStatsCache;

import java.awt.Color;

public class FatigueOverlay implements IGuiOverlay {
    public static final FatigueOverlay INSTANCE = new FatigueOverlay();
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        float fatigue = ClientStatsCache.getFatigue();
        if (fatigue <= 0) return;

        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = screenHeight / 4 - 25;
        int filled = (int) ((fatigue / 100.0f) * BAR_WIDTH);

        Color barColor;
        if (fatigue < 25) barColor = Color.GREEN;
        else if (fatigue < 50) barColor = Color.YELLOW;
        else if (fatigue < 75) barColor = Color.ORANGE;
        else barColor = Color.RED;

        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, Color.DARK_GRAY.darker().getRGB());
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, barColor.getRGB());
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, Color.GRAY.getRGB());
        graphics.fill(x, y, x + 1, y + BAR_HEIGHT, Color.GRAY.getRGB());
        graphics.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, Color.GRAY.getRGB());
        graphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, Color.GRAY.getRGB());

        String text = String.format("%d%%", (int) fatigue);
        graphics.drawString(Minecraft.getInstance().font, text, x + BAR_WIDTH + 5, y, Color.WHITE.getRGB());
    }
}
