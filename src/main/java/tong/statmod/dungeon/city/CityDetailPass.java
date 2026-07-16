package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Final urban dressing pass shared by the whole floor-0 city. */
final class CityDetailPass {
    private CityDetailPass() {}

    static void build(ServerLevel level) {
        decorateRing(level, CityPlan.INNER_RING_RADIUS, 24);
        decorateRing(level, CityPlan.OUTER_RING_RADIUS, 36);
        decoratePlazaApproaches(level);
    }

    private static void decorateRing(ServerLevel level, int radius, int count) {
        Block lamp = Blocks.LANTERN;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            int x = CityPlan.CENTER_X + (int) Math.round(Math.cos(angle) * radius);
            int z = CityPlan.CENTER_Z + (int) Math.round(Math.sin(angle) * radius);
            BlockPos base = new BlockPos(x, CityPlan.GROUND_Y + 1, z);
            for (int y = 0; y <= 4; y++) set(level, base.above(y), Blocks.POLISHED_BLACKSTONE_WALL);
            set(level, base.above(5), lamp);

            int sideX = (int) Math.signum(Math.cos(angle));
            int sideZ = (int) Math.signum(Math.sin(angle));
            set(level, base.offset(sideZ * 2, 0, -sideX * 2), CityMaterialPalette.stool());
            if ((i & 1) == 0) {
                set(level, base.offset(-sideZ * 2, 0, sideX * 2), CityMaterialPalette.basket());
            } else {
                set(level, base.offset(-sideZ * 2, 0, sideX * 2), Blocks.FLOWER_POT);
            }
        }
    }

    private static void decoratePlazaApproaches(ServerLevel level) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] direction : directions) {
            for (int distance = 55; distance <= 85; distance += 10) {
                BlockPos p = new BlockPos(
                        CityPlan.CENTER_X + direction[0] * distance,
                        CityPlan.GROUND_Y + 1,
                        CityPlan.CENTER_Z + direction[1] * distance);
                set(level, p, Blocks.CHISELED_STONE_BRICKS);
                set(level, p.above(), CityMaterialPalette.rope());
                set(level, p.above(2), Blocks.SEA_LANTERN);
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 3);
    }
}
