package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import tong.statmod.StatMod;
import tong.statmod.dungeon.DungeonDimensions;

/**
 * Ouvre lentement la Porte du Donjon quand un joueur approche (« la porte s'ouvre en grondant »)
 * et la referme quand plus personne n'est à proximité. Les battants sont des blocs d'obsidienne
 * dans le passage — pas de redstone (règle du donjon).
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class CityGateOpener {

    private static final int CHECK_EVERY_TICKS = 20;
    private static final double OPEN_RADIUS = 14.0;
    private static final double CLOSE_RADIUS = 24.0;

    private static boolean closed = true;

    private CityGateOpener() {}

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % CHECK_EVERY_TICKS != 0) return;
        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv == null || lv.players().isEmpty() || !CityGenerator.isBuilt(lv)) return;

        BlockPos g = CityPlan.gateCenter();
        boolean playerNear = false, playerFar = true;
        for (ServerPlayer p : lv.players()) {
            double d = p.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(g));
            if (d <= OPEN_RADIUS) playerNear = true;
            if (d <= CLOSE_RADIUS) playerFar = false;
        }

        if (closed && playerNear) {
            setDoors(lv, false);
            lv.playSound(null, g, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5F, 0.5F);
        } else if (!closed && playerFar) {
            setDoors(lv, true);
            lv.playSound(null, g, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.5F, 0.5F);
        }
    }

    /** Pose ({@code close=true}) ou retire les battants d'obsidienne du passage. */
    static void setDoors(ServerLevel lv, boolean close) {
        BlockPos g = CityPlan.gateCenter();
        var state = close ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
        for (int x = -DungeonGateBuilder.HALF_W; x <= DungeonGateBuilder.HALF_W; x++) {
            for (int y = 1; y <= DungeonGateBuilder.PASSAGE_H; y++) {
                // Battants sur le plan z du seuil, en épargnant la colonne du téléporteur (x=0,y=1).
                if (x == 0 && y == 1) continue;
                lv.setBlock(g.offset(x, y, 3), state, 3);
            }
        }
        closed = close;
    }
}
