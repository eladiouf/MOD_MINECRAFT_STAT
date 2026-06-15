package tong.statmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import tong.statmod.integration.PlayerDataBridge;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

@OnlyIn(Dist.CLIENT)
public final class StatHudOverlay {
    private StatHudOverlay() {}

    public static boolean visible;
    public static int scrollOffset;

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath("statmod", "stats_overlay"),
                StatHudOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        int soulLevel = ClientStatCache.getSoulLevel();
        int maxStat = soulLevel > 0 ? Math.min(soulLevel, 100) : 100;

        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = screenW - 180;
        int y = 10 + scrollOffset;
        int lineH = font.lineHeight + 2;
        int maxLines = (screenH - 20) / lineH;

        RaceDisplay raceInfo = getRaceDisplay(player);
        String header = String.format("\u00a7e\u2726 Tensura \u2726\u00a7r %s | Soul %d | Cap %d",
                raceInfo.display, soulLevel, maxStat);

        int boxW = 172;
        int headerH = lineH + 4;
        int contentH = Math.min(StatType.values().length, maxLines - 1) * lineH;
        int totalH = headerH + contentH + 6;

        RenderSystem.enableBlend();
        graphics.fill(x - 2, y - 2, x + boxW, y + totalH, 0x88000000);
        RenderSystem.disableBlend();

        graphics.drawString(font, header, x, y, 0xFFFFAA00);
        y += headerH;

        int shown = 0;
        for (StatType stat : StatType.values()) {
            if (shown >= maxLines - 1) break;
            int level = ClientStatCache.getLevel(stat.index);
            int xp = ClientStatCache.getXp(stat.index);
            int needed = PlayerStatData.requiredXp(level);
            int color = stat.hasPerks() ? 0xFFFFFFFF : 0xFF808080;
            if (level >= maxStat) color = 0xFFFFAA00;
            String line = String.format("%s: Lv.%d (%d/%d)", stat.displayName, level, xp, needed);
            graphics.drawString(font, line, x, y, color);
            y += lineH;
            shown++;
        }
    }

    private static RaceDisplay getRaceDisplay(Player player) {
        try {
            String raceId = PlayerDataBridge.getRaceId(player);
            if (raceId != null && !raceId.isEmpty()) {
                return new RaceDisplay(raceId.replace("tensura:", ""), 0xFFAAFFAA);
            }
        } catch (Exception ignored) {}
        return new RaceDisplay("Unknown", 0xFF808080);
    }

    private record RaceDisplay(String display, int color) {}
}
