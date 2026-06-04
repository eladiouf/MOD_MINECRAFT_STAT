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

@Mod.EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlobalLevelOverlay implements IGuiOverlay {
    public static final GlobalLevelOverlay INSTANCE = new GlobalLevelOverlay();
    private static final ResourceLocation XP_BAR = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "gui/xp_bar_fill");
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
    private static int lastGlobalLevel = -1;
    private static long levelUpTime = 0;
    private static int comboCount = 0;
    private static long lastHitTime = 0;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.options.hideGui) return;

        int globalLevel = ClientStatsCache.getGlobalLevel();
        float progress = ClientStatsCache.getGlobalXpProgress();

        if (globalLevel > lastGlobalLevel && lastGlobalLevel != -1) {
            levelUpTime = System.currentTimeMillis();
        }
        lastGlobalLevel = globalLevel;

        long now = System.currentTimeMillis();
        if (comboCount > 0 && now - lastHitTime > 3000) {
            comboCount = 0;
        }

        int barX = screenWidth / 2 - BAR_WIDTH / 2;
        int barY = 2;
        graphics.fill(barX - 2, barY - 2, barX + BAR_WIDTH + 2, barY + BAR_HEIGHT + 2, 0x80000000);

        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF333333);
        int fillWidth = (int)(BAR_WIDTH * progress);
        int barColor = 0xFF44AA44;
        if (progress >= 0.9f) barColor = 0xFFFFAA00;
        if (progress >= 1.0f) barColor = 0xFF55FFFF;
        graphics.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, barColor);

        String levelText = "Lv." + globalLevel;
        int textColor = globalLevel >= 80 ? 0xFFFFAA00 : globalLevel >= 50 ? 0xFFAAAAFF : 0xFFFFFFFF;
        float animScale = 1.0f;
        if (now - levelUpTime < 1000) {
            animScale = 1.0f + (1.0f - (now - levelUpTime) / 1000.0f) * 0.5f;
            textColor = 0xFF55FF55;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(barX, barY - 12, 0);
        graphics.pose().scale(animScale, animScale, 1);
        graphics.drawString(mc.font, levelText, (BAR_WIDTH - mc.font.width(levelText)) / 2, 0, textColor);
        graphics.pose().popPose();

        String pctText = (int)(progress * 100) + "%";
        graphics.drawString(mc.font, pctText, barX + BAR_WIDTH + 4, barY, 0xFF888888);

        if (comboCount > 0) {
            String comboText = comboCount + "x COMBO";
            int alpha = (int)(Math.max(0, (3000 - (now - lastHitTime)) / 3000.0) * 255);
            int comboColor = (alpha << 24) | 0xFFFF5500;
            graphics.drawString(mc.font, comboText, barX, barY + BAR_HEIGHT + 4, comboColor);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
    }

    public static void onHit() {
        comboCount++;
        lastHitTime = System.currentTimeMillis();
    }

    public static void resetCombo() { comboCount = 0; }
}
