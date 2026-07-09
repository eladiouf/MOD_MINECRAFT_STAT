package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static net.minecraft.world.level.block.Blocks.*;

/**
 * DungeonMasterpiece — briques partagées du Trial Dungeon.
 *
 * <p>Après la refonte 2026-07-04, cette classe ne conserve que ce qui est réellement utilisé :
 * <ul>
 *   <li>{@link #buildUnderside} — cône rocheux flottant sous chaque île (appelé par
 *       {@link DungeonArchitect#buildFloor}).</li>
 *   <li>{@link #mobPool} — table des mobs de vague par tier (fallback si un thème n'a aucun mob ;
 *       {@link ModdedMobPool} et {@link DungeonMobSpawner}).</li>
 * </ul>
 * L'ancienne île plate circulaire ({@code buildIsland} & co.) a été retirée : la génération passe
 * par {@link DungeonArchitect} + {@link IslandTerrainShaper}.
 */
public final class DungeonMasterpiece {

    static final int R = 80;

    private DungeonMasterpiece() {}

    static BlockState B(Block b) { return b.defaultBlockState(); }
    static void S(ServerLevel lv, BlockPos p, BlockState s) { lv.setBlock(p, s, 3); }
    static BlockPos O(BlockPos p, int x, int y, int z) { return p.offset(x, y, z); }
    /**
     * Sol de l'île : union du disque de rayon {@code R} ET du rectangle de la forteresse (+marge).
     * Garantit que la forteresse (dont les coins dépassent le disque après agrandissement) a
     * toujours du sol dessous — sinon les tours d'angle flotteraient au-dessus du vide.
     */
    static boolean in(int dx, int dz) {
        if (dx * dx + dz * dz <= R * R) return true;
        return Math.abs(dx) <= DungeonArchitect.HX + 2 && Math.abs(dz) <= DungeonArchitect.HZ + 2;
    }

    // ═══════════════ UNDERSIDE CONE ═══════════════
 
    /** Cône rocheux organique sous l'île (profond au centre, fin au bord, avec supports de forteresse). */
    static void buildUnderside(ServerLevel lv, BlockPos sp, BlockPalette t, IslandShaper shaper, int floor) {
        long islandSeed = IslandShaper.seedFor(floor);
        // La boucle doit couvrir l'emprise de la FORTERESSE (HX/HZ+2), pas seulement le cône
        // organique (R) : depuis l'agrandissement (HX=134 > R=80), la branche « support bracket »
        // ci-dessous était inatteignable et le sol de forteresse au-delà de r=80 flottait sur du
        // vide, sans la moindre épaisseur (audit architecture 2026-07-09).
        int ext = Math.max(R, Math.max(DungeonArchitect.HX, DungeonArchitect.HZ) + 2);
        for (int dx = -ext; dx <= ext; dx++) {
            for (int dz = -ext; dz <= ext; dz++) {
                int depth = 0;
                if (shaper.isInside(dx, dz)) {
                    depth = shaper.depthAt(dx, dz);
                } else if (Math.abs(dx) <= DungeonArchitect.HX + 2 && Math.abs(dz) <= DungeonArchitect.HZ + 2) {
                    // Gradual support bracket/pillar for fortress corners overlapping organic bounds
                    int distToEdgeX = DungeonArchitect.HX + 2 - Math.abs(dx);
                    int distToEdgeZ = DungeonArchitect.HZ + 2 - Math.abs(dz);
                    int edgeDist = Math.min(distToEdgeX, distToEdgeZ);
                    depth = Math.max(2, Math.min(6, edgeDist + 1));
                }

                if (depth <= 0) continue;

                long cellSeed = islandSeed + dx * 374761393L + dz * 668265263L;
                java.util.Random rng = new java.util.Random(cellSeed);

                for (int dy = 1; dy <= depth; dy++) {
                    BlockState s = getUndersideBlock(dx, dy, dz, t, rng);
                    S(lv, O(sp, dx, -dy, dz), s);
                }

                // Place hanging vegetation / blocks under the bottom-most rock
                BlockPos belowPos = O(sp, dx, -depth - 1, dz);
                if (lv.getBlockState(belowPos).isAir()) {
                    int hangRoll = rng.nextInt(100);
                    if (hangRoll < 12) { // 12% chance
                        BlockState hangState = B(AIR);
                        if (t.light() == SHROOMLIGHT || t.base() == POLISHED_BLACKSTONE_BRICKS) {
                            hangState = B(WEEPING_VINES);
                        } else if (t.base() == OBSIDIAN) {
                            hangState = B(GLOW_LICHEN);
                        } else {
                            hangState = hangRoll < 6 ? B(GLOW_LICHEN) : B(HANGING_ROOTS);
                        }
                        if (hangState != B(AIR)) {
                            S(lv, belowPos, hangState);
                        }
                    }
                }
            }
        }
    }

    private static BlockState getUndersideBlock(int dx, int dy, int dz, BlockPalette t, java.util.Random rng) {
        Block base = t.underside();
        boolean deepslate = (base == DEEPSLATE || base == POLISHED_DEEPSLATE || base == COBBLED_DEEPSLATE);
        
        int r = rng.nextInt(100);
        if (r < 8) { // 8% chance of ores
            int oreRoll = rng.nextInt(100);
            if (oreRoll < 40) return B(deepslate ? DEEPSLATE_COAL_ORE : COAL_ORE);
            else if (oreRoll < 70) return B(deepslate ? DEEPSLATE_COPPER_ORE : COPPER_ORE);
            else if (oreRoll < 90) return B(deepslate ? DEEPSLATE_IRON_ORE : IRON_ORE);
            else if (oreRoll < 97) return B(deepslate ? DEEPSLATE_LAPIS_ORE : LAPIS_ORE);
            else return B(deepslate ? DEEPSLATE_DIAMOND_ORE : DIAMOND_ORE);
        } else if (r < 18) {
            return B(t.decorPrimary());
        } else if (r < 22) {
            return B(t.decorSecondary());
        }
        return B(base);
    }

    // ═══════════════ MOB TABLE (vagues de combat) ═══════════════

    /**
     * Pool de mobs vanilla organisé par difficulté (progression logique).
     * Combiné aux mobs moddés par {@link ModdedMobPool#getCombinedPool}.
     */
    static List<EntityType<?>> mobPool(FloorPalette t) {
        return switch(t) {
            case EARLY -> List.of(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
                EntityType.HUSK, EntityType.CAVE_SPIDER, EntityType.SILVERFISH,
                EntityType.PILLAGER, EntityType.CREEPER, EntityType.WITCH,
                EntityType.BOGGED, EntityType.SLIME, EntityType.ZOMBIE_VILLAGER
            );
            case MID -> List.of(
                EntityType.STRAY, EntityType.DROWNED, EntityType.CAVE_SPIDER,
                EntityType.VINDICATOR, EntityType.EVOKER, EntityType.RAVAGER,
                EntityType.PIGLIN, EntityType.HOGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.BLAZE,
                EntityType.WITCH, EntityType.CREEPER, EntityType.ENDERMAN,
                EntityType.BREEZE, EntityType.BOGGED, EntityType.SLIME
            );
            case LATE -> List.of(
                EntityType.VEX, EntityType.RAVAGER, EntityType.VINDICATOR, EntityType.EVOKER,
                EntityType.WITHER_SKELETON, EntityType.BLAZE, EntityType.HOGLIN, EntityType.PIGLIN_BRUTE,
                EntityType.ENDERMAN, EntityType.SHULKER, EntityType.PHANTOM,
                EntityType.WITCH, EntityType.CAVE_SPIDER, EntityType.DROWNED,
                EntityType.BREEZE, EntityType.GUARDIAN, EntityType.MAGMA_CUBE
            );
            case ABYSS -> List.of(
                EntityType.ELDER_GUARDIAN, EntityType.WARDEN, EntityType.PIGLIN_BRUTE,
                EntityType.WITHER_SKELETON, EntityType.BLAZE, EntityType.HOGLIN,
                EntityType.ENDERMAN, EntityType.SHULKER, EntityType.PHANTOM,
                EntityType.RAVAGER, EntityType.VEX, EntityType.VINDICATOR,
                EntityType.EVOKER, EntityType.GUARDIAN, EntityType.MAGMA_CUBE
            );
        };
    }
}
