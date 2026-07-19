package tong.statmod.dungeon.ai.living;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DungeonLivingEventPolicy {
    private DungeonLivingEventPolicy() {}

    public static List<DungeonLivingRole> safehouseRoles(int floor) {
        if (floor <= 0) return List.of();
        List<DungeonLivingRole> roles = new ArrayList<>(4);
        roles.add(DungeonLivingRole.WOUNDED_SURVIVOR);
        if (floor >= 11) roles.add(DungeonLivingRole.WANDERING_MERCHANT);
        if (floor >= 21) roles.add(DungeonLivingRole.SCAVENGER);
        if (floor >= 31) roles.add(DungeonLivingRole.PRISONER);
        return List.copyOf(roles);
    }

    public static Optional<DungeonLivingRole> combatRole(int floor, int roomIndex, int ordinal) {
        if (floor >= 31 && roomIndex == 4 && ordinal == 0) {
            return Optional.of(DungeonLivingRole.RIVAL_EXPLORER);
        }
        if (floor >= 51 && roomIndex == 12 && ordinal == 0) {
            return Optional.of(DungeonLivingRole.RITUALIST);
        }
        if (floor >= 71 && roomIndex == 15 && ordinal == 0) {
            return Optional.of(DungeonLivingRole.ENGINEER);
        }
        return Optional.empty();
    }
}
