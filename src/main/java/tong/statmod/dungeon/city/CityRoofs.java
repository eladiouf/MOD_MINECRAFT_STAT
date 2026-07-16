package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/** Briques de massing pour casser le "tout en boîtes" : toits pentus, coupoles, arches, flèches. */
final class CityRoofs {
    private static final int FLAG = 3;

    private CityRoofs() {}

    /** Toit à deux pentes (pignon sur l'axe X) au-dessus d'un bâtiment rx×rz, base à baseY. */
    static void gableRoof(ServerLevel lv, BlockPos c, int rx, int rz, int baseY, Block stair, Block ridge) {
        for (int z = -rz; z <= rz; z++) {
            int rise = rz - Math.abs(z);
            int y = baseY + Math.min(rise, rz);
            Direction face = z < 0 ? Direction.SOUTH : Direction.NORTH;
            for (int x = -rx; x <= rx; x++) {
                if (z == 0) { set(lv, c.offset(x, baseY + rz, 0), ridge.defaultBlockState()); continue; }
                set(lv, c.offset(x, y, z), stairState(stair, face));
                set(lv, c.offset(x, y - 1, z), Blocks.POLISHED_ANDESITE.defaultBlockState()); // sous-toit plein
            }
        }
    }

    /** Toit hippé (4 pentes) : pyramide en escaliers. */
    static void hipRoof(ServerLevel lv, BlockPos c, int r, int baseY, Block stair) {
        for (int layer = 0; layer <= r; layer++) {
            int rr = r - layer;
            int y = baseY + layer;
            for (int x = -rr; x <= rr; x++) for (int z = -rr; z <= rr; z++) {
                boolean edge = Math.abs(x) == rr || Math.abs(z) == rr;
                if (!edge) continue;
                Direction f = Math.abs(x) >= Math.abs(z) ? (x < 0 ? Direction.EAST : Direction.WEST)
                                                         : (z < 0 ? Direction.SOUTH : Direction.NORTH);
                set(lv, c.offset(x, y, z), stairState(stair, f));
            }
        }
        set(lv, c.offset(0, baseY + r, 0), Blocks.LANTERN.defaultBlockState());
    }

    /** Coupole hémisphérique (guilde / sanctuaire / hall des héros). */
    static void dome(ServerLevel lv, BlockPos c, int r, int baseY, Block shell) {
        for (int y = 0; y <= r; y++) {
            double rr = Math.sqrt(Math.max(0, (double) r * r - (double) y * y));
            int ri = (int) Math.round(rr);
            for (int x = -ri; x <= ri; x++) for (int z = -ri; z <= ri; z++) {
                int d2 = x * x + z * z;
                if (d2 <= ri * ri && d2 >= (ri - 1) * (ri - 1))
                    set(lv, c.offset(x, baseY + y, z), shell.defaultBlockState());
            }
        }
        set(lv, c.offset(0, baseY + r, 0), Blocks.SEA_LANTERN.defaultBlockState());
    }

    /** Arche en escaliers dans un mur (ouverture largeur w, hauteur h) centrée en c. */
    static void archway(ServerLevel lv, BlockPos c, int w, int h, Block stair) {
        for (int x = -w; x <= w; x++)
            for (int y = 1; y <= h; y++)
                set(lv, c.offset(x, y, 0), Blocks.AIR.defaultBlockState());
        set(lv, c.offset(-w, h, 0), stairState(stair, Direction.WEST));
        set(lv, c.offset(w, h, 0), stairState(stair, Direction.EAST));
        set(lv, c.offset(0, h + 1, 0), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
    }

    /** Flèche-repère : fût carré + toit conique + fanal sommital. */
    static void spire(ServerLevel lv, BlockPos c, int radius, int height, Block wall, Block roof) {
        for (int y = 0; y <= height; y++)
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
                if (Math.abs(x) == radius || Math.abs(z) == radius)
                    set(lv, c.offset(x, y, z), wall.defaultBlockState());
        for (int layer = 0; layer <= radius + 2; layer++) {
            int rr = radius + 1 - layer;
            for (int x = -rr; x <= rr; x++) for (int z = -rr; z <= rr; z++)
                if (Math.abs(x) == rr || Math.abs(z) == rr)
                    set(lv, c.offset(x, height + 1 + layer, z), roof.defaultBlockState());
        }
        set(lv, c.offset(0, height + radius + 4, 0), Blocks.SEA_LANTERN.defaultBlockState());
        set(lv, c.offset(0, height + radius + 5, 0), Blocks.END_ROD.defaultBlockState());
    }

    private static BlockState stairState(Block stair, Direction facing) {
        BlockState s = stair.defaultBlockState();
        if (stair instanceof StairBlock)
            s = s.setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.BOTTOM);
        return s;
    }

    private static void set(ServerLevel lv, BlockPos p, BlockState s) { lv.setBlock(p, s, FLAG); }
}
