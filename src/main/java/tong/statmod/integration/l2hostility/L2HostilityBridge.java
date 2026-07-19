package tong.statmod.integration.l2hostility;

import net.minecraft.world.entity.LivingEntity;

public final class L2HostilityBridge {
    private L2HostilityBridge() {}

    public static boolean loaded() {
        return false;
    }

    public static int levelForFloor(int floor) {
        return 1;
    }

    public static void applyFloorLevel(LivingEntity mob, int floor) {
    }
}
