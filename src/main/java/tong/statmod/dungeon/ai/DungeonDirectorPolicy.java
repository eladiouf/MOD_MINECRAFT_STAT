package tong.statmod.dungeon.ai;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DungeonDirectorPolicy {
    public record PlayerSnapshot(int floor, boolean creative, boolean spectator, boolean alive) {}

    private DungeonDirectorPolicy() {}

    public static Set<Integer> occupiedFloors(List<PlayerSnapshot> players) {
        Set<Integer> floors = new LinkedHashSet<>();
        for (PlayerSnapshot player : players) {
            if (!player.creative() && !player.spectator() && player.alive() && player.floor() > 0) {
                floors.add(player.floor());
            }
        }
        return floors;
    }
}
