package tong.statmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.dungeon.DungeonDimensions;
import tong.statmod.dungeon.DungeonTeleportHandler;
import tong.statmod.dungeon.DungeonThemes;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DungeonHudOverlay {

    private DungeonHudOverlay() {}

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("dungeon_overlay", DungeonHudOverlay::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
            int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (!player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        Font font = mc.font;
        int floor = DungeonTeleportHandler.floorAtPos(player.getBlockX(), player.getBlockZ());
        
        // Retrieve values from the local ClientStatsState
        ClientStatsState state = ClientStatsCache.state();
        int maxFloor = state.dungeonFloorReached();
        int dungeonPoints = state.dungeonPoints();
        String type = floorType(floor);
        String tier = tierName(floor);
        int nextBoss = ((floor / 10) + 1) * 10;

        // Étage conquis si la sortie est déjà débloquée (floorReached > floor courant).
        boolean conquered = maxFloor > floor;

        String title = "§6☠ §lTrial Dungeon";
        String line1 = "§fFloor " + floor + " §7· " + type;
        String line2 = tier + " §7· Boss §f#" + nextBoss + " §7· Max §f" + maxFloor;
        String points = "§e✦ §fPoints: §e" + dungeonPoints;
        String line3 = objectiveLine(floor, conquered);

        // Nom du thème de l'étage (chaque étage a le sien).
        DungeonThemes.Theme theme = DungeonThemes.forFloor(floor);

        // Compteur d'ennemis restants (étages de combat non conquis) — lisibilité de l'objectif.
        // Compté côté client : le donjon n'a aucun spawn naturel, tous les hostiles sont les nôtres.
        java.util.List<String> lineList = new java.util.ArrayList<>(java.util.List.of(
                title, line1, "§7» " + theme.displayName(), line2, points, line3));
        boolean combatFloor = floor > 0 && floor % 10 != 0 && floor % 5 != 0;
        if (combatFloor && !conquered) {
            int enemies = countEnemies(mc, player);
            if (enemies > 0) lineList.add("§c⚔ §fEnemies: §c" + enemies);
        }
        String[] lines = lineList.toArray(new String[0]);

        float scale = 0.75f;
        int lineH = (int)(font.lineHeight * scale) + 2;
        int boxW = 0;
        for (String s : lines) boxW = Math.max(boxW, (int)(font.width(s) * scale));
        boxW += 8;
        int boxH = lineH * lines.length + 4;
        int x = screenWidth - boxW - 4;
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

        for (int i = 0; i < lines.length; i++) {
            graphics.drawString(font, lines[i], 0, (font.lineHeight + 1) * i, 0xFFFFFF);
        }

        pose.popPose();
    }

    /** Compte les ennemis (hostiles) proches du joueur — le donjon n'ayant aucun spawn naturel,
     *  tous les {@link net.minecraft.world.entity.monster.Enemy} chargés sont ceux de la vague. */
    private static int countEnemies(Minecraft mc, Player player) {
        if (mc.level == null) return 0;
        net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(90.0);
        return mc.level.getEntities(player, area,
                e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive()).size();
    }

    /** Ligne d'objectif : ce qu'il reste à accomplir, ou « conquis » si la sortie est ouverte. */
    private static String objectiveLine(int floor, boolean conquered) {
        if (conquered) return "§a✔ Conquered §7· exit open";
        if (floor % 10 == 0) return "§c☠ Slay the boss";
        if (floor % 5 == 0) return "§e✦ Loot the vault";
        return "§e⚔ Clear all enemies";
    }

    private static String floorType(int floor) {
        if (floor % 10 == 0) return "§cBoss";
        if (floor % 5 == 0) return "§eTreasure";
        return "§aCombat";
    }

    private static String tierName(int floor) {
        if (floor <= 10) return "§aEARLY";
        if (floor <= 25) return "§eMID";
        if (floor <= 50) return "§6LATE";
        return "§cABYSS";
    }
}
