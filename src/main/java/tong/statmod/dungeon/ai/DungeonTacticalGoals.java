package tong.statmod.dungeon.ai;

import net.minecraft.world.entity.Mob;

public final class DungeonTacticalGoals {
    private DungeonTacticalGoals() {}

    public static void ensureAttached(Mob mob) {
        DungeonAiActor.tacticalRole(mob);
    }
}
