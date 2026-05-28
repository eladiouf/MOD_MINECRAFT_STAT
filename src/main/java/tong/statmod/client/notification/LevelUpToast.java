package tong.statmod.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;

@Mod.EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT)
public class LevelUpToast {
    private static String activeText = "";
    private static long startTime = 0;
    private static final long DURATION = 3000;

    public static void show(String text) {
        activeText = text;
        startTime = System.currentTimeMillis();
    }

    public static void onLevelUp(String statName, int newLevel) {
        String msg;
        if (newLevel == 100) {
            msg = "§6✦ " + statName + " §e→ §6NIVEAU MAX §6✦";
        } else if (newLevel % 10 == 0) {
            msg = "§e★ " + statName + " §e→ Palier " + newLevel + " ★";
        } else {
            msg = "§6" + statName + " → Level " + newLevel + "!";
        }
        show(msg);
    }

    @SubscribeEvent
    public static void onRenderOverlay(CustomizeGuiOverlayEvent event) {
        if (activeText.isEmpty()) return;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > DURATION) {
            activeText = "";
            return;
        }

        float alpha = 1.0f;
        if (elapsed > DURATION - 500) {
            alpha = (DURATION - elapsed) / 500.0f;
        }

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        GuiGraphics graphics = event.getGuiGraphics();
        int x = screenWidth / 2 - 60;
        int y = screenHeight / 2 + 40;

        graphics.setColor(1, 1, 1, alpha);
        graphics.drawString(Minecraft.getInstance().font, activeText, x, y, 0xFFFF00);
        graphics.setColor(1, 1, 1, 1);
    }
}
