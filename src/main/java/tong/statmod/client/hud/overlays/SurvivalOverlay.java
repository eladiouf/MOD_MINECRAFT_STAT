package tong.statmod.client.hud.overlays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.hud.components.HudBar;
import tong.statmod.client.texture.TextureCache;

import static tong.statmod.client.texture.TextureCache.drawInkText;

@Mod.EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SurvivalOverlay implements IGuiOverlay {
    public static final SurvivalOverlay INSTANCE = new SurvivalOverlay();
    public static final int PANEL_START_Y = 32;

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

        int x = 4;
        int y = PANEL_START_Y;

        renderBarWithIcon(graphics, x, y, healthBar, "hud_heart.png", 0xFF4444, (int)(health * 100) + "%", 228);
        y += 10;
        renderBarWithIcon(graphics, x, y, foodBar, "hud_food.png", 0xFFA500, mc.player.getFoodData().getFoodLevel() + "/20", 228);
        y += 10;
        renderBarWithIcon(graphics, x, y, fatigueBar, "hud_fatigue.png", getFatigueColor(fatigue), (int)(fatigue * 100) + "%", 32);
        y += 10;
        renderBarWithIcon(graphics, x, y, thirstBar, "hud_thirst.png", 0x3399FF, (int) ClientStatsCache.getThirst() + "/100", 32);
    }

    private void renderBarWithIcon(GuiGraphics graphics, int x, int y, HudBar bar,
                                    String iconName, int fillColor, String label, int texSize) {
        ResourceLocation tex = TextureCache.get(iconName);
        graphics.blit(tex, x, y, 7, 7, 0, 0, texSize, texSize, texSize, texSize);
        int darkColor = darken(fillColor, 0.5f);
        bar.renderGradientWithBorder(graphics, x + 10, y, fillColor, darkColor, BAR_BORDER_COLOR);
        drawInkText(graphics, Minecraft.getInstance().font, label, x + 10 + BAR_WIDTH + 4, y, VALUE_COLOR);
    }

    private static int darken(int color, float factor) {
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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
