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
    private static final int VALUE_COLOR = 0xFF6B4C1E;

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

        renderBar(graphics, font, x, y, healthBar, "\u2665", health, 0xFF4444, (int)(health * 100) + "%");
        y += 10;
        renderBar(graphics, font, x, y, foodBar, "\u2615", food, 0xFFA500, mc.player.getFoodData().getFoodLevel() + "/20");
        y += 10;
        renderBar(graphics, font, x, y, fatigueBar, "\u26A1", fatigue, getFatigueColor(fatigue), (int)(fatigue * 100) + "%");
        y += 10;
        renderBar(graphics, font, x, y, thirstBar, "\uD83D\uDCA7", thirst, 0x3399FF, (int) ClientStatsCache.getThirst() + "/100");
    }

    private void renderBar(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                           int x, int y, HudBar bar, String icon, float value,
                           int fillColor, String label) {
        graphics.drawString(font, icon, x, y, LABEL_COLOR);
        bar.renderWithBorder(graphics, x + 10, y, fillColor, BAR_BORDER_COLOR);
        graphics.drawString(font, label, x + 10 + BAR_WIDTH + 4, y, VALUE_COLOR);
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
