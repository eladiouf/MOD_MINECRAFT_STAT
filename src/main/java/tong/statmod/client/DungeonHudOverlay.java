package tong.statmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import tong.statmod.dungeon.DungeonDimensions;
import tong.statmod.dungeon.DungeonTeleportHandler;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

@OnlyIn(Dist.CLIENT)
public final class DungeonHudOverlay {

    private DungeonHudOverlay() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath("statmod", "dungeon_overlay"),
                DungeonHudOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (!player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        Font font = mc.font;
        int floor = DungeonTeleportHandler.floorAtPos(player.getBlockX(), player.getBlockZ());
        PlayerStatData data = player.getData(ModAttachments.STATS);
        int maxFloor = data.getDungeonFloorReached();
        String type = floorType(floor);
        String tier = tierName(floor);
        int nextBoss = ((floor / 10) + 1) * 10;

        String title = "\u00a76\u2620 \u00a7lTrial Dungeon";
        String line1 = "\u00a7fFloor " + floor + " \u00a77\u00b7 " + type;
        String line2 = tier + " \u00a77\u00b7 Boss \u00a7f#" + nextBoss + " \u00a77\u00b7 Max \u00a7f" + maxFloor;

        int screenW = mc.getWindow().getGuiScaledWidth();
        float scale = 0.75f;
        int lineH = (int)(font.lineHeight * scale) + 2;
        int titleW = (int)(font.width(title) * scale);
        int line1W = (int)(font.width(line1) * scale);
        int line2W = (int)(font.width(line2) * scale);
        int boxW = Math.max(Math.max(titleW, line1W), line2W) + 8;
        int boxH = lineH * 3 + 4;
        int x = screenW - boxW - 4;
        int y = 4;

        // Fond semi-transparent en coordonnées écran absolues (avant tout scale/translate).
        RenderSystem.enableBlend();
        graphics.fill(x, y, x + boxW, y + boxH, 0x80000000);
        RenderSystem.disableBlend();

        // Texte : origine au coin haut-gauche du contenu puis mise à l'échelle.
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + 4, y + 4, 0);
        pose.scale(scale, scale, 1.0f);

        graphics.drawString(font, title, 0, 0, 0xFFFFFF);
        graphics.drawString(font, line1, 0, font.lineHeight + 1, 0xFFFFFF);
        graphics.drawString(font, line2, 0, (font.lineHeight + 1) * 2, 0xFFFFFF);

        pose.popPose();
    }

    private static String floorType(int floor) {
        if (floor % 10 == 0) return "\u00a7cBoss";
        if (floor % 5 == 0) return "\u00a7eTreasure";
        return "\u00a7aCombat";
    }

    private static String tierName(int floor) {
        if (floor <= 10) return "\u00a7aEARLY";
        if (floor <= 25) return "\u00a7eMID";
        if (floor <= 50) return "\u00a76LATE";
        return "\u00a7cABYSS";
    }
}
