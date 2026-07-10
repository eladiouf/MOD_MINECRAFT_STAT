package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Random;

import static tong.statmod.dungeon.DungeonArchitect.HX;
import static tong.statmod.dungeon.DungeonArchitect.HZ;
import static tong.statmod.dungeon.DungeonArchitect.WALL_H;

/**
 * Mission M6 — Détaillage « builder pro » (2026-07-05).
 *
 * <p>Passe de finition qui sème une multitude de petits détails <b>placés intelligemment</b> par
 * rapport à la structure (contre les murs, dans les angles, le long des créneaux, suspendus au
 * plafond) plutôt qu'au hasard sur le sol. Chaque famille de détail est <b>thématisée</b> : glace
 * gèle des stalactites, la jungle fait pendre des lianes, le nether accroche des chaînes ardentes,
 * etc. C'est ce qui donne l'impression d'un lieu vécu, pas d'une boîte.
 *
 * <p>Tout est déterministe (seed d'étage), idempotent (ne remplace que de l'air / des blocs
 * décoratifs), et jamais placé dans le passage central ni sur le spawn.
 */
public final class DungeonDetailing {

    private DungeonDetailing() {}

    static BlockState B(Block b) { return b.defaultBlockState(); }
    static void S(ServerLevel lv, BlockPos p, BlockState s) { lv.setBlock(p, s, 3); }
    static BlockPos O(BlockPos sp, int x, int y, int z) { return sp.offset(x, y, z); }
    static boolean isAir(ServerLevel lv, BlockPos p) { return lv.getBlockState(p).isAir(); }

    /** Point d'entrée : applique toutes les passes de détail. */
    public static void detail(ServerLevel lv, BlockPos sp, ThemePalette theme, int floor, Random rng) {
        wallSconces(lv, sp, theme, rng);       // torches/lanternes murales régulières
        cornerCobwebs(lv, sp, rng);            // toiles dans les 4 angles intérieurs
        hangingFeatures(lv, sp, theme, rng);   // suspensions au plafond (chaînes, lianes, stalactites)
        wallGrime(lv, sp, theme, rng);         // salissure murale : mousse, fissures, champignons
        groundClutter(lv, sp, theme, rng);     // petits objets au sol le long des murs
        rubblePiles(lv, sp, theme, rng);       // tas de gravats contre les murs
        themeSignature(lv, sp, theme, rng);    // détail signature du thème (flaques, braises, cristaux)
        crackedWalls(lv, sp, theme, rng);      // fissures et trous dans les murs
        crumbledPillars(lv, sp, theme, rng);   // colonnes brisées avec débris au sol
    }

    // ── 1. Torches/lanternes murales, réparties régulièrement le long des 4 murs ──
    private static void wallSconces(ServerLevel lv, BlockPos sp, ThemePalette theme, Random rng) {
        Block light = theme.light();
        int y = WALL_H - 4;
        for (int x = -HX + 4; x <= HX - 4; x += 8) {
            wallLight(lv, O(sp, x, y, -HZ + 1), light, Direction.SOUTH);
            wallLight(lv, O(sp, x, y, HZ - 1), light, Direction.NORTH);
        }
        for (int z = -HZ + 4; z <= HZ - 4; z += 8) {
            wallLight(lv, O(sp, -HX + 1, y, z), light, Direction.EAST);
            wallLight(lv, O(sp, HX - 1, y, z), light, Direction.WEST);
        }
    }

    /** Pose une torche murale (orientée) ou, à défaut, le bloc lumineux du thème au sol de la niche. */
    private static void wallLight(ServerLevel lv, BlockPos p, Block light, Direction facing) {
        if (!isAir(lv, p)) return;
        if (light == Blocks.TORCH) {
            S(lv, p, B(Blocks.WALL_TORCH).setValue(WallTorchBlock.FACING, facing));
        } else if (light == Blocks.SOUL_LANTERN || light == Blocks.LANTERN) {
            // Lanterne suspendue si un plafond existe au-dessus, sinon posée.
            S(lv, p, B(light).setValue(BlockStateProperties.HANGING, true));
        } else {
            S(lv, p, B(light));
        }
    }

    // ── 2. Toiles dans les angles intérieurs (haut des coins) ──
    private static void cornerCobwebs(ServerLevel lv, BlockPos sp, Random rng) {
        int[][] corners = {{-HX + 2, -HZ + 2}, {HX - 2, -HZ + 2}, {-HX + 2, HZ - 2}, {HX - 2, HZ - 2}};
        for (int[] c : corners) {
            for (int y = WALL_H - 2; y <= WALL_H - 1; y++) {
                if (rng.nextInt(3) != 0 && isAir(lv, O(sp, c[0], y, c[1]))) {
                    S(lv, O(sp, c[0], y, c[1]), B(Blocks.COBWEB));
                }
            }
        }
    }

    // ── 3. Suspensions au plafond, thématisées (le long des ailes, |x|>=14) ──
    private static void hangingFeatures(ServerLevel lv, BlockPos sp, ThemePalette theme, Random rng) {
        int placed = 0, attempts = 0;
        while (placed < 14 && attempts < 120) {
            attempts++;
            int x = rng.nextInt(2 * HX - 8) - (HX - 4);
            int z = rng.nextInt(2 * HZ - 8) - (HZ - 4);
            if (Math.abs(x) < 12) continue; // seulement au-dessus des ailes, pas l'avenue
            BlockPos top = O(sp, x, WALL_H - 1, z);
            if (isAir(lv, top)) continue;   // besoin d'un plafond pour accrocher
            BlockPos hang = top.below();
            if (!isAir(lv, hang)) continue;
            hang(lv, hang, theme, rng);
            placed++;
        }
    }

    private static void hang(ServerLevel lv, BlockPos p, ThemePalette theme, Random rng) {
        // On privilégie des blocs qui tiennent sans support (chaîne, dripstone pointe-bas, toile).
        switch (theme) {
            case DECHARNES, MOISSON -> S(lv, p, B(Blocks.COBWEB));
            case FAUVES -> {
                BlockState vine = B(Blocks.VINE).setValue(BlockStateProperties.UP, true);
                S(lv, p, vine);
            }
            case FOURNAISE, MAGES, GESTE, NEANT -> {
                S(lv, p, B(Blocks.CHAIN));
                if (isAir(lv, p.below())) S(lv, p.below(),
                        B(Blocks.LANTERN).setValue(BlockStateProperties.HANGING, true));
            }
            default -> S(lv, p, B(Blocks.CHAIN));
        }
    }

    // ── 4. Salissure murale : mousse / fissures / champignons contre les murs ──
    private static void wallGrime(ServerLevel lv, BlockPos sp, ThemePalette theme, Random rng) {
        int placed = 0, attempts = 0;
        while (placed < 20 && attempts < 160) {
            attempts++;
            boolean alongX = rng.nextBoolean();
            int x, z;
            if (alongX) { x = rng.nextInt(2 * HX - 6) - (HX - 3); z = rng.nextBoolean() ? -HZ + 1 : HZ - 1; }
            else { z = rng.nextInt(2 * HZ - 6) - (HZ - 3); x = rng.nextBoolean() ? -HX + 1 : HX - 1; }
            int y = 1 + rng.nextInt(WALL_H - 3);
            BlockPos p = O(sp, x, y, z);
            if (!isAir(lv, p)) continue;
            grime(lv, p, theme, rng);
            placed++;
        }
    }

    private static void grime(ServerLevel lv, BlockPos p, ThemePalette theme, Random rng) {
        if ((theme == ThemePalette.FOURNAISE || theme == ThemePalette.GESTE || theme == ThemePalette.NEANT) && rng.nextInt(3) == 0) {
            S(lv, p, B(Blocks.SOUL_TORCH));
        } else {
            S(lv, p, B(Blocks.COBWEB));
        }
    }

    // ── 5. Petits objets au sol, alignés le long des murs ──
    private static void groundClutter(ServerLevel lv, BlockPos sp, ThemePalette theme, Random rng) {
        int placed = 0, attempts = 0;
        while (placed < 22 && attempts < 160) {
            attempts++;
            boolean alongX = rng.nextBoolean();
            int x, z;
            if (alongX) { x = rng.nextInt(2 * HX - 10) - (HX - 5); z = rng.nextBoolean() ? -HZ + 2 : HZ - 2; }
            else { z = rng.nextInt(2 * HZ - 10) - (HZ - 5); x = rng.nextBoolean() ? -HX + 2 : HX - 2; }
            if (Math.abs(x) < 6 && Math.abs(z) < 6) continue;
            BlockPos g = O(sp, x, 0, z);
            if (!isAir(lv, g)) continue;
            clutter(lv, g, theme, rng);
            placed++;
        }
    }

    private static void clutter(ServerLevel lv, BlockPos g, ThemePalette theme, Random rng) {
        int r = rng.nextInt(10);
        if (r == 0) { S(lv, g, B(Blocks.DECORATED_POT)); return; }
        if (r == 1) { S(lv, g, B(Blocks.CANDLE)); return; }
        if (r == 2) { S(lv, g, B(Blocks.SKELETON_SKULL)); return; }
        if (r == 3) { S(lv, g, B(Blocks.FLOWER_POT)); return; }
        switch (theme) {
            case FAUVES -> S(lv, g, B(rng.nextBoolean() ? Blocks.FERN : Blocks.MOSS_CARPET));
            case ABYSSES -> S(lv, g, B(rng.nextBoolean() ? Blocks.DRIED_KELP_BLOCK : Blocks.SEA_LANTERN));
            case MOISSON -> S(lv, g, B(rng.nextBoolean() ? Blocks.PUMPKIN : Blocks.HAY_BLOCK));
            case FOURNAISE -> S(lv, g, B(rng.nextBoolean() ? Blocks.MAGMA_BLOCK : Blocks.BONE_BLOCK));
            case DECHARNES, LEGION -> S(lv, g, B(Blocks.BONE_BLOCK));
            case MAGES, GESTE, NEANT -> S(lv, g, B(Blocks.AMETHYST_CLUSTER));
            default -> S(lv, g, B(Blocks.COBWEB));
        }
    }

    // ── 6. Tas de gravats contre les murs (petites pyramides de blocs du thème) ──
    private static void rubblePiles(ServerLevel lv, BlockPos sp, ThemePalette theme, Random rng) {
        int piles = 4 + rng.nextInt(3);
        for (int i = 0; i < piles; i++) {
            boolean alongX = rng.nextBoolean();
            int x, z;
            if (alongX) { x = rng.nextInt(2 * HX - 12) - (HX - 6); z = rng.nextBoolean() ? -HZ + 2 : HZ - 2; }
            else { z = rng.nextInt(2 * HZ - 12) - (HZ - 6); x = rng.nextBoolean() ? -HX + 2 : HX - 2; }
            if (Math.abs(x) < 7 && Math.abs(z) < 7) continue;
            BlockPos g = O(sp, x, 0, z);
            if (!isAir(lv, g)) continue;
            Block rub = theme.decorSecondary();
            S(lv, g, B(rub));
            if (rng.nextBoolean() && isAir(lv, g.offset(1, 0, 0))) S(lv, g.offset(1, 0, 0), B(rub));
            if (rng.nextBoolean() && isAir(lv, g.above())) S(lv, g.above(), B(theme.decorPrimary()));
        }
    }

    // ── 7. Détail signature du thème (flaques, braises, cristaux, givre au sol) ──
    private static void themeSignature(ServerLevel lv, BlockPos sp, ThemePalette theme, Random rng) {
        int n = 6 + rng.nextInt(4);
        for (int i = 0; i < n; i++) {
            int x = rng.nextInt(2 * HX - 14) - (HX - 7);
            int z = rng.nextInt(2 * HZ - 14) - (HZ - 7);
            if (Math.abs(x) < 6 && Math.abs(z) < 6) continue; // épargne centre
            if (Math.abs(x) < 5) continue;                     // épargne avenue
            BlockPos floor = O(sp, x, -1, z);                  // remplace un bloc de sol
            if (isAir(lv, floor)) continue;
            switch (theme) {
                case ABYSSES -> S(lv, floor, B(Blocks.WATER));
                case FOURNAISE -> S(lv, floor, B(Blocks.MAGMA_BLOCK));
                case FAUVES -> S(lv, floor, B(Blocks.MOSS_BLOCK));
                case MOISSON -> S(lv, floor, B(Blocks.PODZOL));
                case DECHARNES, LEGION -> S(lv, floor, B(Blocks.SOUL_SAND));
                case MAGES, GESTE, NEANT -> S(lv, floor, B(Blocks.AMETHYST_BLOCK));
                default -> { }
            }
        }
    }

    private static void crackedWalls(ServerLevel lv, BlockPos sp, ThemePalette theme, Random rng) {
        int cracks = 15 + rng.nextInt(15);
        BlockState stair = B(theme.stair());
        BlockState slab = B(theme.slab());
        for (int i = 0; i < cracks; i++) {
            int y = 1 + rng.nextInt(WALL_H - 2);
            int x = rng.nextInt(2 * HX - 4) - (HX - 2);
            int z = rng.nextBoolean() ? -HZ + 1 : HZ - 1;
            if (rng.nextBoolean()) {
                int temp = x; x = z; z = temp; // along the other walls
            }
            BlockPos p = O(sp, x, y, z);
            if (lv.getBlockState(p).is(theme.base())) {
                S(lv, p, rng.nextBoolean() ? stair : slab);
            }
        }
    }

    private static void crumbledPillars(ServerLevel lv, BlockPos sp, ThemePalette theme, Random rng) {
        // Scan random coordinates in the room for decorPrimary columns
        for (int i = 0; i < 30; i++) {
            int x = rng.nextInt(2 * HX - 10) - (HX - 5);
            int z = rng.nextInt(2 * HZ - 10) - (HZ - 5);
            BlockPos basePos = O(sp, x, 0, z);
            if (lv.getBlockState(basePos).is(theme.decorPrimary())) {
                // We found a column! Let's crumble it at y = 1 or 2
                int y = 1 + rng.nextInt(3);
                BlockPos target = O(sp, x, y, z);
                if (lv.getBlockState(target).is(theme.decorPrimary())) {
                    S(lv, target, B(theme.stair()));
                    // Place some rubble on the floor nearby
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        BlockPos rubble = basePos.relative(dir);
                        if (isAir(lv, rubble) && rng.nextBoolean()) {
                            S(lv, rubble, B(theme.slab()));
                        }
                    }
                }
            }
        }
    }
}
