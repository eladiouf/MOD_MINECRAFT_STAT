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
        decorateAvenueHouses(level);
    }

    /** Densité au sol : petites maisons de remplissage le long des 6 avenues (place → anneau int). */
    private static void decorateAvenueHouses(ServerLevel level) {
        java.util.Random rng = new java.util.Random(0x4055EL);
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(90.0 + k * 60.0);
            for (int r = CityPlan.PLAZA_RADIUS + 10; r <= CityPlan.INNER_RING_RADIUS - 8; r += 12) {
                for (int side : new int[]{-6, 6}) {
                    int x = (int) Math.round(CityPlan.CENTER_X + Math.cos(a) * r - Math.sin(a) * side);
                    int z = (int) Math.round(CityPlan.CENTER_Z + Math.sin(a) * r + Math.cos(a) * side);
                    if (!CityPlan.inCity(x, z) || CityPlan.onAvenue(x, z)) continue;
                    fillerHouse(level, new BlockPos(x, CityPlan.GROUND_Y + 1, z), rng.nextInt(4));
                }
            }
        }
    }

    /** Maison compacte 7×7 à toit pignon (réutilise CityRoofs), variante de matériau. */
    private static void fillerHouse(ServerLevel lv, BlockPos c, int variant) {
        Block wall = switch (variant) {
            case 0 -> Blocks.STONE_BRICKS;
            case 1 -> Blocks.MUD_BRICKS;
            case 2 -> Blocks.DEEPSLATE_BRICKS;
            default -> Blocks.COBBLESTONE;
        };
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            lv.setBlock(c.offset(x, -1, z), Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
            boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
            if (edge) for (int y = 0; y <= 3; y++) lv.setBlock(c.offset(x, y, z), wall.defaultBlockState(), 3);
        }
        for (int y = 1; y <= 2; y++) lv.setBlock(c.offset(0, y, 3), Blocks.AIR.defaultBlockState(), 3); // porte
        lv.setBlock(c.offset(2, 2, 3), Blocks.GLASS_PANE.defaultBlockState(), 3);
        lv.setBlock(c.offset(-2, 2, 3), Blocks.GLASS_PANE.defaultBlockState(), 3);
        CityRoofs.gableRoof(lv, c.offset(0, 0, 0), 3, 3, 4, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB);
        lv.setBlock(c.offset(0, 1, 0), Blocks.LANTERN.defaultBlockState(), 3);
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
