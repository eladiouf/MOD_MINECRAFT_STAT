package tong.statmod.integration.waystones;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import tong.statmod.dungeon.FloorPalette;

public final class WaystonesBridge {
    private WaystonesBridge() {}

    public static boolean loaded() {
        return false;
    }

    public static void placeCheckpoint(ServerLevel lv, BlockPos pos, FloorPalette tier, int floor) {
        // no-op stub
    }
}
