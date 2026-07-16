package tong.statmod.dungeon;

import java.util.List;
import java.util.function.BiPredicate;

/** Pure policy reserving every challenge floor for one FTB team. */
final class DungeonFloorAdmission {
    private DungeonFloorAdmission() {}

    static <T> boolean canEnter(int floor, T entrant, List<T> occupants,
                                BiPredicate<T, T> sameTeam, boolean teamManagerReady) {
        if (floor == 0) return true;
        if (!teamManagerReady) return false;
        for (T occupant : occupants) {
            if (!sameTeam.test(entrant, occupant)) return false;
        }
        return true;
    }
}
