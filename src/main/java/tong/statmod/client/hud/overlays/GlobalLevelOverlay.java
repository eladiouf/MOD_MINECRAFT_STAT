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
public class GlobalLevelOverlay implements IGuiOverlay {
    public static final GlobalLevelOverlay INSTANCE = new GlobalLevelOverlay();

    private static final int TITLE_COLOR = 0xFFF6E7A8;
    private static final int TEXT_COLOR = 0xFFDDDDEE;
    private static final int PANEL_COLOR = 0xCC111018;
    private static final int PANEL_BORDER = 0xFF34223E;
    private static final int BORDER_ACCENT = 0xFFAA6DFF;
    private static final int XP_START = 0xFF4A00E0;
    private static final int XP_END = 0xFF8E2DE2;

    private final HudBar xpBar = new HudBar(GlobalLevelHudLayout.BAR_WIDTH, GlobalLevelHudLayout.BAR_HEIGHT, 0.18f);
    private static int lastGlobalLevel = -1;
    private static long levelUpTime = 0;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.options.hideGui) return;

        int globalLevel = ClientStatsCache.getGlobalLevel();
        float progress = Math.max(0f, Math.min(1f, ClientStatsCache.getGlobalXpProgress()));

        if (globalLevel > lastGlobalLevel && lastGlobalLevel != -1) {
            levelUpTime = System.currentTimeMillis();
        }
        lastGlobalLevel = globalLevel;

        long now = System.currentTimeMillis();
        int x = GlobalLevelHudLayout.X;
        int y = GlobalLevelHudLayout.Y;

        graphics.fill(x - 2, y - 2, x + GlobalLevelHudLayout.BAR_WIDTH + 2, y + GlobalLevelHudLayout.PANEL_HEIGHT, PANEL_COLOR);
        graphics.fill(x - 1, y - 1, x + GlobalLevelHudLayout.BAR_WIDTH + 1, y, PANEL_BORDER);
        graphics.fill(x - 1, y, x, y + GlobalLevelHudLayout.PANEL_HEIGHT, PANEL_BORDER);
        graphics.fill(x + GlobalLevelHudLayout.BAR_WIDTH, y, x + GlobalLevelHudLayout.BAR_WIDTH + 1, y + GlobalLevelHudLayout.PANEL_HEIGHT, PANEL_BORDER);
        graphics.fill(x - 1, y + GlobalLevelHudLayout.PANEL_HEIGHT, x + GlobalLevelHudLayout.BAR_WIDTH + 1, y + GlobalLevelHudLayout.PANEL_HEIGHT + 1, PANEL_BORDER);

        if (now - levelUpTime < 1000) {
            int alpha = (int)(120 * (1f - (now - levelUpTime) / 1000.0f));
            graphics.fill(x - 2, y - 2, x + GlobalLevelHudLayout.BAR_WIDTH + 2, y + GlobalLevelHudLayout.PANEL_HEIGHT + 1,
                (alpha << 24) | 0xFFE8D27A);
        }

        String levelText = "Niveau " + globalLevel;
        int textColor = globalLevel >= 80 ? 0xFFF6C35C : globalLevel >= 50 ? 0xFFDDDDEE : TITLE_COLOR;
        if (now - levelUpTime < 1000) {
            textColor = 0xFFFFF0A8;
        }
        graphics.drawString(mc.font, levelText, x + 4, y + GlobalLevelHudLayout.TITLE_Y, textColor, false);

        xpBar.setFill(progress);
        xpBar.renderGradient(graphics, x, y + GlobalLevelHudLayout.BAR_Y, XP_START, XP_END);
        graphics.fill(x - 1, y + GlobalLevelHudLayout.BAR_Y - 1, x + GlobalLevelHudLayout.BAR_WIDTH + 1, y + GlobalLevelHudLayout.BAR_Y, BORDER_ACCENT);

        String pctText = Math.round(progress * 100) + "%";
        graphics.drawString(mc.font, pctText, x + GlobalLevelHudLayout.BAR_WIDTH + 4, y + GlobalLevelHudLayout.BAR_Y - 1, TEXT_COLOR, false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        INSTANCE.xpBar.tick();
    }
}
