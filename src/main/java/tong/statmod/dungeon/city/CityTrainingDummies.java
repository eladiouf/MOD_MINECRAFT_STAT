package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Target dummies rebuilt with floor 0 (vanilla target blocks). */
final class CityTrainingDummies {
    private CityTrainingDummies() {}

    static void spawn(ServerLevel level) {
        BlockPos c = CityPlan.trainingGround().above();
        int[][] positions = {
                {-31, -16}, {-29, -4}, {-32, 11},
                {-7, -19}, {5, -13}, {-3, -4}, {7, 5}, {-6, 14}, {4, 21},
                {19, -17}, {28, -11}, {22, -3}, {30, 6}, {18, 13}, {27, 20}
        };
        for (int[] position : positions) {
            BlockPos base = c.offset(position[0], 1, position[1]);
            level.setBlock(base, Blocks.TARGET.defaultBlockState(), 3);
        }
    }
}
