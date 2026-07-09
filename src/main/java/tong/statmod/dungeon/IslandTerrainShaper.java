package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Mission M6 — Relief organique de l'île (refonte 2026-07-04).
 *
 * <p><b>Rôle</b> : habiller le <i>pourtour</i> de l'île (le sol naturel qui entoure la forteresse
 * {@link DungeonArchitect}) avec du relief et de la végétation thématisés par tier. L'intérieur de
 * la forteresse reste <b>plat et propre</b> : {@code DungeonArchitect} y bâtit ses salles.
 *
 * <p><b>Pourquoi seulement le pourtour ?</b> L'ancienne version sculptait toute la silhouette puis
 * la forteresse effaçait l'intérieur (air-clear {@code y≥0}) et le cône underside recouvrait les
 * creux — tout le relief intérieur était perdu, et les crevasses perçaient des trous. On ne sculpte
 * donc que la couronne extérieure, en relief <b>montant uniquement</b> (jamais de trou), atténué
 * près des murs pour ne pas cogner la forteresse.
 *
 * <p><b>Déterministe</b> : même seed → même relief. Aucun état Minecraft partagé.
 */
public final class IslandTerrainShaper {

    /** Amplitude max d'un monticule de pourtour (blocs). Volontairement doux → sol naturel. */
    private static final int RIM_MAX_HEIGHT = 4;
    /** Largeur (blocs) sur laquelle le relief remonte depuis le mur de la forteresse. */
    private static final int TAPER_WIDTH = 6;

    private final long seed;
    private final IslandShaper shaper;

    /** {@code floor} n'influe plus sur la géométrie du pourtour (relief doux uniforme) mais reste
     * dans la signature pour un futur usage (densité de décor par tier, p. ex.). */
    public IslandTerrainShaper(long seed, int floor, int radius) {
        this.seed = seed;
        this.shaper = new IslandShaper(seed, radius);
    }

    /**
     * Construit le sol organique du pourtour de l'île à {@code sp}. Ne touche <b>jamais</b> à
     * l'emprise de la forteresse ({@code |dx|≤HX && |dz|≤HZ}) : {@link DungeonArchitect} y règne.
     * Le relief est ajouté au-dessus de {@code y=0} (le cône underside fournit déjà le sol
     * {@code y=-1}), donc aucune colonne ne peut créer de vide.
     */
    public void buildIslandGround(ServerLevel lv, BlockPos sp, BlockPalette t, FloorPalette tier) {
        int radius = shaper.radius();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!shaper.isInside(dx, dz)) continue;

                int h = rimHeightAt(dx, dz);
                if (h > 0) {
                    // Colonne pleine y=0..h-1 posée sur le sol underside (y=-1). Sommet = surface (thème).
                    for (int y = 0; y < h; y++) {
                        lv.setBlock(sp.offset(dx, y, dz), rimBlock(t, y, h), 3);
                    }
                    // Végétation / cristaux sur les sommets dégagés.
                    if (shouldDecorate(dx, dz)) {
                        addDecoration(lv, sp.offset(dx, h, dz), tier, t, dx, dz);
                    }
                }

                // Try spawning water/lava cascades at the boundary edges
                tryPlaceCascade(lv, sp, dx, dz, h, t);
            }
        }
    }

    private void tryPlaceCascade(ServerLevel lv, BlockPos sp, int dx, int dz, int h, BlockPalette t) {
        if (!shaper.isBoundary(dx, dz)) return;

        Random rng = new Random(seed + dx * 79L + dz * 37L);
        if (rng.nextDouble() > 0.02) return; // 2% chance

        int insideNeighbors = 0;
        int outsideX = 0;
        int outsideZ = 0;
        
        int[] dirs = {1, 0, -1, 0, 1};
        for (int i = 0; i < 4; i++) {
            int nx = dx + dirs[i];
            int nz = dz + dirs[i + 1];
            if (shaper.isInside(nx, nz)) {
                insideNeighbors++;
            } else {
                outsideX = dirs[i];
                outsideZ = dirs[i + 1];
            }
        }

        // Requires a clear cliff edge (at least 2 neighbors inside)
        if (insideNeighbors < 2) return;

        boolean infernal = t.light() == Blocks.SHROOMLIGHT || t.base() == Blocks.POLISHED_BLACKSTONE_BRICKS;
        BlockState fluid = infernal ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();

        int fluidY = Math.max(0, h);
        BlockPos fluidPos = sp.offset(dx, fluidY, dz);
        BlockPos inwardPos = sp.offset(dx - outsideX, fluidY, dz - outsideZ);
        
        // Put a dam block behind the fluid to prevent flooding inward
        if (lv.getBlockState(inwardPos).isAir()) {
            lv.setBlock(inwardPos, t.decorPrimary().defaultBlockState(), 3);
        }
        
        // Place fluid source
        lv.setBlock(fluidPos, fluid, 3);
        
        // Ensure there is solid support underneath the fluid source
        BlockPos belowPos = fluidPos.below();
        if (lv.getBlockState(belowPos).isAir()) {
            lv.setBlock(belowPos, t.decorPrimary().defaultBlockState(), 3);
        }
    }

    private void spawnMicroTree(ServerLevel lv, BlockPos pos, BlockPalette t, Random rng) {
        Block trunk = Blocks.OAK_FENCE;
        Block leaves = Blocks.OAK_LEAVES;
        
        Block baseBlock = t.base();
        if (baseBlock == Blocks.STONE_BRICKS || baseBlock == Blocks.STONE) {
            trunk = Blocks.OAK_FENCE;
            leaves = rng.nextBoolean() ? Blocks.OAK_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES;
        } else if (baseBlock == Blocks.MOSSY_COBBLESTONE) {
            trunk = Blocks.JUNGLE_FENCE;
            leaves = Blocks.JUNGLE_LEAVES;
        } else if (baseBlock == Blocks.CALCITE) {
            trunk = Blocks.CHERRY_FENCE;
            leaves = Blocks.CHERRY_LEAVES;
        } else if (baseBlock == Blocks.POLISHED_BLACKSTONE_BRICKS) {
            trunk = Blocks.NETHER_BRICK_FENCE;
            leaves = Blocks.NETHER_WART_BLOCK;
        } else if (baseBlock == Blocks.OBSIDIAN) {
            trunk = Blocks.WARPED_FENCE;
            leaves = Blocks.WARPED_WART_BLOCK;
        } else if (baseBlock == Blocks.BONE_BLOCK) {
            trunk = Blocks.DARK_OAK_FENCE;
            leaves = Blocks.DARK_OAK_LEAVES;
        } else if (baseBlock == Blocks.PACKED_ICE) {
            trunk = Blocks.SPRUCE_FENCE;
            leaves = Blocks.SPRUCE_LEAVES;
        } else if (baseBlock == Blocks.PRISMARINE_BRICKS) {
            trunk = Blocks.OAK_FENCE;
            leaves = Blocks.BRAIN_CORAL_BLOCK;
        } else if (baseBlock == Blocks.CARVED_PUMPKIN) {
            trunk = Blocks.DARK_OAK_FENCE;
            leaves = Blocks.MANGROVE_LEAVES;
        }

        int trunkHeight = 1 + rng.nextInt(2);
        for (int y = 0; y < trunkHeight; y++) {
            lv.setBlock(pos.above(y), trunk.defaultBlockState(), 3);
        }
        
        BlockPos leafStart = pos.above(trunkHeight);
        for (int lx = -1; lx <= 1; lx++) {
            for (int lz = -1; lz <= 1; lz++) {
                if (Math.abs(lx) == 1 && Math.abs(lz) == 1 && rng.nextBoolean()) continue;
                BlockPos leafPos = leafStart.offset(lx, 0, lz);
                if (lv.getBlockState(leafPos).isAir()) {
                    lv.setBlock(leafPos, leaves.defaultBlockState(), 3);
                }
            }
        }
        if (lv.getBlockState(leafStart.above()).isAir()) {
            lv.setBlock(leafStart.above(), leaves.defaultBlockState(), 3);
        }
    }

    /**
     * Hauteur du monticule de pourtour en (dx,dz) — <b>fonction pure testable</b> (aucun accès
     * monde). Invariants garantis :
     * <ul>
     *   <li><b>0 dans l'emprise de la forteresse</b> ({@code |dx|≤HX && |dz|≤HZ}) → l'intérieur
     *       reste plat, {@link DungeonArchitect} y règne.</li>
     *   <li><b>Toujours ≥ 0</b> → relief montant uniquement, jamais de trou dans le sol.</li>
     *   <li>Atténué sur {@link #TAPER_WIDTH} blocs depuis le mur pour épouser la coque.</li>
     * </ul>
     */
    public int rimHeightAt(int dx, int dz) {
        int fx = DungeonArchitect.HX;
        int fz = DungeonArchitect.HZ;
        // L'intérieur de la forteresse reste plat.
        if (Math.abs(dx) <= fx && Math.abs(dz) <= fz) return 0;

        // Distance de Chebyshev hors de l'emprise → 0 collé au mur, grandit vers le bord.
        int outX = Math.max(0, Math.abs(dx) - fx);
        int outZ = Math.max(0, Math.abs(dz) - fz);
        int out = Math.max(outX, outZ);
        double taper = Math.min(1.0, out / (double) TAPER_WIDTH);

        // Deux harmoniques → collines douces, positif garanti (décalé dans [0,1]).
        double scale = 0.11;
        double n1 = Math.sin(dx * scale + seed * 0.001);
        double n2 = Math.cos(dz * scale - seed * 0.0007);
        double noise = (n1 * n2 + 1.0) / 2.0; // [0,1]

        return (int) Math.round(RIM_MAX_HEIGHT * noise * taper);
    }

    /** Bloc de la colonne de pourtour : cœur en base, sommet en surface d'accent. */
    private BlockState rimBlock(BlockPalette t, int y, int h) {
        if (y == h - 1) return t.accent().defaultBlockState();          // surface foulée
        if (y >= h - 2) return t.decorPrimary().defaultBlockState();    // sous-couche rocheuse
        return t.base().defaultBlockState();                            // masse
    }

    /** ~40 % des sommets reçoivent un décor (bruit indépendant de la hauteur). */
    private boolean shouldDecorate(int dx, int dz) {
        double noise = Math.sin(dx * 0.15 + seed) * Math.cos(dz * 0.15 + seed * 0.7);
        return noise > 0.2;
    }

    /** Décor de surface thématisé par tier (posé sur le sommet du monticule). */
    private void addDecoration(ServerLevel lv, BlockPos pos, FloorPalette palette, BlockPalette t, int dx, int dz) {
        Random rng = new Random(seed + dx * 31L + dz);
        
        if (rng.nextDouble() < 0.15) {
            spawnMicroTree(lv, pos, t, rng);
            return;
        }

        Block baseBlock = t.base();
        if (baseBlock == Blocks.PACKED_ICE) {
            lv.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
            return;
        } else if (baseBlock == Blocks.PRISMARINE_BRICKS) {
            double r = rng.nextDouble();
            if (r < 0.4) lv.setBlock(pos, Blocks.SEA_PICKLE.defaultBlockState(), 3);
            else if (r < 0.8) lv.setBlock(pos, Blocks.SEAGRASS.defaultBlockState(), 3);
            else lv.setBlock(pos, Blocks.SEA_LANTERN.defaultBlockState(), 3);
            return;
        } else if (baseBlock == Blocks.POLISHED_BLACKSTONE_BRICKS) {
            double r = rng.nextDouble();
            if (r < 0.4) lv.setBlock(pos, Blocks.CRIMSON_ROOTS.defaultBlockState(), 3);
            else if (r < 0.7) lv.setBlock(pos, Blocks.WARPED_ROOTS.defaultBlockState(), 3);
            else if (r < 0.9) lv.setBlock(pos, Blocks.WITHER_ROSE.defaultBlockState(), 3);
            else lv.setBlock(pos, Blocks.SOUL_FIRE.defaultBlockState(), 3);
            return;
        }

        switch (palette) {
            case EARLY -> {
                double r = rng.nextDouble();
                if (r < 0.3) lv.setBlock(pos, Blocks.MOSS_CARPET.defaultBlockState(), 3);
                else if (r < 0.7) lv.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 3);
                else if (r < 0.85) lv.setBlock(pos, Blocks.POPPY.defaultBlockState(), 3);
                else lv.setBlock(pos, Blocks.DANDELION.defaultBlockState(), 3);
            }
            case MID -> {
                double r = rng.nextDouble();
                if (r < 0.5) lv.setBlock(pos, Blocks.FERN.defaultBlockState(), 3);
                else if (r < 0.85) lv.setBlock(pos, Blocks.POPPY.defaultBlockState(), 3);
                else lv.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 3);
            }
            case LATE -> {
                double r = rng.nextDouble();
                if (r < 0.4) lv.setBlock(pos, Blocks.RED_MUSHROOM.defaultBlockState(), 3);
                else if (r < 0.8) lv.setBlock(pos, Blocks.BROWN_MUSHROOM.defaultBlockState(), 3);
                else lv.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), 3);
            }
            case ABYSS -> {
                double r = rng.nextDouble();
                if (r < 0.3) lv.setBlock(pos, Blocks.AMETHYST_CLUSTER.defaultBlockState(), 3);
                else if (r < 0.6) lv.setBlock(pos, Blocks.SCULK_VEIN.defaultBlockState(), 3);
                else if (r < 0.8) lv.setBlock(pos, Blocks.SCULK_SENSOR.defaultBlockState(), 3);
                else lv.setBlock(pos, Blocks.END_ROD.defaultBlockState(), 3);
            }
        }
    }
}
