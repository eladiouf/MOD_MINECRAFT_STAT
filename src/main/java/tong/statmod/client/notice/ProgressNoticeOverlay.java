package tong.statmod.client.notice;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.client.stats.StatPresentation;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ProgressNoticeOverlay {
    private static final int WIDTH = 160;
    private static final int HEIGHT = 20;
    private static final int GAP = 3;
    private static final int BACKGROUND_ALPHA = 110;
    private static final int BORDER_ALPHA = 150;
    private static final int TEXT_ALPHA = 200;

    private ProgressNoticeOverlay() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("progress_notices", ProgressNoticeOverlay::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
            int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.renderDebug) {
            return;
        }
        List<ProgressNotice> notices = ClientProgressNotices.snapshot();
        int x = screenWidth - WIDTH - 8;
        int y = 8;
        for (ProgressNotice notice : notices) {
            int backgroundAlpha = Math.round(notice.alpha() * BACKGROUND_ALPHA);
            int borderAlpha = Math.round(notice.alpha() * BORDER_ALPHA);
            int textAlpha = Math.round(notice.alpha() * TEXT_ALPHA);
            int background = backgroundAlpha << 24 | 0x241A12;
            int border = borderAlpha << 24 | 0xB18A4A;
            graphics.fill(x, y, x + WIDTH, y + HEIGHT, background);
            graphics.fill(x, y, x + 2, y + HEIGHT, border);

            Component statName = Component.translatable(
                    StatPresentation.of(notice.stat()).nameKey());
            Component text = Component.translatable(
                    "notice.statmod.level_up", statName, notice.newLevel());
            int foreground = textAlpha << 24 | 0xE2CF9E;
            graphics.drawString(minecraft.font, text, x + 6, y + 6, foreground, false);
            y += HEIGHT + GAP;
        }
    }
}
