package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/** Runtime controls for the floor-0 arena entrance. */
public final class CityArenaController {
    private CityArenaController() {}

    public static void teleport(ServerPlayer player, ServerLevel dungeon) {
        BlockPos c = CityPlan.arena().above();
        player.teleportTo(dungeon, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    public static void openGate(ServerLevel level) { setGate(level, true); }
    public static void closeGate(ServerLevel level) { setGate(level, false); }

    private static void setGate(ServerLevel level, boolean open) {
        BlockPos c = CityPlan.arena().above();
        for (int z = 27; z <= 29; z++) for (int x = -4; x <= 4; x++) for (int y = 1; y <= 8; y++) {
            level.setBlock(c.offset(x, y, z),
                    open ? Blocks.AIR.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState(), 3);
        }
    }
}
