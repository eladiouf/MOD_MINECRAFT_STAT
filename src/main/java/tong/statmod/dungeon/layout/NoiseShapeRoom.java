package tong.statmod.dungeon.layout;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.dungeon.BlockPalette;
import tong.statmod.dungeon.noise.OpenSimplex2S;

public final class NoiseShapeRoom {

    private static final long PILLAR_SEED_XOR = 0xABCDL;
    private static final long FLOOR_FEATURE_SEED_XOR = 0x5678L;
    private static final long DERIVE_MULT = 0x9E3779B97F4A7C15L;

    private final long seed;

    public NoiseShapeRoom(long floorSeed, int roomIndex) {
        long z = floorSeed ^ (roomIndex * DERIVE_MULT + 0x1234L);
        this.seed = z;
    }

    public boolean inside(int lx, int lz, int w, int d) {
        if (lx < 0 || lz < 0 || lx > w || lz > d) return false;
        if (isDoorway(lx, lz, w, d)) return true;
        double nx = (lx - w / 2.0) / (w / 2.0);
        double nz = (lz - d / 2.0) / (d / 2.0);
        float noise = OpenSimplex2S.noise2(seed, nx * 3.0, nz * 3.0);
        double dist = Math.sqrt(nx * nx + nz * nz);
        double threshold = -0.7 + dist * 0.2;
        return noise > threshold;
    }

    public static boolean isDoorway(int lx, int lz, int w, int d) {
        int cx = w / 2, cz = d / 2;
        return (Math.abs(lx - cx) <= 1 && (lz == 0 || lz == d))
            || (Math.abs(lz - cz) <= 1 && (lx == 0 || lx == w));
    }

    public boolean insideAbs(int absX, int absZ, int minX, int minZ, int maxX, int maxZ) {
        return inside(absX - minX, absZ - minZ, maxX - minX, maxZ - minZ);
    }

    public void placePillars(ServerLevel lv, BlockPos sp, BlockPalette t,
                             int minX, int minZ, int maxX, int maxZ, int ceilH) {
        int w = maxX - minX, d = maxZ - minZ;
        for (int lx = 4; lx < w - 4; lx += 5) {
            for (int lz = 4; lz < d - 4; lz += 5) {
                float nn = OpenSimplex2S.noise2(seed ^ PILLAR_SEED_XOR, lx * 0.08, lz * 0.08);
                if (nn > 0.3 && inside(lx, lz, w, d)) {
                    int px = minX + lx, pz = minZ + lz;
                    for (int y = 0; y < ceilH - 1; y++)
                        S(lv, O(sp, px, y, pz), B(t.decorPrimary()));
                    S(lv, O(sp, px, ceilH - 1, pz), B(t.accent()));
                }
            }
        }
    }

    public void placeFloorFeatures(ServerLevel lv, BlockPos sp, BlockPalette t,
                                   int minX, int minZ, int maxX, int maxZ, int blockFloor) {
        int w = maxX - minX, d = maxZ - minZ;
        boolean infernal = t.light() == Blocks.SHROOMLIGHT
                || t.base() == Blocks.POLISHED_BLACKSTONE_BRICKS;
        BlockState fluid = infernal
                ? Blocks.LAVA.defaultBlockState()
                : Blocks.WATER.defaultBlockState();

        for (int lx = 2; lx < w - 2; lx++) {
            for (int lz = 2; lz < d - 2; lz++) {
                if (!inside(lx, lz, w, d)) continue;
                float f = OpenSimplex2S.noise2(seed ^ FLOOR_FEATURE_SEED_XOR,
                        (minX + lx) * 0.06, (minZ + lz) * 0.06);
                int ax = minX + lx, az = minZ + lz;
                if (f > 0.5 && (lx < 3 || lx > w - 4 || lz < 3 || lz > d - 4)) {
                    int h = (int) ((f - 0.5) * 4);
                    for (int y = 1; y <= h; y++)
                        S(lv, O(sp, ax, y, az), B(t.accent()));
                } else if (f < -0.5 && lx > 3 && lx < w - 4 && lz > 3 && lz < d - 4) {
                    int depth = (int) ((f + 0.5) * 4);
                    for (int y = -1; y >= -depth; y--)
                        S(lv, O(sp, ax, y, az), fluid);
                }
            }
        }
    }

    private static BlockPos O(BlockPos o, int x, int y, int z) {
        return o.offset(x, y, z);
    }

    private static BlockState B(Block block) {
        return block.defaultBlockState();
    }

    private static void S(ServerLevel lv, BlockPos pos, BlockState state) {
        lv.setBlock(pos, state, 3);
    }
}
