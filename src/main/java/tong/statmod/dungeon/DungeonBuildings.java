package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static tong.statmod.dungeon.DungeonArchitect.HX;
import static tong.statmod.dungeon.DungeonArchitect.HZ;

/**
 * Mission M6 — Petits bâtiments dans les ailes (2026-07-05).
 *
 * <p>Depuis l'agrandissement des étages (HX=64, HZ=52), les ailes de combat sont vastes. Cette
 * passe y sème quelques <b>constructions</b> (tours de guet, huttes, ruines, sanctuaires, puits)
 * bâties par code, thématisées par la {@link BlockPalette}, pour donner l'impression d'un lieu
 * habité/fortifié — sans dépendre de schematics (API structure fragile en 1.21.1).
 *
 * <p>Placement intelligent : uniquement dans les ailes ({@code |x| entre 16 et HX-4}), jamais sur
 * l'avenue centrale ni le cœur nord ; espacement minimal entre bâtiments ; on ne pose que sur un
 * sol libre.
 */
public final class DungeonBuildings {

    private DungeonBuildings() {}

    static BlockState B(Block b) { return b.defaultBlockState(); }
    static void S(ServerLevel lv, BlockPos p, BlockState s) { lv.setBlock(p, s, 3); }
    static BlockPos O(BlockPos sp, int x, int y, int z) { return sp.offset(x, y, z); }
    static boolean air(ServerLevel lv, BlockPos p) { return lv.getBlockState(p).isAir(); }

    static BlockState stair(Block b, Direction f) {
        return B(b).setValue(StairBlock.FACING, f).setValue(StairBlock.HALF, Half.BOTTOM);
    }

    /** Place plusieurs bâtiments dans les ailes de l'étage (déterministe par seed). */
    public static void place(ServerLevel lv, BlockPos sp, BlockPalette t, Random rng) {
        List<BlockPos> used = new ArrayList<>();
        int target = 4 + rng.nextInt(3); // 4-6 bâtiments
        int placed = 0, attempts = 0;
        while (placed < target && attempts < 60) {
            attempts++;
            int side = rng.nextBoolean() ? 1 : -1;
            int x = side * (18 + rng.nextInt(HX - 24));          // dans l'aile, à l'écart de l'avenue
            int z = rng.nextInt(2 * HZ - 24) - (HZ - 12);        // sur la longueur, marge aux murs
            if (Math.abs(z + HZ - 11) < 10) continue;            // épargne la zone du cœur (nord)
            BlockPos base = O(sp, x, 0, z);
            if (tooClose(used, base) || !clearArea(lv, base)) continue;

            switch (rng.nextInt(5)) {
                case 0 -> watchtower(lv, base, t);
                case 1 -> hut(lv, base, t, rng);
                case 2 -> ruin(lv, base, t, rng);
                case 3 -> shrine(lv, base, t);
                default -> well(lv, base, t);
            }
            used.add(base);
            placed++;
        }
    }

    private static boolean tooClose(List<BlockPos> used, BlockPos p) {
        for (BlockPos u : used) if (u.distSqr(p) < 12 * 12) return true;
        return false;
    }

    /** Vérifie qu'une petite emprise 7×7 au sol est libre (air au-dessus du sol). */
    private static boolean clearArea(ServerLevel lv, BlockPos base) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (!air(lv, base.offset(dx, 1, dz))) return false;
        }
        return true;
    }

    // ═══════════════ types de bâtiments ═══════════════

    /** Tour de guet : fût 3×3 creux, ~7 de haut, créneaux + fanal au sommet. */
    private static void watchtower(ServerLevel lv, BlockPos b, BlockPalette t) {
        BlockState wall = B(t.base());
        BlockState brick = B(t.accent());
        int h = 7;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            boolean edge = dx != 0 || dz != 0;
            if (edge) for (int y = 0; y < h; y++) S(lv, O(b, dx, y, dz), (y % 3 == 2) ? brick : wall);
        }
        // porte (face sud)
        S(lv, O(b, 0, 0, 1), B(Blocks.AIR));
        S(lv, O(b, 0, 1, 1), B(Blocks.AIR));
        // couronne crénelée + fanal
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            if ((Math.abs(dx) == 1 || Math.abs(dz) == 1) && ((dx + dz) & 1) == 0)
                S(lv, O(b, dx, h, dz), brick);
        S(lv, O(b, 0, h - 1, 0), B(t.light()));
    }

    /** Hutte : cabane 5×5 avec toit en escaliers et une porte. */
    private static void hut(ServerLevel lv, BlockPos b, BlockPalette t, Random rng) {
        BlockState wall = B(t.accent());
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            if (edge) for (int y = 0; y < 3; y++) S(lv, O(b, dx, y, dz), wall);
        }
        // porte
        S(lv, O(b, 0, 0, 2), B(Blocks.AIR));
        S(lv, O(b, 0, 1, 2), B(Blocks.AIR));
        // toit en escaliers (pyramide simple)
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
            S(lv, O(b, dx, 3, dz), B(t.slab()));
        S(lv, O(b, 0, 4, 0), B(t.decorPrimary()));
        // lumière intérieure
        S(lv, O(b, 0, 0, 0), B(t.light()));
    }

    /** Ruine : murs partiels effondrés + gravats (feel abandonné). */
    private static void ruin(ServerLevel lv, BlockPos b, BlockPalette t, Random rng) {
        BlockState wall = B(t.base());
        BlockState rub = B(t.decorSecondary());
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            if (!edge) continue;
            int hh = rng.nextInt(4); // hauteur irrégulière (0-3)
            for (int y = 0; y < hh; y++) S(lv, O(b, dx, y, dz), wall);
        }
        // gravats au sol
        for (int i = 0; i < 5; i++) {
            int gx = rng.nextInt(5) - 2, gz = rng.nextInt(5) - 2;
            if (air(lv, O(b, gx, 0, gz))) S(lv, O(b, gx, 0, gz), rub);
        }
    }

    /** Sanctuaire : socle 3×3 + 4 colonnes + bloc lumineux surélevé. */
    private static void shrine(ServerLevel lv, BlockPos b, BlockPalette t) {
        BlockState base = B(t.accent());
        BlockState pillar = B(t.decorPrimary());
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            S(lv, O(b, dx, 0, dz), base);
        for (int cx : new int[]{-1, 1}) for (int cz : new int[]{-1, 1}) {
            S(lv, O(b, cx, 1, cz), pillar);
            S(lv, O(b, cx, 2, cz), pillar);
        }
        // dais + relique lumineuse
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            S(lv, O(b, dx, 3, dz), base);
        S(lv, O(b, 0, 1, 0), B(t.light()));
    }

    /** Puits : anneau de pierre 3×3 + eau au centre + margelle. */
    private static void well(ServerLevel lv, BlockPos b, BlockPalette t) {
        BlockState rim = B(t.wallBlock());
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) {
                S(lv, O(b, 0, 0, 0), B(Blocks.WATER));
            } else {
                S(lv, O(b, dx, 0, dz), B(t.decorPrimary()));
                S(lv, O(b, dx, 1, dz), rim);
            }
        }
        // arceau au-dessus
        S(lv, O(b, -1, 3, 0), B(t.accent()));
        S(lv, O(b, 1, 3, 0), B(t.accent()));
        S(lv, O(b, 0, 3, 0), B(t.light()));
    }
}
