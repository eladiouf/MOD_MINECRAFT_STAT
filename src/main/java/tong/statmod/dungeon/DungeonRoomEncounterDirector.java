package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tong.statmod.STATMod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Runtime progression through the existing DungeonRoomChain, one encounter at a time. */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonRoomEncounterDirector {
    private static final Map<Integer, RoomEncounterProgress> STATES = new HashMap<>();
    private static int tick;

    private DungeonRoomEncounterDirector() {}

    public static void beginFloor(ServerLevel level, int floor) {
        if (!isCombatFloor(floor)) return;
        STATES.compute(floor, (ignored, current) -> current == null || current.isComplete()
                ? new RoomEncounterProgress(requiredRoomIndices()) : current);
    }

    public static boolean onActiveRoomCleared(int floor) {
        RoomEncounterProgress state = STATES.computeIfAbsent(floor,
                ignored -> new RoomEncounterProgress(requiredRoomIndices()));
        return state.clearActiveRoom();
    }

    public static void resetActiveRoom(int floor) {
        RoomEncounterProgress state = STATES.get(floor);
        if (state != null) state.resetActiveRoom();
    }



    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tick % 5 != 0) return;
        ServerLevel level = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (level == null) return;

        for (ServerPlayer player : level.players()) {
            int floor = DungeonTeleportHandler.floorAtPos(player.getBlockX(), player.getBlockZ());
            if (!isCombatFloor(floor)) continue;
            RoomEncounterProgress state = STATES.computeIfAbsent(floor,
                    ignored -> new RoomEncounterProgress(requiredRoomIndices()));
            if (state.hasActiveRoom()) continue;
            int room = roomAt(floor, player.blockPosition());
            if (!requiredRoomIndices().contains(room) || state.isCleared(room)) continue;
            if (DungeonMobSpawner.requestRoomWave(level, floor, room)) {
                state.activate(room);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§6Rencontre " + room + " activée"), true);
            }
        }
    }

    static int roomAt(int floor, BlockPos worldPos) {
        BlockPos origin = DungeonTeleportHandler.floorSpawnPos(floor);
        int x = worldPos.getX() - origin.getX();
        int z = worldPos.getZ() - origin.getZ();
        for (DungeonLayout.Room room : DungeonLayout.rooms()) {
            if (x >= room.minX() && x <= room.maxX() && z >= room.minZ() && z <= room.maxZ()) {
                return room.index();
            }
        }
        return -1;
    }

    static Set<Integer> requiredRoomIndices() {
        Set<Integer> rooms = new HashSet<>();
        for (DungeonLayout.Room room : DungeonLayout.rooms()) {
            if (!room.isFirst() && room.index() != DungeonLayout.ROOM_COUNT / 2) rooms.add(room.index());
        }
        return Set.copyOf(rooms);
    }

    private static boolean isCombatFloor(int floor) {
        return floor > 0 && floor % 5 != 0;
    }
}
