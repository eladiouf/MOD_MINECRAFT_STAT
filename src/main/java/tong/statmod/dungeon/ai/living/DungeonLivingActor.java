package tong.statmod.dungeon.ai.living;

import java.util.Optional;
import net.minecraft.world.entity.Mob;
import tong.statmod.dungeon.ai.DungeonAiActor;
import tong.statmod.dungeon.ai.DungeonFaction;
import tong.statmod.dungeon.ai.DungeonTacticalRole;

public final class DungeonLivingActor {
    public static final String LIVING_ROLE_TAG = "statmod_living_role";
    public static final String NON_COMBAT_TAG = "statmod_living_non_combat";
    public static final String PRISONER_RELEASED_TAG = "statmod_prisoner_released";
    public static final String PRISONER_OWNER_TAG = "statmod_prisoner_owner";

    private DungeonLivingActor() {}

    public static void initializeNeutral(Mob mob, DungeonLivingRole role, int floor, int room) {
        mob.getPersistentData().putString(LIVING_ROLE_TAG, role.name());
        mob.getPersistentData().putBoolean(NON_COMBAT_TAG, true);
        mob.getPersistentData().putInt("statmod_dungeon_room", room);
        DungeonAiActor.initialize(mob, DungeonFaction.INHABITANTS, floor,
                floor + ":living:" + room, DungeonTacticalRole.WARDEN);
    }

    public static Optional<DungeonLivingRole> role(Mob mob) {
        try {
            return Optional.of(DungeonLivingRole.valueOf(
                    mob.getPersistentData().getString(LIVING_ROLE_TAG)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static boolean isNonCombat(Mob mob) {
        return mob.getPersistentData().getBoolean(NON_COMBAT_TAG);
    }
}
