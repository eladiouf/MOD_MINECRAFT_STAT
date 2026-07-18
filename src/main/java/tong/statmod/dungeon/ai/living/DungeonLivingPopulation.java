package tong.statmod.dungeon.ai.living;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import tong.statmod.dungeon.DungeonLayout;
import tong.statmod.dungeon.DungeonMobSpawner;
import tong.statmod.dungeon.DungeonRoomChain;
import tong.statmod.dungeon.DungeonSpawnGuard;
import tong.statmod.entity.AdventurerEntities;

public final class DungeonLivingPopulation {
    private static final int[][] OFFSETS = {{-4, -2}, {4, -2}, {-4, 3}, {4, 3}};

    private DungeonLivingPopulation() {}

    public static void populateSafehouse(ServerLevel level, BlockPos floorSpawn, int floor) {
        int roomIndex = DungeonLayout.ROOM_COUNT / 2;
        DungeonLayout.Room room = DungeonLayout.rooms().get(roomIndex);
        BlockPos center = floorSpawn.offset(room.centerX(),
                DungeonRoomChain.roomYOffset(roomIndex, floor) + 1, room.centerZ());
        AABB floorBounds = new AABB(floorSpawn).inflate(DungeonMobSpawner.FLOOR_SCAN_RADIUS);
        boolean alreadyPopulated = !level.getEntitiesOfClass(Mob.class, floorBounds,
                mob -> mob.getPersistentData().getBoolean(DungeonLivingActor.NON_COMBAT_TAG)
                        && mob.getPersistentData().getInt("statmod_dungeon_room") == roomIndex)
                .isEmpty();
        if (alreadyPopulated) return;

        List<DungeonLivingRole> roles = DungeonLivingEventPolicy.safehouseRoles(floor);
        int count = Math.min(4, roles.size());
        for (int index = 0; index < count; index++) {
            DungeonLivingRole role = roles.get(index);
            BlockPos position = center.offset(OFFSETS[index][0], 0, OFFSETS[index][1]);
            spawn(level, position, floor, roomIndex, role);
        }
    }

    private static void spawn(ServerLevel level, BlockPos position, int floor, int room,
                              DungeonLivingRole role) {
        EntityType<?> type = switch (role) {
            case WANDERING_MERCHANT -> EntityType.WANDERING_TRADER;
            case SCAVENGER -> EntityType.FOX;
            default -> AdventurerEntities.ADVENTURER.get();
        };
        Object spawned = DungeonSpawnGuard.spawnAuthorized(
                () -> type.spawn(level, position, MobSpawnType.STRUCTURE));
        if (!(spawned instanceof Mob mob)) return;
        mob.setPersistenceRequired();
        mob.moveTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5,
                180.0f, 0.0f);
        DungeonLivingActor.initializeNeutral(mob, role, floor, room);
        mob.setCustomName(Component.literal(displayName(role)));
        mob.setCustomNameVisible(true);
        if (role == DungeonLivingRole.WOUNDED_SURVIVOR) {
            mob.setHealth(Math.max(1.0f, mob.getMaxHealth() * 0.45f));
        }
    }

    private static String displayName(DungeonLivingRole role) {
        return switch (role) {
            case WOUNDED_SURVIVOR -> "Survivant blessé";
            case PRISONER -> "Prisonnier";
            case WANDERING_MERCHANT -> "Marchand du donjon";
            case SCAVENGER -> "Récupérateur";
            default -> "Habitant du donjon";
        };
    }
}
