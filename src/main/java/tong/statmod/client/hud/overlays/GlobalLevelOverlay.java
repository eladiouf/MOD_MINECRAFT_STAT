package tong.statmod.client.hud.overlays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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
import tong.statmod.client.texture.TextureCache;

import static tong.statmod.client.texture.TextureCache.drawInkText;

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
        drawInkText(graphics, font, levelText, x, y, LEVEL_COLOR);

        // XP bar background and fill
        graphics.fill(x, y + 12, x + XP_BAR_WIDTH, y + 12 + XP_BAR_HEIGHT, XP_BG_COLOR);
        ResourceLocation xpFillTex = TextureCache.get("xp_bar_fill.png");
        int filledW = (int)(xpProgress * XP_BAR_WIDTH);
        if (filledW > 0) {
            graphics.blit(xpFillTex, x, y + 12, filledW, XP_BAR_HEIGHT, 0, 0, 64, 17, 64, 17);
        }

        // XP percentage
        String pctText = Math.round(xpProgress * 100) + "%";
        drawInkText(graphics, font, pctText, x + XP_BAR_WIDTH + 4, y + 10, 0xFF6B4C1E);

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
