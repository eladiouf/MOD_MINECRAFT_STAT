package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import tong.statmod.dungeon.QuarkDungeonDecorator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import tong.statmod.STATMod;
import tong.statmod.dungeon.DungeonSpawnGuard;

/** Tensura training dummies rebuilt with floor 0. */
final class CityTrainingDummies {
    static final String TAG = "statmod_city_training_dummy";
    private CityTrainingDummies() {}

    static void spawn(ServerLevel level) {
        BlockPos c = CityPlan.trainingGround().above();
        int[][] positions = {
                {-31, -16}, {-29, -4}, {-32, 11},
                {-7, -19}, {5, -13}, {-3, -4}, {7, 5}, {-6, 14}, {4, 21},
                {19, -17}, {28, -11}, {22, -3}, {30, 6}, {18, 13}, {27, 20}
        };
        Block dummy = QuarkDungeonDecorator.resolve("tensura:training_dummy");
        if (dummy == null) dummy = Blocks.TARGET;
        ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath("tensura", "training_dummy");
        EntityType<?> dummyType = BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)
                ? BuiltInRegistries.ENTITY_TYPE.get(entityId) : null;
        if (dummyType == null) {
            STATMod.LOGGER.warn("[City] Entity tensura:training_dummy introuvable; socles seulement");
        }
        for (int[] position : positions) {
            BlockPos base = c.offset(position[0], 1, position[1]);
            level.setBlock(base, dummy.defaultBlockState(), 3);
            if (dummyType != null) {
                Entity entity = dummyType.create(level);
                if (entity != null) {
                    entity.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0f, 0.0f);
                    entity.getPersistentData().putBoolean(TAG, true);
                    DungeonSpawnGuard.spawnAuthorized(() -> {
                        level.addFreshEntity(entity);
                        return entity;
                    });
                }
            }
        }
    }
}
